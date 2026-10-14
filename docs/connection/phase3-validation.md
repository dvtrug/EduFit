# Connection Phase 3 — validation

Branch `feat/connection`, base `ec7d11a`. Phase3 has5 parent tasks T11–T15, implemented as10 executable slices T11a/b, T12a/b/c, T13a/b/c, T14, T15. Each slice is committed/pushed after verify.

## Prerequisite: Discovery-AI integration

Discovery-AI content is already integrated into main through `f07d490` and PR19 (`d208371`). At the audit snapshot, origin/feat/discovery-ai=`f47e2ce`. AI, Discovery and migration directory Git trees on main match that branch byte-for-byte. The branch tip is not an ancestor due separate integration history; dry-run merging the old branch again has7 conflicts involving the removed monolith, adapted Profile test and newer Profile/frontend work. No missing AI implementation requires that redundant merge.

Current Connection includes main; E2E/API/architecture audit verify passed73 tests (Connection HTTP17, Discovery HTTP10, App46 including real PostgreSQL DiscoveryRead30, E2E7 and architecture9), no failures/errors/skips. Log `.codex-doc-review/discovery-ai-connection-audit.log`. DiscoveryQueryService differs only formatting, and existing frontend Connection writer/facade contracts remain compatible.

Current Discovery frontend formats desiredSlots as localized display strings; retain legacy-compatible rollout until client/data inventory permits strict-v1. This is the known T13a rollout requirement, not a reason to enable strict mode now.

## T11a — invitation consent/privacy

- Active actor, current recipient role/status after locks and active opposite-role inviter on accept; profile user/student IDs revalidated. Both invitation directions preserve consent: link created only on recipient accept.
- Pending pair query is bounded by the unique pending-pair invariant; latest directional decline query returns only one row. No full-history scan in invite flow. Unknown/inactive/same-role/self/pending/cooldown/confirmed targets retain identical generic success.
- Existing confirmed link closes pending invitation as WITHDRAWN and returns typed conflict; PostgreSQL proves closure commits before HTTP409 and honors outer rollback.
- Scoped invitation history adds reinviteAfter when present, allowing known-history cooldown visibility. Existing fields and pending/no-cooldown JSON shape stay unchanged; names/roles and batch hydrate remain T18c. This additive field fulfills T11a's cooldown acceptance and will be retained in T18c.
- RED:6 consent cases ran,3 failures for inactive recipient, inactive actor and missing system closure (`phase3-T11a-red.log`). GREEN affected reactor verify:182 tests (Connection143, Notification6, App33), no failures/errors/skips (`phase3-T11a-green.log`). Includes18 focused invitation cases,19 PostgreSQL Connection tests and7 main E2E tests.
- No publisher migration or new schema in T11a. Clock, lock order, default rollback and HTTP route shapes retained. Strict coverage remains a separately tracked gate; checkpoint verification follows the remaining slices.

## T11b — invitation transition events

- Invitation create/accept/decline now publish ID-only events. System already-linked closure publishes WITHDRAWN with a null actor; user withdraw/expiry dispatch remain their later task scope. No direct invitation notification writes remain.
- Mandatory BEFORE_COMMIT consumers create exactly one notification per transition; accepted notifications target the confirmed link, and declined notifications carry no student scope.
- Functional RED: three real PostgreSQL entrypoint cases failed before publisher migration (`phase3-T11b-red-functional.log`). An earlier test-context wiring failure was corrected separately; it is not functional RED evidence.
- GREEN:194 tests (Connection143, Notification6, App45), zero failures/errors/skips (`phase3-T11b-green.log`). Real PostgreSQL tests prove invitation create/accept/decline delivery, terminal retry deduplication, and link/invitation rollback when notification insertion fails. Existing business transaction tests use the production listener/writer through a test-only repository bridge; actual notification persistence is covered separately.
- Affected reactor verify succeeded. Coverage warnings remain tracked for the final checkpoint; this focused run does not establish the strict coverage gate.

## T12a — structured audit persistence

