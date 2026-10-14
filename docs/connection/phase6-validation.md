# Connection Phase 6 — validation

Branch `feat/connection`. Phase 6 comprises 5 comprehensive verification and cleanup slices:
- **T20**: Complete transition permission and boundary matrices (`test(connection): complete transition permission and boundary matrices`)
- **T21**: Secured REST contracts, privacy and CSRF verification (`fix(connection): enforce secured REST contracts and privacy`)
- **T22**: PostgreSQL migration, atomicity, and concurrency matrix (`T22a`, `T22b`, `T22c`)
- **T23**: ArchUnit non-empty architecture boundaries (`test(connection): enforce nonempty architecture boundaries`)
- **T24**: Finalize module boundaries and consumer compatibility (`refactor(connection): finalize module boundaries and consumers`)
- **Checkpoint 6**: Full verification passed, clean reactor, zero skips, non-empty ArchUnit.

---

## T20 — Complete transition permission and boundary matrices

### 1. Implementation
- **Testing Specification Synchronization (`docs/in-development-testing-specification.md`)**:
  - Corrected Section 7.2 Connection testing specification to match authoritative business rules:
    - Updated `link-invitation-rules.csv`: replaced incorrect 48h TTL with true 14-day (336-hour) TTL (BR-29), tested boundary points at 24h, 335h (valid pending), 336h (exact expiry boundary), and 337h (expired conflict).
    - Replaced non-existent status `REVOKED` on link invitation with actual entity statuses: `PENDING`, `ACCEPTED`, `DECLINED`, `WITHDRAWN`, `EXPIRED`.
    - Standardized error codes to real shared platform error codes (`NONE`, `CONFLICT_DETECTED`, `FORBIDDEN_ACCESS`), removing non-existent enum names (`INVITATION_EXPIRED`, `INVITATION_ALREADY_USED`, `INVITATION_REVOKED`, `FORBIDDEN_ROLE`).
    - Added comprehensive `connection-request-matrix.csv` covering tutor actions (`ACCEPT`, `REJECT`), student/parent actions (`CANCEL`), 7-day (168-hour) TTL boundary, role permissions, and terminal status rejections.
  - Updated Section 9 checklist item for connection testdata.
- **Data-Driven Testing (DDT) CSV Resources**:
  - `backend/modules/connection/src/test/resources/testdata/link-invitation-rules.csv` (9 parameterized scenarios).
  - `backend/modules/connection/src/test/resources/testdata/connection-request-matrix.csv` (12 parameterized scenarios).
- **Parameterized DDT Test Suite (`ConnectionMatrixDdtTest`)**:
  - `verifyLinkInvitationMatrixFromCsv`: reads `link-invitation-rules.csv` with `@ParameterizedTest` and `@CsvFileSource`, verifying exact boundary behaviors at 24h, 335h, 336h, 337h, terminal transitions, and tutor role rejection.
  - `verifyConnectionRequestMatrixFromCsv`: reads `connection-request-matrix.csv` with `@ParameterizedTest` and `@CsvFileSource`, verifying tutor accept/reject, student/parent cancel, tutor cancel rejection (`FORBIDDEN_ACCESS`), 168h TTL expiry boundary, and terminal state conflicts.
  - `accountIdVersusProfileIdDistinctionIsStrictlyEnforced`: verifies that confusing account ID (`studentUser`) with profile ID (`studentId`) is strictly rejected with `ForbiddenOperationException`.
  - `parentWithMultipleStudentsMaintainsIndependentConsentAndAccess`: verifies parent linked to multiple students can create requests independently, revoking link for Student 1 immediately cuts access to Student 1 without affecting Student 2, and Student 1 can still cancel older parent requests.
  - `cooldownBoundaryAtExactSevenDaysNanoseconds`: tests exact 7-day (604,800-second) cooldown to the nanosecond (`minusNanos(1)` active vs exact time satisfied).

