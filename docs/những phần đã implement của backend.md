# Những phần đã implement của backend

Ngày đối chiếu: **11/10/2026 (Asia/Saigon)**. Phạm vi: backend EduFit, tài liệu nghiệp vụ/kiến trúc và **toàn bộ 12 branch trên remote origin**. Không kiểm tra frontend. Đây là báo cáo hiện trạng và truy vết quyết định, không tự phê duyệt thêm yêu cầu.

## 1. Kết luận để chốt hiện trạng

- **Đã vào main:** nền tảng Maven modular monolith, IAM, hồ sơ Tutor/Student cơ bản, catalog, nộp/duyệt xác minh, lưu file local, AI gateway và toàn bộ schema nghiệp vụ cơ sở.
- **Có code trên branch nhưng chưa vào main:** Discovery trên `feat/discovery` (cũng có trong `feat/connection`); Connection trên `feat/connection`; Scheduling trên `feature/backend-schedule`.
- **Chưa hoàn thành:** learning goals chưa có HTTP CRUD; AI bio chưa có luồng Profile gọi gateway; Verification chưa ghi audit/thông báo; Scheduling chưa nối Connection/Progress thật; Notification mới có sink ghi dữ liệu; Progress và Review chưa có implementation nghiệp vụ trên bất kỳ branch remote nào được đọc.
- **Auth thực tế là Argon2id + session cookie, chưa có JWT implementation.** JWT + refresh token chỉ là track nghiên cứu trong ADR-001.
- **Có schema không đồng nghĩa có tính năng chạy được.** Main có 31 bảng ứng dụng trong migrations; Connection thêm bảng thứ 32. Không có một branch nào hiện chứa đồng thời đầy đủ Connection/Discovery và implementation Scheduling.

“Đã có code” bên dưới nghĩa là đọc thấy controller/service/repository/adapter tương ứng. Không đồng nghĩa đã triển khai production hoặc đã nghiệm thu toàn bộ SRS. Lần rà soát này không chạy app, test, migration, không đọc dữ liệu DB sống; các số test được dẫn là kết quả đã ghi trong tài liệu trước đó.

## 2. Các tài liệu nói gì và cần đọc đúng chỗ nào

Các đường dẫn tương đối mở bản ở checkout hiện tại; để kiểm tra một branch khác dùng các liên kết commit cố định ở phụ lục. Với SRS DOCX, tìm bằng mã UC/tên heading; số P là thứ tự paragraph trong `word/document.xml`, tính từ 1, bao gồm paragraph trong bảng, **không phải số trang Word**.

| Tài liệu | Chỗ cần đọc | Nội dung dùng để chốt / giới hạn |
| --- | --- | --- |
| [SRS Edufit.docx](SRS%20Edufit.docx) | Bảng Group 1a–6, P211–509; đặc tả UC bắt đầu P966 | Danh sách use case, actor, tiền/hậu điều kiện, BR/FR/NFR. Có mâu thuẫn giữa bảng tổng quan và phần chi tiết, xem mục 9. |
| [ADR-001](ADR-001-tech-stack.md) | Decision 1 (dòng 42), 2 (71), 3 (97), 5 (150), 6 (205), 7 (236) | Spring Boot, PostgreSQL, JPA; session là primary, JWT là secondary; local storage trước, Supabase là đích triển khai. |
| [ADR-002](ADR-002-multi-module-architecture.md) | Decision 1 (24), 2 (153), 3a (199), 3b (239), 3c (336) | 9 module nghiệp vụ, 4 platform; public facade/event; domain thuần cho scheduling/progress/discovery; migration tập trung; ownership platform. |
| [SADS](software-architecture-design-specification.md) | §4.1; §4.2.A dòng 682; §4.3 dòng 799; §4.4 dòng 932 | Schema, trách nhiệm module, API thiết kế, luồng xử lý. Bảng API thiết kế phải đối chiếu controller, không dùng làm danh sách API đã implement. |
| [Backend structure guide](backend-module-structure-guide.md) | §1.1, §1.2 và phần quy tắc giao tiếp | Vai trò `api/web/application/domain/infra`, module CRUD và module có domain phức tạp. |
| [Authentication contract](authentication-contract.md) | §1.2 dòng 40, §1.3 dòng 54, §2 dòng 60, §3 dòng 260 | Cookie, CSRF, 7 API auth nghiệp vụ và API lấy CSRF theo thiết kế; runtime còn khác contract. |
| [IAM test specification](iam-test-cases-specification.md), [IAM RBL report](reports/iam-authentication-rbl-report.md) | Các nhóm register/login/password và kết quả kiểm chứng | Bằng chứng test/lịch sử IAM; không thay code hiện tại khi mô tả session revocation. |
| [Discovery API contract](discovery-api-contract.md) | “Hai loại API”, “Search”, “Matching” | Contract hiện thực của 3 routes, filter, quyền, input/output, lỗi. |
| [Discovery flow](discovery-module-flow.md) | “Phạm vi module” dòng 13; “Luồng matching” dòng 71; “Đã có và giới hạn còn lại” dòng 128 | Công thức 5 yếu tố; candidate cap; quota, fallback, transaction/event và các thiếu sót đã thừa nhận. |
| [Discovery performance](discovery-performance-report.md) | Query count, local p95, candidate-cap recall | Bằng chứng PostgreSQL cục bộ; không phải SLA production. |
| [Connection contract](connection/connection-api-contract.md) | §2 dòng 22; §4 dòng 55; §5 dòng 68; §6 dòng 78; §7 dòng 88 | 10 routes hiện có; semantics/input đã chốt cho refactor; route paging/admin và CSRF là đích task sau. |
| [Connection flow](connection/connection-module-flow.md) | “Liên kết Parent - Student”, “Yêu cầu gia sư”, “Đồng thời và dữ liệu” | Luồng và cấu trúc service sau Phase 1. |
| [Connection Phase 1](connection/phase1-validation.md) | “Review / giới hạn còn lại”, “Checkpoint 1 — kết quả cuối” | T02–T07 hoàn tất cấu trúc; 348 tests ghi nhận pass nhưng strict coverage FAIL. |
| [Connection checklist](../tasks/connection/todo.md) | Phase 0–1 dòng 41–148; Phase 2–7 dòng 150–352 | T00–T07 đã đánh dấu x; T08–T25 chưa x. Contract đích không được xem là đã triển khai. |
| [Connection Flyway fix](connection/flyway-fix-validation.md) | “Manifest hiện hành” dòng 11; “Phạm vi tương thích” dòng 36 | V1.09 seed main, V1.10 seed Connection, V1.11 relink/quota; lineage V1.09 Connection cũ cần xử lý riêng. |
| Scheduling implementation plan | `docs/scheduling/scheduling_implementation_plan.md` trên branch Scheduling; §1, §7, §8 | File tự ghi **Under Review**. Dùng để hiểu đề xuất, không coi mọi đoạn mẫu/tất cả yêu cầu đã được phê duyệt. Link cố định ở phụ lục nguồn. |
| [Master sprint plan](master_sprint_plan_and_ai_learning_framework.md) | Sprint 1 dòng 324; 2 dòng 365; 3 dòng 406; 4 dòng 450; 5 dòng 493 | Roadmap/phân công và kiến thức dự kiến áp dụng, không là chứng nhận hoàn thành. |
| [Project overview](PROJECT_OVERVIEW.md) | §3.1; §4; §5 | Bối cảnh/module map hữu ích, nhưng §4 còn ghi 9 module rỗng và V1 rỗng: **hiện đã lỗi thời**. |
| [DBML](edufit-schema.dbml), [schema changes](edufit-schema-review-changes.md) | Bảng và các thay đổi schema | Mô hình thiết kế. Hiện trạng schema triển khai lấy từ Flyway ở mục 6, không suy từ DBML. |

