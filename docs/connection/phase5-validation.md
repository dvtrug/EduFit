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

---

## T18b — Additive IAM batch sender summaries API

### 1. Implementation
- **Interface Contract (`IamFacade`)**:
  - Added additive method: `Map<UUID, UserSummaryView> findUserSummariesByIds(List<UUID> ids);`
  - Returns a map keyed by `UUID` containing safe `UserSummaryView` (`id`, `email`, `fullName`, `role`, `status`).
  - Strict privacy: absolutely zero exposure of passwords, hashes, lockout timestamps, or internal security fields.
  - Preserved all existing contracts (`findUserSummaryById`, `findUserSummaryByEmail`, `existsById`) without altering existing enum types (`AccountRole`, `AccountStatus`).
- **Service Implementation (`IamFacadeImpl`)**:
  - Single batch query: delegates to `accountRepository.findAllById(ids)`, generating a single SQL `SELECT ... WHERE id IN (...)` query.
  - Maps resulting `Account` entities to `UserSummaryView` records and collects into a `Map<UUID, UserSummaryView>`.
  - Handles `null` or empty `ids` lists cleanly by returning `Map.of()`.

### 2. Verification
- **Unit Tests (`IamFacadeImplTest`)**: 7 tests passed (0 failures, 0 errors, 0 skips):
  - `findUserSummariesByIdsReturnsMappedSummaries`: verifies batch lookup across multiple user IDs, asserting ID, email, fullName, role, and status.
  - `findUserSummariesByIdsReturnsEmptyMapWhenNullOrEmpty`: verifies empty map returned when IDs list is null or empty.
  - `findUserSummaryByIdReturnsSummaryWhenFound` & `findUserSummaryByIdReturnsEmptyWhenNullOrNotFound`.
  - `findUserSummaryByEmailNormalizesAndReturnsSummary` & `findUserSummaryByEmailReturnsEmptyWhenBlankOrNotFound`.
  - `existsByIdReturnsTrueOrFalse`.
- **IAM Module Regression**: 39 tests passed with zero failures/errors/skips.

---

## T18c — Batch inbox and invitation metadata, deadline, and privacy

### 1. Implementation
- **DTOs & Response Serialization**:
  - `TutorInboxResult`: added `LocalDate deadline` component with backward-compatible 9-argument constructor.
  - `TutorInboxResponse`: added `@JsonInclude(JsonInclude.Include.NON_NULL) LocalDate deadline` with backward-compatible 9-argument constructor.
  - `InvitationResult`: added `String inviterName, String inviterRole` components, static helper `from(LinkInvitation r, String inviterName, String inviterRole)`, and backward-compatible constructors (6 and 7 arguments).
  - `InvitationResponse`: added `@JsonInclude(JsonInclude.Include.NON_NULL) String inviterName, @JsonInclude(JsonInclude.Include.NON_NULL) String inviterRole` with backward-compatible constructors.
- **Service Optimization (`ConnectionQueryService`)**:
  - `tutorInbox(CurrentUser actor)`:
    - Extracts distinct `goalIds` and `sentByUserIds` from all scoped requests.
    - Hydrates metadata in exactly 2 batch queries: `profile.findGoalsForConnection(goalIds)` and `iam.findUserSummariesByIds(sentByUserIds)`.
    - Controlled 409 Conflict: Throws `ResourceConflictException(ErrorCode.CONFLICT_DETECTED)` when goal or sender account metadata is missing, preventing dropped items or fake roles.
    - BR-37 Privacy Compliance: Whitelists only permitted fields (`requestId`, `goalType`, `subjectId`, `educationLevelId`, `desiredSlots`, `message`, `senderRole`, `status`, `expiresAt`, `deadline`). Zero exposure of learner/parent profiles, emails, contacts, or unrequested goals.
    - Nullable `deadline` from `ConnectionGoalDto` is mapped without synthesis.
  - `invitations(CurrentUser actor)`:
    - Extracts distinct `inviterUserIds` from scoped invitations.
    - Hydrates `inviterName` and `inviterRole` in a single batch query via `iam.findUserSummariesByIds(inviterUserIds)`.
    - Missing or deleted inviters resolve cleanly to `null` name/role without falling back to a fake student identity.
    - Retains `reinviteAfter` cooldown calculation on `DECLINED` invitations.