- Added immutable AuditRecord with occurrence time and account-ID metadata; old five-argument AuditSink API and callers remain compatible. JpaAuditSink overrides structured input to persist the complete snapshot.
- V1.14 adds JSONB metadata with empty-object default to existing audit rows; actor/reason/time columns are retained. Earlier migration bytes/checksums remain unchanged. Updated all migration count/upgrade assertions in this same slice.
- RED:3 audit tests produced1 failure and1 error before adapter/schema support (`phase3-T12a-red.log`). GREEN affected verify:18 tests, zero failures/errors/skips (`phase3-T12a-green.log`):audit3, migration scripts2, Flyway PostgreSQL4, architecture9.
- PostgreSQL verifies structured account IDs/reason/business time, defensive snapshot copy, legacy API, outer rollback, upgrade from V1.13 preserving historical audit rows and from V1.09 preserving seeded/user-edited data. Flyway validates and rerun is no-op. No local user DB was migrated.

## T12b — revocation authorization and atomic audit

- Student owner or Admin with a nonblank reason may revoke; actor must still have an ACTIVE account with the current role. Current Student metadata is required for both roles so audit cannot silently omit one party.
- Link and structured audit use one Clock instant in the existing required transaction; no business rows are deleted. Snapshot stores parentUserId/studentId/studentUserId, with actor/link/reason/time in existing audit fields.
- RED:8 unit cases,5 failures before changes (`phase3-T12b-red.log`). The first affected verify passed unit cases but lost Docker, producing infrastructure errors and skipped PostgreSQL cases (`phase3-T12b-green.log`); it is not acceptance evidence. Docker is being restarted before rerunning.

- GREEN rerun with Docker restored:112 tests, zero failures/errors/skips (`phase3-T12b-green-rerun.log`):Connection17, App95 including real audit5, Connection PostgreSQL19, Profile PostgreSQL62 and architecture9. Real database trigger failure rolls back the link; successful service revocation persists the structured snapshot while retaining Student data.

## T12c — revocation notifications

- Revoke publishes an ID-only link transition; direct NotificationSink calls are removed. BEFORE_COMMIT writer notifies Parent always and Student when an Admin acted. Private audit reason never enters the event or notification text.
- Parent revocation notification is minimal and has no about_student_id, so it can remain an account-level notice after consent ends. Student recipient retains their own student scope. Notification timestamps use the transition time.
- RED:8 real audit/notification PostgreSQL cases,3 failures before publication (`phase3-T12c-red.log`). GREEN affected verify:131 tests (Connection17, Notification6, App108), no failures/errors/skips (`phase3-T12c-green.log`).
- Real service/database tests verify both recipient roles, terminal retry deduplication and no reason leakage. A trigger rejects the second (Student) notification after Parent insert: link stays CONFIRMED, audit and both notifications are absent after rollback. Real audit failure also rolls back notification/business rows. Profile and invitation regressions remain green.

## T13a — request input and slot rollout

- Component validates message<=500 and nonempty, nonblank slots<=100 in both modes. Strict-v1 adds exact weekday/24h grammar, start<end, duplicate/overlap rejection, with adjacency allowed; only an internal interval copy is sorted. HTTP DTO validates basic boundaries before the service.
- Configuration `edufit.connection.slots.validation-mode` defaults to legacy-compatible; unknown modes fail configuration. Current Vietnamese frontend display strings and legacy point times remain accepted in compatibility mode and unchanged on read. No local data/client inventory supports enabling strict; the contract records this dependency.
- RED:22 parser cases,17 strict grammar/overlap failures before parser support (`phase3-T13a-red.log`). GREEN affected verify:291 tests (Connection178, Notification6, App107), no failures/errors/skips (`phase3-T13a-green.log`). Includes22 parser/mode cases,5 new HTTP input cases, existing legacy PostgreSQL readers, Profile62 and AI/Discovery/Connection E2E7. Strict mode is not enabled globally.

## T13b — create/cancel rules

