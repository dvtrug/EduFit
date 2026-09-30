# BÁO CÁO RBL: LÕI AUTHENTICATION TRONG MODULE IAM (EDUFIT)

> **Môn học:** SWP391 — Dự án Phần mềm  
> **Dự án:** EduFit — Nền tảng kết nối Gia sư và Học sinh/Phụ huynh  
> **Phân hệ thực hiện:** `backend/modules/iam` (Identity & Access Management Core)  
> **Phương pháp tiếp cận:** Requirements-Based Logic (RBL) & Test-Driven Development (TDD)  
> **Trạng thái:** Hoàn thành (100% Passed)  

---

## 1. Mục tiêu và Phạm vi Kiểm định (Objectives & Scope)

### 1.1. Mục tiêu
Báo cáo này chứng minh bằng mã nguồn và kiểm thử tự động rằng toàn bộ các quy tắc nghiệp vụ (Business Rules - BR) và yêu cầu phi chức năng (NFRs) liên quan đến chức năng Xác thực (Authentication) trong tài liệu SRS EduFit đã được hiện thực chính xác, cô lập và đáng tin cậy bên trong module **`backend/modules/iam`**.

### 1.2. Phạm vi Lõi IAM (IAM Core Scope)
* **Bao gồm (In-Scope):**
  1. Mô hình thực thể Tài khoản (`Account`, `AccountRole`, `AccountStatus`).
  2. Chuẩn hóa email (trim, lowercase) và kiểm tra trùng lặp dữ liệu (BR-01).
  3. Chính sách mật khẩu (`PasswordPolicy` - BR-02) và cơ chế băm mật khẩu bảo mật cao Argon2id (`Argon2PasswordService` - NFR-01).
  4. Quy trình Đăng ký tài khoản kích hoạt trực tiếp ở trạng thái `ACTIVE` (BR-03, BR-04).
  5. Quản lý vòng đời One-Time Token (`AccountToken`: sinh 256-bit ngẫu nhiên, chỉ lưu băm SHA-256 64 ký tự hex, hạn 30 phút cho đặt lại mật khẩu - BR-05, NFR-03).
  6. Xác thực danh tính người dùng (`AuthenticateAccountService`), bộ đếm lỗi và quy tắc khóa tài khoản 15 phút sau 5 lần sai liên tiếp (Lockout Rule - BR-06).
  7. Quên mật khẩu (`RequestPasswordResetService` - chống dò quét tài khoản NFR-06), đặt lại mật khẩu (`ResetPasswordService`) và đổi mật khẩu (`ChangePasswordService`).
  8. Bề mặt công khai `vn.edufit.iam.api` (`IamFacadeImpl`, `UserSummaryView`) phục vụ các module khác đọc danh tính an toàn.
  9. Tầng Web Controller (`AuthController`) phân giải REST HTTP theo hợp đồng `authentication-contract.md`, thiết lập phiên bảo mật Stateful qua cookie `EDUFIT_SESSION` và chuẩn hóa phản hồi `ApiResponse<T>`.

---

## 2. Công nghệ Kiểm thử Sử dụng (Testing Technology)

* **Ngôn ngữ & Nền tảng:** Java 21 LTS, Spring Boot 4.x.
* **Khung kiểm thử:** JUnit 5 (Jupiter), AssertJ, Mockito.
* **Kiểm thử Đơn vị (Unit Tests):** Chạy trực tiếp trên JVM bằng Java thuần và Mockito; thời gian thực thi mili-giây, không mở Spring ApplicationContext nặng nề, hoàn toàn độc lập với mạng hay máy chủ CSDL bên ngoài.
* **Kiểm soát thời gian:** Sử dụng `java.time.Clock` tiêm phụ thuộc (Dependency Injection) để kiểm thử chính xác thời điểm hết hạn token và thời điểm mở khóa lockout mà không dùng `Thread.sleep()`.

---

## 3. Danh mục Business Rules & Yêu cầu Phi Chức Năng (SRS Traceability)

| Mã Quy tắc | Tên Quy tắc | Mô tả chi tiết từ SRS & Quyết định Kỹ thuật |
|---|---|---|
| **BR-01** | Chuẩn hóa & Tính duy nhất của Email | Email đăng ký phải đúng định dạng, được cắt khoảng trắng (trim) và chuyển về chữ thường (`toLowerCase(Locale.ROOT)`). Không được trùng email trong hệ thống (không phân biệt chữ hoa/thường). |
| **BR-02** | Chính sách Mật khẩu (Password Policy) | Mật khẩu tối thiểu 8 ký tự, phải chứa ít nhất 1 chữ cái và ít nhất 1 chữ số. Đổi/reset mật khẩu không được trùng mật khẩu cũ. |
| **BR-03** | Giới hạn Vai trò Đăng ký (Role Restriction) | Người dùng đăng ký công khai chỉ được chọn vai trò: `STUDENT`, `PARENT`, hoặc `TUTOR`. Tuyệt đối cấm đăng ký quyền `ADMIN`. Vai trò được gán cố định khi tạo. |
| **BR-04** | Trạng thái Tài khoản Đăng ký Mới | Tài khoản đăng ký mới kích hoạt ngay ở trạng thái `ACTIVE` và có thể đăng nhập tức thì. |
| **BR-05** | Vòng đời One-time Token | Token đặt lại mật khẩu (hạn 30 phút) chỉ được dùng 1 lần. Raw token 256-bit chỉ gửi qua email; CSDL chỉ lưu chuỗi băm SHA-256. Thao tác tiêu thụ token phải mang tính nguyên tử (atomic). |
| **BR-06** | Quy tắc Khóa Đăng nhập (Lockout Rule) | Sau 5 lần nhập sai mật khẩu liên tiếp, tài khoản bị khóa tạm thời 15 phút. Trong thời gian khóa, mọi nỗ lực đăng nhập đều bị từ chối. Đăng nhập thành công xóa bộ đếm lỗi và cập nhật `lastLoginAt`. |
| **NFR-01** | Lưu trữ Mật khẩu An toàn | Mật khẩu bắt buộc băm bằng Argon2id (64 MiB RAM, 3 iterations, 1 parallelism) kèm salt ngẫu nhiên. Tuyệt đối không lưu hoặc in plaintext password. |
| **NFR-03** | Lưu trữ Token An toàn | Tuyệt đối không lưu raw token vào CSDL. CSDL chỉ lưu chuỗi băm hex SHA-256 (64 ký tự). |
| **NFR-06** | Chống Dò quét Tài khoản (Account Enumeration) | Đăng nhập sai trả về lỗi chung `INVALID_CREDENTIALS` kèm Dummy Hash Verification chống Timing Attack. Yêu cầu reset mật khẩu luôn trả về phản hồi trung tính dù email có tồn tại hay không. |
| **NFR-11** | Tính Toàn vẹn Giao dịch (Atomicity) | Tiêu thụ token và đổi mật khẩu nằm trong cùng một transaction `@Transactional`. |

---

## 4. Ma trận Truy Vết & Đính Kèm Đoạn Code Kiểm Thử (RBL Traceability & Code Evidence)

### 4.1. Bảng Ma Trận Tổng Hợp

Ma trận mô tả 25 test case nghiệp vụ. Một số test dùng `@ParameterizedTest`, vì vậy Maven Surefire thực thi tổng cộng 32 lượt test.

