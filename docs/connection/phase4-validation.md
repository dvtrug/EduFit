# Connection Phase 4 — validation

Branch `feat/connection`, base `1b3f497`. Phase 4 comprises two tasks T16 and T17, followed by Checkpoint 4:
- **T16**: Shared bounded expiry service and on-access guards (`feat(connection): centralize bounded invitation and request expiry`)
- **T17**: Scheduler multi-batch/safe retries (`feat(connection): schedule automatic expiry with safe retries`)
- **Checkpoint 4**: Automatic scheduler verification, race guards, and invariant integrity.

---

## T16 — Shared bounded expiry service and on-access integration

### 1. Implementation
- **Centralized `ConnectionExpiryService`**:
  - `findExpiredInvitationCandidateIds(Instant now, int limit)`: queries bounded candidate invitation IDs with stable sort `expiresAt ASC, id ASC`.
  - `findExpiredRequestCandidateIds(Instant now, int limit)`: queries bounded candidate request IDs with stable sort `expiresAt ASC, id ASC`.
  - `expireInvitation(UUID id, Instant now)`: locks pair `(inviter, invited)`, rechecks `PENDING` status and `ConnectionTransitions.isDue(expiresAt, now)`. On match, mutates status to `EXPIRED`, flushes persistence context, resolves student ID, and publishes `LinkInvitationChangedEvent` (cause `SYSTEM_EXPIRY`).
  - `expireRequest(UUID id, Instant now)`: locks student `studentId`, rechecks `PENDING` status and `ConnectionTransitions.isDue(expiresAt, now)`. On match, mutates status to `EXPIRED`, flushes persistence context, and publishes `ConnectionRequestChangedEvent` (newStatus `EXPIRED`).
  - Terminal statuses (`ACCEPTED`, `DECLINED`, `WITHDRAWN`, `CANCELLED`, `REJECTED`, or already `EXPIRED`) return `false` and generate zero events/notifications (no-op).
- **Repositories**:
  - `LinkInvitationRepository.findCandidateExpiredIds`: JPQL query selecting `i.id` where `i.status = 'PENDING'` and `i.expiresAt <= :now` ordered by `i.expiresAt ASC, i.id ASC`.
  - `ConnectionRequestRepository.findCandidateExpiredIds`: JPQL query selecting `r.id` where `r.status = 'PENDING'` and `r.expiresAt <= :now` ordered by `r.expiresAt ASC, r.id ASC`.
- **Notification Consumers (`ConnectionTransitionListener` & `ConnectionNotificationWriter`)**:
  - Consumes `LinkInvitationChangedEvent` with `EXPIRED` status via BEFORE_COMMIT listener (`fallbackExecution=false`), delivering `LINK_EXPIRED` notification to inviter (`inviterUserId`).
  - Consumes `ConnectionRequestChangedEvent` with `EXPIRED` status via BEFORE_COMMIT listener, delivering `CONNECTION_EXPIRED` to Tutor (`tutorUserId`).
  - Request sender recipient matrix:
    - If Student was sender: delivers `CONNECTION_EXPIRED` to Student.
    - If Parent was sender and link is currently `CONFIRMED`: delivers `CONNECTION_EXPIRED` to Parent.
    - If Parent was sender but link was `REVOKED`: delivers `CONNECTION_EXPIRED` to Student; revoked Parent receives zero notifications.
- **On-Access Integration**:
  - `ConnectionRequestService`: `request`, `cancelRequest`, `respondRequest` call `expiryService.expireRequest(id, now)`.
  - `LinkInvitationService`: `invite`, `respondInvitation`, `withdrawInvitation` call `expiryService.expireInvitation(id, now)`.
  - `ConnectionQueryService`: `invitations`, `requests`, and `tutorInbox` invoke `expiryService`.
  - Preserved backward-compatible constructors across all services for existing unit test fixtures.

### 2. Verification
- **Unit & Boundary Tests**:
  - `ConnectionExpiryServiceTest`: 11 test cases passed.
    - Exact clock boundary: `now < deadline` (no expiry), `now == deadline` (expires), `now > deadline` (expires).
    - Terminal state no-op (no mutation, no lock, no event publication).
    - Candidate queries delegate to repositories with bounded pagination and stable ordering.
  - `ConnectionTransitionListenerTest`: 13 test cases passed.
    - Recipient matrix verified for invitation expiry (`LINK_EXPIRED` to inviter).
    - Recipient matrix verified for request expiry (Tutor + Student sender, Tutor + Confirmed Parent sender, Tutor + Student recipient when Parent link revoked).
- **PostgreSQL Integration Tests (`ConnectionTransactionPostgresTest`)**: 26 test cases passed, zero failures/errors/skips.
  - `candidateExpiredQueriesReturnBoundedAndOrderedRows`: verifies bounded limit and `expiresAt ASC` ordering on PostgreSQL.
  - `expireRequestCommitsStatusAndNotificationAndHonorsOuterRollback`: verifies status `EXPIRED` and `CONNECTION_EXPIRED` notification persist on commit, and honor outer transaction rollback.
  - `onAccessRequestCreationExpiresOverdueRowAndReleasesQuota`: verifies overdue request is transitioned to `EXPIRED` on access, releasing quota and allowing 5th pending request to be created.
  - `accessVsExpiryRaceBetweenTutorAcceptAndSystemExpiry`: concurrent execution of tutor acceptance and system expiry on border request resolves cleanly into exactly one terminal status (`ACCEPTED` with 1 class, or `EXPIRED` with 0 classes), with no half-states or constraint violations.