## 3. Trạng thái từng module và platform

| Thành phần | Phạm vi đã xác định trong SRS/ADR | Code hiện có | Chưa làm hết / cần chốt thêm |
| --- | --- | --- | --- |
| `iam` | UC1.1–1.4: tài khoản, login/logout, quên/reset/đổi mật khẩu | Main: register ACTIVE, chuẩn hóa email, password policy, Argon2id, login session, counter khóa, token reset/email, `/me`, facade user summary | Change/reset password chưa thấy hủy các session khác; forgot-password chưa thấy rate limit; không có route `/auth/csrf`; chưa có Zalo OAuth/JWT. Email verification là khác biệt yêu cầu, xem mục 9. |
| `profile` | UC1.5–1.9: Tutor/Student profile, goal, AI bio | Main: tạo/cập nhật Tutor qua PUT, môn/cấp học, lịch rảnh có kiểm tra overlap; Student cấp học/khu vực; catalog; trạng thái xác minh qua facade. Connection/Discovery bổ sung batch query, goal/slot read model, candidate search | Chưa có goal CRUD HTTP và quản lý goal availability; chưa có AI bio endpoint/service nối gateway. UC1.8/1.9 mới đáp ứng phần profile cơ bản. |
| `verification` | UC1.10–1.12: submit, upload, admin approve/reject | Main: multipart submit, metadata credential/file, chặn pending trùng, queue phân trang, review PENDING, reject reason, cập nhật Tutor status | Chưa nối audit/notification; chưa có HTTP tải/xem proof bảo vệ quyền dù Storage có retrieve; chưa thấy job orphan cleanup. Không kết luận review concurrent an toàn chỉ từ check PENDING. |
| `discovery` | UC2.1–2.5: search/filter, matching, giải thích AI, detail | `feat/discovery` và `feat/connection`: 3 routes, VERIFIED filter, search/sort/page, matching 5 yếu tố, Parent confirmed-link scope, fallback AI, log/event | Detail chưa tích hợp review/scheduling; cap 200 không đảm bảo global Top N; quota AI concurrent chưa atomic; search chưa tie-break duy nhất, relevance là alias rating. Chưa vào main. |
| `connection` | UC3.1–3.5: invitation hai chiều, consent, revoke, gửi/đáp request; accept tạo class | `feat/connection`: 10 routes; invitation/link/request/query services, role/owner/link checks, quota, row/advisory locks, tạo ACTIVE class, notification sink và revoke audit | Mới hết Phase 1 T07 và hotfix migration. T08–T25 còn: guards/Clock/transaction/event, consent/privacy, audit snapshot, validate slot/reason, expiry job, batch metadata, page/admin lookup, security và full concurrency/quality gate. |
| `scheduling` | UC4.1–4.6: propose/conflict/respond/reschedule/cancel/outcome; calendar | `feature/backend-schedule`: 8 routes, domain state machine/policies, JPA adapters, row locks, history/events, expiry/reminder jobs, test concurrency | Connection validator mặc định mock; Progress adapter chỉ log; chưa có notification consumer. Quyền Parent, mapping account UUID với Tutor/Student profile UUID và bảo vệ detail cần hoàn thiện. Chưa vào main/Connection branch. |
| `progress` | UC5.1–5.5: plan/milestone trọng số, note, dashboard, multi-child, teaching history | Migrations có 4 bảng; Maven module có khung | Không có `src/main` Java nghiệp vụ/API trên 12 branch; chưa có calculator, facade, dashboard/note persistence. Scheduling ghi note vào history không thay thế `session_note` của Progress. |
| `review` | UC6.1–6.2: review sau buổi hoàn thành, một review/cặp, danh sách/rating | Migration có review và unique constraint; module có khung | Chưa có controller/service/repository nghiệp vụ, eligibility check, aggregate rating update và consumer cho tutor detail. |
| `notification` | UC6.3: xem thông báo; FR cho các lifecycle events | Main chỉ schema/khung; Connection branch có entity/repository nội bộ và `JpaNotificationSink` ghi thông báo trực tiếp | Chưa có HTTP list/read/read-all, badge/unread query; chưa có event listener đầy đủ cho Verification/Scheduling/Progress/Review. Sink không phải implementation UC6.3 hoàn chỉnh. |
| `platform/shared` | CurrentUser, error/envelope, value objects, contracts chung | Main có interface CurrentUser, Money, TimeInterval, domain event, lỗi/response; Connection thêm AuditSink/NotificationSink | Không phải module use case. Coverage baseline còn thiếu; không coi toàn bộ quality gate đã đạt. |
| `platform/ai` | AI gateway cho bio/match, timeout, log/usage query support | Main có Gemini/Stub provider, gateway, sanitizer, request logger/usage query. Discovery/Connection thêm xử lý deadline và tích hợp explanation | Mặc định `stub`, chưa chứng nhận Gemini production; chưa nối bio. Quota theo feature do caller áp dụng, không tự có chỉ vì gateway có usage query. |
| `platform/storage` | store/retrieve/delete proof; local → Supabase | Main có local disk, key/path handling, MIME detection Tika, sanitize tên, giới hạn 10 MiB, PDF/JPEG/PNG | Chưa có Supabase adapter; retrieve nội bộ chưa thành HTTP download với quyền; cleanup file lỗi có trong submit nhưng chưa có scheduled orphan sweep. |
| `platform/audit` | FR-02/15: audit quyết định admin/revoke, insert-only | Main có bảng/khung; Connection branch có AuditLog/JpaAuditSink và gọi khi revoke | Chưa tích hợp review Verification; structured snapshot và query/admin API chưa có. |
| `app` và hạ tầng backend | Một JVM, một DB; wiring/security/error/OpenAPI/jobs/migrations | Main có boot app, config/security/CORS, exception handler, JDBC session dependency, Flyway, OpenAPI/Actuator, virtual threads, timezone, Maven/test tooling | CSRF exempt `/api/v1/**` trên main/Connection; không suy annotation/comment thành cơ chế đã enforce. Integration liên branch còn thiếu. |

Nguồn code chi tiết có link commit tại mục 10–12. “Chưa có” được kết luận từ cây source và controller trên tất cả 12 refs ở mục 8, không chỉ từ branch checkout.

## 4. Những công nghệ và cơ chế thực sự đang áp dụng