| STT | Mã Test Case | Rule liên quan | Tên Class Kiểm Thử | Tên Test Method | Kết quả Dự Kiến | Trạng thái |
|:---:|---|---|---|---|---|:---:|
| 1 | `IAM-DOM-001` | BR-04 | `AccountTest` | `shouldInitializeAccountInActiveStatus` | Account tạo mới có status `ACTIVE`, email trim/lowercase | **PASSED** |
| 2 | `IAM-DOM-002` | BR-03 | `AccountTest` | `shouldPreventAdminRoleRegistration` | Tạo vai trò `ADMIN` bị ném lỗi `FORBIDDEN` | **PASSED** |
| 3 | `IAM-DOM-005` | BR-06 | `AccountTest` | `shouldIncrementFailedAttemptsAndLockAccountAtFifthFailure` | Sai 1-4 tăng đếm; sai lần 5 khóa tài khoản 15 phút | **PASSED** |
| 4 | `IAM-DOM-006` | BR-06 | `AccountTest` | `shouldDenyLoginWhenAccountIsCurrentlyLocked` | `canAuthenticate()` trả về false trong 15 phút bị khóa | **PASSED** |
| 5 | `IAM-DOM-007` | BR-06 | `AccountTest` | `shouldAllowLoginAndResetCounterAfterLockExpires` | Cho phép đăng nhập lại sau khi hết thời gian khóa 15 phút | **PASSED** |
| 6 | `IAM-DOM-008` | BR-06 | `AccountTest` | `shouldResetFailedAttemptsOnSuccessfulLogin` | Đăng nhập đúng reset counter = 0 và ghi nhận `lastLoginAt` | **PASSED** |
| 7 | `IAM-DOM-009` | BR-02 | `PasswordPolicyTest` | `shouldAcceptValidPassword` | Mật khẩu >= 8 ký tự có cả chữ và số được chấp nhận | **PASSED** |
| 8 | `IAM-DOM-010` | BR-02 | `PasswordPolicyTest` | `shouldRejectShortOrBlankPassword` | Mật khẩu dưới 8 ký tự hoặc rỗng bị từ chối `VALIDATION_FAILED` | **PASSED** |
| 9 | `IAM-DOM-011` | BR-02 | `PasswordPolicyTest` | `shouldRejectPasswordWithoutDigitsOrLetters` | Mật khẩu thiếu chữ hoặc thiếu số bị từ chối `VALIDATION_FAILED` | **PASSED** |
| 10 | `IAM-DOM-012` | BR-05, NFR-03 | `AccountTokenTest` | `shouldCreateValidTokenWithCorrectExpiry` | Token reset hết hạn sau 30 phút, hash SHA-256 64 ký tự | **PASSED** |
| 11 | `IAM-DOM-013` | BR-05 | `AccountTokenTest` | `shouldConsumeTokenSuccessfullyOnce` | Tiêu thụ token thành công, đánh dấu `usedAt = now` | **PASSED** |
| 12 | `IAM-DOM-014` | BR-05 | `AccountTokenTest` | `shouldRejectExpiredOrAlreadyUsedToken` | Token quá hạn hoặc đã dùng bị từ chối | **PASSED** |
| 13 | `IAM-REG-001` | BR-01, BR-03, BR-04 | `RegisterAccountServiceTest` | `shouldRegisterAccountSuccessfullyInActiveStatus` | Đăng ký thành công tạo account `ACTIVE`, băm Argon2id | **PASSED** |
| 14 | `IAM-REG-003` | BR-01 | `RegisterAccountServiceTest` | `shouldRejectRegistrationWhenEmailAlreadyExists` | Trùng email ném lỗi `USER_ALREADY_EXISTS` (409) | **PASSED** |
| 15 | `IAM-REG-004` | BR-03 | `RegisterAccountServiceTest` | `shouldRejectAdminRegistrationAttempt` | Đăng ký vai trò `ADMIN` bị chặn `FORBIDDEN` (403) | **PASSED** |
| 16 | `IAM-AUTH-001` | BR-06, NFR-01 | `AuthenticateAccountServiceTest` | `shouldAuthenticateSuccessfullyWhenCredentialsAreValid` | Đăng nhập đúng trả về `Account`, reset counter về 0 | **PASSED** |
| 17 | `IAM-AUTH-002` | NFR-06 | `AuthenticateAccountServiceTest` | `shouldReturnSameErrorForWrongPasswordAndNonExistentEmail` | Sai email hay sai pass đều trả cùng mã `INVALID_CREDENTIALS` | **PASSED** |
| 18 | `IAM-AUTH-004` | BR-06 | `AuthenticateAccountServiceTest` | `shouldLockAccountAfterFiveConsecutiveFailures` | Sai 5 lần liên tiếp kích hoạt khóa 15 phút, ném `ACCOUNT_LOCKED` | **PASSED** |
| 19 | `IAM-PWD-001` | NFR-06 | `PasswordResetAndChangeTest` | `shouldReturnNeutralResultEvenWhenEmailDoesNotExist` | Yêu cầu reset email không tồn tại trả về trung tính 200 OK | **PASSED** |
| 20 | `IAM-PWD-002` | BR-05, NFR-11 | `PasswordResetAndChangeTest` | `shouldResetPasswordSuccessfully` | Đặt lại mật khẩu thành công tiêu thụ token và cập nhật hash mới | **PASSED** |
| 21 | `IAM-PWD-003` | UC1.4 | `PasswordResetAndChangeTest` | `shouldRejectWhenCurrentPasswordIsIncorrect` | Đổi mật khẩu sai mật khẩu hiện tại bị từ chối | **PASSED** |
| 22 | `IAM-PWD-004` | BR-02 | `PasswordResetAndChangeTest` | `shouldRejectWhenNewPasswordMatchesCurrentPassword` | Mật khẩu mới trùng mật khẩu cũ bị từ chối | **PASSED** |
| 23 | `IAM-SEC-001` | NFR-01 | `Argon2PasswordServiceTest` | `shouldHashAndVerifyPasswordSuccessfully` | Băm mật khẩu ra định dạng Argon2id và so khớp đúng | **PASSED** |
| 24 | `IAM-SEC-002` | NFR-01 | `Argon2PasswordServiceTest` | `shouldProduceDifferentHashesForSamePasswordDueToSalt` | Cùng mật khẩu ra các chuỗi hash khác nhau do salt ngẫu nhiên | **PASSED** |
| 25 | `IAM-SEC-003` | NFR-01 | `Argon2PasswordServiceTest` | `shouldRejectWrongPassword` | So khớp mật khẩu sai trả về false | **PASSED** |

---

### 4.2. Đính Kèm Đoạn Code Kiểm Thử & Giải Thích Chi Tiết Từng Ca Kiểm Thử

Dưới đây là chi tiết mã nguồn test thật được trích xuất trực tiếp từ các file trong thư mục `backend/modules/iam/src/test/java/vn/edufit/iam`, phân tích chi tiết theo quy tắc nghiệp vụ và mẫu kiểm thử **Arrange - Act - Assert (AAA)**:

---

#### [1] Nhóm Kiểm Thử Thực Thể Account ([`AccountTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTest.java))