### 2. Verification
- **Test Execution**:
  - `ConnectionMatrixDdtTest`: 24 tests passed (0 failures, 0 errors, 0 skips).
  - `connection` module suite: **271 unit tests passed with zero failures, errors, or skips**.
  - All existing scenarios (8 baseline scenarios, 30 controller tests, 38 query tests, 28 invitation transition tests, 24 request transition tests, 22 request rules tests, 18 consent tests, 11 expiry service tests, 6 expiry job tests) verified GREEN.

---

## T21 — Secured REST contracts, privacy and CSRF verification

### 1. Implementation
- **Full Security Filter Chain Test Suite (`ConnectionHttpSecurityPostgresTest`)**:
  - Implemented end-to-end integration test running on full Spring Boot application context with real PostgreSQL testcontainer, `springSessionRepositoryFilter`, and `springSecurityFilterChain`.
  - Avoided any mocked security identities or bypasses; tested with real Argon2 password hashing and database-persisted sessions.
- **Unauthenticated Access Protection (401 Unauthorized)**:
  - Verified across all connection endpoints: legacy unpaged (`/invitations`, `/links`, `/requests`), paged (`/invitations/page`, `/links/page`, `/requests/page`), and admin (`/admin/parent-student/links`).
  - Unauthenticated requests are rejected with HTTP 401 and JSON payload `{"code": "UNAUTHORIZED", ...}`.
- **SPA CSRF Verification**:
  - Tested Spring Security 6 SPA CSRF protection with `CookieCsrfTokenRepository.withHttpOnlyFalse()`:
    - Missing CSRF token on mutating requests (`POST /parent-student/invite`) -> HTTP 403 `FORBIDDEN`.
    - Mismatched/invalid CSRF header `X-XSRF-TOKEN` -> HTTP 403 `FORBIDDEN`.
    - Valid session cookie + matching `X-XSRF-TOKEN` header -> HTTP 200 OK.
- **Role-Based Authorization & Privacy**:
  - `GET /api/v1/connections/admin/parent-student/links`:
    - Student session -> HTTP 403 `FORBIDDEN`.
    - Parent session -> HTTP 403 `FORBIDDEN`.
    - Tutor session -> HTTP 403 `FORBIDDEN`.
    - Admin session -> HTTP 200 OK with minimal link list.
- **Malformed Input & Validation Mappings (400 Bad Request)**:
  - Malformed non-UUID path variables (`/requests/not-a-uuid/cancel`) map to HTTP 400 with `ErrorCode.VALIDATION_FAILED`.
  - Malformed/unreadable JSON request bodies map to HTTP 400 with `ErrorCode.VALIDATION_FAILED`.
  - Missing required boolean `accept` on `/requests/{id}/respond` maps to HTTP 400 with `ErrorCode.VALIDATION_FAILED`.
- **Strict BR-37 Privacy Whitelist**:
  - Verified Tutor Inbox response contains only permitted fields (`requestId`, `goalType`, `senderRole`, `desiredSlots`, `deadline`, `status`).
  - Confirmed absolute absence of student/parent emails, phone numbers, contact info, password hashes, or unrequested goal details (e.g. `budget_max`).

### 2. Verification
- **Test Execution**:
  - `ConnectionHttpSecurityPostgresTest`: 5 integration tests passed (0 failures, 0 errors, 0 skips).
  - Reactor test suite GREEN.

---

## T22a — PostgreSQL Flyway clean/upgrade tests and retained history

### 1. Implementation
- **Schema Upgrade & Retained History Verification (`FlywayMigrationIT`)**:
  - Implemented `connectionUpgradePreservesHistoricalDataAndEnforcesIntegrityConstraints` on real PostgreSQL testcontainer.
  - Setup isolated schema `connection_migration_upgrade` at baseline V1.10 (prior to Connection V1.11 relink and optimistic locking schema).
  - Seeded comprehensive historical data before upgrade:
    - `link_invitation`: 1 `PENDING` invitation and 1 `EXPIRED` invitation across parent and student accounts.
    - `parent_student_link`: 1 `REVOKED` link with detailed audit info (`revoked_at`, `revoked_by`, `revoke_reason`).
    - Verified that at V1.10, the legacy table constraint `uk_parent_student` strictly blocked creating new links between the same parent and student even after revocation (`SQLException` thrown).
    - `connection_request`: 1 `PENDING` request with structured JSONB `desired_slots` array (`[{"dayOfWeek": 2, "startTime": "18:00", "endTime": "20:00"}]`) and 1 `REJECTED` request with `reject_reason`.
  - Executed Flyway upgrade from V1.10 -> V1.14:
    - Successfully applied 4 migrations (`V1.11__allow_relink_after_revocation.sql`, `V1.12__create_ai_match_reservations.sql`, `V1.13__seed_demo_matching_data.sql`, `V1.14__add_structured_audit_metadata.sql`).
    - `upgraded.validate()` succeeded with zero checksum or validation mismatches.
    - Subsequent `upgraded.migrate()` confirmed 0 migrations executed (idempotent no-op).