| Cơ chế | Hiện trạng cụ thể | Nguồn đọc code |
| --- | --- | --- |
| Java 21, Spring Boot, Maven multi-module | POM main khai báo Java 21, Spring Boot **4.1.1**, 9 business modules và platform/app; một process monolith | `backend/pom.xml`, POM app/modules/platform; ADR-001/002 |
| PostgreSQL, JPA/Hibernate, Flyway | PostgreSQL là DB; `ddl-auto=validate`, `open-in-view=false`; Flyway tập trung `app/src/main/resources/db/migration`, validate checksum, out-of-order=false | `application.yml`, SQL migrations; Docker Compose định hướng PostgreSQL 17 |
| Argon2id | **IAM dùng argon2-jvm 2.11**, iterations=3, memory=65536 KiB, parallelism=1; verify/hash đều qua Argon2PasswordService | `iam/infra/security/Argon2PasswordService.java`, services register/login/change/reset |
| Spring PasswordEncoder | SecurityConfig còn khai báo encoder default Spring 5.8 (memory 16384, iterations 2). Đây không phải tham số của service IAM đang hash password | `app/config/SecurityConfig.java`; tránh gộp hai implementation làm một |
| Session cookie | Login tạo HttpSession, lưu SecurityContext + ACCOUNT_ID/ROLE; logout invalidate phiên hiện tại; config cookie EDUFIT_SESSION, HttpOnly, SameSite=Lax, idle timeout 60m; có dependency Spring Session JDBC và 2 bảng | AuthController, application.yml, app/pom.xml. Chưa kiểm chứng store/session policy runtime ở lần audit này |
| JWT / refresh token | **Không thấy issuer/verifier/filter/refresh endpoint hoặc dependency JWT thực thi trên 12 branch**; từ JWT trong CurrentUser chỉ là comment | ADR-001 Decision 5 secondary track; scan toàn backend remote |
| One-time reset token | SecureRandom, token raw 256-bit, lưu hash SHA-256; reset TTL 30 phút, consume token, vô hiệu token reset trước đó; email gửi qua EmailSender | TokenCryptoService, RequestPasswordResetService, ResetPasswordService, email adapter |
| Login abuse protection | Có code đếm 5 lần sai/khóa 15 phút, lỗi credentials chung và dummy hash | AuthenticateAccountService, Account. Cần kiểm chứng lưu counter khi service ném runtime exception trong transaction; không khẳng định test persistence đã pass |
| RBAC / ownership | URL security + EnableMethodSecurity, PreAuthorize Verification và guard theo CurrentUser trong use case; Parent scope qua confirmed link ở Discovery/Connection | SecurityConfig, controllers/services tương ứng |
| CSRF | Có CookieCsrfTokenRepository. **Main và Connection miễn toàn `/api/v1/**`**, nên API write không được bảo vệ bởi cấu hình này. Discovery branch cũ có policy khác; không suy bảo vệ Discovery cũ còn nguyên sau tích hợp | SecurityConfig từng ref; Connection contract §1 và T21 |
| File validation/storage | Tika đọc content để nhận MIME, allow PDF/JPEG/PNG, default 10 MiB; local root `var/credentials`; storage key riêng, sanitize tên | FileValidationService, LocalCredentialStorageService, CredentialStorageProperties |
| Concurrency / DB integrity | FK, CHECK, partial UNIQUE; GiST exclusion theo tutor/student và range `[)` khi status SCHEDULED; Connection advisory/row locks + quota lock; Scheduling pessimistic lock và version | SQL V1.04/V1.05/V1.11, PostgresConnectionLock, scheduling repository adapters |
| Matching | Điểm subject 30%, level 20%, budget 20%, schedule 15%, rating 15%; hard eligibility subject/mode/area + VERIFIED; candidates mặc định 200; AI giải thích kết quả đầu, fallback rule text | Discovery MatchScorer/MatchWeights, matching/explanation services, Profile candidate query |
| AI | Gemini adapter + offline Stub mặc định, timeout 15s, output sanitizer, request metadata logging; explanation quota 10/24h theo account trên branch Discovery/Connection | AiGatewayService, AiProperties, MatchExplanationService |
| Events | Spring in-process events, facade giao tiếp JVM; Discovery có MatchingExecutedEvent, Scheduling có session events; Connection hiện gọi sink trực tiếp | Event classes/services; không có durable outbox/retry guarantee được triển khai |
| API/documentation/validation | REST `/api/v1`, ApiResponse/ApiError, Bean Validation, exception handler, springdoc OpenAPI **3.1.0** dependency | Controllers, shared response, app OpenApiConfig |
| Runtime | Virtual threads bật, Hikari pool, timezone Asia/Ho_Chi_Minh, graceful shutdown, scheduling config, Actuator health | application.yml, EduFitApplication, SchedulingConfig |
| Quality tooling | JUnit/Mockito, ArchUnit, PostgreSQL Testcontainers, JaCoCo; CI backend; `jacoco.haltOnFailure=false` mặc định | POM/workflow/test files. Có tooling không chứng minh coverage hoặc full suite đạt |

## 5. Use case nào đã có và mức hoàn thành

Tất cả UC dưới đây nằm trong SRS. “Phạm vi thiết kế” không tự có nghĩa mọi chi tiết đã được team phê duyệt thống nhất; các mâu thuẫn cần giữ riêng ở mục 9.

| UC | Tính năng | Trạng thái backend theo code |
| --- | --- | --- |
| UC1.1 | Register Account | Có trên main, account ACTIVE ngay. Không thực thi mandatory email verification của SRS cũ. |
| UC1.2 | Login | Có email/password + session; Zalo OAuth chưa có. |
| UC1.3 | Logout | Có, hủy session hiện tại. |
| UC1.4 | Manage Password | Có forgot/reset/change; session revocation toàn account và rate limit reset chưa hoàn thiện. |
| UC1.5 | Create Tutor Profile | Có, tạo qua PUT nếu chưa tồn tại; tách thao tác subject/availability. |
| UC1.6 | Edit Tutor Profile | Có core; không tự suy cơ chế material-change re-verification đã đầy đủ. |
| UC1.7 | Generate AI-Suggested Bio | Chưa có luồng/HTTP consumer Profile, dù gateway và feature AI tồn tại. |
| UC1.8 | Create Student Profile | Có basic profile; thiếu create goals qua HTTP. |
| UC1.9 | Edit Student Profile | Có basic update; thiếu manage goals/goal slots qua HTTP. |
| UC1.10 | Submit Verification Request | Có submit, pending/profile state; thiếu notification. |
| UC1.11 | Upload Proof of Credentials | Có upload/local validation trong submit; thiếu protected proof HTTP retrieval/Supabase/orphan sweep. |
| UC1.12 | Review Tutor Verification | Có approve/reject/queue; thiếu audit và notification. |
| UC2.1 | Search Tutors | Có trên Discovery/Connection, public VERIFIED search; khác precondition đăng nhập của SRS. |
| UC2.2 | Filter Tutor List | Có filter kết hợp, sort/page; relevance chưa là thuật toán tìm theo độ liên quan. |
| UC2.3 | Get Matched Tutors | Có matching rule 5 yếu tố, goal authorization/log; Top N chỉ trong candidate cap. |
| UC2.4 | Explain Best Recommendation | Có tích hợp cho kết quả đầu, quota/fallback; provider thật chưa chứng nhận, quota concurrent chưa atomic. |
| UC2.5 | View Tutor Profile Detail | Có profile/subjects/availability; thiếu review/scheduling consumer, không coi trang detail đầy đủ SRS. |
| UC3.1 | Send Link Invitation | Có core, quota, history/cooldown; các hoàn thiện T11 còn pending. |
| UC3.2 | Respond to Link Invitation | Có accept/decline tạo link; consent/privacy/event hoàn thiện chưa xong. |
| UC3.3 | Revoke Link | Có Student/Admin revoke + audit sink; structured snapshot, recipients/admin lookup còn thiếu. |
| UC3.4 | Send Connection Request | Có create/list/cancel với scope; strict slots/expiry job/paging còn thiếu. |
| UC3.5 | Respond to Connection Request | Có accept/reject và accept tạo ACTIVE class; guard/concurrency matrix/notification hoàn thiện còn trong T14/T22. |
| UC4.1 | Propose Session | Có trên Scheduling branch; validator lớp mock, Parent scope và UUID mapping chưa hoàn thiện. |
| UC4.2 | Check Schedule Conflict | Có policy + query + GiST DB protection; không có endpoint riêng vì là thao tác include. |
| UC4.3 | Respond to Session Proposal | Có service/route/state transition/locks; còn tích hợp scope thật. |
| UC4.4 | Reschedule Session | Có propose và respond reschedule; còn tích hợp liên module. |
| UC4.5 | Cancel Session | Có cancel reason/late policy/history/event; chưa có delivery notification hoàn chỉnh. |
| UC4.6 | Record Session Outcome | Có COMPLETED/ABSENT và history/event; ProgressSyncAdapter chỉ log, chưa lưu note vào Progress. |
| UC5.1 | Manage Learning Plan | Chưa có nghiệp vụ, mới schema. |
| UC5.2 | Add Session Note | Chưa có implementation Progress; note xuất hiện ở history/event Scheduling chưa thay thế use case này. |
| UC5.3 | View Progress Dashboard | Chưa có. |
| UC5.4 | View Multi-Student Dashboard | Chưa có. |
| UC5.5 | View Own Teaching History | Chưa có API tổng hợp theo yêu cầu này; SessionFacade/calendar không đồng nghĩa teaching history đầy đủ. |
| UC6.1 | Submit Tutor Review | Chưa có, mới schema. |
| UC6.2 | View Received Reviews | Chưa có, mới schema/rating fields. |
| UC6.3 | View Notifications | Chưa có API đọc; Connection branch mới có writer sink. |