* **Test Case `IAM-DOM-001`: Khởi tạo tài khoản mặc định ở trạng thái ACTIVE (BR-04, BR-01)**
  * **Business Logic:** Khi đăng ký, hệ thống tự động cắt khoảng trắng thừa, chuyển email về chữ thường và đặt trạng thái tài khoản là `ACTIVE` ngay lập tức, không bắt người dùng phải qua bước xác thực email.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-001: Khởi tạo tài khoản mặc định ở trạng thái ACTIVE (BR-04)")
void shouldInitializeAccountInActiveStatus() {
  // Arrange & Act: Khởi tạo với email viết hoa, khoảng trắng thừa
  Account account = Account.create(
      "Student@EduFit.vn ",
      "$argon2id$v=19$m=65536,t=3,p=1$hashed...",
      " Nguyen Van A ",
      AccountRole.STUDENT
  );

  // Assert: Chuẩn hóa email, họ tên, vai trò và trạng thái
  assertEquals("student@edufit.vn", account.getEmail(), "Email phải được cắt khoảng trắng và viết thường");
  assertEquals("Nguyen Van A", account.getFullName(), "Họ tên phải được cắt khoảng trắng");
  assertEquals(AccountRole.STUDENT, account.getRole());
  assertEquals(AccountStatus.ACTIVE, account.getStatus(), "Trạng thái mặc định phải là ACTIVE");
  assertEquals(0, account.getFailedLoginAttempts(), "Bộ đếm lỗi khởi đầu bằng 0");
  assertNull(account.getLockedUntil(), "Tài khoản mới không bị khóa");
}
```
  * **Giải thích (AAA):**
    * *Arrange & Act:* Tạo đối tượng thực thể bằng phương thức factory `Account.create(...)` với dữ liệu đầu vào chứa ký tự hoa và khoảng trắng thừa.
    * *Assert:* Kiểm tra hàm getter xác nhận `email` đã về `"student@edufit.vn"`, `fullName` đã được trim, trạng thái là `ACTIVE`, `failedLoginAttempts = 0`, và `lockedUntil = null`.

---

* **Test Case `IAM-DOM-002`: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)**
  * **Business Logic:** Quy tắc nghiệp vụ cấm tuyệt đối mọi hành vi tự cấp quyền hoặc đăng ký công khai tài khoản với vai trò Quản trị viên (`ADMIN`).
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-002: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)")
void shouldPreventAdminRoleRegistration() {
  // Arrange & Act & Assert: Cố tình truyền AccountRole.ADMIN vào hàm khởi tạo
  assertThrows(InvalidOperationException.class, () ->
      Account.create(
          "admin@edufit.vn",
          "hashed...",
          "Admin User",
          AccountRole.ADMIN
      )
  );
}
```
  * **Giải thích (AAA):**
    * *Arrange & Act:* Thử gọi phương thức tạo tài khoản với vai trò `AccountRole.ADMIN`.
    * *Assert:* Phương thức ném ngay ngoại lệ `InvalidOperationException`, ngăn chặn việc tạo thực thể vi phạm phân quyền ngay tại Domain Layer.

---

* **Test Case `IAM-DOM-005`: Tăng bộ đếm và khóa tài khoản 15 phút ở lần sai thứ 5 (BR-06)**
  * **Business Logic:** Nhập sai từ 1 đến 4 lần thì chỉ cộng dồn bộ đếm `failedLoginAttempts`; đến lần thứ 5 liên tiếp, tài khoản bị kích hoạt trạng thái khóa tạm thời trong đúng 15 phút.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-005: Đăng nhập sai 5 lần liên tiếp kích hoạt khóa tài khoản 15 phút (BR-06)")
void shouldIncrementFailedAttemptsAndLockAccountAtFifthFailure() {
  // Arrange: Khởi tạo tài khoản active và mốc thời gian cố định
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Student C", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");

  // Act & Assert 1: Sai 4 lần đầu
  for (int i = 1; i <= 4; i++) {
    account.recordLoginFailure(now);
    assertEquals(i, account.getFailedLoginAttempts(), "Bộ đếm tăng tương ứng với số lần sai");
    assertFalse(account.isLocked(now), "Chưa đủ 5 lần thì chưa bị khóa");
  }

  // Act & Assert 2: Lần thứ 5 sai liên tiếp
  account.recordLoginFailure(now);
  assertEquals(5, account.getFailedLoginAttempts());
  assertTrue(account.isLocked(now), "Lần thứ 5 sai phải kích hoạt trạng thái khóa");
  assertEquals(now.plus(Duration.ofMinutes(15)), account.getLockedUntil(), "Thời điểm mở khóa phải là now + 15 phút");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Tạo thực thể `Account` với mốc thời gian chuẩn ISO `2026-09-29T10:00:00Z`.
    * *Act & Assert:* Dùng vòng lặp kiểm tra từng lần từ 1 đến 4, xác nhận chưa bị khóa. Ở lần gọi thứ 5, xác nhận `isLocked(now) == true` và `lockedUntil` là `10:15:00Z`.

---

* **Test Case `IAM-DOM-006`: Từ chối xác thực khi tài khoản đang trong thời gian khóa (BR-06)**
  * **Business Logic:** Khi tài khoản đang trong thời hạn 15 phút bị khóa, phương thức `canAuthenticate()` luôn trả về `false`.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-006: canAuthenticate() trả về false khi tài khoản đang trong thời gian bị khóa")
void shouldDenyLoginWhenAccountIsCurrentlyLocked() {
  // Arrange: Tạo tài khoản và kích hoạt khóa qua 5 lần thất bại tại 10:00:00
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Student D", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  for (int i = 0; i < 5; i++) {
    account.recordLoginFailure(now);
  }

  // Act & Assert: Kiểm tra tại thời điểm 10:05:00 (vẫn trong khoảng 15 phút khóa)
  Instant fiveMinutesLater = now.plus(Duration.ofMinutes(5));
  assertTrue(account.isLocked(fiveMinutesLater), "Sau 5 phút tài khoản vẫn phải ở trạng thái khóa");
  assertFalse(account.canAuthenticate(fiveMinutesLater), "canAuthenticate() phải trả về false");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Giả lập trạng thái tài khoản bị khóa lúc 10:00.
    * *Act:* Kiểm tra trạng thái tại thời điểm `now + 5 phút` (10:05).
    * *Assert:* Xác nhận `isLocked` là `true` và `canAuthenticate` là `false`.

---

* **Test Case `IAM-DOM-007`: Tự động mở khóa sau khi hết 15 phút (BR-06)**
  * **Business Logic:** Khi thời gian hiện tại vượt qua mốc `lockedUntil` (sau 15 phút), tài khoản tự động hết hạn khóa và cho phép xác thực bình thường.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-007: Cho phép đăng nhập lại sau khi hết thời gian 15 phút khóa")
void shouldAllowLoginAndResetCounterAfterLockExpires() {
  // Arrange: Tài khoản bị khóa từ mốc 10:00:00 đến 10:15:00
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Student E", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  for (int i = 0; i < 5; i++) {
    account.recordLoginFailure(now);
  }

  // Act: Kiểm tra tại thời điểm 10:16:00 (đã quá thời hạn 15 phút)
  Instant sixteenMinutesLater = now.plus(Duration.ofMinutes(16));

  // Assert: Khóa đã tự động hết hiệu lực
  assertFalse(account.isLocked(sixteenMinutesLater), "Tài khoản không còn bị khóa sau 16 phút");
  assertTrue(account.canAuthenticate(sixteenMinutesLater), "canAuthenticate() phải trả về true");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Tạo tài khoản và đưa vào trạng thái khóa lúc 10:00 (khóa đến 10:15).
    * *Act:* Đưa thời gian kiểm tra tới `10:16` (+16 phút).
    * *Assert:* Xác nhận `isLocked` trả về `false` và `canAuthenticate` trả về `true`.

