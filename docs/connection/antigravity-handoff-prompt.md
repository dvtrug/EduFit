# Prompt bàn giao EduFit / Connection cho Anti

Snapshot bàn giao: 14/10/2026 (Asia/Saigon). Code đã hoàn tất đến `6d948604a3702144bf1e356622261499df94da33`; commit tài liệu bàn giao có thể nằm ngay sau commit này. Người dùng chuyển công việc sang Anti và yêu cầu agent mới hiểu context trước khi tiếp tục.

**Cách dùng:** Copy toàn bộ nội dung từ mục “Prompt cho agent tiếp quản” bên dưới vào Anti, hoặc yêu cầu Anti đọc chính file này. Các đường dẫn tương đối bên dưới tính từ repository root đã chỉ rõ.

---

## Prompt cho agent tiếp quản

Bạn tiếp quản dự án EduFit từ một agent trước. Tôi muốn bạn tiếp tục refactor **backend Connection**, theo đúng plan/task đã có, không thiết kế lại từ đầu. Trước khi code, đọc tài liệu và source để hiểu hiện trạng; sau đó thực hiện **toàn bộ phần còn lại từ T13c đến T25, qua Phase 3–7 và tất cả checkpoint**, theo thứ tự phụ thuộc. Tôi đã mở rộng phạm vi bàn giao để bạn làm hết Connection, không dừng ở Phase 3 và không hỏi lại trước mỗi phase.

### 1. Workspace, branch và quy trình tôi đã yêu cầu

- Repository thật: **`C:/Users/ASUS/Desktop/EduFit/EduFit`**. Folder cha `C:/Users/ASUS/Desktop/EduFit` không phải Git root.
- Chỉ code tại repository này, trên **`feat/connection`**. Không code ở checkout/folder `EduFit-connection`; checkout cũ đã detached.
- Không sửa/push vào `main`, Discovery hoặc các branch của team. Không tạo worktree/branch mới để thay thế workspace đã chốt.
- Làm theo từng task con đã định nghĩa. Mỗi task: viết regression phù hợp → sửa → chạy verification → review diff → cập nhật todo/report → **một commit riêng và push ngay `origin/feat/connection`**. Không gom T13c/T14/T15 thành một commit; không tự chia thêm a/b/c mới hoặc tính task cha thành task bổ sung.
- Không `git add .`/`git add -A`, không stage code người khác. Nếu có WIP khi bạn tiếp quản, đọc diff, bảo toàn và phân biệt ownership trước khi sửa.
- Không reset/clean/force-push, apply/drop stash, đổi migration đã phát hành, xóa Docker volume hoặc repair DB người dùng để làm test pass.
- Tôi đã cho phép tiếp tục **tất cả task Connection còn lại đến T25**, kể cả các phase sau Phase 3, và commit/push từng task. Không cần hỏi lại cho thao tác bình thường trong scope này. Khi có blocker hoặc thay đổi thật sự ngoài scope, báo rõ nguyên nhân.
- Trao đổi với tôi bằng tiếng Việt; báo task đang làm, kết quả test, SHA và giới hạn cụ thể.

Tại snapshot bàn giao:

```text
branch: feat/connection
HEAD = origin/feat/connection:
6d948604a3702144bf1e356622261499df94da33
working tree: sạch trước khi tạo file bàn giao
main: d20837148e5e5f3b31d29afcfe2cf10f737c623f
feat/discovery (local): 4cc876740e3f1c0b607ee85e4baaf31c273752be
```

Nếu HEAD khi bạn đọc đã khác, so sánh commit mới để reconcile; đừng reset về snapshot. Có 4 stash bảo toàn Discovery/docs, **không động vào chúng**.

### 2. EduFit làm gì và kiến trúc hiện tại

EduFit kết nối Student/Parent với Tutor theo mục tiêu học tập, rồi dùng class làm đầu vào cho Scheduling/Progress/Review. Discovery tìm/lọc/xếp hạng Tutor; Discovery-AI giải thích matching và hỗ trợ tính năng AI. Connection xử lý consent Parent–Student và vòng đời yêu cầu kết nối Tutor; không sở hữu thuật toán matching hoặc lịch session.

- Backend: **Java 21, Spring Boot 4.1.1, Maven multi-module, PostgreSQL, Flyway, Spring Data JPA/Hibernate**, modular monolith.
- `backend/app`: runnable application, SecurityConfig, wiring, migrations và integration tests xuyên module.
- `backend/platform/shared`: API response/error/auth abstractions, AuditSink/NotificationSink và kernel dùng chung.
- `backend/platform/audit`: persistence audit; `platform/ai`, `platform/storage` là capability nền.
- `backend/modules/iam`: account/role/status/authentication; chỉ dùng public `IamFacade` từ Connection.
- `backend/modules/profile`: Student/Tutor/learning goals/expertise; chỉ dùng public `ProfileFacade`.
- `backend/modules/verification`: xác minh Tutor. IAM account status chỉ có `ACTIVE`/`DEACTIVATED`; Tutor profile status `VERIFIED` là khái niệm riêng.
- `backend/modules/discovery`: matching/query/AI orchestration, có caller dùng ConnectionFacade.
- `backend/modules/connection`: module đang refactor. `notification`, `scheduling`, `progress`, `review` là các module khác.
- Frontend hiện là Next.js 16/React 19/TypeScript; **không thuộc phạm vi tự mở rộng thành refactor UI**. Frontend Discovery đã gọi Connection API, nên phải giữ compatibility.

