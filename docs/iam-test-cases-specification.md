# ĐẶC TẢ CHI TIẾT TEST CASES & TRUY VẾT QUY TẮC NGHIỆP VỤ (BR & NFR) — PHÂN HỆ IAM

- **Dự án:** EduFit — Nền tảng kết nối Gia sư và Học sinh/Phụ huynh (SWP391)
- **Module:** `backend/modules/iam` (Identity & Access Management)
- **Tài liệu tham chiếu:** [SRS EduFit](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/SRS%20Edufit.docx), [ADR-001](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/ADR-001-tech-stack.md), [ADR-002](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/ADR-002-multi-module-architecture.md), [authentication-contract.md](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/authentication-contract.md)
- **Phiên bản:** 2.0 (Cập nhật luồng đăng ký kích hoạt trực tiếp `ACTIVE`)

---

## 1. Chi Tiết Các Quy Tắc Nghiệp Vụ (Business Rules - BR)

Các quy tắc nghiệp vụ định nghĩa logic hành vi bắt buộc mà phân hệ IAM phải thực thi:

### BR-01: Chuẩn hóa & Tính duy nhất của Email (Email Normalization & Uniqueness)
* **Nội dung quy tắc:** 
  * Email là định danh duy nhất của người dùng trong hệ thống.
  * Trước khi lưu trữ hoặc tìm kiếm, chuỗi email bắt buộc phải được cắt bỏ khoảng trắng thừa ở hai đầu (`trim()`) và chuyển đổi toàn bộ về ký tự thường không phân biệt ngữ cảnh địa phương (`toLowerCase(Locale.ROOT)`).
  * Hệ thống không phân biệt chữ hoa, chữ thường khi kiểm tra trùng lặp (ví dụ: `User@EduFit.vn` và `user@edufit.vn` được xem là cùng một tài khoản).
  * Nghiêm cấm tồn tại 2 tài khoản trùng normalized email trong cơ sở dữ liệu.
* **Mã lỗi phát sinh khi vi phạm:** `USER_ALREADY_EXISTS` (HTTP 409 Conflict).

### BR-02: Chính sách Mật khẩu (Password Policy)
* **Nội dung quy tắc:**
  * Mật khẩu người dùng nhập vào phải thỏa mãn đồng thời 3 điều kiện:
    1. Độ dài tối thiểu **8 ký tự**.
    2. Chứa ít nhất **1 ký tự chữ cái** (`a-z`, `A-Z`).
    3. Chứa ít nhất **1 ký tự chữ số** (`0-9`).
  * Khi đổi mật khẩu hoặc đặt lại mật khẩu, mật khẩu mới **không được trùng** với mật khẩu hiện tại/gần nhất.
* **Mã lỗi phát sinh khi vi phạm:** 
  * `VALIDATION_FAILED` (HTTP 400 Bad Request) — nếu mật khẩu yếu.
  * `PASSWORD_REUSE_FORBIDDEN` (HTTP 400 Bad Request) — nếu trùng mật khẩu cũ.

### BR-03: Giới hạn Vai trò khi Đăng ký Tự do (Role Restriction)
* **Nội dung quy tắc:**
  * Người dùng đăng ký công khai qua form giao diện chỉ được phép chọn một trong ba vai trò hợp lệ: `STUDENT` (Học sinh), `PARENT` (Phụ huynh), hoặc `TUTOR` (Gia sư).
  * **Tuyệt đối cấm** việc tự đăng ký tài khoản với vai trò Quản trị viên (`ADMIN`). Quyền ADMIN chỉ được cấp thông qua cơ chế seed dữ liệu nội bộ hoặc phân quyền quản trị cấp cao.
  * Vai trò được gán cố định khi tài khoản được tạo và không thể tự ý thay đổi.
* **Mã lỗi phát sinh khi vi phạm:** `FORBIDDEN` (HTTP 403 Forbidden).