- Create requires a current ACTIVE Student/Parent actor, ownership/confirmed-link access under the Student lock, matching goal/student account metadata, ACTIVE goal, and VERIFIED Tutor with ACTIVE TUTOR account and matching expertise. Pending quota uses COUNT after expiring/flush, with existing duplicate and active-class guards retained.
- Cancel rechecks ACTIVE actor and current Student/Parent access after locking the fresh request; Student can cancel their older Parent-sent request. Terminal/expired outcomes preserve typed conflict behavior.
- RED:14 rules tests,4 failures for missing actor/tutor account and goal ownership guards (`phase3-T13b-red.log`). GREEN affected verify:121 tests (Connection23, App98), no failures/errors/skips (`phase3-T13b-green.log`). PostgreSQL proves the sixth pending request is blocked and one expired row releases quota; pending count remains5 after replacement. Main AI/Discovery E2E and Profile62 regressions pass.

## T13c — request creation/cancellation events and recipients

- `ConnectionRequestService.request` and `cancelRequest` publish `ConnectionRequestChangedEvent` with full ID metadata (`studentId`, `sentByUserId`, `studentUserId`, `tutorUserId`, `occurredAt`, null `classId`). Direct `NotificationSink` call on request creation is removed to prevent duplicate notifications.
- `ConnectionTransitionListener` consumes request creation (oldStatus=null -> PENDING) and cancellation (PENDING -> CANCELLED) via transactional listener BEFORE_COMMIT (`fallbackExecution=false`).
- `ConnectionNotificationWriter` persists `CONNECTION_REQUESTED` to Tutor, and additionally to Student when Parent was the sender (`!studentUserId.equals(sentByUserId)`). It persists `CONNECTION_CANCELLED` to Tutor, and additionally to Student when Parent performed cancellation (`!studentUserId.equals(actorUserId)`). Older requests created by Parent can be cancelled by Student without notifying Student about their own action.
- RED: 17 tests, 7 failures before event publication and listener/writer wiring (`phase3-T13c-red.log`). GREEN affected verify: 323 tests (Connection 192, App 131), zero failures/errors/skips (`phase3-T13c-green.log`). Real PostgreSQL tests prove Student/Parent recipient delivery matrix, terminal retry deduplication, and atomic rollback of request and notifications when single or second notification insertion fails. Main AI/Discovery E2E and Profile regressions pass.

## T14 — Tutor accept/reject, atomic class and recipients

- `ConnectionRequestService.respondRequest` enforces `requireRole(actor, "TUTOR")`, `requireActiveActor(actor)`, and revalidates that the locked request target tutor matches `tutorId`, tutor profile has `VERIFIED` status, and tutor userId matches `actor.getUserId()`.
- On accept, revalidates: goal is ACTIVE, `goal.studentId().equals(request.getStudentId())`, tutor teaches subject and education level, and no ACTIVE class exists for the student-tutor pair. Creates exactly one ACTIVE `TutoringClass` with assigned UUID, publishes `ConnectionRequestChangedEvent` (newStatus="ACCEPTED", `classId=created.getId()`) and `TutoringClassCreatedEvent` (`newStatus="ACTIVE"`, `classId=created.getId()`, `requestId=id`, `goalId=goalId`). Direct notification sink calls are completely removed.
- On reject, enforces optional `reason <= 300` characters before writing; mutates request status to REJECTED and stores reason; publishes `ConnectionRequestChangedEvent` (newStatus="REJECTED", `classId=null`). When accept is true, reason length is not checked.
- `ConnectionNotificationWriter` consumes `ACCEPTED` and `REJECTED` events via BEFORE_COMMIT listener. Student recipient always receives `CONNECTION_ACCEPTED` (targetType="CLASS", targetId=classId) or `CONNECTION_REJECTED` (targetType="CONNECTION_REQUEST", targetId=requestId). If Parent was the sender (`!studentUserId.equals(sentByUserId)`), Parent only receives the notification if `facade.hasConfirmedParentLink(sentByUserId, studentId)` is true; revoked Parent receives zero notifications. Class-created event produces no duplicate notification.
- RED: 31 tests, 7 failures/errors before implementation (`phase3-T14-red.log`). GREEN affected verify: 344 tests (Connection 200, Notification 8, App 136), zero failures/errors/skips (`phase3-T14-green.log`). Real PostgreSQL Testcontainers tests verify: Student and confirmed Parent recipient delivery, revoked Parent exclusion, retry deduplication on terminal conflict, atomic rollback of request and class when notification fails, and rollback of request and notifications when class insert fails.
