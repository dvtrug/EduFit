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
