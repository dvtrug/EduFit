# Connection Phase 5 — validation

Branch `feat/connection`, base `1ea58da`. Phase 5 comprises 5 executable slices:
- **T18a**: Additive Profile batch goal metadata API (`feat(profile): expose batch goal metadata for connection`)
- **T18b**: Additive IAM batch sender summaries API (`feat(iam): expose batch sender summaries for connection`)
- **T18c**: Batch inbox/invitation DTO, deadline/privacy/query counts (`perf(connection): batch scoped inbox and invitation metadata`)
- **T19a**: Compatible scoped paging for history (`feat(connection): add compatible scoped history paging`)
- **T19b**: Authorized Admin link lookup (`feat(connection): add authorized admin link lookup`)
- **Checkpoint 5**: BR-37 compliant, no N+1 metadata queries, stable pagination, and authorized Admin lookup.

---

## T18a — Additive Profile batch goal metadata API

### 1. Implementation
- **Interface Contract (`ProfileFacade`)**:
  - Added additive method: `Map<UUID, ConnectionGoalDto> findGoalsForConnection(List<UUID> goalIds);`
  - Returns a map keyed by `goalId` containing `ConnectionGoalDto` (`goalId`, `studentId`, `studentUserId`, `subjectId`, `educationLevelId`, `goalType`, `deadline`, `status`).
  - Preserved all existing contracts (`findGoalForConnection`, `findVerifiedCandidatesBySubject`, `searchTutors`, etc.) without breaking callers.
- **Service Implementation (`ProfileFacadeImpl`)**:
  - Reused existing injected repositories (`learningGoalRepository` and `studentProfileRepository`) without modifying the `ProfileFacadeImpl` constructor.
  - Efficient 2-query batch fetch:
    1. `learningGoalRepository.findAllById(goalIds)` batches goal lookups in a single `SELECT ... WHERE goal_id IN (...)`.
    2. Collects unique student IDs and queries `studentProfileRepository.findAllById(studentIds)` in a single `SELECT ... WHERE student_id IN (...)`.
  - Maps goals and student metadata into a `Map<UUID, ConnectionGoalDto>`.
  - Handles `null` or empty `goalIds` gracefully by returning `Map.of()`.

### 2. Verification
- **Unit Tests (`ProfileFacadeImplTest`)**: 24 tests passed (including 2 new tests):
  - `shouldBatchFindGoalsForConnection`: verifies batch lookup for multiple goals, verifying `studentUserId`, `educationLevelId`, `goalType`, `deadline`, and `status`.
  - `shouldReturnEmptyMapWhenGoalIdsEmptyOrNull`: verifies empty map returned when input list is empty or null.
- **Profile Module Regression**: 55 tests passed, including `MatchingCandidatesPostgresTest` (6 PostgreSQL tests) with zero failures/errors/skips.
- **Discovery Module Regression**: 114 tests passed with zero failures/errors/skips, verifying consumer caller compatibility.
