# Kế hoạch hoàn thành RBL Authentication — module IAM

## 1. Đích bàn giao

Phạm vi chỉ gồm business logic Authentication trong `backend/modules/iam`; chưa làm REST controller, Spring Security session/cookie, SMTP thật hoặc frontend.

Kết thúc kế hoạch phải có đủ ba sản phẩm cô yêu cầu:

1. Business logic Authentication chạy được trong `src/main`.
2. JUnit 5 test thật trong `src/test`, gồm mô tả phương thức và test case.
3. Báo cáo `docs/reports/iam-authentication-rbl-report.md` truy vết được `Rule -> Business logic -> Test case -> Test method -> Kết quả`.

Một rule chỉ được hoàn thành khi production code tồn tại, test kiểm tra đúng hành vi, test đã chạy thành công và báo cáo khớp code.

## 2. Phạm vi Business Rule

| Rule | Business logic phải chứng minh |
|---|---|
| BR-01 | Email được trim/lowercase và không trùng |
| BR-02 | Mật khẩu tối thiểu 8 ký tự, có chữ và số |
| BR-03 | Self-registration chỉ cho STUDENT, PARENT, TUTOR; cấm ADMIN |
| BR-04 | Account mới được kích hoạt trực tiếp ở trạng thái ACTIVE |
| BR-05 | Password reset token có hạn 30 phút và chỉ dùng một lần |
| BR-06 | Sai 5 lần khóa 15 phút; login thành công reset bộ đếm |
| NFR-01 | Mật khẩu được băm Argon2id với salt |
| NFR-03 | Chỉ lưu SHA-256 token hash 64 ký tự, không lưu raw token |
| NFR-06 | Không tiết lộ email tồn tại qua login/forgot password |
| NFR-11 | Consume token và đổi trạng thái account là nguyên tử |

Ma trận này là nguồn chuẩn. JavaDoc, `@DisplayName`, Test Case ID và báo cáo phải dùng đúng mã ở đây.

## 3. Trạng thái khởi đầu

IAM hiện có business logic đăng ký, đăng nhập, lockout và quản lý mật khẩu. Phạm vi không còn email verification; mọi account đăng ký hợp lệ được tạo ở trạng thái `ACTIVE`.

- Test hết thời gian khóa nói reset counter nhưng không assert việc reset.
- Token tại đúng `expiresAt` hiện vẫn được xem là hợp lệ.
- Một số token hash giả trong test không phải SHA-256 hex 64 ký tự.
- Báo cáo và test case phải phản ánh đúng luồng đăng ký kích hoạt trực tiếp.

## 4. Quy trình cho mỗi task

1. Chốt rule và test case.
2. Viết/chỉnh test, chạy và thấy fail đúng lý do.
3. Viết production code tối thiểu.
4. Chạy test riêng rồi toàn bộ IAM.
5. Cập nhật đúng dòng báo cáo RBL.

Không dùng `@Disabled`, test rỗng, `assertTrue(true)`, `Thread.sleep()`, mạng thật hoặc SMTP thật.

## 5. Các phase triển khai

### Phase 0 — Hiệu chỉnh nền tảng

#### RBL-00: Chuẩn hóa ma trận và báo cáo

**Nhiệm vụ:** Sửa mã BR/NFR, bỏ tuyên bố chưa có bằng chứng, phân biệt `Passed`, `Not Run`, `Not Implemented`.

**Acceptance criteria:** Mỗi test gắn đúng rule; báo cáo không nói PasswordPolicy/Testcontainers/Clock đã dùng khi code chưa có; Surefire result đúng lần chạy mới nhất.

**Dependencies:** Không. **Scope:** S.

#### RBL-01: Sửa Account/Token domain tests

**Test cases:**

- `IAM-DOM-007`: hết lockout được thử login; chỉ reset counter khi login thành công.
- `IAM-TOKEN-BOUNDARY-001`: `now == expiresAt` là hết hạn.
- `IAM-TOKEN-HASH-001`: từ chối hash không phải 64 ký tự hex.

**Acceptance criteria:** Ba trường hợp biên có test độc lập; toàn bộ test cũ vẫn pass sau hiệu chỉnh.

**Dependencies:** RBL-00. **Scope:** S.

### Checkpoint A

- Chạy `mvn -B -ntp -pl modules/iam -am test`.
- Không còn mô tả test khác assertion.

### Phase 1 — Password và Registration

#### RBL-02: PasswordPolicy (BR-02)

**Logic:** Từ chối null/rỗng, dưới 8 ký tự, thiếu chữ hoặc thiếu số.

**Tests:** Accept hợp lệ; reject 7 ký tự; chỉ chữ; chỉ số; null/rỗng.

**Acceptance criteria:** `PasswordPolicyTest` bao phủ biên, lỗi dùng `VALIDATION_FAILED`.

**Dependencies:** RBL-01. **Scope:** S.

#### RBL-03: RegisterAccountService (BR-01, BR-03, BR-04, NFR-01)