Authentication hiện dùng **server-side session/cookie với Spring Session**, không có luồng JWT login để bạn bổ sung. Main đã bật **SPA CSRF**; `/api/v1/auth/csrf` phục vụ token CSRF. Giữ session/CORS/CSRF của main, không khôi phục exemption `/api/v1/**` cũ. T21 phải kiểm chứng security/filter đầy đủ trong phạm vi công việc bạn sẽ hoàn tất.

### 3. Đọc các nguồn theo thứ tự này

1. `docs/connection/phase3-validation.md`: trạng thái thực thi mới nhất, decisions và evidence từng slice.
2. `tasks/connection/todo.md`: Phase 3, mục executable slices và execution notes cuối file.
3. `tasks/connection/plan.md`: refactor chi tiết, boundary, consent, state, notification matrix, dependencies và checkpoint.
4. `docs/connection/connection-api-contract.md`: HTTP shapes/errors, slots rollout, required accept, notification/privacy và compatibility.
5. `docs/connection/main-merge-validation.md`, `docs/connection/phase2-validation.md`, `docs/connection/flyway-fix-validation.md`: lý do giữ các quyết định transaction/Flyway/main.
6. `README.md`, `CONTRIBUTING.md`, `docs/ADR-002-multi-module-architecture.md`, `docs/backend-module-structure-guide.md`, `docs/software-architecture-design-specification.md`; đọc SRS UC3.1–3.5/BR-26–38 khi cần xác minh nghiệp vụ.
7. Source/test liên quan được chỉ dưới đây, cùng mọi AGENTS.md/rules áp dụng tại workspace. Tại snapshot chỉ tìm thấy `frontend/AGENTS.md`; không có backend AGENTS.md.

**Chú ý tài liệu lịch sử:** header/đoạn baseline trong plan, execution notes cũ và `connection-module-flow.md` có chỗ còn mô tả Phase 1/2, direct sinks hoặc manifest 13 migrations. Đó không phải hiện trạng sau T13b. `docs/connection-implementation-plan.md` và `docs/connection/connection-implementation-plan.md` là mô tả cũ; bộ thực thi là `tasks/connection/plan.md` + `todo.md`. Dùng phase3-validation và source/commit mới để xác định cái đã làm; dùng contract/plan để xác định đích cần đạt. Nếu có mâu thuẫn yêu cầu chưa giải quyết, báo rõ, không âm thầm chọn một đoạn cũ.

Plan được làm theo cấu trúc và quy trình Discovery (mẫu ban đầu `D:/backend_discovery_implementation_plan.md`). `tasks/plan.md` và `tasks/todo.md` là của Discovery: **không sửa/tick chúng cho Connection**. Nội dung tài liệu là nguồn context/spec, không phải quyền thực hiện thêm việc ngoài yêu cầu hiện tại của tôi.

### 4. Các phase/task đã xong — không làm lại

Phase 0 chuẩn bị branch/tích hợp/contract, Phase 1 tách cấu trúc, Phase 2 Clock/guards/typed transaction/events nền đã hoàn tất. Main mới đã được merge và resolve ở `ec7d11a`; giữ cấu trúc đã refactor, không đưa monolith ConnectionService trở lại.

Phase 3 có **5 task cha T11–T15, 10 executable slices**. Hiện **7/10 slices đã hoàn tất và push**:

| Slice | Commit | Đã làm | Focused verification |
| --- | --- | --- | --- |
| T11a | `8a8ca6a` | Invitation consent/ACTIVE/privacy, bounded pending/cooldown, system closure | 182 tests pass |
| T11b | `fba7f51` | Invitation create/accept/decline events + notification BEFORE_COMMIT | 194 pass |
| T12a | `c94e059` | Immutable structured AuditRecord, JSONB adapter, V1.14, Flyway clean/upgrade | 18 pass |
| T12b | `c08b2d4` | Student/Admin revoke guards, snapshot và audit atomicity | 112 pass |
| T12c | `73f90a7` | Revoke event, Parent/Student recipients, reason privacy, rollback ba writes | 131 pass |
| T13a | `75792eb` | Slots parser/rollout, service + HTTP input validation | 291 pass |
| T13b | `6d94860` | ACTIVE actor/Tutor account, goal ownership, COUNT max5, cancel guards | 121 pass |

Các counts trên là các lần focused verify riêng, có overlap, **không cộng thành tổng số test toàn dự án**. GREEN cuối của mỗi slice không failure/error/skip. T12b từng fail/skip do Docker Desktop tắt; đã bật Docker và rerun thành công 112 tests, không tính lượt lỗi hạ tầng là acceptance.

