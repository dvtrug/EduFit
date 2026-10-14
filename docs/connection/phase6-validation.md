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