- **Broader Regression Suite**: 112 tests across affected modules (`Connection*Test`, `Invitation*Test`, `LinkRevocationTest`, `LearningGoalPostgresTest`, `ConnectionNotificationTransactionTest`) passed, 0 failures, 0 errors, 0 skips.

---

## T17 — Scheduler expiry multi-batch and safe retries

### 1. Implementation
- **Scheduled Job (`ConnectionExpiryJob`)**:
  - Implemented in `backend/modules/connection/src/main/java/vn/edufit/connection/infra/scheduling/ConnectionExpiryJob.java`.
  - Annotated with `@Component` and `@ConditionalOnProperty(name = "edufit.connection.expiry-job.enabled", havingValue = "true", matchIfMissing = true)`, allowing clean disablement in specific integration fixtures when required.
  - Configurable execution interval (`edufit.connection.expiry-job.interval-ms`, default 60000ms / 60s) and batch size (`edufit.connection.expiry-job.batch-size`, default 100).
  - Multi-batch draining loops: continuously processes candidate batches for invitations and requests until candidates returned are fewer than `batchSize`.
  - Transactional isolation: delegates to `ConnectionExpiryService` through its Spring proxy (`expireInvitation` / `expireRequest`), ensuring each candidate row transition runs in its own short, independent transaction with short-lived locks.
  - Error resilience: individual row processing exceptions are captured, logged without PII (only aggregate ID and error message), and increment failure counters. The scheduler loop continues uninterrupted for remaining items in the batch.
  - Safe retries: uncommitted/failed candidate rows remain `PENDING` and are naturally re-polled and retried in subsequent scheduler cycles.
  - Cycle loop protection: tracks `attemptedInvitations` and `attemptedRequests` within a cycle to prevent infinite looping if a row repeatedly fails in the same cycle.
  - Non-PII summary logging: logs `scanned`, `expired`, `failed` counters and execution duration in milliseconds.

### 2. Verification
- **Unit Tests (`ConnectionExpiryJobTest`)**: 6 test cases passed, 0 failures, 0 errors, 0 skips.
  - `noCandidatesReturnsZeroCounts`: zero candidates produce 0 scanned/expired/failed.
  - `singleBatchAllSuccess`: candidate row transitions and counters verified.
  - `multiBatchDrainingProcessesAllCandidatesAcrossBatches`: draining multiple batches verified with `batchSize = 2` across 3 items.
  - `itemFailureDoesNotCrashJobAndAllowsRemainingItemsToProcess`: single item failure captured; subsequent items successfully proceed.
  - `failedItemIsRetriedInSubsequentCycle`: previously failed item successfully retried in the next scheduled cycle.
  - `terminalCandidatesReturnFalseAndAreCountedAsScannedNotExpired`: already-terminal candidates return false and are recorded as scanned but not expired.
- **PostgreSQL Integration Tests (`ConnectionTransactionPostgresTest`)**: 29 test cases passed, 0 failures, 0 errors, 0 skips.
  - `schedulerExpiryProcessesOverdueRowsAndDeliversNotificationsWithoutUserAccess`: background job execution transitions overdue rows to `EXPIRED` and generates `LINK_EXPIRED` and `CONNECTION_EXPIRED` notifications without any user access.
  - `schedulerMultiBatchProcessesAllCandidatesOnPostgres`: multi-batch draining with `batchSize = 2` successfully transitions all 4 overdue requests on real PostgreSQL.
  - `concurrentSchedulersDoNotDuplicateExpiryOrNotifications`: two concurrent scheduler executions on the same overdue request result in exactly one transition and zero duplicate notifications.
- **Regression Suite**: 124 tests across affected modules (`Connection*Test`, `Invitation*Test`, `LinkRevocationTest`, `LearningGoalPostgresTest`, `ConnectionNotificationTransactionTest`, `Architecture*Test`) passed with 0 failures, 0 errors, 0 skips.

---

## Checkpoint 4 — Scheduler, On-Access Expiry, and Concurrency Guards

- **Automatic Scheduled Expiry**: `ConnectionExpiryJob` runs periodically in the background, reliably draining multiple batches and recovering from transient item failures without thread death.
- **Access Guard Parity**: On-access read/write operations and background scheduled jobs share the identical central `ConnectionExpiryService`, ensuring consistent state transitions and event publishing.
- **Invariant Integrity**: Expired rows properly release pending quota limits (allowing up to 5 active pending requests) and unique pending pair constraints on PostgreSQL.
- **Atomic Notification Guarantee**: BEFORE_COMMIT transactional listeners deliver notifications only upon successful commit; failed transactions roll back notifications atomically.
