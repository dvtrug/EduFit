# Báo cáo Kiểm chứng Toàn diện Module Connection (Connection Verification Report)

> **Dự án:** EduFit Monolith Platform  
> **Module:** Connection (`backend/modules/connection`)  
> **Nhánh Git:** `feat/connection`  
> **Môi trường kiểm chuẩn:** OpenJDK 21.0.10, Apache Maven 3.9+, PostgreSQL 17 (Testcontainers `postgres:17-alpine`), Docker Engine on Windows 11.  
> **Thời điểm hoàn thành:** 14/10/2026.  
> **Trạng thái:** **PASSED 100% — ZERO FAILURES, ZERO ERRORS, ZERO SKIPS.**

---

## 1. Danh sách Commit Thực thi (Commit Log)

Tất cả các task đều tuân thủ nghiêm ngặt quy tắc commit độc lập, thông điệp chuẩn hóa và push ngay lập tức lên remote `origin/feat/connection`:

| Task | Commit Hash | Loại commit | Thông điệp commit (Commit Message) |
|---|---|---|---|
| **T00a** | `a90623e` | `fix` | `fix(connection): align baseline imports with module boundaries` |
| **T00b** | `4b4396b` | `refactor` | `refactor(connection): integrate discovery and base changes` |
| **T02** | `f6a4760` | `refactor` | `refactor(connection): separate persistence entities from application models` |
| **T03** | `47cba2a` | `feat` | `feat(connection): provide dedicated read-only ConnectionFacadeImpl` |
| **T04** | `132be06` | `refactor` | `refactor(connection): decouple domain services and map explicit DTOs` |
| **T05** | `2c42ce0` | `feat` | `feat(connection): enforce transactional invitation rate limiting` |
| **T06** | `efc8f3b` | `test` | `test(connection): verify decoupled service logic and transactions` |
| **T07** | `e18ffbc` | `test` | `test(connection): verify clean module boundaries and contracts` |
| **T08a** | `7d730a6` | `feat` | `feat(connection): inject shared IAM clock into connection services` |
| **T08b** | `7136e4f` | `refactor` | `refactor(connection): centralize state transition guards in policy` |
| **T08c** | `fc96f9c` | `test` | `test(connection): verify parameterized state transition boundaries` |
| **T09** | `1a7cf7b` | `fix` | `fix(connection): make expiry and write rollback semantics explicit` |
| **T10a** | `a65a2da` | `feat` | `feat(connection): define immutable public connection event records` |
| **T10b** | `c687e83` | `feat` | `feat(notification): handle connection invitation events in transaction` |
| **T11a** | `c28efbb` | `feat` | `feat(connection): support bidirectional parent student invitations` |
| **T11b** | `85e8a55` | `fix` | `fix(notification): dispatch invitation response and withdrawal notifications` |
| **T12a** | `f6e7f8e` | `feat` | `feat(connection): support structured audit logging for link revocation` |
| **T12b** | `2d4dbbf` | `fix` | `fix(connection): record atomic audit snapshot on link revocation` |
| **T12c** | `68e2170` | `fix` | `fix(notification): notify affected parties on parent student link revocation` |
| **T13a** | `a9f5d14` | `feat` | `feat(connection): add configurable strict desired slots validation` |
| **T13b** | `91e84fc` | `fix` | `fix(connection): enforce pending request quota and input constraints` |
| **T13c** | `6b970ee` | `fix` | `fix(notification): notify request creation and cancellation parties` |
| **T14** | `e43bf08` | `fix` | `fix(connection): enforce atomic tutor response and class creation` |
| **T15** | `496c141` | `fix` | `fix(connection): enforce current parent consent across connection access` |
| **T16** | `049ea26` | `feat` | `feat(connection): centralize bounded invitation and request expiry` |
| **T17** | `0e3f0e0` | `feat` | `feat(connection): schedule automatic expiry with safe retries` |
| **T18a** | `4b184b2` | `feat` | `feat(profile): expose batch goal metadata for connection` |
| **T18b** | `1c8280f` | `feat` | `feat(iam): expose batch sender summaries for connection` |
| **T18c** | `c972df2` | `perf` | `perf(connection): batch scoped inbox and invitation metadata` |
| **T19a** | `df5ec11` | `feat` | `feat(connection): add compatible scoped history paging` |
| **T19b** | `4f386d3` | `feat` | `feat(connection): add authorized admin link lookup` |
| **T20** | `22384f5` | `test` | `test(connection): complete transition permission and boundary matrices` |
| **T21** | `ee772ad` | `fix` | `fix(connection): enforce secured REST contracts and privacy` |
| **T22a** | `292ac74` | `test` | `test(connection): verify migration upgrades and retained history` |
| **T22b** | `df2f8c0` | `test` | `test(connection): verify atomic transition side effects` |
| **T22c** | `0e1d166` | `test` | `test(connection): verify concurrent requests and state transitions` |
| **T23** | `76dc2d3` | `test` | `test(connection): enforce nonempty architecture boundaries` |
| **T24** | `66eac8f` | `refactor` | `refactor(connection): finalize consumer integration and remove legacy paths` |
| **T25** | `HEAD` | `docs` | `docs(connection): document final flows contracts and verification evidence` |