## 6. Database hiện tại

**Theo mã migration, không phải truy vấn DB đang chạy.** Main có 31 bảng ứng dụng; `flyway_schema_history` là bảng kỹ thuật Flyway, không tính vào 31. V1.01–V1.08 tạo schema; V1.09 seed. Trên Connection có thêm V1.10 seed tương thích và V1.11 thêm quota/version/relink (32 bảng). Danh sách cột và link tới dòng DDL nằm ở mục 12.

| Nhóm / migration | Bảng | Dữ liệu và tính năng phục vụ | Mức dùng trong code |
| --- | --- | --- | --- |
| IAM V1.01 | account | Email, hash, role/status, lock/fail count, login/password metadata | IAM đang dùng |
| IAM V1.01 | email_token | Hash token, loại, hạn, thời điểm sử dụng, account | Reset password đang dùng; tên Entity là AccountToken |
| IAM V1.01 | SPRING_SESSION, SPRING_SESSION_ATTRIBUTES | Session và serialized attributes | JDBC session infrastructure; có schema/dependency |
| Profile V1.02 | education_level, subject | Catalog cấp học/môn, active/order | Catalog/Profile/Discovery |
| Profile V1.02 | student_profile, tutor_profile | Hồ sơ học sinh/gia sư, teaching mode, học phí, verified/rating cache | Profile/Verification; Discovery đọc |
| Profile V1.02 | tutor_subject, tutor_availability_slot | Môn/cấp gia sư và lịch rảnh tuần | Profile API; Discovery đọc |
| Profile V1.02 | learning_goal, goal_availability_slot | Mục tiêu môn/cấp, budget, deadline, mode/area và lịch mong muốn | Connection/Discovery đọc; chưa có HTTP quản trị goal |
| Verification V1.03 | verification_request, credential, credential_file | Yêu cầu duyệt, reviewer/reason/time; bằng cấp và metadata storage | Nộp/duyệt đang dùng |
| Connection V1.04 | link_invitation, parent_student_link | Invite/status/TTL/cooldown; confirmed/revoked link | Có schema main; nghiệp vụ ở Connection branch |
| Connection V1.04 | connection_request, class | Goal/tutor/student/sender, desired slots JSONB; quan hệ dạy-học ACTIVE | Có schema main; nghiệp vụ ở Connection branch |
| Scheduling V1.05 | tutoring_session, session_reschedule_proposal, session_history | Buổi học, proposal đổi lịch, status/cancel/outcome/history/version | Có schema main; nghiệp vụ ở Scheduling branch |
| Progress V1.06 | session_note, learning_plan, milestone, plan_history | Note, plan, trọng số/mốc đạt, snapshot/history | Mới schema, chưa nghiệp vụ |
| Review/Notification V1.07 | review | Rating/content và completed sessions tại lúc review | Mới schema, chưa nghiệp vụ |
| Review/Notification V1.07 | notification | Người nhận, loại/target, nội dung, trạng thái đọc | Connection branch có writer, chưa API đọc |
| Platform V1.08 | audit_log | Actor/action/target/reason/time | Main schema; Connection branch ghi revoke |
| Platform V1.08 | ai_request_log | Actor/feature/status/latency/time; schema không có cột provider/token count | AI gateway đang dùng |
| Platform V1.08 | matching_run_log | Actor/goal/time/result count | Main schema; Discovery branch ghi matching |
| Connection V1.11 | connection_invite_quota | Actor/window start/attempt count | Chỉ Connection branch; hỗ trợ quota có lock |

Các ràng buộc đã có trong DDL: FK liên module; unique email; partial unique cho PENDING request/invitation/verification/reschedule và ACTIVE class; unique review theo cặp; CHECK giá trị trạng thái; GiST exclusion cho lịch SCHEDULED cả Tutor và Student. GiST dùng `[start,end)`, hai buổi chạm biên không overlap. `end_at > start_at` có ở DB; giới hạn thời lượng 30–240 phút do policy Scheduling kiểm tra, không nhầm thành CHECK duration đã có trong migration main.

**Không gộp schema từ các branch một cách cơ học:** branch `feature/setup-database-flyway` dùng migration `V2026...`/mô hình users cũ; `feature/iam-architecture` có V1 init cũ. `feat/discovery` dùng V1.09 relink trong khi main dùng V1.09 seed. Hotfix Connection đã tách thành V1.11 và giữ seed V1.09/V1.10; chuyển branch/merge còn phải xét lineage lịch sử như báo cáo Flyway. Lần audit này không thay đổi database.

## 7. API hiện có và tính năng được mở ra

Danh mục chính xác từng method/path có link controller/dòng ở mục 11. Tổng số **route HTTP nghiệp vụ khai báo trong controller** (không tính Swagger/Actuator/error):

- Main: **24 routes** = IAM 7 + Profile 10 + Catalog 3 + Verification 4.
- Connection branch: **37 routes** = 24 baseline + Discovery 3 + Connection 10.
- Scheduling branch: **32 routes** = 24 baseline + Scheduling 8.
- Hợp danh mục trên các branch: **45 method/path duy nhất**. Đây là hợp danh sách source, **không phải 45 routes đã cùng chạy trong một app hiện tại**.

| Nhóm | Tính năng API | Quyền / giới hạn đáng chú ý |
| --- | --- | --- |
| `/api/v1/auth` | register, login, logout, forgot-password, reset-password, change-password, me | Public URL prefix; change/me tự kiểm CurrentUser. Không có route csrf/verify-email/resend/refresh/Zalo. |
| `/api/v1/profiles` | me; student me GET/PUT; student theo ID; tutor me GET/PUT; tutor theo ID; thêm/xóa môn; thay toàn bộ availability slots | Có security URL cho các `/me` theo role; đọc theo ID cần đối chiếu ownership khi tích hợp Parent, không suy chỉ authenticated là đủ scope. Chưa có goal CRUD/bio AI. |
| `/api/v1/catalogs` | Lấy chung catalog hoặc education-levels/subjects | Public, hỗ trợ nhập liệu có cấu trúc. |
| `/api/v1/verifications`, `/api/v1/admin/verifications` | Multipart submit, latest request của Tutor, admin pending queue, admin review | Tutor/Admin qua PreAuthorize; chưa có download proof endpoint. |
| `/api/v1/discovery` | Search/filter/page; verified tutor detail; matching theo goal/topN | GET public, POST STUDENT/PARENT + owner/confirmed link. Main cho phép URL Discovery nhưng chưa có controller Discovery. |
| `/api/v1/connections` | invite, list/respond/withdraw; links list/revoke; requests create/list/cancel/respond | Actor từ session; scope tự thân/confirmed link; Tutor chỉ inbox/request của mình; Student/Admin revoke. Paging/admin lookup mới là contract đích. |
| `/api/v1/scheduling` | Propose/respond, reschedule/respond reschedule, cancel, outcome, detail, calendar | Có controller nhưng lớp validation mock; `getSessionDetail` đọc repository theo ID chưa kiểm owner tại method. Không chứng nhận scope Parent hay account/profile UUID đã đúng. |