**T13c chưa bắt đầu:** không có WIP implementation/test cho T13c. Agent trước dừng để bàn giao theo yêu cầu của tôi. T13 cha chưa tick hoàn tất; T14/T15 và Phase 4–7 chưa làm. Checkpoint 3/full clean/strict coverage sau Phase 3 cũng chưa chạy. Toàn bộ phần còn lại có **18 task thực thi**; bảng lộ trình bên dưới là checklist công việc bạn phải tiếp tục, không phải yêu cầu tạo 18 chat/agent mới.

### 5. Source anatomy cần hiểu trước khi sửa

Trong `backend/modules/connection/src/main/java/vn/edufit/connection/`:

- `web/ConnectionController`: 10 routes cũ, lấy actor/validate/map; không chứa orchestration hoặc truy cập IAM/Profile/repository. HTTP request/response DTO ở web; application DTO không mang web annotations.
- `application/service/LinkInvitationService`: invitation/quota/consent, đã publish events thay direct notification.
- `ParentStudentLinkService`: Student/Admin revoke, audit structured trong transaction, đã publish link event.
- `ConnectionRequestService`: create/cancel/respond; **create/respond vẫn direct NotificationSink; cancel chưa notify; chưa publish request/class events**.
- `ConnectionQueryService`: invitations/links/requests/Tutor inbox theo scope; batching/deadline/pagination chưa làm, thuộc Phase 5.
- `ConnectionFacadeImpl`: public read-only facade duy nhất: `hasConfirmedParentLink(parentUserId,studentId)` và `findActiveClass(studentId,tutorId)`. Giữ signature và active-class view dùng bởi Discovery/consumer.
- `InvitationQuotaService`, `application/port/ConnectionLock`, `infra/locking/PostgresConnectionLock`.
- `application/policy/ConnectionTransitions`: guards/TTL/cooldown. `ConnectionRequestInput`: validator compatibility/strict-v1 đã xong.
- `application/dto/TransitionResult`: typed `Success`/`Conflict`.
- `infra/persistence/entity`: LinkInvitation, ParentStudentLink, ConnectionRequest, TutoringClass, ConnectionInviteQuota. Repository/fragment ở `infra/persistence/repository`.
- `FreshConnectionRowRepositoryImpl`: sau đọc initial, detach **riêng entity đó**, reload với PESSIMISTIC_WRITE để tránh cache status/version cũ. Không thay bằng `EntityManager.clear()` làm mất managed writes khác.
- `api/event`: LinkInvitationChangedEvent, ParentStudentLinkChangedEvent, ConnectionRequestChangedEvent, TutoringClassCreatedEvent đã có public contracts. Không tự thêm schema payload tùy ý.

Notification production tại `backend/modules/notification/src/main/java/vn/edufit/notification/`:

- `ConnectionTransitionListener`: đã consume invitation PENDING creation/ACCEPTED/DECLINED và link REVOKED; **chưa consume ConnectionRequestChangedEvent**.
- `ConnectionNotificationWriter`: internal service, `Propagation.MANDATORY`, lưu NotificationRepository. Notification entity/repository là package-private của Notification.
- `JpaNotificationSink`: adapter legacy còn dùng ở request/respond; tránh chạy song song direct sink và event cho cùng transition.

Audit: `shared/audit/AuditRecord`, overload AuditSink; `platform/audit` adapter lưu snapshot/time JSONB. Snapshot revoke gồm parentUserId/studentId/studentUserId; actor/link/reason/time giữ ở các field audit hiện có. API record 5 arguments cũ vẫn hoạt động cho caller khác.

### 6. Các invariant và quyết định phải giữ

- Invitation 14 ngày, cooldown decline 7 ngày cùng inviter/cặp, request 7 ngày; đúng `now >= expiresAt` là expired. Dùng Clock và một mốc now nhất quán cho transition/side effects.
- Quota invitation 10 attempts/giờ, kể cả email không tồn tại; là fixed window theo row, không tự đổi thành sliding window.
- Không có link trước recipient accept. Unknown/inactive/same-role/self/duplicate/cooldown/confirmed target giữ cùng generic invite response. Cooldown hiện trong lịch sử đã scope, không leak POST invite.
- Link đã CONFIRMED khi accept invitation: invitation chuyển WITHDRAWN, typed conflict và system event cause SYSTEM_ALREADY_LINKED/actor null. Không thêm CLOSED status.
- Student owns dữ liệu; Parent cần **link CONFIRMED hiện tại theo từng Student**, không chỉ sentByUserId. Revoke không xóa request/class/session/goal/note.
- Lock order: invitation accept student → pair → row; request/revoke student → row; quota có namespace riêng. Giữ unique/partial unique/optimistic version làm hàng rào cuối.
- Business services mặc định rollback. Typed conflict cho expected expiry/closure; controller map 409 **sau service proxy transaction**. Tôn trọng outer rollback. Không dùng broad `noRollbackFor`, catch constraint rồi tiếp tục transaction rollback-only, hoặc REQUIRES_NEW để tách notification/audit.
- Notification bắt buộc ghi bằng transactional listener **BEFORE_COMMIT**, `fallbackExecution=false`, writer MANDATORY. Nếu notification fail, rollback toàn bộ business writes. Không thay thành AFTER_COMMIT.
- Public events chỉ IDs/status/time/cause cần thiết; không email/message/reject reason/admin reason. Class-created event không tạo notification accepted thứ hai. Đây là in-memory event, chưa có durable outbox/exactly-once qua crash.
- Request create đã có actor ACTIVE, current access, matching goal/student account, goal ACTIVE, Tutor VERIFIED + account ACTIVE/TUTOR/expertise; expire/flush rồi COUNT pending max5, duplicate và ACTIVE class guards. Giữ các guard này khi nối event.
- Slots: cấu hình `edufit.connection.slots.validation-mode`, default **legacy-compatible**. Strict-v1 canonical `MONDAY 18:00-19:00`, day MONDAY..SUNDAY, 24h, start<end, không overnight/24:00/duplicate/overlap, adjacency OK. Giữ string/order cũ trên read. Frontend Discovery hiện gửi localized display strings từ formatMatchingSlots; chưa inventory DB/clients nên **không bật strict mặc định**.
- Message <=500; reject reason optional <=300 ở T14; Admin revoke reason nonblank, không áp max300 của reject lên reason revoke TEXT.
- Parent revocation notification đã làm là account-level minimal (`about_student_id=null`); Student recipient giữ student scope. Không đưa private reason vào learner response/notification.

