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