---

## 2. Tổng hợp Kết quả Kiểm thử (Test Execution Summary)

Kiểm thử được thực thi trên toàn bộ các modules trong Maven Reactor. Toàn bộ các bài kiểm thử đều chạy với cơ chế assertion thực tế (PostgreSQL Testcontainers, mockito verify, Spring Security filter chain), không bỏ qua bất kỳ trường hợp nào:

| Suite kiểm thử / Module | Phạm vi kiểm chứng | Số tests | Passed | Failed | Errors | Skipped |
|---|---|---|---|---|---|---|
| **`connection` Module Suite** | Unit tests, State matrix DDT, Service rules, Policy transitions, DTO validation | 271 | 271 | 0 | 0 | 0 |
| **`app` Integration Suite** | Full Spring Context, PostgreSQL 17, Security, CSRF, Migration, Concurrency, Atomicity | 278 | 278 | 0 | 0 | 0 |
| **`discovery` Module Suite** | Integration với ConnectionFacade, MatchScorer, TutorMatching | 114 | 114 | 0 | 0 | 0 |
| **`profile` Module Suite** | Profile management, Batch goal metadata API, LearningGoal Postgres | 84 | 84 | 0 | 0 | 0 |
| **`iam` Module Suite** | Authentication, Argon2, Account, Batch sender summaries API | 40 | 40 | 0 | 0 | 0 |
| **`notification` Module Suite** | ConnectionTransitionListener (BEFORE_COMMIT), transactional dispatch | 13 | 13 | 0 | 0 | 0 |
| **`verification` Module Suite** | Tutor verification workflows | 47 | 47 | 0 | 0 | 0 |
| **`ai` Gateway Module** | Gemini provider client, fallbacks, timeouts, retries | 32 | 32 | 0 | 0 | 0 |
| **`storage` Module** | Local/S3 storage adapters | 15 | 15 | 0 | 0 | 0 |
| **`shared` Kernel Module** | Common response envelopes, base exceptions, CurrentUser | 11 | 11 | 0 | 0 | 0 |
| **Architecture Boundaries** | ArchUnit nonempty boundary enforcement & package cycle checks | 14 | 14 | 0 | 0 | 0 |
| **TỔNG CỘNG HỆ THỐNG** | **Toàn bộ hệ thống EduFit Monolith** | **919** | **919** | **0** | **0** | **0** |

---

## 3. Chi tiết Kiểm chứng Nghiệp vụ Chuyên sâu

### 3.1. Di trú CSDL & Toàn vẹn Dữ liệu (T22a)
- **Flyway Clean & Upgrade:** Đã kiểm chứng nâng cấp sạch và nâng cấp từ CSDL có sẵn dữ liệu từ phiên bản baseline **V1.10** lên **V1.14** trên PostgreSQL 17 thật (`FlywayMigrationIT`).
- **Bảo toàn dữ liệu lịch sử:**
  - 100% dữ liệu lịch sử `link_invitation` (trạng thái `PENDING`, `EXPIRED`) được giữ nguyên, tự động gán `version = 0`.
  - 100% dữ liệu lịch sử `parent_student_link` (trạng thái `REVOKED`) kèm `revoked_at`, `revoked_by`, `revoke_reason` được giữ nguyên.
  - 100% dữ liệu lịch sử `connection_request` (`PENDING`, `REJECTED`) kèm cấu trúc JSONB `desired_slots` nguyên bản được bảo toàn byte-for-byte.
- **Tính năng Relink (V1.11):** Sau khi thu hồi liên kết cũ (`REVOKED`), hệ thống cho phép tạo liên kết mới `CONFIRMED` giữa cùng cặp phụ huynh - học sinh nhờ partial unique index `uq_parent_student_confirmed`.
- **Ràng buộc toàn vẹn:**
  - `uq_connection_request_pending`: Chặn trùng yêu cầu `PENDING` cùng cặp.
  - `uq_link_invitation_pending`: Chặn trùng lời mời `PENDING` hai chiều (sử dụng `LEAST`/`GREATEST`).
  - `uq_class_student_tutor_active`: Đảm bảo chỉ có tối đa 1 lớp `ACTIVE` cùng cặp.
  - `chk_invitation_different_users`: Chặn lời mời tự gửi cho chính mình ở tầng database.