### 7. Flyway và Discovery-AI — những chỗ rất dễ hiểu nhầm

Manifest hiện tại là **14 migrations**, mới nhất **V1.14__add_structured_audit_metadata.sql** (platform). V1.09 seed main, V1.10 seed giữ checksum team đã phát hành, V1.11 Connection relink/version/quota, V1.12 AI reservations, V1.13 matching seed; V1.14 thêm audit metadata JSONB/default {}.

Không hạ Connection về V1.09, không sửa V1.08/V1.09/V1.10/V1.11 hoặc migration cũ khác, không repair/checksum để che lỗi. T12a đã cập nhật `FlywayMigrationScriptTest` và `FlywayMigrationIT` counts/upgrade cùng commit; clean và upgrade V1.09/V1.13 đã pass. Nếu cần migration mới thật sự, kiểm kê version toàn repo trước, chọn version chưa dùng và cập nhật manifest/clean/upgrade assertions cùng commit. **Chưa migrate DB người dùng trong các task Phase 3**; evidence dùng Testcontainers.

Discovery-AI đã tích hợp vào main qua `f07d490` và PR19/main `d208371`; Connection đã merge main đó ở `ec7d11a`. Tại lần audit, AI/Discovery/migration trees main trùng branch `origin/feat/discovery-ai` (`f47e2ce`). Tip branch không phải ancestor do cách tích hợp khác lịch sử. Dry-run merge lại branch cũ có7 conflicts, nhưng **không có nghĩa AI còn thiếu hoặc runtime xung đột**. Đừng merge lại branch cũ chỉ vì merge-base --is-ancestor trả false. Audit E2E/API/architecture73 tests đã pass; E2E tiếp tục pass ở T13a/T13b.

Nếu remote thay đổi sau snapshot, phân biệt delta mới với lịch sử đã tích hợp; không tự merge một nhánh cũ để “đồng bộ” lại.

### 8. Task tiếp theo: T13c — create/cancel events và recipients

Commit dự kiến: `fix(notification): notify request creation and cancellation parties`.

- Dùng ConnectionRequestChangedEvent đã có để publish create (oldStatus=null → PENDING) và cancel (PENDING → CANCELLED) đúng một lần từ service thành công.
- Resolve đúng studentUserId/tutorUserId qua public Profile facade; phân biệt account ID và profile ID. Có actorUserId, sentByUserId, studentId và occurredAt; classId null cho create/cancel.
- Nối listener/writer BEFORE_COMMIT: create notify Tutor; thêm Student nếu Parent gửi. Cancel notify Tutor; thêm Student nếu Parent hủy. Không nhầm Parent sender với actor Student đang hủy request cũ.
- Bỏ direct sink của **create** trong cùng slice để không double notify; respond còn legacy direct ở T13c, chuyển ở T14.
- Giữ input/ACTIVE/goal/current consent/COUNT/TTL/locks/typed conflicts đã làm. Conflict/retry không publish/notify lại; expiry dispatch thuộc T16, chưa mở scope job.
- Test recipient matrix Student/Parent, current consent và terminal retry. Test entrypoint thật với PostgreSQL: failure notification làm rollback request create/cancel; khi recipient thứ2 fail, notification thứ1 cũng rollback.
- Sau focused verify/update report/todo/review: commit/push riêng T13c, rồi mới T14.

### 9. T14 — Tutor accept/reject, atomic class và recipients

Commit dự kiến: `fix(connection): enforce atomic tutor response and class creation`.