- **Data Integrity & Schema Contract Verification**:
  - **Retained History**:
    - All historical `link_invitation` rows preserved intact with newly added column `version = 0`.
    - Historical `parent_student_link` with `REVOKED` status and revoke reasons preserved with `version = 0`.
    - Historical `connection_request` preserved with `version = 0`, and complex JSONB `desired_slots` preserved byte-for-byte.
  - **Relink Support (V1.11)**:
    - Successfully inserted a new `CONFIRMED` link between the same parent and student after previous revocation.
    - Verified partial unique index `uq_parent_student_confirmed` blocks a second simultaneous `CONFIRMED` link for the same pair.
    - Verified multiple historical `REVOKED` links are permitted for auditability.
  - **Connection Partial Indexes & Constraints**:
    - `uq_connection_request_pending`: verified rejection of duplicate `PENDING` requests for the same student-tutor pair.
    - `uq_link_invitation_pending`: verified two-way duplicate prevention using `LEAST`/`GREATEST` (student cannot send invite to parent if parent already sent invite to student).
    - `uq_class_student_tutor_active`: verified rejection of duplicate simultaneous `ACTIVE` classes.
    - `chk_invitation_different_users`: verified check constraint prevents self-invitations.
    - `connection_invite_quota`: verified table creation, insert, and query operations.

### 2. Verification
- **Test Execution**:
  - `FlywayMigrationIT`: 5 tests passed (0 failures, 0 errors, 0 skips).
  - `FlywayMigrationScriptTest`: 2 tests passed (0 failures, 0 errors, 0 skips).
  - Reactor test suite GREEN.

---

## T22b — Atomic transition side effects and failure rollback matrix

### 1. Implementation
- **Outer Transaction Rollback Verification**:
  - `invitationAcceptRollsBackCompletelyOnOuterRollback`: verified that when `invitations.respondInvitation` executes within an outer transaction marked for rollback (`status.setRollbackOnly()`), the `link_invitation` strictly remains `PENDING` and no `parent_student_link` rows are persisted.
  - `requestCreationRollsBackCompletelyOnOuterRollback`: verified that when `requests.request` executes within an outer transaction marked for rollback, zero `connection_request` rows are left in the database, preserving pending limits and quotas.
  - Existing `acceptedRequestStillRollsBackWithOuterTransaction`, `expiredInvitationCommitsBeforeHttpConflictAndOuterRollbackIsRespected`, and `expireRequestCommitsStatusAndNotificationAndHonorsOuterRollback` confirm comprehensive outer rollback compliance.
- **Transactional Event Listener Failure Rollback (BEFORE_COMMIT)**:
  - `cancelRequestRollsBackCompletelyWhenNotificationListenerFails`: verified that if notification delivery fails during request cancellation, the transaction rolls back atomically, leaving the request in `PENDING` status.
  - `rejectRequestRollsBackCompletelyWhenNotificationListenerFails`: verified that if notification delivery fails during tutor rejection, the request remains in `PENDING` status, `reject_reason` remains `null`, and zero class rows are created.
  - `studentRevokeRollsBackWhenNotificationFails`: verified that if notification delivery fails during link revocation by a student, the `parent_student_link` remains in `CONFIRMED` status with `revoked_at` remaining `null`.
  - Existing `invitationAndLinkRollbackTogetherWhenNotificationWriteFails` and `acceptCreatesClassAtomicallyAndQueryHydratesInsideTransaction` confirm listener failure rollback for invitation acceptance and tutor acceptance.