### BR-04: Trạng thái Tài khoản khi Đăng ký (Initial Account Status)
* **Nội dung quy tắc:**
  * Tài khoản sau khi đăng ký thành công qua API `POST /api/v1/auth/register` sẽ được kích hoạt trực tiếp ở trạng thái **`ACTIVE`**.
  * Người dùng có thể tiến hành đăng nhập vào hệ thống ngay lập tức mà không cần qua bước kích hoạt token qua email.
  * Tài khoản ở trạng thái `ACTIVE` có đầy đủ quyền hạn truy cập các chức năng tương ứng với vai trò của mình.
* **Mã lỗi liên quan:** `USER_NOT_ACTIVE` (HTTP 400) — nếu tài khoản bị vô hiệu hóa (`DEACTIVATED`).

### BR-05: Vòng đời Token Dùng Một Lần (One-Time Token Lifecycle)
* **Nội dung quy tắc:**
  * Áp dụng cho chức năng Quên và Đặt lại mật khẩu (Password Reset):
    * Thời gian sống (TTL) của token đặt lại mật khẩu là **30 phút**.
    * Token chỉ được sử dụng duy nhất **1 lần** (đánh dấu `used_at = now` ngay khi được tiêu thụ).
    * Khi người dùng yêu cầu gửi lại mã mới, toàn bộ các token cùng loại chưa dùng trước đó của tài khoản sẽ bị vô hiệu hóa ngay lập tức.
* **Mã lỗi phát sinh khi vi phạm:**
  * `INVALID_TOKEN` (HTTP 400) — token không tồn tại hoặc sai định dạng.
  * `TOKEN_EXPIRED` (HTTP 400) — token đã quá thời hạn 30 phút.
  * `TOKEN_ALREADY_USED` (HTTP 400) — token đã được tiêu thụ trước đó.

### BR-06: Quy tắc Khóa Đăng nhập Tạm thời (Account Lockout Rule)
* **Nội dung quy tắc:**
  * Nhằm ngăn chặn tấn công dò quét mật khẩu Brute-force:
    * Hệ thống duy trì bộ đếm `failed_login_attempts`.
    * Mỗi lần người dùng nhập sai mật khẩu của một tài khoản có thật, bộ đếm tăng thêm 1.
    * Khi sai liên tiếp **5 lần**, tài khoản bị khóa tạm thời trong vòng **15 phút** (`locked_until = now + 15 minutes`).
    * Trong thời gian khóa, mọi nỗ lực đăng nhập (dù đúng hay sai mật khẩu) đều bị từ chối ngay lập tức.
    * Sau khi hết 15 phút, người dùng được phép thử lại; nếu đăng nhập thành công, bộ đếm lỗi được reset về `0` và thời gian khóa được xóa bỏ (`lockedUntil = null`).
* **Mã lỗi phát sinh khi vi phạm:** `ACCOUNT_LOCKED` (HTTP 400 Bad Request).

---

## 2. Chi Tiết Yêu Cầu Phi Chức Năng (Non-Functional Requirements - NFR)

Các yêu cầu phi chức năng thiết lập tiêu chuẩn kỹ thuật về an toàn thông tin, bảo mật và tính toàn vẹn:

### NFR-01: Lưu trữ Mật khẩu An toàn Bậc cao (Memory-Hard Password Hashing)
* **Chi tiết kỹ thuật:**
  * Mật khẩu tuyệt đối không được lưu dưới dạng văn bản thô (Plaintext).
  * Bắt buộc sử dụng thuật toán băm **Argon2id** (thuật toán đoạt giải Password Hashing Competition) với cấu hình bộ nhớ cao:
    * `memory`: **65,536 KiB (64 MiB RAM)**
    * `iterations`: **3 vòng lặp**
    * `parallelism`: **1 luồng**
    * Tự động sinh Salt ngẫu nhiên bảo mật 16 bytes cho mỗi mật khẩu.
  * Ngăn ngừa hoàn toàn việc tấn công bằng phần cứng chuyên dụng (GPU/ASIC) và bảng Rainbow Table. Mật khẩu thô không bao giờ được in ra console log, file log hay exception message.