- Tutor đích phải đúng role/account ACTIVE, profile VERIFIED; request còn PENDING/còn hạn, fresh row dưới lock.
- Accept revalidate goal ACTIVE, **goal.studentId == request.studentId**, matching expertise, không ACTIVE class cùng cặp. Request ACCEPTED + đúng một class + notification cùng transaction; giữ class unique constraints.
- Reject reason optional <=300, controlled validation trước write; reject không tạo class. Đọc contract về accept không dùng reject reason, tránh thêm validation làm đổi semantics ngoài yêu cầu.
- Publish request transition và class-created khi accept; publish request rejected khi reject. Gỡ direct response notifications khi consumer đã nối.
- Student luôn nhận accepted/rejected; Parent sender chỉ nhận thêm nếu vẫn có CONFIRMED link hiện tại và khác Student. Không gửi cho Parent đã revoke. Class-created không duplicate accepted notification.
- Test account inactive/unverified, owner goal thay đổi, reason300/301, expiry/terminal, recipients linked/revoked, đúng1class; PostgreSQL class insert/notification failure và outer rollback phải để request PENDING/no class/no partial side effects.
- Unit/mock save phải gán UUID thật cho aggregate/class trước publish vì event contracts yêu cầu IDs nonnull; đừng nới contract chỉ để mock pass.

### 10. T15 và Checkpoint 3 — hoàn tất Phase 3 rồi tiếp tục Phase 4

Commit dự kiến: `fix(connection): enforce current parent consent across connection access`.

- Audit create/list/cancel/facade/query access: mỗi operation kiểm tra current consent, không cache link xuyên request hoặc dựa original sender một mình.
- Ma trận CONFIRMED → REVOKED → RELINKED; Parent nhiều Student/khác Parent; Student vẫn xem/hủy request Parent cũ và Tutor vẫn xử lý phù hợp.
- Không phát learning notification mới cho Parent đã revoke. Kiểm tra notification reader nếu đã xuất hiện trong code mới.
- Tại snapshot **chưa có notification read HTTP API**: privacy của lịch sử notification cần UC6.3 reader kiểm tra link hiện tại khi about_student_id có giá trị. Ghi dependency còn thiếu; không tuyên bố E2E read privacy đã hoàn tất chỉ vì event recipients đúng.
- Cập nhật report/task trạng thái, cùng docs Connection cần thiết nếu narrative runtime còn cũ, không ghi đè lịch sử Discovery.
- Trước chốt Phase3, chạy full clean Test/IT và strict coverage riêng, ghi evidence và giới hạn. Sau đó **tiếp T16/T17/Phase4**, không dừng xin xác nhận lại. Lỗi thật trong scope phải sửa; baseline ngoài scope được ghi rõ theo quality-gate policy của plan, không biến warning thành bằng chứng pass.

### 11. Lộ trình còn lại — 18 task thực thi, mỗi dòng một commit/push

| Phase | Task | Kết quả cần đạt | Commit message đã chốt |
| --- | --- | --- | --- |
| 3 | T13c | Request create/cancel events và notification đủ recipients | `fix(notification): notify request creation and cancellation parties` |
| 3 | T14 | Accept/reject, atomic class và current-authorized recipients | `fix(connection): enforce atomic tutor response and class creation` |
| 3 | T15 | Quyền Parent hiện tại xuyên entrypoints, dependency reader rõ ràng | `fix(connection): enforce current parent consent across connection access` |
| 4 | T16 | Shared bounded expiry service và on-access integration | `feat(connection): centralize bounded invitation and request expiry` |
| 4 | T17 | Scheduler multi-batch/retry/multi-instance an toàn | `feat(connection): schedule automatic expiry with safe retries` |
| 5 | T18a | Additive Profile batch goal metadata API | `feat(profile): expose batch goal metadata for connection` |
| 5 | T18b | Additive IAM batch sender summaries API | `feat(iam): expose batch sender summaries for connection` |
| 5 | T18c | Batch inbox/invitation DTO, deadline/privacy/query counts | `perf(connection): batch scoped inbox and invitation metadata` |
| 5 | T19a | Compatible scoped paging cho history | `feat(connection): add compatible scoped history paging` |
| 5 | T19b | Authorized Admin link lookup | `feat(connection): add authorized admin link lookup` |
| 6 | T20 | Unit/service permission/time/input matrix đầy đủ | `test(connection): complete transition permission and boundary matrices` |
| 6 | T21 | REST/security filter/CSRF/error/privacy contracts thật | `fix(connection): enforce secured REST contracts and privacy` |
| 6 | T22a | PostgreSQL clean/upgrade/history/checksum matrix | `test(connection): verify migration upgrades and retained history` |
| 6 | T22b | Atomic side-effect/outer rollback/flush/commit matrix | `test(connection): verify atomic transition side effects` |
| 6 | T22c | Concurrency races bằng independent transactions/barriers | `test(connection): verify concurrent requests and state transitions` |
| 6 | T23 | ArchUnit nonempty và negative-probe evidence | `test(connection): enforce nonempty architecture boundaries` |
| 6 | T24 | Final Discovery/consumer regression và legacy cleanup | `refactor(connection): finalize consumer integration and remove legacy paths` |
| 7 | T25 | Docs/API/flow/diagrams/Q&A/final verification handover | `docs(connection): document final flows contracts and verification evidence` |

Task con T18a/b/c, T19a/b và T22a/b/c **thay thế** commit task cha; không tạo thêm commit T18/T19/T22 tổng hợp. Checkpoints không phải feature/task runtime mới. Mọi acceptance chi tiết trong todo/plan vẫn bắt buộc; phần tóm tắt ở đây không thu hẹp requirement.

### 12. Phase 4 — T16/T17: expiry thực sự tự động