- **Audit & Class Constraint Persistence Failure**:
  - `revokeRollsBackOnAuditFailureThenCommitsOnSuccess`: verified that audit failure causes full revocation rollback.
  - `databaseClassFailureRollsBackAcceptedRequestAndMapsAfterTransaction`: verified both immediate `flush` and `deferred` constraint triggers on `class` insert fail atomically, leaving request `PENDING` and mapping to controlled HTTP 409 conflict after transaction rollback.

### 2. Verification
- **Test Execution**:
  - `ConnectionTransactionPostgresTest`: **37 tests passed (0 failures, 0 errors, 0 skips)** on Testcontainers PostgreSQL 17.
  - Reactor test suite GREEN.

---

## T22c — Concurrency tests and race matrix

### 1. Implementation
- **Full Race Matrix Implementation (`ConnectionTransactionPostgresTest`)**:
  - Tested concurrently via multiple threads running in separate transactions with barriers (`CountDownLatch`), bounded timeouts (10s), zero random sleeps, against real PostgreSQL 17.
  - **Double invite đối chiều (Opposite-direction invite race)**:
    - `concurrentDoubleInviteBetweenSamePairCreatesOnlyOnePendingInvitation`: Parent and Student concurrently invite each other. Advisory pair lock and partial unique index `uq_link_invitation_pending` guarantee exactly 1 PENDING invitation exists.
  - **Quota 10/11 đồng thời (Concurrent quota consumption)**:
    - `concurrentQuotaConsumptionExceedingTenRejectsEleventhAttempt`: 2 concurrent threads attempt quota consumption when 9 attempts have been made. Exactly 1 thread consumes attempt 10, the other is rejected, attempts never exceed 10.
  - **Double accept invitation**:
    - `concurrentDoubleAcceptInvitationCreatesOnlyOneConfirmedLink`: 2 concurrent threads call `respondInvitation` with accept=true. Exactly 1 succeeds, second receives `Conflict`, exactly 1 `CONFIRMED` link is created.
  - **Accept vs decline / withdraw invitation**:
    - `concurrentAcceptVsDeclineInvitationResultsInDeterministicTerminalState`: 1 thread accepts, 1 thread declines concurrently. Exactly 1 terminal state wins (`ACCEPTED` or `DECLINED`), zero orphan links.
  - **Pending request thứ 5/6 đồng thời (Pending limit 5 race)**:
    - `concurrentSixthPendingRequestsToDifferentTutorsAllowsOnlyFive`: Student has 4 pending requests, 2 concurrent threads attempt to create requests to Tutor A and Tutor B. Exactly 1 succeeds (5th slot), the other receives `Conflict` ("Đã đạt giới hạn 5 yêu cầu chờ"). Total pending requests in database strictly equals 5.
  - **Accept hai lần chỉ một class (Double accept request)**:
    - `concurrentDoubleAcceptRequestCreatesExactlyOneClass`: 2 concurrent calls to accept the same request. Row lock and class unique constraint ensure exactly 1 class created, request status `ACCEPTED`, second call receives `Conflict`.
  - **Accept vs cancel request**:
    - `concurrentAcceptVsCancelRequestResolvesToSingleTerminalState`: Tutor accept vs Student cancel concurrently. Resolves deterministically to either `ACCEPTED` (classCount=1) or `CANCELLED` (classCount=0). Zero half-state.
  - **Revoke vs Parent create request**:
    - `concurrentRevokeVsParentCreateRequestPreservesDataIntegrity`: Student revokes link while Parent concurrently creates request. Resolves safely without deadlocks, link ends in `REVOKED`, revoked parent denied further access.
  - **Accept vs Expiry race & Concurrent schedulers**:
    - Verified via existing `accessVsExpiryRaceBetweenTutorAcceptAndSystemExpiry` and `concurrentSchedulersDoNotDuplicateExpiryOrNotifications`.

### 2. Verification
- **Test Execution**:
  - `ConnectionTransactionPostgresTest`: **45 tests passed (0 failures, 0 errors, 0 skips)** on Testcontainers PostgreSQL 17.
  - Reactor test suite GREEN.