Public Java API (`IamFacade`, `ProfileFacade`, `VerificationFacade`, `DiscoveryFacade`, `ConnectionFacade`, `SessionFacade`, `AiGateway`, Storage service) dùng trong cùng JVM, **không cộng vào số HTTP routes**. Progress/Review/Notification/Audit chưa có REST controller nghiệp vụ trên 12 refs.

## 8. Toàn bộ branch remote đã đối chiếu

Đã `fetch origin --prune` và đối chiếu danh sách bằng `ls-remote --heads origin`. Đọc bằng `git show/ls-tree/grep/diff`, không checkout/merge/cherry-pick, không sửa source. Các branch có chữ frontend vẫn được xét **phần backend và tài liệu liên quan** để không bỏ sót backend thay đổi trong branch đó; không đọc frontend source.

| Remote branch | Commit đã đọc | Ý nghĩa cho báo cáo backend |
| --- | --- | --- |
| main | `6755121122c79dc9705282225d14ffb76e17c29a` | Baseline chính; 24 routes, 31 bảng |
| feat/connection | `77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c` | Connection Phase 1 + Discovery + migration hotfix; 37 routes, 32 bảng |
| feat/discovery | `dd9068e0a14b0e480d1823eab37bc82041166fe5` | Discovery bàn giao T15; Connection baseline; migration lineage cũ |
| feature/backend-schedule | `cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8` | Scheduling implementation, mock/no-op integrations; 32 routes |
| feature/frontend-auth | `6bfb4c4fecef38c2fa54fee1f3e0b84e95de048a` | Backend baseline gần main; không thêm business module mới |
| feature/iam-architecture | `c7524000e549028a17a3f07895d9b64688051544` | IAM/architecture giai đoạn cũ, schema init cũ; không phải implementation JWT |
| feature/platform-ai | `d90ba505e5f2a80d912162345f997d94c04318d6` | AI gateway giai đoạn riêng; code gateway đã có trên main |
| feature/profile | `1f648df93b179a12a9a9542dbd7cd3ce9c7e82df` | Profile riêng; chưa thêm goal HTTP/AI bio |
| feature/setup-database-flyway | `67afcfc5e319c1ff1f4dabb049b9b7c5162aa395` | Migration users/account_tokens cũ, không dùng làm schema hiện tại main |
| feature/setup-testing-quality-gates | `c2f177667d87a132eae59f9b6c8754d1eda14271` | Backend JaCoCo/testing cấu hình; không bổ sung module nghiệp vụ |
| feature/storage-verification | `32724a51ab48f119b574483f77739b6906d857ce` | Storage/Verification giai đoạn riêng; đã có trên main |
| refactor/frontend-auth | `baf57f9a13cc0d9da8a4f7103c4febd7c278814d` | Backend tree **giống hệt main** tại snapshot này |

`origin/HEAD` chỉ là alias main, không tính thành branch thứ 13. Local `feature/verification` hoặc checkout `EduFit-verification` không phải remote head trong danh sách này; không dùng code chưa push để tuyên bố remote đã hoàn thành.

## 9. Các điểm chưa thể coi là đã chốt đồng nhất

1. **Email verification / OAuth:** SRS UC1.1 (P220 và phần UC1.1) yêu cầu verify email; UC1.2 tổng quan nêu Zalo OAuth. RegisterAccountService hiện tạo ACTIVE ngay, authentication contract cũng mô tả register kích hoạt ngay. Đây là thay đổi/khác biệt đặc tả; chưa có bằng chứng Zalo đã implement. Cần thống nhất bản SRS áp dụng, không tự liệt kê email verification là feature hoàn thành.
2. **Session revocation:** ADR-001 primary và auth contract yêu cầu hủy phiên sau đổi/reset mật khẩu. ChangePasswordService/ResetPasswordService hiện chỉ đổi hash/consume token, chưa thấy session invalidation hook. Logout hiện chỉ invalidate phiên gọi request.
3. **CSRF:** auth contract có `GET /auth/csrf` và write protection, controller chưa có GET đó. SecurityConfig main/Connection exempt toàn API; T21 Connection chưa làm. Không ghi “CSRF hoàn tất” chỉ vì có repository bean.
4. **Public discovery:** SRS UC2.1 có precondition login nhưng screen authorization/Discovery contract cho GET public; code đã chọn public. Tài liệu hiện thực rõ, quyết định SRS tổng thể còn cần cập nhật đồng nhất.
5. **Scheduling identity/scope:** Controller truyền `CurrentUser.getUserId()` trong khi session lưu tutor_id/student_id FK vào profile; service so proposer với tutorId/studentId. Chưa thấy adapter resolve account → profile ở controller; Parent confirmed-link scope chưa nối. Detail GET cũng chưa owner guard. Đây là phần tích hợp/quyền còn thiếu, không phải đã nghiệm thu UC4 cho cả ba actor.
6. **Learning notes và dashboard:** Bảng UC5.2 tổng quan P460 nói structured notes, chi tiết P3139/P3168 nói free text/optional, P3170 nói max5000 còn proposed. Tổng quan UC5.4 nói comparative aggregation, chi tiết P3247/P3283 không cộng/trung bình giữa trẻ. Khi implement phải chốt theo phần chi tiết/bản SRS thống nhất, không tự thiết kế thêm.
7. **Plan không phải done:** Scheduling plan còn Under Review. Connection contract §6 có page/admin routes nhưng T19 chưa làm; các event/strict slot/security mục tiêu cũng chưa làm. Project overview §4 đã cũ nên không dùng làm current implementation audit.
8. **Chất lượng kiểm chứng:** Phase1 report ghi 348 tests pass, strict JaCoCo FAIL ở Shared (LINE75.73%, BRANCH71.43%); Connection report LINE75.12%, BRANCH47.73%. Flyway hotfix report ghi 75 affected tests pass và startup local/health UP; không phải rerun full348 hoặc strict gate pass. Discovery performance là local evidence. Báo cáo này không chạy lại các kiểm chứng đó.

## 10. Nguồn code và tài liệu ở commit cố định

Các liên kết dưới đây giữ đúng snapshot đã audit, kể cả khi branch tiếp tục thay đổi. Các phụ lục tiếp theo bổ sung link tới dòng controller/schema.