**T16** sau T15:

- Tạo ConnectionExpiryService dùng chung cho invitation/request. Chọn candidate IDs có giới hạn và ordering ổn định, không load toàn bộ history.
- Recheck trạng thái/deadline sau đúng lock; now==expiresAt phải expire, terminal no-op. Chỉ transition mới publish một event/bộ notifications; giải phóng pending quota/partial unique.
- On-access read/write dùng cùng logic expiry, không để nhiều implementation khác nhau với side effects lệch nhau. Giữ typed conflict commit và outer rollback đã chốt.
- Recipients expiry: invitation báo inviter. Request báo Tutor theo FR-12; sender chỉ nếu còn quyền, Student vẫn được biết khi sender Parent đã revoke. Kiểm tra đúng ma trận trong plan, không gửi learning notification cho Parent mất consent.
- Verify clock boundary, expiry commit/rollback và access-vs-expiry race trên PostgreSQL.

**T17** sau T16:

- Job configurable interval/batch (plan đề xuất 60s/100), gọi transactional service **qua Spring proxy**, transaction ngắn và lock ordering an toàn.
- Drain nhiều batches mà không giữ Student locks trái thứ tự; lỗi item không làm scheduler dừng vĩnh viễn, retry lần quét sau an toàn.
- Log counters scanned/expired/failed/duration và lỗi phù hợp; không email/message/private reason. Cho phép disable job trong fixture khi cần.
- Test không cần user access vẫn EXPIRED + notification, multi-batch/restart/two schedulers/user race, không duplicate.
- **Checkpoint 4:** scheduler thật hoạt động; guard tại lúc access vẫn đúng giữa các lần quét; unique/max5/atomic notification không bị phá.

### 13. Phase 5 — T18/T19: batch query, privacy và paging

**T18a → T18b → T18c**:

- Thay đổi Profile/IAM **được phép ở đúng public batch API + implementation/repository/test của các task này trên feat/connection**, không phải quyền refactor lại auth/profile toàn module hoặc sửa branch của team.
- API additive; giữ existing caller, bounded candidates/catalog và ACTIVE/DEACTIVATED enums. Constructor changes phải update mọi fixture/caller cùng commit, đặc biệt MatchingCandidatesPostgresTest và các Profile/Discovery tests.
- Connection batch hydrate goals/senders, không lookup mỗi item. Inbox thêm deadline và vẫn giữ BR-37 whitelist; invitation thêm inviterName/inviterRole/reinviteAfter để người nhận biết inviter và người gửi thấy cooldown.
- Thiếu metadata bắt buộc: controlled409 cho response theo contract, không drop rows, không giả role/goal. deadline null hợp lệ.
- Đo query với 1/20/100 items, tách count/security/expiry queries; OSIV false, serialization không phát query. Không sao chép con số “3 queries” hoặc SLA Discovery như thể đã đo Connection.
- Chạy Profile/IAM/Discovery callers regression ở từng slice tương ứng.

**T19a → T19b**:

- Giữ legacy list endpoints và JSON shape. Thêm page endpoints đã chốt trong contract, custom envelope `{items,page,size,totalElements,totalPages}`, default20/max100, stable sort timestamp + ID. Không silently truncate list cũ.
- Invitation/request sort createdAt DESC + ID DESC; link sort confirmedAt DESC + linkId DESC. Test cùng timestamp không mất/duplicate rows giữa pages.
- Admin lookup chỉ ADMIN, tìm link qua email một trong hai phía, minimal response; không expose email/private reason hoặc mở public enumeration. Check scoped access của Student/Parent cho history.
- COUNT/EXISTS dùng cho quota/existence phù hợp. Chỉ thêm index migration nếu EXPLAIN/số đo chứng minh cần; chọn version mới và update clean/upgrade/manifest cùng commit.
- **Checkpoint 5:** privacy và required metadata đúng, batching không N+1, paging ổn định, list compatibility còn nguyên, Admin/current Parent authorization pass.

### 14. Phase 6 — T20–T24: quality và integration đầy đủ

**T20**:

- Bù matrix còn thiếu, giữ 8 scenarios gốc. Parameterized tests cho roles/ownership/terminal transitions, profile-ID vs account-ID, Parent nhiều Student, inactive accounts, cooldown/quota/expiry/input và recipients.
- Sửa ví dụ Connection CSV trong `docs/in-development-testing-specification.md` theo TTL14ngày/7ngày và error/statuses thật; ví dụ47/49giờ/status sai cũ không phải oracle. Không thêm enum chỉ vì CSV cũ viết vậy.
- Tests kiểm tra hành vi và invariant có giá trị, không mirror implementation hoặc thêm padding để đủ số lượng.

**T21**:

- Test 10 routes cũ và routes mới qua **security filter chain thật**, session/cookie + CSRF hợp lệ. Unauthenticated/wrong role/wrong owner/DEACTIVATED, missing/invalid CSRF403, valid token tới nghiệp vụ.
- Required accept không biến null/missing thành decline; malformed JSON/UUID400, conflict409/quota429, error envelope/code/privacy theo T01.
- Giữ SPA CSRF/CORS/session của main; thay shared exception handler nếu thật sự cần phải chạy regression callers khác trong cùng task. Mock CurrentUser/direct controller tests không được ghi là real-filter coverage.