---

## T23 — ArchUnit nonempty architecture boundaries

### 1. Implementation
- **Boundary Rules for Connection Module (`ArchitectureBoundaryTest`)**:
  - `connection_application_does_not_depend_on_web`: `vn.edufit.connection.application..` strictly forbidden from depending on `vn.edufit.connection.web..` (`allowEmptyShould(false)`).
  - `connection_web_does_not_depend_on_infra`: `vn.edufit.connection.web..` strictly forbidden from depending on `vn.edufit.connection.infra..` (`allowEmptyShould(false)`).
  - `connection_api_does_not_expose_implementation`: `vn.edufit.connection.api..` only depends on `java..`, `vn.edufit.connection.api..`, and `vn.edufit.shared..` (`allowEmptyShould(false)`), strictly preventing any leakage of JPA/Hibernate, infrastructure, or web adapters.
  - `outsiders_use_connection_public_api`: classes outside `vn.edufit.connection..` must not access internal packages (`application..`, `infra..`, `web..`) (`allowEmptyShould(false)`).
  - `connection_uses_only_public_edufit_dependencies`: `vn.edufit.connection..` only depends on its own types, `vn.edufit.shared..`, or public module APIs (`vn.edufit.*.api..`) (`allowEmptyShould(false)`).
  - `production_import_contains_expected_layers`: asserts production imports include core layers across Discovery and Connection (`ConnectionFacade`, `LinkInvitationChangedEvent`, `ConnectionRequestChangedEvent`, `ConnectionFacadeImpl`, `LinkInvitationService`, `ConnectionRequestService`, `ParentStudentLinkService`, `ConnectionExpiryService`, `ConnectionQueryService`, `ConnectionController`, `LinkInvitationRepository`, `ConnectionRequestRepository`, `ParentStudentLinkRepository`, `TutoringClassRepository`, `ConnectionExpiryJob`), and explicitly asserts `connectionClassesCount >= 20` to guarantee rules never pass vacuously on empty classes.
  - Package acyclic enforcement: `ArchitectureTest.root_packages_should_have_no_dependency_cycles` verifies acyclic dependencies among all root packages `vn.edufit.(*)..`.
- **Negative Probe Evidence**:
  - Injected temporary architectural violation into `ConnectionFacadeImpl` (`private vn.edufit.connection.web.request.InviteBody probeViolation;`), introducing an illegal `application -> web` dependency.
  - Executed `mvn test -pl app "-Dtest=ArchitectureBoundaryTest"`.
  - ArchUnit immediately detected the violation and failed the build with exact diagnostics:
    ```
    [ERROR] Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package 'vn.edufit.connection.application..' should depend on classes that reside in a package 'vn.edufit.connection.web..'' was violated (1 times):
    Field <vn.edufit.connection.application.service.ConnectionFacadeImpl.probeViolation> has type <vn.edufit.connection.web.request.InviteBody> in (ConnectionFacadeImpl.java:0)
    ```
  - Reverted probe violation; clean build restored 100% GREEN status.

### 2. Verification
- **Test Execution**:
  - `ArchitectureBoundaryTest`: 11 tests passed (0 failures, 0 errors, 0 skips).
  - `ArchitectureTest`: 3 tests passed (0 failures, 0 errors, 0 skips).
  - Total: 14 architecture tests passed (0 failures, 0 errors, 0 skips).
  - Reactor test suite GREEN.

---

## T24 — Finalize consumer integration and remove legacy paths