### NFR-03: Lưu trữ Token An toàn (Zero Plaintext Token Storage)
* **Chi tiết kỹ thuật:**
  * Token thô 256-bit sinh ngẫu nhiên bằng `SecureRandom` chỉ gửi một lần duy nhất qua email của người dùng.
  * Cơ sở dữ liệu tuyệt đối **không lưu raw token**. CSDL chỉ lưu chuỗi băm **SHA-256** dạng Hex (chuẩn 64 ký tự).
  * Khi người dùng gửi token lên qua API, hệ thống tính toán chuỗi băm SHA-256 của chuỗi gửi lên rồi mới truy vấn CSDL. Dù CSDL có bị rò rỉ, kẻ tấn công cũng không thể tái tạo lại token hợp lệ.

### NFR-04: Quản lý Phiên Đăng nhập An toàn (Session Management Policy)
* **Chi tiết kỹ thuật:**
  * Sử dụng cơ chế Server-side Stateful Session lưu tại PostgreSQL (`SPRING_SESSION`).
  * Định danh phiên gửi qua Cookie `EDUFIT_SESSION` với cờ bảo mật:
    * `HttpOnly = true` (Javascript trình duyệt không thể truy cập, chống XSS lấy cắp session).
    * `SameSite = Lax` (Bảo vệ chống tấn công CSRF cross-origin).
    * `Path = /`
  * Thời hạn phiên: Hết hạn sau **60 phút không có thao tác** (Sliding inactivity).
  * Khi đổi mật khẩu hoặc reset mật khẩu thành công: Kích hoạt thu hồi toàn bộ các phiên làm việc đang hoạt động khác.

### NFR-06: Chống Dò quét Tài khoản (Account Enumeration & Timing Resistance)
* **Chi tiết kỹ thuật:**
  * Khi đăng nhập thất bại: Luôn trả về thông báo chung `INVALID_CREDENTIALS` mà không nói rõ là "sai email" hay "sai mật khẩu".
  * **Timing Attack Defense:** Nếu email người dùng nhập không tồn tại trong hệ thống, hệ thống vẫn thực thi một hàm băm giả lập (`dummy verify`) với độ trễ tương đương để kẻ tấn công không thể dựa vào thời gian phản hồi để biết email có tồn tại hay không.
  * Khi yêu cầu quên mật khẩu: Luôn trả về phản hồi trung tính thành công (`200 OK`) dù email đó có tồn tại hay không.

### NFR-11: Tính Toàn vẹn Giao dịch (Transaction Atomicity)
* **Chi tiết kỹ thuật:**
  * Các thao tác nghiệp vụ phức tạp phải được bọc trong một `@Transactional` duy nhất:
    * Tiêu thụ token reset mật khẩu và cập nhật mật khẩu mới băm bằng Argon2id.
    * Nếu xảy ra bất kỳ lỗi runtime nào giữa chừng, toàn bộ giao dịch cơ sở dữ liệu phải được Rollback nguyên trạng (ACID).

---

## 3. Danh Mục Chi Tiết Toàn Bộ Test Cases

Dưới đây là chi tiết từng ca kiểm thử, dữ liệu đầu vào, các bước thực hiện, kết quả mong đợi và mã nguồn Java tương ứng:

---

### Nhóm 1: Kiểm thử Thực thể & Logic Miền Tài khoản (`AccountTest.java`)

#### Test Case 1: `IAM-DOM-001`
* **Rule liên quan:** BR-04
* **Mục tiêu:** Kiểm tra tài khoản tạo mới mặc định có trạng thái `ACTIVE`, email được chuẩn hóa và các bộ đếm bằng 0.
* **Dữ liệu đầu vào:**
  * Email: `"Student@EduFit.vn "` (có khoảng trắng thừa và chữ hoa)
  * PasswordHash: `"$argon2id$v=19$m=65536,t=3,p=1$..."`
  * FullName: `" Nguyen Van A "`
  * Role: `AccountRole.STUDENT`