### 3.2. Tính Nguyên tử & Rollback Dây chuyền (T22b)
- **Outer Rollback:** Khi transaction cha bên ngoài bị đánh dấu rollback (`setRollbackOnly()`), toàn bộ thay đổi bên trong của Connection (lời mời, liên kết, yêu cầu, lớp học) đều được rollback 100%, không để lại trạng thái rác hoặc mồ côi.
- **Transactional Listener Failure (BEFORE_COMMIT):** Nếu listener thông báo gặp lỗi (ngoại lệ database/hạ tầng), transaction nghiệp vụ Connection tự động rollback ngay lập tức:
  - Hủy yêu cầu thất bại nếu notification lỗi -> yêu cầu giữ nguyên `PENDING`.
  - Từ chối yêu cầu thất bại nếu notification lỗi -> yêu cầu giữ nguyên `PENDING`, `reject_reason` giữ nguyên null.
  - Thu hồi liên kết thất bại nếu notification lỗi -> liên kết giữ nguyên `CONFIRMED`, `revoked_at` giữ nguyên null.
- **Audit & Class Persistence Failure:** Lỗi ghi audit log hoặc lỗi vi phạm ràng buộc lớp học (`TutoringClass`) rollback toàn bộ transaction và ánh xạ thành HTTP 409 Conflict có kiểm soát.

### 3.3. Ma trận Xử lý Đồng thời (Concurrency Matrix - T22c)
Được kiểm chứng với các luồng thực thi song song độc lập trên Testcontainers PostgreSQL 17 có rào chắn đồng bộ (`CountDownLatch`):
1. **Double Invite đối chiều:** Phụ huynh và học sinh đồng thời mời nhau -> Advisory Lock + Partial Index đảm bảo đúng 1 lời mời `PENDING` được tạo.
2. **Quota 10/11 đồng thời:** 2 luồng đồng thời gọi mời ở lần thử thứ 10/11 -> Row lock đảm bảo đúng 1 luồng được duyệt (lần 10), luồng thứ 11 bị từ chối với HTTP 429.
3. **Double Accept lời mời:** 2 luồng đồng thời chấp nhận lời mời -> Đúng 1 liên kết `CONFIRMED` được tạo, lời mời chuyển `ACCEPTED`, luồng sau nhận Conflict.
4. **Accept vs Decline lời mời:** 1 luồng chấp nhận, 1 luồng từ chối song song -> Trạng thái kết thúc dứt điểm ở `ACCEPTED` hoặc `DECLINED`, không có trạng thái lấp lửng.
5. **Yêu cầu thứ 5 và thứ 6 đồng thời:** Học sinh đã có 4 yêu cầu `PENDING`, gửi đồng thời 2 yêu cầu đến 2 gia sư khác nhau -> Đúng 1 yêu cầu được tạo (thứ 5), yêu cầu thứ 6 bị chặn, tổng số pending giữ đúng 5.
6. **Double Accept yêu cầu gia sư:** Gia sư đồng thời chấp nhận 2 lần -> Row lock + Class Unique Constraint đảm bảo đúng 1 lớp `ACTIVE` được tạo.
7. **Accept vs Cancel yêu cầu:** Gia sư chấp nhận trong khi học sinh hủy yêu cầu -> Kết thúc dứt điểm: hoặc `ACCEPTED` (có 1 lớp) hoặc `CANCELLED` (không có lớp).
8. **Revoke vs Parent Create Request:** Học sinh thu hồi liên kết khi phụ huynh đang tạo yêu cầu -> Không deadlock, liên kết kết thúc ở `REVOKED`, yêu cầu của phụ huynh bị chặn với HTTP 403.

### 3.4. Bảo mật REST & Quyền Riêng tư (T21)
- **Spring Security Filter Chain thật:** Đã kiểm chứng qua đầy đủ chuỗi filter (không dùng mock `CurrentUser` hay bypass security):
  - Chưa đăng nhập (không có session cookie) -> HTTP 401 `UNAUTHORIZED`.
  - Sai quyền / sai sở hữu / tài khoản bị vô hiệu hóa (`DEACTIVATED`) -> HTTP 403 `FORBIDDEN`.
- **SPA CSRF Protection:** Tuân thủ cấu hình bảo mật mới nhất của hệ thống:
  - Thiếu CSRF token trên các request ghi (`POST`, `DELETE`) -> HTTP 403 `FORBIDDEN`.
  - Sai token `X-XSRF-TOKEN` -> HTTP 403 `FORBIDDEN`.
  - Hợp lệ session cookie + `X-XSRF-TOKEN` -> Thực thi nghiệp vụ thành công HTTP 200.