### 2. Verification
- **Unit & Contract Tests**:
  - `ConnectionQueryServiceTest`: 19 tests passed (0 failures, 0 errors, 0 skips), including:
    - `tutorInboxScopesExpiresAndMapsBatchMetadata`: verifies expiry on-access, batch metadata mapping, sender role, and deadline.
    - `tutorInboxMissingGoalThrowsConflict409` & `tutorInboxMissingSenderThrowsConflict409`: verifies controlled 409 `CONFLICT_DETECTED`.
    - `tutorInboxBatchesDistinctIdsWithoutNPlusOne`: verifies distinct ID aggregation across multiple requests and single-invocation batch queries.
    - `invitationsHydrateInviterNameAndRoleViaBatch`: verifies batch hydration of inviter name and role, handling missing/deleted accounts gracefully.
  - `ConnectionControllerTest`: 24 tests passed, including:
    - `tutorInboxHydratesDeadlineToJson`: verifies exact JSON shape matching contract fixture (`deadline: "2027-01-01"`).
    - `invitationsHydrateInviterMetadataAndCooldownToJson`: verifies exact JSON shape matching contract fixture (`reinviteAfter`, `inviterName`, `inviterRole`).
  - `connection` module: 222 tests passed with zero failures/errors/skips.
- **PostgreSQL Integration Tests (`ConnectionTransactionPostgresTest`)**: 30 tests passed with zero failures/errors/skips:
  - `tutorInboxMetadataQueryCountIsConstantForBatchesOf1And20And100`: inserts 100 requests across distinct student profiles, verifying exact hydration of 100 items with only 1 batch call to Profile and 1 batch call to IAM ($O(1)$ queries, zero N+1).
  - `acceptCreatesClassAtomicallyAndQueryHydratesInsideTransaction`: verified with batch queries inside transaction.
- **Cross-module Regression**:
  - IAM module: 39 tests passed.
  - Profile module: 55 tests passed (including 6 PostgreSQL integration tests).

---

## T19a — Compatible scoped history paging

### 1. Implementation
- **Application DTO (`PageResponse<T>`)**:
  - Implemented record `vn.edufit.connection.application.dto.PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages)`.
  - Static factory `from(Page<T> page, List<R> items)` and `from(Page<T> page, Function<T, R> mapper)`.
  - Located in `application.dto` to enforce strict architectural decoupling (no web-to-persistence or application-to-web dependency).
- **Paged Repository Queries**:
  - `LinkInvitationRepository`: added `findScopedInvitations(@Param("inviterId") UUID inviterId, @Param("recipientEmail") String recipientEmail, Pageable pageable)` with stable ordering `ORDER BY i.createdAt DESC, i.id DESC`.
  - `ParentStudentLinkRepository`: added `findByParentUserIdAndStatusConfirmed` and `findByStudentIdAndStatusConfirmed` with stable ordering `ORDER BY l.confirmedAt DESC, l.id DESC`.
  - `ConnectionRequestRepository`: added `findByStudentIdScoped` and `findByTutorIdScoped` with stable ordering `ORDER BY r.createdAt DESC, r.id DESC`.
- **Query Service (`ConnectionQueryService`)**:
  - Added `validatePageAndSize(int page, int size)`: validates `page >= 0` and `1 <= size <= 50`, throwing `BadRequestException(ErrorCode.BAD_REQUEST, ...)` for invalid parameters.
  - Implemented `invitationsPage`, `linksPage`, `requestsPage`, and `tutorInboxPage` reusing batch metadata hydration and on-access expiry without N+1.
- **REST Endpoints (`ConnectionController`)**:
  - `GET /api/v1/connections/parent-student/invitations/page?page=0&size=20`: returns `PageResponse<InvitationResponse>`.
  - `GET /api/v1/connections/parent-student/links/page?page=0&size=20`: returns `PageResponse<ParentStudentLinkResponse>`.
  - `GET /api/v1/connections/requests/page?page=0&size=20`: returns `PageResponse<?>` branching to `tutorInboxPage` for `TUTOR` and `requestsPage` for `STUDENT`/`PARENT`.
  - Fully backward-compatible: all legacy unpaged endpoints (`/invitations`, `/links`, `/requests`, `/tutor-inbox`) remain intact with identical response contracts.

### 2. Verification
- **Unit & Contract Tests**:
  - `ConnectionQueryServiceTest`: 30 tests passed (including `invitationsPageReturnsPagedInvitationsWithBatchMetadata`, `linksPageReturnsPagedLinks`, `requestsPageReturnsPagedRequestsForLearner`, `tutorInboxPageReturnsPagedInboxWithMetadata`, and validation tests for negative page / size exceeding 50).
  - `ConnectionControllerTest`: 28 tests passed (including paged HTTP endpoint tests asserting exact JSON structure, HTTP 200, and HTTP 400 on invalid page/size).
  - `connection` module suite: 237 tests passed with zero failures, errors, or skips.