* **Các bước thực hiện:**
  1. Gọi hàm `Account.create("Student@EduFit.vn ", hash, " Nguyen Van A ", AccountRole.STUDENT)`.
  2. Kiểm tra các giá trị thuộc tính của đối tượng tạo ra.
* **Kết quả mong đợi:**
  * `account.getEmail()` bằng `"student@edufit.vn"`.
  * `account.getFullName()` bằng `"Nguyen Van A"`.
  * `account.getStatus()` bằng `AccountStatus.ACTIVE`.
  * `account.getFailedLoginAttempts()` bằng `0`.
  * `account.getLockedUntil()` mang giá trị `null`.
* **Method trong code:** [`AccountTest.shouldInitializeAccountInActiveStatus()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L23-L37)
* **Trạng thái:** **PASSED**

#### Test Case 2: `IAM-DOM-002`
* **Rule liên quan:** BR-03
* **Mục tiêu:** Chặn tuyệt đối hành vi tạo tài khoản với vai trò `ADMIN`.
* **Dữ liệu đầu vào:** Role: `AccountRole.ADMIN`.
* **Các bước thực hiện:**
  1. Gọi `Account.create("admin@edufit.vn", "hash...", "Admin", AccountRole.ADMIN)`.
* **Kết quả mong đợi:**
  * Bắt được ngoại lệ `InvalidOperationException`.
  * `ex.getErrorCode()` bằng `ErrorCode.FORBIDDEN`.
* **Method trong code:** [`AccountTest.shouldPreventAdminRoleRegistration()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L40-L51)
* **Trạng thái:** **PASSED**

#### Test Case 3: `IAM-DOM-005`
* **Rule liên quan:** BR-06
* **Mục tiêu:** Tăng bộ đếm khi đăng nhập sai từ 1 đến 4 lần; khóa tài khoản 15 phút ở lần sai thứ 5.
* **Dữ liệu đầu vào:** Thời điểm hiện tại `now = Instant.parse("2026-09-30T10:00:00Z")`.
* **Các bước thực hiện:**
  1. Tạo tài khoản `Account`.
  2. Gọi `account.recordLoginFailure(now)` 4 lần liên tiếp.
  3. Kiểm tra số lần sai và cờ khóa.
  4. Gọi `account.recordLoginFailure(now)` lần thứ 5.
* **Kết quả mong đợi:**
  * Sau 4 lần sai: `failedLoginAttempts == 4`, `lockedUntil == null`, `isLocked(now) == false`.
  * Sau lần 5: `failedLoginAttempts == 5`, `lockedUntil == 2026-09-30T10:15:00Z`, `isLocked(now) == true`.
* **Method trong code:** [`AccountTest.shouldIncrementFailedAttemptsAndLockAccountAtFifthFailure()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L85-L105)
* **Trạng thái:** **PASSED**

#### Test Case 4: `IAM-DOM-006`
* **Rule liên quan:** BR-06
* **Mục tiêu:** Từ chối quyền đăng nhập (`canAuthenticate() == false`) khi tài khoản đang trong thời gian bị khóa.
* **Các bước thực hiện:**
  1. Khóa tài khoản tại thời điểm `now`.
  2. Kiểm tra `canAuthenticate(now.plusMinutes(5))`.
* **Kết quả mong đợi:** Trả về `false`.
* **Method trong code:** [`AccountTest.shouldDenyLoginWhenAccountIsCurrentlyLocked()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L108-L125)
* **Trạng thái:** **PASSED**

#### Test Case 5: `IAM-DOM-007`
* **Rule liên quan:** BR-06
* **Mục tiêu:** Khi đã hết 15 phút khóa, tài khoản được phép thử đăng nhập lại.
* **Các bước thực hiện:**
  1. Khóa tài khoản lúc 10:00.
  2. Kiểm tra tại thời điểm 10:16 (sau 16 phút).