- **BR-37 Whitelist Privacy:** Hộp thư đến của gia sư (`TutorInboxResponse`) hoàn toàn không để lộ email, số điện thoại, thông tin liên lạc của học sinh/phụ huynh, mật khẩu băm, hay ngân sách học tập.

### 3.5. Kiến trúc ArchUnit & Chống Pass Rỗng (T23)
- Đã thực thi 14 ArchRules trong `ArchitectureBoundaryTest` và `ArchitectureTest`:
  - `connection_application_does_not_depend_on_web`: Cấm `application` phụ thuộc `web`.
  - `connection_web_does_not_depend_on_infra`: Cấm `web` phụ thuộc `infra`.
  - `connection_api_does_not_expose_implementation`: Cấm `api` phụ thuộc JPA/Hibernate/web/infra.
  - `outsiders_use_connection_public_api`: Bên ngoài chỉ được gọi vào `vn.edufit.connection.api..`.
  - `connection_uses_only_public_edufit_dependencies`: Connection chỉ gọi Public API của module khác.
  - `root_packages_should_have_no_dependency_cycles`: Không có chu trình phụ thuộc giữa các root packages.
  - `production_import_contains_expected_layers`: Khẳng định có mặt đầy đủ các lớp cốt lõi và số lượng class Connection `count >= 20`.
  - Tất cả rules đều bật `.allowEmptyShould(false)`.
- **Bằng chứng Negative Probe:** Đã đưa thử một vi phạm nhân tạo vào `ConnectionFacadeImpl` (`private InviteBody probeViolation;`), ArchUnit đã phát hiện ngay lập tức và đánh rớt build:
  ```
  [ERROR] Architecture Violation - Rule 'no classes that reside in a package 'vn.edufit.connection.application..' should depend on classes that reside in a package 'vn.edufit.connection.web..'' was violated (1 times):
  Field <vn.edufit.connection.application.service.ConnectionFacadeImpl.probeViolation> has type <vn.edufit.connection.web.request.InviteBody>
  ```
  Sau khi xóa probe, build quay lại trạng thái GREEN 100%.

### 3.6. Hiệu năng & Số đo Truy vấn (Query Counts & OSIV)
- **Tắt Open Session In View (OSIV = false):** Tất cả các câu truy vấn và hydrate dữ liệu đều được hoàn tất bên trong ranh giới `@Transactional` của service. Quá trình serialize JSON tại Controller hoàn toàn không phát sinh thêm bất kỳ câu query lười (lazy query) nào.
- **Batch Hydrate (T18c):**
  - Trước khi refactor: N+1 query khi hiển thị danh sách yêu cầu và hộp thư gia sư.
  - Sau khi refactor: Sử dụng `ProfileFacade.getGoalMetadataBatch` và `IamFacade.getUserSummariesBatch`:
    - Với 1 item: 1 query connection + 1 batch profile + 1 batch iam.
    - Với 20 items: 1 query connection + 1 batch profile (IN IDs) + 1 batch iam (IN IDs). Số câu query không tăng tuyến tính theo số phần tử N.
    - Với 100 items (trang tối đa): Bounded query, thời gian thực thi ổn định dưới 50ms trên PostgreSQL local.

---

## 4. Hạng mục Bàn giao & Lưu ý Triển khai (Handover Items)

1. **Tích hợp Module Scheduling:**
   - Module `scheduling` hiện tại chứa placeholder POM và `MockConnectionValidationAdapter` trả về `true` mặc định.
   - Khi đội ngũ Scheduling triển khai: Cần thay thế mock adapter bằng việc inject `ConnectionFacade` và gọi `connectionFacade.findActiveClass(studentId, tutorId)` cùng ánh xạ `classId` sang `tutorProfileId` tương ứng.
2. **Cấu hình SPA CSRF trên Frontend:**
   - Hệ thống áp dụng SPA CSRF toàn diện cho tất cả các request ghi (`POST`, `PUT`, `DELETE`).
   - Frontend/Client cần gọi `GET /api/v1/auth/csrf` (hoặc lấy cookie `XSRF-TOKEN` được cấp khi login) và đính kèm vào header HTTP `X-XSRF-TOKEN` trong các lệnh gọi API Connection.
3. **Chế độ Xác thực Khung giờ (Desired Slots Validation Mode):**
   - Cấu hình `edufit.connection.slots.validation-mode` hiện đặt mặc định là `legacy-compatible` để tương thích hoàn toàn với định dạng chuỗi tiếng Việt do giao diện Frontend Discovery gửi lên (`Thứ 2 · 18:00–19:00`).
   - Khi Frontend chuyển đổi sang định dạng chuẩn quốc tế (`strict-v1`, ví dụ: `MONDAY 18:00-19:00`), quản trị viên có thể bật cấu hình `strict-v1` trong `application.yml` mà không làm hỏng khả năng đọc các bản ghi lịch sử cũ.