| Nhóm bằng chứng | File tại snapshot |
| --- | --- |
| Nền tảng (main) | [pom.xml](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/pom.xml) |
| Nền tảng (main) | [pom.xml](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/pom.xml) |
| Nền tảng (main) | [application.yml](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/application.yml) |
| Nền tảng (main) | [SecurityConfig.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/java/vn/edufit/config/SecurityConfig.java) |
| Nền tảng (main) | [OpenApiConfig.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/java/vn/edufit/config/OpenApiConfig.java) |
| IAM (main) | [Argon2PasswordService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/infra/security/Argon2PasswordService.java) |
| IAM (main) | [TokenCryptoService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/infra/security/TokenCryptoService.java) |
| IAM (main) | [AuthenticateAccountService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/application/AuthenticateAccountService.java) |
| IAM (main) | [RegisterAccountService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/application/RegisterAccountService.java) |
| IAM (main) | [ChangePasswordService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/application/ChangePasswordService.java) |
| IAM (main) | [ResetPasswordService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/application/ResetPasswordService.java) |
| IAM (main) | [RequestPasswordResetService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/application/RequestPasswordResetService.java) |
| Profile / Verification (main) | [StudentProfileService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/application/service/StudentProfileService.java) |
| Profile / Verification (main) | [TutorProfileService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/application/service/TutorProfileService.java) |
| Profile / Verification (main) | [AdminVerificationService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/application/service/AdminVerificationService.java) |
| Profile / Verification (main) | [TutorVerificationService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/application/service/TutorVerificationService.java) |
| Storage / AI (main) | [FileValidationService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/platform/storage/src/main/java/vn/edufit/storage/infra/FileValidationService.java) |
| Storage / AI (main) | [LocalCredentialStorageService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/platform/storage/src/main/java/vn/edufit/storage/infra/LocalCredentialStorageService.java) |
| Storage / AI (main) | [AiGatewayService.java](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/platform/ai/src/main/java/vn/edufit/ai/application/AiGatewayService.java) |
| Discovery / Connection (connection) | [MatchScorer.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/domain/policy/MatchScorer.java) |
| Discovery / Connection (connection) | [TutorMatchingService.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/application/service/TutorMatchingService.java) |
| Discovery / Connection (connection) | [MatchExplanationService.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/application/service/MatchExplanationService.java) |
| Discovery / Connection (connection) | [ConnectionRequestService.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/application/service/ConnectionRequestService.java) |
| Discovery / Connection (connection) | [LinkInvitationService.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/application/service/LinkInvitationService.java) |
| Discovery / Connection (connection) | [ParentStudentLinkService.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/application/service/ParentStudentLinkService.java) |
| Discovery / Connection (connection) | [JpaNotificationSink.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/notification/src/main/java/vn/edufit/notification/JpaNotificationSink.java) |
| Discovery / Connection (connection) | [JpaAuditSink.java](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/platform/audit/src/main/java/vn/edufit/audit/JpaAuditSink.java) |
| Discovery / Connection (connection) | [phase1-validation.md](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/docs/connection/phase1-validation.md) |
| Discovery / Connection (connection) | [flyway-fix-validation.md](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/docs/connection/flyway-fix-validation.md) |
| Scheduling (scheduling) | [MockConnectionValidationAdapter.java](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/infra/adapter/MockConnectionValidationAdapter.java) |
| Scheduling (scheduling) | [ProgressSyncAdapter.java](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/infra/adapter/ProgressSyncAdapter.java) |
| Scheduling (scheduling) | [ProposeSessionService.java](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/application/service/ProposeSessionService.java) |
| Scheduling (scheduling) | [RecordSessionOutcomeService.java](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/application/service/RecordSessionOutcomeService.java) |
| Scheduling (scheduling) | [scheduling_implementation_plan.md](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/docs/scheduling/scheduling_implementation_plan.md) |

## 11. Danh mục từng HTTP route đã khai báo

Nguồn được trích từ annotation controller, không từ bảng API thiết kế. Main và mỗi branch được đọc độc lập; cột Branch ghi nơi lấy implementation bổ sung. Quyền và phần chưa hoàn thiện xem mục 3, 5, 7, 9.