* **Kết quả mong đợi:** `account.isLocked(afterLock)` trả về `false`.
* **Method trong code:** [`AccountTest.shouldAllowLoginAndResetCounterAfterLockExpires()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L128-L148)
* **Trạng thái:** **PASSED**

#### Test Case 6: `IAM-DOM-008`
* **Rule liên quan:** BR-06
* **Mục tiêu:** Đăng nhập thành công xóa sạch bộ đếm lỗi và cập nhật `lastLoginAt`.
* **Các bước thực hiện:**
  1. Cho tài khoản nhập sai 3 lần (`failedAttempts = 3`).
  2. Gọi `account.recordSuccessfulLogin(now)`.
* **Kết quả mong đợi:**
  * `failedLoginAttempts == 0`.
  * `lockedUntil == null`.
  * `lastLoginAt == now`.
* **Method trong code:** [`AccountTest.shouldResetFailedAttemptsOnSuccessfulLogin()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java#L151-L168)
* **Trạng thái:** **PASSED**

---

### Nhóm 2: Kiểm thử Chính Sách Mật Khẩu (`PasswordPolicyTest.java`)

#### Test Case 7: `IAM-DOM-009`
* **Rule liên quan:** BR-02
* **Mục tiêu:** Chấp nhận các mật khẩu hợp lệ (>= 8 ký tự, có cả chữ và số).
* **Dữ liệu thử nghiệm:** `"Password123@"`, `"Abc12345"`, `"StrongPass1"`.
* **Kết quả mong đợi:** Không có ngoại lệ nào phát sinh (`assertDoesNotThrow`).
* **Method trong code:** [`PasswordPolicyTest.shouldAcceptValidPassword()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordPolicyTest.java#L22-L28)
* **Trạng thái:** **PASSED**

#### Test Case 8: `IAM-DOM-010`
* **Rule liên quan:** BR-02
* **Mục tiêu:** Từ chối các mật khẩu rỗng, khoảng trắng hoặc dưới 8 ký tự.
* **Dữ liệu thử nghiệm:** `""`, `"   "`, `"short1"`, `"1234567"`, `"Abcdefg"`.
* **Kết quả mong đợi:** Ném `InvalidOperationException` kèm `ErrorCode.VALIDATION_FAILED`.
* **Method trong code:** [`PasswordPolicyTest.shouldRejectShortOrBlankPassword()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordPolicyTest.java#L30-L40)
* **Trạng thái:** **PASSED**

#### Test Case 9: `IAM-DOM-011`
* **Rule liên quan:** BR-02
* **Mục tiêu:** Từ chối mật khẩu dài >= 8 ký tự nhưng chỉ toàn chữ hoặc chỉ toàn số.
* **Dữ liệu thử nghiệm:** `"OnlyLettersLong"`, `"1234567890"`.
* **Kết quả mong đợi:** Ném `InvalidOperationException` kèm `ErrorCode.VALIDATION_FAILED`.
* **Method trong code:** [`PasswordPolicyTest.shouldRejectPasswordWithoutDigitsOrLetters()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordPolicyTest.java#L42-L58)
* **Trạng thái:** **PASSED**

---

### Nhóm 3: Kiểm thử One-Time Token (`AccountTokenTest.java`)

#### Test Case 10: `IAM-DOM-012`
* **Rule liên quan:** BR-05, NFR-03
* **Mục tiêu:** Khởi tạo token với mã băm SHA-256 (64 ký tự hex) và hạn dùng chuẩn.
* **Dữ liệu đầu vào:**
  * Raw token hash: 64 ký tự hex hợp lệ.
  * TTL: 30 phút đối với `PASSWORD_RESET`.
* **Kết quả mong đợi:** `token.isValid(now)` trả về `true`, `usedAt` là `null`.
* **Method trong code:** [`AccountTokenTest.shouldCreateValidTokenWithCorrectExpiry()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTokenTest.java#L24-L48)
* **Trạng thái:** **PASSED**

#### Test Case 11: `IAM-DOM-013`
* **Rule liên quan:** BR-05
* **Mục tiêu:** Tiêu thụ token thành công đúng 1 lần duy nhất (`consume`).
* **Các bước thực hiện:**
  1. Tạo token hợp lệ.
  2. Gọi `token.consume(now)`.
* **Kết quả mong đợi:**
  * `token.isUsed()` trả về `true`.
  * `token.getUsedAt()` bằng `now`.
  * `token.isValid(now)` trả về `false`.
* **Method trong code:** [`AccountTokenTest.shouldConsumeTokenSuccessfullyOnce()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTokenTest.java#L51-L71)
* **Trạng thái:** **PASSED**

#### Test Case 12: `IAM-DOM-014`
* **Rule liên quan:** BR-05
* **Mục tiêu:** Từ chối tiêu thụ token đã quá hạn hoặc token đã sử dụng trước đó.
* **Kết quả mong đợi:** Ném `InvalidOperationException` kèm `TOKEN_EXPIRED` hoặc `TOKEN_ALREADY_USED`.
* **Method trong code:** [`AccountTokenTest.shouldRejectExpiredOrAlreadyUsedToken()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTokenTest.java#L74-L105)
* **Trạng thái:** **PASSED**

---

### Nhóm 4: Kiểm thử Use Case Đăng Ký Tài Khoản (`RegisterAccountServiceTest.java`)

#### Test Case 13: `IAM-REG-001`
* **Rule liên quan:** BR-01, BR-03, BR-04, NFR-01
* **Mục tiêu:** Đăng ký thành công tạo tài khoản trực tiếp ở trạng thái `ACTIVE` (không cần token hay email kích hoạt).
* **Dữ liệu đầu vào:**
  * Email: `"Student@EduFit.vn "`
  * Password: `"Password123@"`
  * FullName: `" Nguyen Van A "`
  * Role: `AccountRole.STUDENT`
* **Các bước thực hiện:**
  1. Mock `accountRepository.existsByEmail("student@edufit.vn")` trả về `false`.
  2. Gọi `registerService.register(...)`.
* **Kết quả mong đợi:**
  * Tài khoản được lưu với email `"student@edufit.vn"`.
  * Trạng thái `account.getStatus()` là `AccountStatus.ACTIVE`.
  * Mật khẩu được băm qua Argon2id.
* **Method trong code:** [`RegisterAccountServiceTest.shouldRegisterAccountSuccessfullyInActiveStatus()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/RegisterAccountServiceTest.java#L47-L64)
* **Trạng thái:** **PASSED**

#### Test Case 14: `IAM-REG-003`
* **Rule liên quan:** BR-01
* **Mục tiêu:** Từ chối đăng ký khi email đã tồn tại trong hệ thống.
* **Các bước thực hiện:**
  1. Mock `accountRepository.existsByEmail(...)` trả về `true`.
  2. Gọi `registerService.register(...)`.
* **Kết quả mong đợi:** Ném `ResourceConflictException` với mã lỗi `USER_ALREADY_EXISTS`.
* **Method trong code:** [`RegisterAccountServiceTest.shouldRejectRegistrationWhenEmailAlreadyExists()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/RegisterAccountServiceTest.java#L67-L83)
* **Trạng thái:** **PASSED**

#### Test Case 15: `IAM-REG-004`
* **Rule liên quan:** BR-03
* **Mục tiêu:** Chặn đứng nỗ lực tự đăng ký tài khoản với vai trò `ADMIN`.
* **Các bước thực hiện:** Gọi đăng ký với `role = AccountRole.ADMIN`.
* **Kết quả mong đợi:** Ném `InvalidOperationException` với mã `ErrorCode.FORBIDDEN`.
* **Method trong code:** [`RegisterAccountServiceTest.shouldRejectAdminRegistrationAttempt()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/RegisterAccountServiceTest.java#L86-L101)
* **Trạng thái:** **PASSED**

---

### Nhóm 5: Kiểm thử Đăng Nhập & Bảo Mật Khóa (`AuthenticateAccountServiceTest.java`)

#### Test Case 16: `IAM-AUTH-001`
* **Rule liên quan:** BR-06, NFR-01
* **Mục tiêu:** Đăng nhập thành công khi mật khẩu đúng và tài khoản `ACTIVE`.
* **Các bước thực hiện:**
  1. Chuẩn bị Account trạng thái `ACTIVE`.
  2. Mock `passwordService.verify` trả về `true`.
  3. Gọi `authService.authenticate(email, password)`.
* **Kết quả mong đợi:**
  * Trả về đúng đối tượng `Account`.
  * Bộ đếm `failedLoginAttempts == 0`.
  * `lastLoginAt` được ghi nhận.
* **Method trong code:** [`AuthenticateAccountServiceTest.shouldAuthenticateSuccessfullyWhenCredentialsAreValid()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AuthenticateAccountServiceTest.java#L52-L72)
* **Trạng thái:** **PASSED**

#### Test Case 17: `IAM-AUTH-002`
* **Rule liên quan:** NFR-06
* **Mục tiêu:** Chống dò quét tài khoản (Enumeration Resistance) — Sai mật khẩu hay sai email đều trả về cùng mã lỗi `INVALID_CREDENTIALS`.
* **Các bước thực hiện:**
  1. Thử đăng nhập với email không tồn tại trong hệ thống.
  2. Thử đăng nhập với email có thật nhưng mật khẩu sai.
* **Kết quả mong đợi:** Cả 2 trường hợp đều ném lỗi `INVALID_CREDENTIALS` (không tiết lộ sự tồn tại của email).
* **Method trong code:** [`AuthenticateAccountServiceTest.shouldReturnSameErrorForWrongPasswordAndNonExistentEmail()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AuthenticateAccountServiceTest.java#L74-L105)
* **Trạng thái:** **PASSED**

#### Test Case 18: `IAM-AUTH-004`
* **Rule liên quan:** BR-06
* **Mục tiêu:** Khóa tài khoản sau 5 lần nhập sai liên tiếp.
* **Các bước thực hiện:**
  1. Nhập sai 4 lần liên tiếp: kiểm tra mỗi lần đều trả lỗi `INVALID_CREDENTIALS` và counter tăng dần.
  2. Nhập sai lần thứ 5: kiểm tra hệ thống kích hoạt khóa và ném lỗi `ACCOUNT_LOCKED`.
* **Kết quả mong đợi:** Lần thứ 5 ném `ACCOUNT_LOCKED`, `account.getLockedUntil()` mang giá trị thời gian sau 15 phút.
* **Method trong code:** [`AuthenticateAccountServiceTest.shouldLockAccountAfterFiveConsecutiveFailures()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AuthenticateAccountServiceTest.java#L125-L162)
* **Trạng thái:** **PASSED**

---

### Nhóm 6: Kiểm thử Quên & Đổi Mật Khẩu (`PasswordResetAndChangeTest.java`)

#### Test Case 19: `IAM-PWD-001`
* **Rule liên quan:** NFR-06
* **Mục tiêu:** Yêu cầu quên mật khẩu trả về trung tính khi email không tồn tại trong CSDL.
* **Các bước thực hiện:**
  1. Mock `findByEmail` trả về `Optional.empty()`.
  2. Gọi `requestResetService.requestReset("unknown@edufit.vn")`.
* **Kết quả mong đợi:**
  * Hàm chạy êm xuôi không ném exception (`assertDoesNotThrow`).
  * Tuyệt đối không gọi `emailSender.sendPasswordResetEmail(...)`.
* **Method trong code:** [`PasswordResetAndChangeTest.shouldReturnNeutralResultEvenWhenEmailDoesNotExist()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordResetAndChangeTest.java#L93-L100)
* **Trạng thái:** **PASSED**

#### Test Case 20: `IAM-PWD-002`
* **Rule liên quan:** BR-05, NFR-01, NFR-11
* **Mục tiêu:** Đặt lại mật khẩu thành công tiêu thụ token và cập nhật mật khẩu mới băm bằng Argon2id.
* **Các bước thực hiện:**
  1. Cung cấp token reset hợp lệ.
  2. Gọi `resetPasswordService.resetPassword(rawToken, "NewPassword123@")`.
* **Kết quả mong đợi:**
  * `token.isUsed()` chuyển thành `true`.
  * Mật khẩu của tài khoản được cập nhật thành chuỗi băm mới.
* **Method trong code:** [`PasswordResetAndChangeTest.shouldResetPasswordSuccessfully()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordResetAndChangeTest.java#L102-L132)
* **Trạng thái:** **PASSED**

#### Test Case 21: `IAM-PWD-003`
* **Rule liên quan:** UC1.4
* **Mục tiêu:** Từ chối yêu cầu đổi mật khẩu khi người dùng nhập sai mật khẩu hiện tại.
* **Kết quả mong đợi:** Ném `InvalidOperationException` kèm `ErrorCode.INVALID_CURRENT_PASSWORD`.
* **Method trong code:** [`PasswordResetAndChangeTest.shouldRejectWhenCurrentPasswordIsIncorrect()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordResetAndChangeTest.java#L134-L155)
* **Trạng thái:** **PASSED**

#### Test Case 22: `IAM-PWD-004`
* **Rule liên quan:** BR-02
* **Mục tiêu:** Từ chối yêu cầu đổi mật khẩu khi mật khẩu mới trùng với mật khẩu hiện tại.
* **Kết quả mong đợi:** Ném `InvalidOperationException` kèm `ErrorCode.PASSWORD_REUSE_FORBIDDEN`.
* **Method trong code:** [`PasswordResetAndChangeTest.shouldRejectWhenNewPasswordMatchesCurrentPassword()`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordResetAndChangeTest.java#L157-L178)
* **Trạng thái:** **PASSED**

---

## 4. Bảng Ma Trận Truy Vết Nhanh (Traceability Matrix)

| Quy Tắc | Tên Quy Tắc | Mã Test Case | Class Kiểm Thử | Kết Quả |
|---|---|---|---|:---:|
| **BR-01** | Email trim, lowercase & Unique | `IAM-REG-001`, `IAM-REG-003` | `RegisterAccountServiceTest` | **PASSED** |
| **BR-02** | Password >= 8 ký tự, có chữ & số | `IAM-DOM-009`, `IAM-DOM-010`, `IAM-DOM-011`, `IAM-PWD-004` | `PasswordPolicyTest`, `PasswordResetAndChangeTest` | **PASSED** |
| **BR-03** | Cấm tự đăng ký ADMIN | `IAM-DOM-002`, `IAM-REG-004` | `AccountTest`, `RegisterAccountServiceTest` | **PASSED** |
| **BR-04** | Đăng ký kích hoạt ngay `ACTIVE` | `IAM-DOM-001`, `IAM-REG-001` | `AccountTest`, `RegisterAccountServiceTest` | **PASSED** |
| **BR-05** | Token reset 30p, dùng 1 lần | `IAM-DOM-012`, `IAM-DOM-013`, `IAM-DOM-014`, `IAM-PWD-002` | `AccountTokenTest`, `PasswordResetAndChangeTest` | **PASSED** |
| **BR-06** | Khóa 15p sau 5 lần sai liên tiếp | `IAM-DOM-005`, `IAM-DOM-006`, `IAM-DOM-007`, `IAM-DOM-008`, `IAM-AUTH-004` | `AccountTest`, `AuthenticateAccountServiceTest` | **PASSED** |
| **NFR-01** | Băm mật khẩu bằng Argon2id | `IAM-REG-001`, `IAM-AUTH-001`, `Argon2PasswordServiceTest` | `RegisterAccountServiceTest`, `Argon2PasswordServiceTest` | **PASSED** |
| **NFR-03** | CSDL chỉ lưu SHA-256 token hash | `IAM-DOM-012`, `AccountTokenTest` | `AccountTokenTest` | **PASSED** |
| **NFR-06** | Chống Timing & Dò quét email | `IAM-AUTH-002`, `IAM-PWD-001` | `AuthenticateAccountServiceTest`, `PasswordResetAndChangeTest` | **PASSED** |
| **NFR-11** | Tính nguyên tử giao dịch | `IAM-PWD-002` | `PasswordResetAndChangeTest` | **PASSED** |