**Logic:** Normalize email -> validate role/password -> kiểm tra trùng -> hash Argon2 -> tạo account ACTIVE -> save.

**Tests:** Thành công cho ba public role; chặn ADMIN; email trùng; normalize trước lookup; password yếu; result không lộ hash.

**Acceptance criteria:** Test bằng fake/mock ports, không cần Spring/database.

**Dependencies:** RBL-02 và repository/password-hasher contracts. **Scope:** M.

### Checkpoint B

- BR-01 đến BR-04 và NFR-01 có test xanh.
- Chưa tạo HTTP endpoint hoặc gửi email thật.

### Phase 2 — Password reset token

#### RBL-04: Secure token generation/storage (BR-05, NFR-03)

**Logic:** Sinh raw token 256-bit, chỉ lưu SHA-256 hash; password reset TTL 30 phút.

**Tests:** Token khác nhau; hash deterministic 64 hex; không lưu raw; TTL đúng; expired/used bị từ chối.

**Acceptance criteria:** Production entity không có trường raw token.

**Dependencies:** RBL-01. **Scope:** M.

### Checkpoint C

- Password reset token có TTL 30 phút, dùng một lần và không lưu raw token.
- Review transaction boundary và dữ liệu nhạy cảm.

### Phase 3 — Authentication và lockout

#### RBL-06: AuthenticateAccountService (BR-04, BR-06, NFR-06)

**Logic:** Normalize email; cùng `INVALID_CREDENTIALS` cho email không tồn tại/sai password; từ chối non-active; kiểm tra lock; ghi failure; login đúng reset counter và cập nhật lastLogin.

**Tests:** Login đúng; sai email/sai password cùng lỗi; deactivated; failure 1-4; lần 5 khóa; trong khóa; đúng deadline; success reset.

**Acceptance criteria:** Chỉ trả identity an toàn, không tạo session, không lộ password hash.

**Dependencies:** RBL-03. **Scope:** M.

### Checkpoint D

- Register -> authenticate chạy qua service tests.
- BR-06 và NFR-06 có happy path và abuse path.

### Phase 4 — Password recovery/change

#### RBL-07: Request/Reset Password (BR-02, BR-05, NFR-06, NFR-11)

**Logic:** Forgot luôn neutral; reset kiểm tra token/policy, thay hash, consume token và revoke toàn bộ session.

**Tests:** Email có/không tồn tại cùng result; token invalid/expired/used; password yếu; reset thành công; revocation được gọi.

**Dependencies:** RBL-02, RBL-04. **Scope:** M.

#### RBL-08: Change Password (BR-02, NFR-01)

**Logic:** Xác minh mật khẩu hiện tại; chặn reuse; validate/hash mật khẩu mới; revoke các session khác.

**Tests:** Current sai; reuse; password yếu; success; hash đổi; revocation đúng chế độ.

**Dependencies:** RBL-02, RBL-06. **Scope:** M.

### Phase 5 — Persistence evidence

#### RBL-09: PostgreSQL integration tests

**Tests:** Unique normalized email; mapping round-trip; optimistic locking; double-consume token đồng thời chỉ một request thành công.

**Acceptance criteria:** Dùng PostgreSQL/Testcontainers, tên `*IT`, không thay bằng H2.

**Dependencies:** Schema/repository adapters và RBL-04. **Scope:** M.

### Phase 6 — Báo cáo bàn giao

#### RBL-10: Chốt báo cáo

**Nhiệm vụ:** Kiểm kê test thật, mô tả Arrange/Act/Expected, loại test và kết quả Surefire.

**Acceptance criteria:**

- Mọi dòng `Passed` trỏ tới class/method tồn tại.
- Mỗi rule có positive và negative/boundary test phù hợp.
- Tổng test bằng Surefire; không còn `Not Run` trong phạm vi nộp.
- Ghi rõ ngoài phạm vi: controller, session, SMTP, frontend.

**Dependencies:** RBL-00 đến RBL-09. **Scope:** S.

### Checkpoint cuối — Definition of Done

- Maven IAM test thành công; không failed/error/skipped/disabled.
- Ma trận `Rule -> Logic -> Test case -> Method -> Result` đầy đủ.
- Báo cáo chỉ tuyên bố điều code và test đã chứng minh.

## 6. Thứ tự từng buổi

1. RBL-00 + RBL-01.
2. RBL-02.
3. RBL-03 rồi review.
4. RBL-04.
5. RBL-06 rồi review.
6. RBL-07.
7. RBL-08.
8. RBL-09.
9. RBL-10.

## 7. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Báo cáo đi trước code | Chỉ ghi `Passed` từ Surefire thực tế |
| Entity test xanh nhưng flow sai | Bắt buộc có application-service tests |
| Race condition token | PostgreSQL atomic-consume integration test |
| Test phụ thuộc thời gian | Truyền `Instant`/`Clock`, không sleep |
| Lộ dữ liệu nhạy cảm | Assert DTO không có hash/raw token |
| Scope phình sang web | Giữ controller/session ngoài plan |