### 1. Implementation & Audit
- **Consumer Compatibility & Contract Verification**:
  - **Discovery Integration**:
    - `StudentGoalQueryService` and `TutorMatchingService` call `ConnectionFacade.hasConfirmedParentLink(parentUserId, studentId)`:
      - Verified: Parent with confirmed link successfully accesses student goals and receives personalized tutor matching.
      - Verified: Student revoking the link immediately denies parent access (HTTP 403 Forbidden) on all next requests (`AiDiscoveryConnectionE2ETest.parentCanUseStudentGoalUntilStudentRevokesTheConfirmedLink`).
      - Verified: Student retains full ownership and uninterrupted access to their own goals and matching after parent revocation.
    - `AiDiscoveryConnectionE2ETest`: 7 end-to-end integration scenarios running on real PostgreSQL 17 testcontainer and faked Gemini provider passed 100%.
  - **Active Class Contract for Scheduling**:
    - `ConnectionFacade.findActiveClass(studentId, tutorId)`:
      - Implemented in `ConnectionFacadeImpl` and verified by 9 unit tests (`ConnectionFacadeImplTest`).
      - Returns `Optional<ActiveClassView>` with `classId`, `studentId`, `tutorId`, `goalId`.
      - **Scheduling Handover Audit**: The current Scheduling module baseline in this repository contains a placeholder POM with `MockConnectionValidationAdapter` defaulting to true. Integration of the production `ConnectionValidationAdapter` that queries `ConnectionFacade.findActiveClass` alongside classId/profile-ID mapping is formally cataloged as a Scheduling module handover dependency.
  - **Notification & Audit Consumer Integrity**:
    - Verified `ConnectionTransitionListener` in module `notification` handles all 4 public event types (`LinkInvitationChangedEvent`, `ParentStudentLinkChangedEvent`, `ConnectionRequestChangedEvent`, `TutoringClassCreatedEvent`) in transactional `BEFORE_COMMIT` phase without circular dependencies.
    - Verified `AuditSink` and `NotificationSink` platform adapters are preserved intact and shared across all modules without breaking callers.
- **Legacy Path & Dead Code Scan**:
  - Scanned all 50 Java source files across `vn.edufit.connection..`.
  - Confirmed zero deprecated APIs (`@Deprecated`), zero unused public methods, and zero circular dependencies.
  - Confirmed strict unidirectional dependency graph: `Discovery -> Connection API`, `Notification -> Connection API Event Contract`. Zero reverse dependencies from `Connection` into `Discovery` or `Notification` internals.

### 2. Verification
- **Full Reactor Test Execution**:
  - `mvn test "-Dsurefire.failIfNoSpecifiedTests=false"`: **BUILD SUCCESS** across all 17 reactor modules.
  - Zero test failures, zero test errors, zero skipped tests.
  - Key module test counts:
    - `app`: 278 tests passed.
    - `connection`: 271 tests passed.
    - `discovery`: 114 tests passed.
    - `profile`: 84 tests passed.
    - `verification`: 47 tests passed.
    - `iam`: 40 tests passed.
    - `ai`: 32 tests passed.
    - `notification`: 13 tests passed.
    - `storage`: 15 tests passed.
    - `shared`: 11 tests passed.
    - Architecture rules: 14 tests passed.

---

## Checkpoint 6 — Phase 6 Verification Complete

All verification criteria for Phase 6 have been satisfied:
1. **Unit & Service Transition Matrix (T20)**: Complete parameterized DDT matrix covering all 8 baseline scenarios, 14-day invitation TTL, 7-day request TTL, 7-day cooldown to nanosecond boundary, and parent multi-student isolation (271 tests GREEN).
2. **REST Security & Privacy (T21)**: Tested against real Spring Security filter chain with real Argon2 hashes, session cookies, and SPA CSRF protection; strict BR-37 whitelist verified.
3. **PostgreSQL Migration & Integrity (T22a)**: Flyway clean & upgrade verified on Testcontainers PostgreSQL 17 from V1.10 -> V1.14 with historical data and JSONB preserved.
4. **Atomic Rollback Matrix (T22b)**: Outer transaction rollback, listener failure rollback, audit failure rollback, and class constraint rollback verified without half-states.
5. **Concurrency Race Matrix (T22c)**: Independent transactions with thread barriers verified against PostgreSQL 17 (opposite invite, double accept, accept vs cancel, quota 10/11, pending limit 5, revoke vs create).
6. **ArchUnit Boundary Enforcement (T23)**: Non-empty boundary rules enforced with negative-probe evidence; zero circular package dependencies.
7. **Consumer Integration & Legacy Cleanup (T24)**: Discovery integration verified GREEN; Scheduling contract defined and cataloged; full reactor GREEN.