---

* **Test Case `IAM-DOM-008`: Đăng nhập thành công xóa sạch bộ đếm lỗi và ghi nhận lastLoginAt (BR-06)**
  * **Business Logic:** Khi người dùng nhập đúng mật khẩu, toàn bộ số lần thử sai trước đó bị reset về 0, thời gian khóa bị gỡ bỏ và hệ thống ghi lại thời điểm đăng nhập thành công.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-008: Đăng nhập thành công xóa sạch bộ đếm lỗi và ghi nhận lastLoginAt (BR-06)")
void shouldResetFailedAttemptsOnSuccessfulLogin() {
  // Arrange: Tài khoản đã bị ghi nhận sai 3 lần liên tiếp
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Student F", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  account.recordLoginFailure(now);
  account.recordLoginFailure(now);
  account.recordLoginFailure(now);
  assertEquals(3, account.getFailedLoginAttempts());

  // Act: Đăng nhập thành công 30 giây sau đó
  Instant loginTime = now.plusSeconds(30);
  account.recordSuccessfulLogin(loginTime);

  // Assert: Reset counter, clear lockout và ghi nhận thời gian
  assertEquals(0, account.getFailedLoginAttempts(), "Bộ đếm lỗi phải được reset về 0");
  assertNull(account.getLockedUntil(), "Thời gian khóa phải được xóa bỏ");
  assertEquals(loginTime, account.getLastLoginAt(), "Ghi nhận đúng thời điểm đăng nhập thành công");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Giả lập tài khoản đã sai 3 lần (`failedLoginAttempts = 3`).
    * *Act:* Gọi `recordSuccessfulLogin(loginTime)`.
    * *Assert:* Kiểm tra `failedLoginAttempts == 0`, `lockedUntil == null` và `lastLoginAt == loginTime`.

---

#### [2] Nhóm Kiểm Thử Chính Sách Mật Khẩu ([`PasswordPolicyTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordPolicyTest.java))

* **Test Case `IAM-DOM-009`: Chấp nhận mật khẩu hợp lệ (BR-02)**
  * **Business Logic:** Mật khẩu đạt tiêu chuẩn khi có độ dài tối thiểu 8 ký tự, chứa ít nhất 1 chữ cái và ít nhất 1 chữ số.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-009: Chấp nhận mật khẩu hợp lệ (>= 8 ký tự, có cả chữ và số)")
void shouldAcceptValidPassword() {
  // Act & Assert: Xác thực các mẫu mật khẩu hợp lệ không gây ngoại lệ
  assertDoesNotThrow(() -> passwordPolicy.validate("Password123@"));
  assertDoesNotThrow(() -> passwordPolicy.validate("Abc12345"));
  assertDoesNotThrow(() -> passwordPolicy.validate("StrongPass1"));
}
```
  * **Giải thích (AAA):**
    * *Act & Assert:* Truyền các chuỗi mật khẩu hợp lệ vào `passwordPolicy.validate(...)` và sử dụng `assertDoesNotThrow` để đảm bảo không có ngoại lệ nào bị ném.

---

* **Test Case `IAM-DOM-010`: Từ chối mật khẩu dưới 8 ký tự hoặc rỗng (BR-02)**
  * **Business Logic:** Bất kỳ mật khẩu nào rỗng, toàn khoảng trắng, hoặc dưới 8 ký tự đều bị từ chối với mã lỗi `VALIDATION_FAILED`.
  * **Đoạn mã kiểm thử:**
```java
@ParameterizedTest
@ValueSource(strings = {"", "   ", "short1", "1234567", "Abcdefg"})
@DisplayName("IAM-DOM-010: Từ chối mật khẩu dưới 8 ký tự hoặc rỗng")
void shouldRejectShortOrBlankPassword(String password) {
  // Act & Assert: Mọi ca kiểm thử đều phải ném InvalidOperationException với mã VALIDATION_FAILED
  InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> passwordPolicy.validate(password)
  );
  assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Sử dụng `@ParameterizedTest` với tập dữ liệu biên: chuỗi rỗng `""`, khoảng trắng `"   "`, 6 ký tự `"short1"`, 7 số `"1234567"`, 7 chữ cái `"Abcdefg"`.
    * *Act & Assert:* Xác nhận phương thức ném `InvalidOperationException` và mã lỗi trả về chính xác là `ErrorCode.VALIDATION_FAILED`.

---

* **Test Case `IAM-DOM-011`: Từ chối mật khẩu thiếu số hoặc thiếu chữ cái (BR-02)**
  * **Business Logic:** Dù mật khẩu dài trên 8 ký tự nhưng chỉ chứa chữ cái mà không có số, hoặc chỉ chứa số mà không có chữ cái thì vẫn không hợp lệ.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-011: Từ chối mật khẩu không có số hoặc không có chữ cái")
void shouldRejectPasswordWithoutDigitsOrLetters() {
  // Act & Assert 1: Chỉ có chữ cái dài trên 8 ký tự
  InvalidOperationException ex1 = assertThrows(
      InvalidOperationException.class,
      () -> passwordPolicy.validate("OnlyLettersLong")
  );
  assertEquals(ErrorCode.VALIDATION_FAILED, ex1.getErrorCode());

  // Act & Assert 2: Chỉ có chữ số dài trên 8 ký tự
  InvalidOperationException ex2 = assertThrows(
      InvalidOperationException.class,
      () -> passwordPolicy.validate("1234567890")
  );
  assertEquals(ErrorCode.VALIDATION_FAILED, ex2.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Act & Assert:* Thử nghiệm với `"OnlyLettersLong"` (15 ký tự nhưng 0 chữ số) và `"1234567890"` (10 ký tự nhưng 0 chữ cái). Cả hai đều bị chặn với mã `VALIDATION_FAILED`.

---

#### [3] Nhóm Kiểm Thử Token Dùng Một Lần ([`AccountTokenTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AccountTokenTest.java))

* **Test Case `IAM-DOM-012`: Khởi tạo Token hợp lệ với thời hạn chính xác (BR-05, NFR-03)**
  * **Business Logic:** Token reset mật khẩu có TTL chuẩn 30 phút, lưu chuỗi băm hex SHA-256 64 ký tự, trạng thái khởi đầu chưa sử dụng và chưa hết hạn.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-012: Khởi tạo Token hợp lệ với thời hạn chính xác")
void shouldCreateValidTokenWithCorrectExpiry() {
  // Arrange: Khởi tạo account và mốc thời gian
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Nguyen Van A", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  Instant expiresAt = now.plus(TokenType.PASSWORD_RESET.getDefaultTtl());

  // Act: Tạo token với mã băm SHA-256 hex chuẩn 64 ký tự
  AccountToken token = AccountToken.create(
      account,
      TokenType.PASSWORD_RESET,
      "a3b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1",
      expiresAt
  );

  // Assert: Xác thực trạng thái ban đầu của token
  assertEquals(account, token.getAccount());
  assertEquals(TokenType.PASSWORD_RESET, token.getTokenType());
  assertNull(token.getUsedAt(), "usedAt ban đầu phải là null");
  assertFalse(token.isUsed(), "Token mới tạo chưa được đánh dấu là used");
  assertFalse(token.isExpired(now), "Token chưa quá hạn tại thời điểm now");
  assertTrue(token.isValid(now), "Token phải hợp lệ để sử dụng");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Chuẩn bị Account và tính toán thời điểm hết hạn `expiresAt = now + 30m`.
    * *Act:* Gọi `AccountToken.create(...)` với chuỗi băm 64 ký tự hex.
    * *Assert:* Kiểm tra tính hợp lệ qua `isValid(now) == true`, `isUsed() == false`, và `isExpired(now) == false`.

---

* **Test Case `IAM-DOM-013`: Tiêu thụ token thành công đúng một lần (BR-05)**
  * **Business Logic:** Token dùng một lần sau khi tiêu thụ (`consume`) sẽ chuyển sang trạng thái đã sử dụng (`usedAt = now`) và không bao giờ được phép tiêu thụ lại lần thứ hai.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-013: Tiêu thụ token thành công đúng một lần (BR-05)")
void shouldConsumeTokenSuccessfullyOnce() {
  // Arrange: Tạo token hợp lệ
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Nguyen Van A", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  AccountToken token = AccountToken.create(
      account,
      TokenType.PASSWORD_RESET,
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      now.plus(Duration.ofMinutes(30))
  );

  // Act: Tiêu thụ token lần đầu
  token.consume(now);

  // Assert 1: Token chuyển sang trạng thái đã sử dụng
  assertTrue(token.isUsed());
  assertEquals(now, token.getUsedAt());
  assertFalse(token.isValid(now), "Token đã dùng không còn hợp lệ");

  // Act & Assert 2: Cố tình tiêu thụ lại lần 2 phải bị từ chối
  InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> token.consume(now)
  );
  assertEquals(ErrorCode.TOKEN_ALREADY_USED, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Tạo token hợp lệ với hạn 30 phút.
    * *Act:* Gọi `token.consume(now)`.
    * *Assert:* Lần 1 thành công cập nhật `usedAt`. Khi cố tình gọi `consume` lần 2, hệ thống lập tức ném lỗi `TOKEN_ALREADY_USED`.

---

* **Test Case `IAM-DOM-014`: Từ chối tiêu thụ token đã hết hạn hoặc đã được sử dụng (BR-05)**
  * **Business Logic:** Một token đã hết hạn (quá 30 phút) thì không được phép tiêu thụ.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-DOM-014: Từ chối tiêu thụ token đã hết hạn hoặc đã được sử dụng")
void shouldRejectExpiredOrAlreadyUsedToken() {
  // Arrange: Token có thời hạn trong quá khứ (now - 10 giây)
  Account account = Account.create(
      "student@edufit.vn", "hashed...", "Nguyen Van A", AccountRole.STUDENT
  );
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  Instant pastExpiry = now.minusSeconds(10);

  AccountToken expiredToken = AccountToken.create(
      account,
      TokenType.PASSWORD_RESET,
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      pastExpiry
  );

  // Act & Assert: Tiêu thụ token hết hạn phải ném InvalidOperationException
  assertThrows(InvalidOperationException.class, () -> expiredToken.consume(now));
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Thiết lập `pastExpiry = now.minusSeconds(10)`.
    * *Act & Assert:* Gọi `expiredToken.consume(now)` phải ném ngoại lệ ngay lập tức, ngăn ngừa việc sử dụng token rác/hết hạn.

---

* **Test Case `IAM-TOKEN-BOUNDARY-001` & `IAM-TOKEN-HASH-001`: Kiểm thử ca biên thời gian & định dạng SHA-256 (BR-05, NFR-03)**
  * **Business Logic:** Token tại đúng thời điểm hết hạn (`now == expiresAt`) được xem là đã hết hạn. Chuỗi băm token bắt buộc phải là SHA-256 hex đủ 64 ký tự (0-9, a-f).
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-TOKEN-BOUNDARY-001: Token tại đúng thời điểm expiresAt được coi là đã hết hạn (BR-05)")
void shouldConsiderTokenExpiredAtExactExpiryInstant() {
  Account account = Account.create("studenta@gmail.com", "hashed...", "A", AccountRole.STUDENT);
  Instant now = Instant.parse("2026-09-29T10:00:00Z");
  AccountToken boundaryToken = AccountToken.create(
      account, TokenType.PASSWORD_RESET,
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      now
  );

  assertTrue(boundaryToken.isExpired(now), "Tại đúng thời điểm expiresAt token phải hết hạn");
  assertFalse(boundaryToken.isValid(now));
  assertThrows(InvalidOperationException.class, () -> boundaryToken.consume(now));
}

@Test
@DisplayName("IAM-TOKEN-HASH-001: Từ chối khởi tạo token nếu mã băm không đúng chuẩn SHA-256 hex 64 ký tự (NFR-03)")
void shouldRejectTokenHashNotMeetingSha256HexRequirements() {
  Account account = Account.create("student@edufit.vn", "hashed...", "A", AccountRole.STUDENT);
  Instant expiresAt = Instant.parse("2026-09-29T11:00:00Z");

  // 1. Quá ngắn (10 ký tự)
  assertThrows(InvalidOperationException.class, () ->
      AccountToken.create(account, TokenType.PASSWORD_RESET, "0123456789", expiresAt)
  );

  // 2. Đủ 64 ký tự nhưng chứa ký tự lạ 'z' (không phải hex)
  String invalidHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdez";
  assertThrows(InvalidOperationException.class, () ->
      AccountToken.create(account, TokenType.PASSWORD_RESET, invalidHex, expiresAt)
  );
}
```
  * **Giải thích (AAA):** Ca kiểm thử biên chứng minh hệ thống không bị lỗi Off-By-One tại biên giây, và bảo vệ cơ sở dữ liệu khỏi các chuỗi băm hỏng hoặc không đạt chuẩn mật mã an toàn NFR-03.

---

#### [4] Nhóm Kiểm Thử Use Case Đăng Ký ([`RegisterAccountServiceTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/RegisterAccountServiceTest.java))

* **Test Case `IAM-REG-001`: Đăng ký thành công tạo tài khoản trực tiếp ACTIVE (BR-04, BR-01, NFR-01)**
  * **Business Logic:** Người dùng đăng ký thành công sẽ có tài khoản kích hoạt ngay ở trạng thái `ACTIVE`, mật khẩu được băm Argon2id và có thể đăng nhập ngay mà không cần xác thực email.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-REG-001: Đăng ký thành công tạo tài khoản trực tiếp ACTIVE (BR-04 mới)")
void shouldRegisterAccountSuccessfullyInActiveStatus() {
  // Arrange: Mock tài khoản chưa tồn tại
  when(accountRepository.existsByEmail("student@edufit.vn")).thenReturn(false);

  // Act: Đăng ký với email có viết hoa và khoảng trắng
  Account account = registerService.register(
      "Student@EduFit.vn ",
      "Password123@",
      " Nguyen Van A ",
      AccountRole.STUDENT
  );

  // Assert: Tài khoản trả về được chuẩn hóa và ở trạng thái ACTIVE
  assertNotNull(account);
  assertEquals("student@edufit.vn", account.getEmail());
  assertEquals("Nguyen Van A", account.getFullName());
  assertEquals(AccountRole.STUDENT, account.getRole());
  assertEquals(AccountStatus.ACTIVE, account.getStatus(), "Tài khoản đăng ký mới phải ở trạng thái ACTIVE");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Giả lập `accountRepository.existsByEmail(...)` trả về `false`.
    * *Act:* Gọi `registerService.register(...)`.
    * *Assert:* Kiểm tra đối tượng trả về có trạng thái `AccountStatus.ACTIVE`, vai trò `STUDENT`, và email được chuẩn hóa `student@edufit.vn`.

---

* **Test Case `IAM-REG-003`: Từ chối đăng ký khi email đã tồn tại trong hệ thống (BR-01)**
  * **Business Logic:** Kiểm tra tính duy nhất của email không phân biệt chữ hoa/thường. Nếu đã tồn tại, trả về ngoại lệ `ResourceConflictException` với mã lỗi `USER_ALREADY_EXISTS` (HTTP 409).
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-REG-003: Từ chối đăng ký khi email đã tồn tại trong hệ thống (BR-01)")
void shouldRejectRegistrationWhenEmailAlreadyExists() {
  // Arrange: Mock email đã tồn tại trong DB
  when(accountRepository.existsByEmail("tutor@edufit.vn")).thenReturn(true);

  // Act & Assert: Đăng ký phải ném ResourceConflictException
  ResourceConflictException ex = assertThrows(
      ResourceConflictException.class,
      () -> registerService.register(
          "tutor@edufit.vn",
          "Password123@",
          "Tutor B",
          AccountRole.TUTOR
      )
  );

  assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Mock `accountRepository.existsByEmail("tutor@edufit.vn")` trả về `true`.
    * *Act & Assert:* Gọi đăng ký và kiểm tra ngoại lệ `ResourceConflictException` kèm mã lỗi chuẩn `USER_ALREADY_EXISTS`.

---

* **Test Case `IAM-REG-004`: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)**
  * **Business Logic:** Tầng nghiệp vụ Use Case ngăn chặn triệt để yêu cầu đăng ký vai trò `ADMIN`, trả về mã lỗi `FORBIDDEN` (HTTP 403).
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-REG-004: Chặn tuyệt đối tự đăng ký với vai trò ADMIN (BR-03)")
void shouldRejectAdminRegistrationAttempt() {
  // Arrange: Giả lập email chưa tồn tại
  when(accountRepository.existsByEmail("admin@edufit.vn")).thenReturn(false);

  // Act & Assert: Đăng ký vai trò ADMIN bị chặn với lỗi FORBIDDEN
  InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> registerService.register(
          "admin@edufit.vn",
          "Password123@",
          "Admin User",
          AccountRole.ADMIN
      )
  );

  assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Chuẩn bị dữ liệu đăng ký với `AccountRole.ADMIN`.
    * *Act & Assert:* `registerService` ném `InvalidOperationException` với `ErrorCode.FORBIDDEN`.

---

#### [5] Nhóm Kiểm Thử Xác Thực & Khóa Tài Khoản ([`AuthenticateAccountServiceTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/AuthenticateAccountServiceTest.java))

* **Test Case `IAM-AUTH-001`: Đăng nhập thành công khi thông tin hợp lệ (BR-06, NFR-01)**
  * **Business Logic:** Khi email và mật khẩu chính xác, tài khoản ở trạng thái `ACTIVE`, hệ thống xác thực thành công, ghi nhận `lastLoginAt` và reset `failedLoginAttempts` về 0.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-AUTH-001: Đăng nhập thành công khi mật khẩu đúng và tài khoản ACTIVE")
void shouldAuthenticateSuccessfullyWhenCredentialsAreValid() {
  // Arrange: Tài khoản ACTIVE với mật khẩu đã băm Argon2id
  Account account = Account.of(
      UUID.randomUUID(),
      "student@edufit.vn",
      "$argon2id$hashed...",
      "Nguyen Van A",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );
  when(accountRepository.findByEmail("student@edufit.vn")).thenReturn(Optional.of(account));
  when(passwordService.verify(eq(account.getPasswordHash()), eq("CorrectPassword123@"))).thenReturn(true);

  // Act: Thực hiện xác thực danh tính
  Account authenticated = authService.authenticate("student@edufit.vn", "CorrectPassword123@");

  // Assert: Xác thực thành công và cập nhật audit timestamp
  assertNotNull(authenticated);
  assertEquals(0, authenticated.getFailedLoginAttempts());
  assertEquals(Instant.parse("2026-09-30T10:00:00Z"), authenticated.getLastLoginAt());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Mock `accountRepository` tìm thấy tài khoản và `passwordService.verify(...)` trả về `true`.
    * *Act:* Gọi `authService.authenticate(...)`.
    * *Assert:* Kiểm tra đối tượng trả về hợp lệ, `failedLoginAttempts = 0`, `lastLoginAt` khớp với mốc `fixedClock`.

---

* **Test Case `IAM-AUTH-002`: Trả về cùng mã lỗi INVALID_CREDENTIALS khi sai mật khẩu hoặc sai email (NFR-06)**
  * **Business Logic:** Để chống dò quét tài khoản (Account Enumeration) và tấn công đo thời gian (Timing Attack), dù email không tồn tại hay sai mật khẩu, hệ thống đều phản hồi cùng mã lỗi `INVALID_CREDENTIALS`. Khi email không tồn tại, hệ thống thực thi Dummy Argon2 Verification để giữ thời gian phản hồi đồng nhất.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-AUTH-002: Trả về cùng mã lỗi INVALID_CREDENTIALS khi sai mật khẩu hoặc sai email (NFR-06)")
void shouldReturnSameErrorForWrongPasswordAndNonExistentEmail() {
  // Case 1: Email không tồn tại trong hệ thống
  when(accountRepository.findByEmail("unknown@edufit.vn")).thenReturn(Optional.empty());
  InvalidOperationException ex1 = assertThrows(
      InvalidOperationException.class,
      () -> authService.authenticate("unknown@edufit.vn", "AnyPassword123@")
  );
  assertEquals(ErrorCode.INVALID_CREDENTIALS, ex1.getErrorCode(), "Email không tồn tại phải trả về INVALID_CREDENTIALS");

  // Case 2: Email tồn tại nhưng sai mật khẩu
  Account account = Account.of(
      UUID.randomUUID(),
      "student@edufit.vn",
      "$argon2id$hashed...",
      "Nguyen Van A",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );
  when(accountRepository.findByEmail("student@edufit.vn")).thenReturn(Optional.of(account));
  when(passwordService.verify(any(), eq("WrongPassword123@"))).thenReturn(false);

  InvalidOperationException ex2 = assertThrows(
      InvalidOperationException.class,
      () -> authService.authenticate("student@edufit.vn", "WrongPassword123@")
  );
  assertEquals(ErrorCode.INVALID_CREDENTIALS, ex2.getErrorCode(), "Sai mật khẩu cũng phải trả về INVALID_CREDENTIALS");
}
```
  * **Giải thích (AAA):**
    * *Case 1 (Email không có thật):* Mock `findByEmail` trả về `Optional.empty()`. Hàm ném `INVALID_CREDENTIALS`.
    * *Case 2 (Sai mật khẩu):* Mock tìm thấy `account` nhưng `passwordService.verify` trả về `false`. Hàm ném cùng mã `INVALID_CREDENTIALS`. Kẻ tấn công bên ngoài không thể phân biệt tài khoản có tồn tại hay không.

---

* **Test Case `IAM-AUTH-004`: Khóa tài khoản 15 phút sau 5 lần nhập sai liên tiếp (BR-06)**
  * **Business Logic:** Nhập sai mật khẩu liên tiếp 5 lần sẽ kích hoạt khóa tài khoản 15 phút và ném lỗi `ACCOUNT_LOCKED`.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-AUTH-004: Khóa tài khoản 15 phút sau 5 lần nhập sai liên tiếp (BR-06)")
void shouldLockAccountAfterFiveConsecutiveFailures() {
  // Arrange: Tài khoản ACTIVE và mật khẩu luôn sai
  Account account = Account.of(
      UUID.randomUUID(),
      "target@edufit.vn",
      "$argon2id$hashed...",
      "Target User",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );
  when(accountRepository.findByEmail("target@edufit.vn")).thenReturn(Optional.of(account));
  when(passwordService.verify(any(String.class), any(String.class))).thenReturn(false);

  // Act & Assert: 4 lần đầu nhập sai ném INVALID_CREDENTIALS
  for (int i = 1; i <= 4; i++) {
    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> authService.authenticate("target@edufit.vn", "WrongPassword")
    );
    assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    assertEquals(i, account.getFailedLoginAttempts());
  }

  // Act & Assert: Lần thứ 5 ném ACCOUNT_LOCKED và kích hoạt khóa
  InvalidOperationException lockEx = assertThrows(
      InvalidOperationException.class,
      () -> authService.authenticate("target@edufit.vn", "WrongPassword")
  );
  assertEquals(ErrorCode.ACCOUNT_LOCKED, lockEx.getErrorCode());
  assertEquals(5, account.getFailedLoginAttempts());
  assertNotNull(account.getLockedUntil());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Giả lập tài khoản và cấu hình `passwordService.verify` luôn trả về `false`.
    * *Act & Assert:* Lặp 4 lần đầu xác nhận mã lỗi `INVALID_CREDENTIALS` và bộ đếm tăng dần. Đến lần thứ 5, xác nhận ngoại lệ đổi thành `ACCOUNT_LOCKED` và `account.getLockedUntil()` khác null.

---

#### [6] Nhóm Kiểm Thử Quên & Đổi Mật Khẩu ([`PasswordResetAndChangeTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/PasswordResetAndChangeTest.java))

* **Test Case `IAM-PWD-001`: Yêu cầu reset mật khẩu trả về trung tính khi email không tồn tại (NFR-06)**
  * **Business Logic:** Khi người dùng gửi yêu cầu quên mật khẩu cho một email không có trong cơ sở dữ liệu, hệ thống không báo lỗi "Email không tồn tại" mà vẫn phản hồi thành công (200 OK) và tuyệt đối không gửi email, ngăn chặn việc dò quét danh sách người dùng.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-PWD-001: Yêu cầu reset mật khẩu trả về trung tính khi email không tồn tại (NFR-06)")
void shouldReturnNeutralResultEvenWhenEmailDoesNotExist() {
  // Arrange: Email hoàn toàn không có trong cơ sở dữ liệu
  when(accountRepository.findByEmail("unknown@edufit.vn")).thenReturn(Optional.empty());

  // Act & Assert: Không ném ngoại lệ, trả về trung tính và không gửi mail
  assertDoesNotThrow(() -> requestResetService.requestReset("unknown@edufit.vn"));
  verify(emailSender, never()).sendPasswordResetEmail(any(), any(), any());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Mock `findByEmail` trả về `empty`.
    * *Act:* Gọi `requestResetService.requestReset(...)`.
    * *Assert:* `assertDoesNotThrow` xác nhận trả về thành công; `verify(..., never())` đảm bảo phương thức gửi email không bao giờ bị kích hoạt.

---

* **Test Case `IAM-PWD-002`: Đặt lại mật khẩu thành công tiêu thụ token và cập nhật mật khẩu mới (BR-05, NFR-11)**
  * **Business Logic:** Người dùng cung cấp raw token hợp lệ và mật khẩu mới đạt chính sách. Hệ thống tiêu thụ token nguyên tử và cập nhật hash Argon2id mới cho tài khoản.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-PWD-002: Đặt lại mật khẩu thành công tiêu thụ token và cập nhật mật khẩu mới")
void shouldResetPasswordSuccessfully() {
  // Arrange: Token hợp lệ tồn tại trong CSDL kèm Account
  Account account = Account.of(
      UUID.randomUUID(),
      "student@edufit.vn",
      "$argon2id$old_hash",
      "Student A",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );

  String rawToken = "reset-raw-token-1234567890";
  String tokenHash = tokenCryptoService.hashToken(rawToken);

  AccountToken token = AccountToken.create(
      account,
      TokenType.PASSWORD_RESET,
      tokenHash,
      Instant.parse("2026-09-30T10:30:00Z")
  );

  when(accountTokenRepository.findByTokenHashAndTokenType(eq(tokenHash), eq(TokenType.PASSWORD_RESET)))
      .thenReturn(Optional.of(token));
  when(passwordService.verify(eq(account.getPasswordHash()), eq("NewPassword123@"))).thenReturn(false);

  // Act: Tiến hành đặt lại mật khẩu với token và mật khẩu mới
  resetPasswordService.resetPassword(rawToken, "NewPassword123@");

  // Assert: Token đã bị đánh dấu sử dụng và mật khẩu đã được cập nhật hash mới
  assertTrue(token.isUsed(), "Token phải được đánh dấu đã sử dụng");
  assertEquals("$argon2id$new_hash", account.getPasswordHash(), "Mật khẩu của tài khoản phải đổi sang hash mới");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Tạo tài khoản với `old_hash`, tạo `AccountToken` liên kết với `tokenHash`.
    * *Act:* Gọi `resetPasswordService.resetPassword(rawToken, "NewPassword123@")`.
    * *Assert:* Xác nhận `token.isUsed() == true` và `account.getPasswordHash()` mang giá trị hash mới.

---

* **Test Case `IAM-PWD-003`: Đổi mật khẩu từ chối khi mật khẩu hiện tại không đúng (UC1.4)**
  * **Business Logic:** Người dùng đang đăng nhập đổi mật khẩu nhưng nhập sai mật khẩu cũ sẽ bị từ chối với mã lỗi `INVALID_CURRENT_PASSWORD`.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-PWD-003: Đổi mật khẩu từ chối khi mật khẩu hiện tại không đúng")
void shouldRejectWhenCurrentPasswordIsIncorrect() {
  // Arrange: Mock tài khoản và cấu hình so khớp mật khẩu hiện tại trả về false
  UUID accountId = UUID.randomUUID();
  Account account = Account.of(
      accountId,
      "user@edufit.vn",
      "$argon2id$correct_current_hash",
      "User D",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );

  when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
  when(passwordService.verify(eq(account.getPasswordHash()), eq("WrongCurrentPass"))).thenReturn(false);

  // Act & Assert: Phải ném ngoại lệ INVALID_CURRENT_PASSWORD
  InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> changePasswordService.changePassword(accountId, "WrongCurrentPass", "NewPassword123@")
  );

  assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Mock `accountRepository.findById` và `passwordService.verify` cho kết quả `false`.
    * *Act & Assert:* Xác nhận ngoại lệ `InvalidOperationException` được ném với mã `INVALID_CURRENT_PASSWORD`.

---

* **Test Case `IAM-PWD-004`: Đổi mật khẩu từ chối khi mật khẩu mới trùng mật khẩu cũ (BR-02)**
  * **Business Logic:** Chính sách mật khẩu cấm người dùng đặt lại hoặc đổi mật khẩu mới trùng khớp hoàn toàn với mật khẩu đang sử dụng (`PASSWORD_REUSE_FORBIDDEN`).
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-PWD-004: Đổi mật khẩu từ chối khi mật khẩu mới trùng mật khẩu hiện tại")
void shouldRejectWhenNewPasswordMatchesCurrentPassword() {
  // Arrange: So khớp mật khẩu mới với mật khẩu hiện tại trả về true (bị trùng)
  UUID accountId = UUID.randomUUID();
  Account account = Account.of(
      accountId,
      "user@edufit.vn",
      "$argon2id$current_hash",
      "User D",
      AccountRole.STUDENT,
      AccountStatus.ACTIVE
  );

  when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
  when(passwordService.verify(eq(account.getPasswordHash()), eq("SamePassword123@"))).thenReturn(true);

  // Act & Assert: Phải ném ngoại lệ PASSWORD_REUSE_FORBIDDEN
  InvalidOperationException ex = assertThrows(
      InvalidOperationException.class,
      () -> changePasswordService.changePassword(accountId, "SamePassword123@", "SamePassword123@")
  );

  assertEquals(ErrorCode.PASSWORD_REUSE_FORBIDDEN, ex.getErrorCode());
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Cấu hình `passwordService.verify` trả về `true` khi so khớp mật khẩu mới với chuỗi hash cũ.
    * *Act & Assert:* Gọi `changePassword` và xác nhận ném ngoại lệ với mã `ErrorCode.PASSWORD_REUSE_FORBIDDEN`.

---

#### [7] Nhóm Kiểm Thử Mật Mã Băm Argon2id ([`Argon2PasswordServiceTest.java`](file:///e:/iykyk/S5/SWP391/Projects/EduFit/backend/modules/iam/src/test/java/vn/edufit/iam/Argon2PasswordServiceTest.java))

* **Test Case `IAM-SEC-001`: Băm mật khẩu ra định dạng Argon2id và so khớp đúng (NFR-01)**
  * **Business Logic:** Chuỗi băm sinh ra phải bắt đầu bằng `$argon2id$`, tuân thủ tham số an toàn (64 MiB, 3 iterations) và so khớp thành công với mật khẩu gốc.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-SEC-001: Băm mật khẩu và so khớp thành công với mật khẩu đúng")
void shouldHashAndVerifyPasswordSuccessfully() {
  // Arrange: Mật khẩu plaintext
  String rawPassword = "StrongPassword123@";

  // Act: Thực hiện băm Argon2id
  String hash = passwordService.hash(rawPassword);

  // Assert: Kiểm tra định dạng chuỗi băm và kết quả so khớp
  assertNotNull(hash);
  assertTrue(hash.startsWith("$argon2id$"), "Chuỗi hash phải theo chuẩn Argon2id");
  assertTrue(passwordService.verify(hash, rawPassword), "Mật khẩu đúng phải so khớp thành công");
}
```
  * **Giải thích (AAA):**
    * *Arrange:* Chuẩn bị chuỗi mật khẩu thô `"StrongPassword123@"`.
    * *Act:* Băm mật khẩu qua `passwordService.hash(rawPassword)`.
    * *Assert:* Xác nhận tiền tố `$argon2id$` và hàm `verify` trả về `true`.

---

* **Test Case `IAM-SEC-002`: Salt ngẫu nhiên tạo chuỗi băm khác biệt cho cùng một mật khẩu (NFR-01)**
  * **Business Logic:** Thuật toán tự động sinh salt bảo mật ngẫu nhiên (CSPRNG) cho mỗi lần băm, đảm bảo hai lần băm cùng một mật khẩu cho ra hai chuỗi băm hoàn toàn khác nhau, vô hiệu hóa hoàn toàn kỹ thuật tấn công Rainbow Table.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-SEC-002: Cùng một mật khẩu tạo ra các chuỗi hash khác nhau nhờ salt ngẫu nhiên")
void shouldProduceDifferentHashesForSamePasswordDueToSalt() {
  // Arrange: Một mật khẩu duy nhất
  String rawPassword = "SamePassword123#";

  // Act: Băm 2 lần riêng biệt
  String hash1 = passwordService.hash(rawPassword);
  String hash2 = passwordService.hash(rawPassword);

  // Assert: Hai chuỗi hash phải khác nhau nhưng đều verify thành công
  assertNotEquals(hash1, hash2, "Mỗi lần băm phải sinh salt mới nên hash không được trùng nhau");
  assertTrue(passwordService.verify(hash1, rawPassword));
  assertTrue(passwordService.verify(hash2, rawPassword));
}
```
  * **Giải thích (AAA):**
    * *Act:* Băm mật khẩu 2 lần liên tiếp được `hash1` và `hash2`.
    * *Assert:* `assertNotEquals(hash1, hash2)` xác nhận tính ngẫu nhiên của salt. Cả hai đều verify thành công với chuỗi mật khẩu gốc.

---

* **Test Case `IAM-SEC-003`: Từ chối khi so khớp với mật khẩu sai (NFR-01)**
  * **Business Logic:** Phương thức `verify` phải trả về `false` khi mật khẩu kiểm tra không trùng khớp với mật khẩu đã băm.
  * **Đoạn mã kiểm thử:**
```java
@Test
@DisplayName("IAM-SEC-003: Từ chối khi so khớp với mật khẩu sai")
void shouldRejectWrongPassword() {
  // Arrange: Chuẩn bị 2 mật khẩu khác nhau
  String correctPassword = "CorrectPassword123$";
  String wrongPassword = "WrongPassword456%";

  // Act: Băm mật khẩu đúng và so khớp với mật khẩu sai
  String hash = passwordService.hash(correctPassword);

  // Assert: verify() bắt buộc phải trả về false
  assertFalse(passwordService.verify(hash, wrongPassword), "Mật khẩu sai phải trả về false");
}
```
  * **Giải thích (AAA):**
    * *Arrange & Act:* Băm `correctPassword`, gọi hàm `verify` với `wrongPassword`.
    * *Assert:* `assertFalse` xác nhận hệ thống từ chối mật khẩu không khớp.

---

## 5. Nhật ký Thực thi & Kết quả Surefire (Execution Log)

* **Tổng số kiểm thử đã chạy trong module IAM:** **32 tests**  
* **Thành công (Passed):** **32 / 32 (100%)**  
* **Thất bại (Failures):** **0**  
* **Lỗi (Errors):** **0**  
* **Bị bỏ qua (Skipped):** **0**  
* **Thời gian thực thi:** **~1.7 giây** (JVM in-memory, không phụ thuộc CSDL bên ngoài)  
* **Trạng thái Build:** **BUILD SUCCESS**  
* **Kiểm thử kiến trúc toàn hệ thống (ArchUnit):** **BUILD SUCCESS** — 0 vi phạm ranh giới module  