| Method | Path | Branch nguồn | Controller và dòng |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | main | [AuthController.java:L83](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L83) |
| POST | `/api/v1/auth/login` | main | [AuthController.java:L104](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L104) |
| POST | `/api/v1/auth/logout` | main | [AuthController.java:L137](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L137) |
| POST | `/api/v1/auth/forgot-password` | main | [AuthController.java:L150](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L150) |
| POST | `/api/v1/auth/reset-password` | main | [AuthController.java:L163](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L163) |
| POST | `/api/v1/auth/change-password` | main | [AuthController.java:L176](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L176) |
| GET | `/api/v1/auth/me` | main | [AuthController.java:L199](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/iam/src/main/java/vn/edufit/iam/web/AuthController.java#L199) |
| GET | `/api/v1/catalogs` | main | [CatalogController.java:L25](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/CatalogController.java#L25) |
| GET | `/api/v1/catalogs/education-levels` | main | [CatalogController.java:L31](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/CatalogController.java#L31) |
| GET | `/api/v1/catalogs/subjects` | main | [CatalogController.java:L37](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/CatalogController.java#L37) |
| GET | `/api/v1/profiles/me` | main | [ProfileController.java:L51](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L51) |
| GET | `/api/v1/profiles/students/me` | main | [ProfileController.java:L78](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L78) |
| PUT | `/api/v1/profiles/students/me` | main | [ProfileController.java:L85](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L85) |
| GET | `/api/v1/profiles/students/{studentId}` | main | [ProfileController.java:L94](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L94) |
| GET | `/api/v1/profiles/tutors/me` | main | [ProfileController.java:L106](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L106) |
| PUT | `/api/v1/profiles/tutors/me` | main | [ProfileController.java:L113](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L113) |
| GET | `/api/v1/profiles/tutors/{tutorId}` | main | [ProfileController.java:L122](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L122) |
| POST | `/api/v1/profiles/tutors/me/subjects` | main | [ProfileController.java:L130](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L130) |
| DELETE | `/api/v1/profiles/tutors/me/subjects` | main | [ProfileController.java:L139](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L139) |
| PUT | `/api/v1/profiles/tutors/me/availability-slots` | main | [ProfileController.java:L149](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/profile/src/main/java/vn/edufit/profile/web/ProfileController.java#L149) |
| GET | `/api/v1/admin/verifications/pending` | main | [AdminVerificationController.java:L39](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/web/AdminVerificationController.java#L39) |
| POST | `/api/v1/admin/verifications/{id}/review` | main | [AdminVerificationController.java:L53](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/web/AdminVerificationController.java#L53) |
| POST | `/api/v1/verifications` | main | [TutorVerificationController.java:L33](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/web/TutorVerificationController.java#L33) |
| GET | `/api/v1/verifications/my-request` | main | [TutorVerificationController.java:L40](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/modules/verification/src/main/java/vn/edufit/verification/web/TutorVerificationController.java#L40) |
| POST | `/api/v1/connections/parent-student/invite` | connection | [ConnectionController.java:L40](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L40) |
| GET | `/api/v1/connections/parent-student/invitations` | connection | [ConnectionController.java:L48](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L48) |
| POST | `/api/v1/connections/parent-student/respond` | connection | [ConnectionController.java:L53](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L53) |
| POST | `/api/v1/connections/parent-student/invitations/{id}/withdraw` | connection | [ConnectionController.java:L59](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L59) |
| GET | `/api/v1/connections/parent-student/links` | connection | [ConnectionController.java:L65](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L65) |
| DELETE | `/api/v1/connections/parent-student/links/{id}` | connection | [ConnectionController.java:L70](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L70) |
| POST | `/api/v1/connections/requests` | connection | [ConnectionController.java:L76](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L76) |
| GET | `/api/v1/connections/requests` | connection | [ConnectionController.java:L82](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L82) |
| POST | `/api/v1/connections/requests/{id}/cancel` | connection | [ConnectionController.java:L90](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L90) |
| POST | `/api/v1/connections/requests/{id}/respond` | connection | [ConnectionController.java:L96](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java#L96) |
| GET | `/api/v1/discovery/tutors` | connection | [DiscoveryController.java:L48](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/web/DiscoveryController.java#L48) |
| GET | `/api/v1/discovery/tutors/{tutorId}` | connection | [DiscoveryController.java:L67](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/web/DiscoveryController.java#L67) |
| POST | `/api/v1/discovery/match` | connection | [DiscoveryController.java:L73](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/modules/discovery/src/main/java/vn/edufit/discovery/web/DiscoveryController.java#L73) |
| POST | `/api/v1/scheduling/sessions/propose` | scheduling | [SessionController.java:L71](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L71) |
| POST | `/api/v1/scheduling/sessions/{id}/respond` | scheduling | [SessionController.java:L91](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L91) |
| POST | `/api/v1/scheduling/sessions/{id}/reschedule` | scheduling | [SessionController.java:L105](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L105) |
| POST | `/api/v1/scheduling/sessions/reschedule-proposals/{proposalId}/respond` | scheduling | [SessionController.java:L121](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L121) |
| POST | `/api/v1/scheduling/sessions/{id}/cancel` | scheduling | [SessionController.java:L137](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L137) |
| POST | `/api/v1/scheduling/sessions/{id}/outcome` | scheduling | [SessionController.java:L151](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L151) |
| GET | `/api/v1/scheduling/sessions/{id}` | scheduling | [SessionController.java:L165](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L165) |
| GET | `/api/v1/scheduling/calendar` | scheduling | [SessionController.java:L174](https://github.com/dvtrug/EduFit/blob/cfa9a8f1e3d3112e61d818b51b794dc6d176ffc8/backend/modules/scheduling/src/main/java/vn/edufit/scheduling/web/SessionController.java#L174) |

## 12. Danh mục bảng và cột từ Flyway

Danh sách cột dưới đây trích DDL main V1.01–V1.08; bảng quota trích Connection V1.11. FK/CHECK/INDEX xem file SQL được liên kết. V1.11 còn thêm cột `version BIGINT NOT NULL DEFAULT 0` vào `link_invitation`, `parent_student_link`, `connection_request` và thay unique cặp link thành partial unique cho trạng thái CONFIRMED.

| Bảng | Cột khai báo trong CREATE TABLE | Migration và dòng |
| --- | --- | --- |
| `link_invitation` | `invitation_id` UUID, `inviter_user_id` UUID, `invited_user_id` UUID, `status` VARCHAR(30), `created_at` TIMESTAMPTZ, `expires_at` TIMESTAMPTZ, `responded_at` TIMESTAMPTZ, `reinvite_after` TIMESTAMPTZ | [V1.04__create_connection_and_class_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/connection/V1.04__create_connection_and_class_tables.sql#L6) |
| `parent_student_link` | `link_id` UUID, `parent_user_id` UUID, `student_id` UUID, `status` VARCHAR(30), `confirmed_at` TIMESTAMPTZ, `revoked_at` TIMESTAMPTZ, `revoked_by` UUID, `revoke_reason` TEXT | [V1.04__create_connection_and_class_tables.sql:L31](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/connection/V1.04__create_connection_and_class_tables.sql#L31) |
| `connection_request` | `request_id` UUID, `student_id` UUID, `tutor_id` UUID, `goal_id` UUID, `sent_by_user_id` UUID, `desired_slots` JSONB, `message` VARCHAR(500), `status` VARCHAR(30), `reject_reason` VARCHAR(300), `created_at` TIMESTAMPTZ, `expires_at` TIMESTAMPTZ, `responded_at` TIMESTAMPTZ | [V1.04__create_connection_and_class_tables.sql:L52](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/connection/V1.04__create_connection_and_class_tables.sql#L52) |
| `class` | `class_id` UUID, `connection_request_id` UUID, `student_id` UUID, `tutor_id` UUID, `goal_id` UUID, `status` VARCHAR(30), `started_at` TIMESTAMPTZ, `ended_at` TIMESTAMPTZ, `end_reason` VARCHAR(300) | [V1.04__create_connection_and_class_tables.sql:L82](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/connection/V1.04__create_connection_and_class_tables.sql#L82) |
| `account` | `user_id` UUID, `email` VARCHAR(255), `password_hash` VARCHAR(255), `full_name` VARCHAR(150), `role` VARCHAR(20), `status` VARCHAR(30), `failed_attempts` INT, `locked_until` TIMESTAMPTZ, `last_login_at` TIMESTAMPTZ, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.01__create_account_and_session_tables.sql:L11](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/iam/V1.01__create_account_and_session_tables.sql#L11) |
| `email_token` | `token_id` UUID, `user_id` UUID, `type` VARCHAR(30), `token_hash` VARCHAR(128), `expires_at` TIMESTAMPTZ, `used_at` TIMESTAMPTZ, `created_at` TIMESTAMPTZ | [V1.01__create_account_and_session_tables.sql:L33](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/iam/V1.01__create_account_and_session_tables.sql#L33) |
| `SPRING_SESSION` | `PRIMARY_ID` CHAR(36), `SESSION_ID` CHAR(36), `CREATION_TIME` BIGINT, `LAST_ACCESS_TIME` BIGINT, `MAX_INACTIVE_INTERVAL` INT, `EXPIRY_TIME` BIGINT, `PRINCIPAL_NAME` VARCHAR(100) | [V1.01__create_account_and_session_tables.sql:L50](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/iam/V1.01__create_account_and_session_tables.sql#L50) |
| `SPRING_SESSION_ATTRIBUTES` | `SESSION_PRIMARY_ID` CHAR(36), `ATTRIBUTE_NAME` VARCHAR(200), `ATTRIBUTE_BYTES` BYTEA | [V1.01__create_account_and_session_tables.sql:L65](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/iam/V1.01__create_account_and_session_tables.sql#L65) |
| `audit_log` | `audit_id` UUID, `actor_user_id` UUID, `action` VARCHAR(60), `target_type` VARCHAR(40), `target_id` UUID, `reason` TEXT, `created_at` TIMESTAMPTZ | [V1.08__create_audit_and_ai_log_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/platform/V1.08__create_audit_and_ai_log_tables.sql#L6) |
| `ai_request_log` | `log_id` UUID, `user_id` UUID, `feature` VARCHAR(40), `status` VARCHAR(20), `latency_ms` INT, `created_at` TIMESTAMPTZ | [V1.08__create_audit_and_ai_log_tables.sql:L23](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/platform/V1.08__create_audit_and_ai_log_tables.sql#L23) |
| `matching_run_log` | `run_id` UUID, `user_id` UUID, `student_id` UUID, `goal_id` UUID, `result_count` INT, `created_at` TIMESTAMPTZ | [V1.08__create_audit_and_ai_log_tables.sql:L39](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/platform/V1.08__create_audit_and_ai_log_tables.sql#L39) |
| `education_level` | `level_id` INT, `name` VARCHAR(80), `sort_order` INT, `is_active` BOOLEAN | [V1.02__create_profile_and_subject_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L6) |
| `subject` | `subject_id` INT, `name` VARCHAR(100), `is_active` BOOLEAN | [V1.02__create_profile_and_subject_tables.sql:L16](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L16) |
| `student_profile` | `student_id` UUID, `user_id` UUID, `education_level_id` INT, `area` VARCHAR(100), `profile_complete` BOOLEAN, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.02__create_profile_and_subject_tables.sql:L25](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L25) |
| `tutor_profile` | `tutor_id` UUID, `user_id` UUID, `display_name` VARCHAR(100), `headline` VARCHAR(200), `bio` TEXT, `teaching_mode` VARCHAR(20), `area` VARCHAR(100), `price_per_session` BIGINT, `experience_years` SMALLINT, `teaching_method` TEXT, `status` VARCHAR(30), `verified_at` TIMESTAMPTZ, `rating_avg` DECIMAL(3,2), `review_count` INT, `version` INT, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.02__create_profile_and_subject_tables.sql:L40](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L40) |
| `tutor_subject` | `tutor_subject_id` UUID, `tutor_id` UUID, `subject_id` INT, `education_level_id` INT | [V1.02__create_profile_and_subject_tables.sql:L69](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L69) |
| `tutor_availability_slot` | `slot_id` UUID, `tutor_id` UUID, `day_of_week` SMALLINT, `start_time` TIME, `end_time` TIME | [V1.02__create_profile_and_subject_tables.sql:L82](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L82) |
| `learning_goal` | `goal_id` UUID, `student_id` UUID, `subject_id` INT, `current_level` SMALLINT, `desired_level` SMALLINT, `goal_type` VARCHAR(30), `deadline` DATE, `mode` VARCHAR(20), `area` VARCHAR(100), `budget_min` BIGINT, `budget_max` BIGINT, `status` VARCHAR(20), `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.02__create_profile_and_subject_tables.sql:L97](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L97) |
| `goal_availability_slot` | `slot_id` UUID, `goal_id` UUID, `day_of_week` SMALLINT, `start_time` TIME, `end_time` TIME | [V1.02__create_profile_and_subject_tables.sql:L124](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/profile/V1.02__create_profile_and_subject_tables.sql#L124) |
| `session_note` | `note_id` UUID, `session_id` UUID, `tutor_id` UUID, `note_text` TEXT, `created_at` TIMESTAMPTZ | [V1.06__create_learning_plan_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/progress/V1.06__create_learning_plan_tables.sql#L6) |
| `learning_plan` | `plan_id` UUID, `class_id` UUID, `summary` VARCHAR(1000), `version` INT, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.06__create_learning_plan_tables.sql:L22](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/progress/V1.06__create_learning_plan_tables.sql#L22) |
| `milestone` | `milestone_id` UUID, `plan_id` UUID, `title` VARCHAR(200), `description` TEXT, `target_date` DATE, `weight` SMALLINT, `status` VARCHAR(30), `achieved_date` DATE, `is_archived` BOOLEAN, `sort_order` INT, `created_at` TIMESTAMPTZ | [V1.06__create_learning_plan_tables.sql:L35](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/progress/V1.06__create_learning_plan_tables.sql#L35) |
| `plan_history` | `history_id` UUID, `plan_id` UUID, `milestone_id` UUID, `action` VARCHAR(40), `actor_user_id` UUID, `old_value` VARCHAR(500), `new_value` VARCHAR(500), `created_at` TIMESTAMPTZ | [V1.06__create_learning_plan_tables.sql:L56](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/progress/V1.06__create_learning_plan_tables.sql#L56) |
| `review` | `review_id` UUID, `tutor_id` UUID, `student_id` UUID, `reviewer_user_id` UUID, `reviewer_type` VARCHAR(20), `rating` SMALLINT, `comment` TEXT, `is_edited` BOOLEAN, `completed_sessions_at_review` INT, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ | [V1.07__create_review_and_notification_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/review/V1.07__create_review_and_notification_tables.sql#L6) |
| `notification` | `notification_id` UUID, `user_id` UUID, `about_student_id` UUID, `type` VARCHAR(60), `title` VARCHAR(200), `short_text` VARCHAR(500), `target_type` VARCHAR(40), `target_id` UUID, `is_read` BOOLEAN, `created_at` TIMESTAMPTZ | [V1.07__create_review_and_notification_tables.sql:L32](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/review/V1.07__create_review_and_notification_tables.sql#L32) |
| `tutoring_session` | `session_id` UUID, `class_id` UUID, `tutor_id` UUID, `student_id` UUID, `start_at` TIMESTAMPTZ, `end_at` TIMESTAMPTZ, `mode` VARCHAR(20), `place_or_link` VARCHAR(500), `repeat_note` VARCHAR(300), `message` VARCHAR(500), `status` VARCHAR(30), `proposed_by` UUID, `responded_by` UUID, `responded_at` TIMESTAMPTZ, `response_reason` VARCHAR(300), `expires_at` TIMESTAMPTZ, `cancelled_by` UUID, `cancelled_at` TIMESTAMPTZ, `cancel_reason` VARCHAR(40), `cancel_comment` VARCHAR(300), `is_late_cancel` BOOLEAN, `outcome_recorded_by` UUID, `outcome_recorded_at` TIMESTAMPTZ, `version` INT, `created_at` TIMESTAMPTZ | [V1.05__create_tutoring_session_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/scheduling/V1.05__create_tutoring_session_tables.sql#L6) |
| `session_reschedule_proposal` | `proposal_id` UUID, `session_id` UUID, `proposed_by` UUID, `new_start_at` TIMESTAMPTZ, `new_end_at` TIMESTAMPTZ, `reason` VARCHAR(300), `status` VARCHAR(30), `expires_at` TIMESTAMPTZ, `responded_by` UUID, `responded_at` TIMESTAMPTZ, `response_reason` VARCHAR(300), `created_at` TIMESTAMPTZ | [V1.05__create_tutoring_session_tables.sql:L63](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/scheduling/V1.05__create_tutoring_session_tables.sql#L63) |
| `session_history` | `history_id` UUID, `session_id` UUID, `reschedule_proposal_id` UUID, `action` VARCHAR(40), `actor_user_id` UUID, `reason` VARCHAR(300), `created_at` TIMESTAMPTZ | [V1.05__create_tutoring_session_tables.sql:L92](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/scheduling/V1.05__create_tutoring_session_tables.sql#L92) |
| `verification_request` | `request_id` UUID, `tutor_id` UUID, `status` VARCHAR(30), `rejection_reason` TEXT, `submitted_at` TIMESTAMPTZ, `reviewed_by` UUID, `reviewed_at` TIMESTAMPTZ | [V1.03__create_verification_tables.sql:L6](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/verification/V1.03__create_verification_tables.sql#L6) |
| `credential` | `credential_id` UUID, `request_id` UUID, `type` VARCHAR(30), `institution` VARCHAR(200), `year` SMALLINT, `note` VARCHAR(500) | [V1.03__create_verification_tables.sql:L26](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/verification/V1.03__create_verification_tables.sql#L26) |
| `credential_file` | `file_id` UUID, `credential_id` UUID, `file_name` VARCHAR(255), `storage_key` VARCHAR(500), `content_type` VARCHAR(100), `size_bytes` BIGINT, `uploaded_at` TIMESTAMPTZ | [V1.03__create_verification_tables.sql:L41](https://github.com/dvtrug/EduFit/blob/6755121122c79dc9705282225d14ffb76e17c29a/backend/app/src/main/resources/db/migration/verification/V1.03__create_verification_tables.sql#L41) |
| `connection_invite_quota` | `user_id` UUID, `window_started_at` TIMESTAMPTZ, `attempts` INT | [V1.11__allow_relink_after_revocation.sql:L10](https://github.com/dvtrug/EduFit/blob/77a9f87cf80dc39aac47d0bfafb5d16c5bb9869c/backend/app/src/main/resources/db/migration/connection/V1.11__allow_relink_after_revocation.sql#L10) |