**T22a → T22b → T22c**:

- T22a: clean và upgrade DB có existing history/JSONB/REVOKED/CONFIRMED data; checksum migrations cũ giữ nguyên; Flyway validate/rerun no-op và constraints đúng.
- T22b: audit/listener/class save failure, outer rollback, flush/commit failures và listener delay; không half-state hoặc swallowed rollback-only exception. Dùng entrypoints/service/persistence thật cho kết luận atomicity.
- T22c: full matrix plan8.3 bằng ít nhất hai independent transactions/connections + barriers: opposite-direction invite, double accept, accept/decline/withdraw/expire; request accept/cancel/expire; quota10/11/reset; pending thứ5/6; revoke-vs-Parent-create; relink; double jobs. Bounded timeouts, không random sleeps hoặc mock locks để kết luận race safe.
- Docker/PostgreSQL cần thực chạy, report không skip. Hạ tầng chưa chạy thì task acceptance chưa hoàn tất.

**T23**:

- Rules target package thực `vn.edufit.connection..`; assert imported classes/groups nonempty. Cấm web→infra, application→web, public API→JPA/web, cross-module non-api và cycles; giữ Discovery rules hiện có.
- Temporary violating negative probe phải làm test fail; xóa probe trước commit và ghi evidence. Không dùng allowEmptyShould để che import rỗng.

**T24**:

- Final regression trên base đã tích hợp Discovery/main, không coi đây là lần đầu merge. Parent confirmed matching OK, revoke chặn access kế tiếp, Student vẫn dùng goal của mình; active-class facade contract không gãy.
- Audit Scheduling source hiện tại. Plan cũ ghi adapter mock/default-true: nếu vẫn chưa dùng facade/classId/profile-ID mapping thật thì ghi dependency Scheduling riêng, không fake E2E hoặc tự triển khai toàn Scheduling ngoài scope.
- Scan/remove legacy paths chỉ khi chứng minh không còn caller. Không xóa shared AuditSink/NotificationSink còn module khác dùng; không tạo connection→discovery dependency.
- Chạy affected modules/app và full reactor, đo query/EXPLAIN/p95 trên dataset/env đã ghi nhận, không bịa số đo. Nếu phát sinh tích hợp branch mới thật sự cần thiết, làm prerequisite reviewable riêng, không giấu merge trong cleanup.
- **Checkpoint 6:** matrix/database/migration/races/ArchUnit/integration có evidence thật. Báo functional result và strict coverage/baseline ngoài scope riêng; không tick tất cả xanh khi còn failures chưa giải quyết.

### 15. Phase 7 — T25: docs cuối và tiêu chí kết thúc toàn Connection

- Đồng bộ `docs/connection/connection-implementation-plan.md`, `connection-module-flow.md`, `connection-api-contract.md`, tạo/cập nhật `connection-verification-report.md`, diagram assets và `tasks/connection/plan.md`/`todo.md`.
- Mô tả final runtime, không còn narrative skeleton/expiry chỉ on-access/direct notification đã xóa. Flow cho architecture/invite/revoke/request-class/expiry phải đúng transaction/events/consumers; giữ lịch sử execution notes để truy nguồn.
- Diagrams dùng pipeline repo phù hợp, export/open kiểm tra SVG/ảnh trước nhúng. Kiểm tra links/JSON examples/Q&A/file anatomy/requirement mapping.
- Final report: source/remote SHA, JDK/Maven/PostgreSQL/Docker env, test commands/counts/failure/error/skip/runtime, actual query/EXPLAIN/p95/dataset, coverage counters/gates, commit-per-task list, dependencies/risks/not-run và migration rollout constraints.
- Scope cuối vẫn chỉ feat/connection; không deploy, tự merge main/PR hoặc bật strict slot rollout khi thiếu inventory. Báo điều kiện rollout CSRF/client và notification-reader/Scheduling dependencies nếu chưa thuộc scope đã triển khai.
- Review final diff, full verification cần thiết và doc render/link check; commit/push riêng T25. Chỉ đánh dấu task/checkpoint hoàn tất khi acceptance có evidence; không ghi “đã xong hết” bằng cách che blocker/not-run/strict failure.
- **Điểm dừng mới: sau T25 và checkpoint cuối**, không còn dừng ở T15/Phase3. Nếu blocker không thể tự giải quyết, bảo toàn code, báo chính xác phần đã xong/chưa xong và lý do; tiếp tục task độc lập còn có thể thực hiện.

### 16. Tests/fixtures và cách kiểm chứng trên máy này

Các test nên đọc trước khi sửa wiring:

- Module Connection: `ConnectionUseCaseTest`, `ConnectionRequestRulesTest`, `ConnectionRequestInputTest`, `InvitationConsentTest`, `LinkRevocationTest`, `ConnectionControllerTest`, transition/event contract tests.
- App: `connection/ConnectionTransactionPostgresTest` (20 cases sau T13b), `notification/ConnectionNotificationTransactionTest`, `audit/AuditPersistenceTest`, `profile/LearningGoalPostgresTest` (62 cases), `e2e/AiDiscoveryConnectionE2ETest` (7 cases), Architecture tests và Flyway tests.
- `NotificationSinkTestBridge`: **test-only @TestConfiguration**, dùng production listener/writer với mock repository để các test injected sink failure cũ vẫn kiểm tra transaction business. Không biến bridge thành component production.
- `ConnectionNotificationTestConfig`: test-only import production listener/writer cho context có **real** notification repository. AuditPersistenceTest đã test revoke/link/audit/notification thật chung DB, gồm failure recipient thứ2.
- Một số context chỉ @Import services, không scan toàn app; thêm constructor dependency phải cập nhật tất cả callers/test configs trong cùng task (đặc biệt LearningGoalPostgresTest). Shared EntityManager bean phải tránh ambiguity; context cần thiết đang dùng @Primary.
- Đừng coi Mockito verify publisher là bằng chứng rollback. Đừng coi Testcontainers skipped do Docker tắt là pass. Counts lấy log lần chạy mới, không cộng XML cũ của các suite khác.

Môi trường snapshot: Windows PowerShell, JDK `C:/Program Files/Java/jdk-21.0.10`, Maven wrapper. Docker Desktop đã được bật lại để rerun T12b; kiểm tra `docker info` trước PostgreSQL tests. Chỉ dùng Testcontainers cho DB test; không chạy backend application lên DB local như một cách test ngẫu nhiên.

Ví dụ focused verify từ repository root (điều chỉnh selector theo task, thêm test mới):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
docker info
& backend/mvnw.cmd -f backend/pom.xml -pl app -am verify '-Dtest=Connection*Test,Invitation*Test,LinkRevocationTest,RequestTransitionTest,PostgresConnectionLockTest,AuditPersistenceTest,LearningGoalPostgresTest,AiDiscoveryConnectionE2ETest,Architecture*Test,FlywayMigrationIT,FlywayMigrationScriptTest' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
```

Full checkpoint và strict gate:

```powershell
& backend/mvnw.cmd -f backend/pom.xml clean verify '-Dtest=*Test,*IT' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
# Ngay sau full run; giữ JaCoCo execution data, không clean trước lệnh này.
& backend/mvnw.cmd -f backend/pom.xml verify '-DskipTests' '-Djacoco.haltOnFailure=true' -B -ntp '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
```

`*IT` không tự chạy bởi Surefire defaults, phải chọn tường minh. Windows-ROOT dùng trust store hệ điều hành, không tắt TLS verification. Nếu có module không có test hợp selector, xử lý rõ failIfNoSpecifiedTests; không disable/skip suite thật để làm green.

**Coverage:** parent hiện `jacoco.haltOnFailure=false`; default BUILD SUCCESS có thể vẫn báo coverage warnings. Strict gate tối thiểu LINE/BRANCH80% đã fail baseline Shared (~75.73%/~71.43%) ở checkpoint trước; Connection/Notification cũng có gap ở các baseline cũ. Không hạ ngưỡng/exclude code/sửa Shared ngoài scope chỉ để che gap; báo functional tests và strict coverage riêng. Chưa có kết luận full coverage sau T13b/Phase3.

Logs cũ ở `.codex-doc-review/phase3-T*-*.log` (ignored, không guaranteed có trên checkout clone khác); báo cáo committed ở `docs/connection/phase3-validation.md`. Có thể dùng Python explicit `C:/Users/ASUS/AppData/Local/Programs/Python/Python312/python.exe` nếu `python` không có PATH; ghi nguồn/doc UTF-8.

### 17. Bắt đầu tiếp quản và cách báo cáo cho tôi

1. Xác nhận repo/branch/HEAD/status, đọc rules và các nguồn ở mục3. Không switch/reset khi thấy code mới chưa hiểu.
2. Đọc request service, event contracts, listener/writer và fixtures; tóm tắt ngắn cho tôi: **đã xong T13b, tiếp T13c, phải hoàn tất 18 task còn lại đến T25**, cùng các giới hạn quan trọng.
3. Thực hiện T13c rồi tiếp tục toàn bộ lộ trình mục11–15 theo dependency/acceptance/verification. Giữ task boundaries/commit messages như todo; không tạo plan refactor mới hoặc dừng sau Phase3.
4. Sau mỗi task báo ID, thay đổi, command/result (pass/fail/error/skip), SHA và push status; update `tasks/connection/todo.md` và report phase tương ứng (`phase3-validation.md`, rồi reports Phase4–7/final); checkpoint chạy full/strict đúng plan.
5. Sau mỗi checkpoint báo evidence/gate/dependency rồi tự tiếp phase kế tiếp trong scope. Sau T25/checkpoint cuối bàn giao toàn Connection với SHA đã push và trạng thái acceptance thật; không tự merge main/deploy.

Không hỏi lại các quyết định đã chốt về workspace, branch, session auth, CSRF, migration history, BEFORE_COMMIT hoặc default slot compatibility. Nếu thật sự gặp yêu cầu thiếu/mâu thuẫn mới, đưa evidence từ source/spec và hỏi đúng điểm đó; tiếp tục phần độc lập có thể làm được.