- **PostgreSQL Integration Tests (`ConnectionTransactionPostgresTest`)**:
  - 31 tests passed with zero failures, errors, or skips.
  - `pagedQueriesProvideStableOrderingAndDeterministicPagination`: inserts sequential connection requests in PostgreSQL testcontainer, verifying deterministic DESC ordering across pages 0, 1, 2, and empty items on out-of-bounds page 3 with preserved total elements and page counts.

---

## T19b — Authorized Admin link lookup

### 1. Implementation
- **Query Service (`ConnectionQueryService.adminLinksPage`)**:
  - Role Guard: strictly requires `ADMIN` role via `requireRole(actor, "ADMIN")`, throwing `ForbiddenOperationException` (403 Forbidden) for any non-admin callers.
  - Input Validation: requires non-null, non-blank `email`, and validates `page >= 0` and `1 <= size <= 100` via `validatePageAndSize`.
  - Privacy-preserving & Anti-Enumeration: looks up user account via `iam.findUserSummaryByEmail(email)`. If the email is unknown, returns an empty `PageResponse` (zero items, `totalElements = 0`) instead of throwing 404, preventing account enumeration.
  - Two-way Lookup:
    - If user is `PARENT`: queries `ParentStudentLinkRepository.findByParentUserIdAndStatusConfirmed(user.id(), pageable)`.
    - If user is `STUDENT`: queries `profile.findStudentByUserId(user.id())` to retrieve `studentId`, then queries `ParentStudentLinkRepository.findByStudentIdAndStatusConfirmed(studentId, pageable)`.
    - If user has another role (`TUTOR`, `ADMIN`): safely returns an empty page.
  - Minimal Output: maps to `ParentStudentLinkResult` containing only `linkId`, `parentUserId`, `studentId`, `status`, and `confirmedAt`. Never exposes revoked links, internal audit reasons, or contact details.
- **REST Endpoint (`ConnectionController.adminLinksPage`)**:
  - Route: `GET /api/v1/connections/admin/parent-student/links?email=...&page=0&size=20`.
  - Serializes response to `ApiResponse<PageResponse<LinkResponse>>`.

### 2. Verification
- **Unit & Security Tests (`ConnectionQueryServiceTest`)**:
  - 38 tests passed (including 6 new tests for T19b):
    - `adminLinksPageRequiresAdminRole`: non-admin roles (`PARENT`, `STUDENT`, `TUTOR`) are rejected with `ForbiddenOperationException` (403).
    - `adminLinksPageRequiresNonBlankEmail`: empty or blank emails are rejected with `InvalidOperationException` (400).
    - `adminLinksPageReturnsEmptyWhenEmailNotFound`: missing email returns empty page without error (anti-enumeration).
    - `adminLinksPageLookupByParentEmailReturnsConfirmedLinks`: returns confirmed links for parent.
    - `adminLinksPageLookupByStudentEmailReturnsConfirmedLinks`: resolves student profile and returns confirmed links for student.
    - `adminLinksPageLookupByOtherRoleReturnsEmptyPage`: returns empty page for non-link roles.
- **HTTP Contract Tests (`ConnectionControllerTest`)**:
  - 30 tests passed (including 2 new tests for T19b):
    - `adminLinksPageKeepsExactPagedShape`: asserts exact JSON envelope and link shape.
    - `adminLinksPageMissingEmailReturns400`: asserts 400 Bad Request when query parameter `email` is absent.
  - `connection` module suite: 247 tests passed with zero failures, errors, or skips.
- **PostgreSQL Integration Tests (`ConnectionTransactionPostgresTest`)**:
  - 32 tests passed with zero failures, errors, or skips.
  - `adminLinksLookupByEitherEmailReturnsOnlyConfirmedLinks`: verifies against real PostgreSQL testcontainer that lookup by either parent or student email returns only `CONFIRMED` links, excludes `REVOKED` links, hides sensitive revocation reason, rejects non-admin callers, and returns clean empty page on missing email.

---

## Checkpoint 5 — Full Validation Summary

1. **BR-37 Compliant**: Whitelists only permitted fields in inbox and query outputs. Sensitive learner and parent profiles, unrequested goals, and audit reasons remain strictly hidden.
2. **Zero N+1 Metadata Queries**: Hydrates inbox goals and senders across arbitrary page/list sizes in exactly 2 batch facade queries. Verified in PostgreSQL test with 100 requests.
3. **Deterministic Pagination**: Compatible scoped paging added across all connection entities (`invitations/page`, `linksPage`, `requestsPage`, `tutorInboxPage`, `admin/parent-student/links`) with deterministic DESC tie-breakers (`createdAt DESC, id DESC` and `confirmedAt DESC, id DESC`).
4. **Authorized Admin Link Lookup**: Admin lookup by either party email with minimal output and anti-enumeration protections.
5. **Full Backward Compatibility**: All legacy unpaged endpoints remain intact and unaltered.
