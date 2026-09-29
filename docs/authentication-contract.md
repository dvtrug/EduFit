# EDUFIT AUTHENTICATION API CONTRACT & THREAT MODEL

- **Module:** IAM (Identity & Access Management) — UC1.1 đến UC1.4
- **Trạng thái:** Frozen (Đã chốt)
- **Tài liệu liên quan:** [ADR-001](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/ADR-001-tech-stack.md), [ADR-002](file:///e:/iykyk/S5/SWP391/Projects/EduFit/docs/ADR-002-multi-module-architecture.md), [tasks/plan.md](file:///e:/iykyk/S5/SWP391/Projects/EduFit/tasks/plan.md)

---

## 1. Nguyên tắc Chung & Quy chuẩn Dữ liệu

### 1.1. Cấu trúc Phản hồi Chuẩn (Response Envelope)
Mọi phản hồi API đều được bọc trong cấu trúc chuẩn của hệ thống:

* **Thành công (Success - 2xx):**
```json
{
  "success": true,
  "message": "Mô tả kết quả thành công",
  "data": { ... },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```

* **Thất bại (Error - 4xx, 5xx):**
```json
{
  "success": false,
  "code": "ERROR_CODE_STRING",
  "message": "Thông báo lỗi hiển thị cho người dùng",
  "details": [
    {
      "field": "email",
      "message": "Email không đúng định dạng"
    }
  ],
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```

### 1.2. Chính sách Phiên làm việc (Session & Cookie Policy)
* **Loại phiên:** Stateful Session quản lý bởi Spring Session JDBC (lưu trong bảng `SPRING_SESSION` tại PostgreSQL).
* **Cookie Name:** `EDUFIT_SESSION`
* **Thuộc tính Cookie:**
  * `HttpOnly`: Bắt buộc `true` (Javascript phía trình duyệt không được đọc cookie).
  * `SameSite`: `Lax` (Bảo vệ chống tấn công CSRF cross-site).
  * `Path`: `/`
  * `Secure`: `false` ở local dev (`http://localhost`), tự động kích hoạt `true` ở production (`https://`).
* **Thời gian sống:** Hết hạn sau **60 phút bất hoạt (sliding inactivity)**.
* **Xử lý đa phiên (Multi-device invalidation):**
  * Đăng xuất thông thường (`POST /logout`): Chỉ thu hồi phiên của thiết bị hiện tại.
  * Đổi mật khẩu thành công (`POST /change-password`): Duy trì phiên hiện tại, hủy tất cả các phiên khác.
  * Đặt lại mật khẩu thành công (`POST /reset-password`): Hủy toàn bộ mọi phiên đang hoạt động.

### 1.3. Chính sách CSRF (Cross-Site Request Forgery)
* Tất cả các request có tính thay đổi dữ liệu (`POST`, `PUT`, `DELETE`) đều phải gửi kèm Header `X-XSRF-TOKEN`.
* Frontend lấy token thông qua endpoint `GET /api/v1/auth/csrf` trước khi thực hiện gửi form.

---

## 2. Chi tiết Danh mục API (Endpoints Specification)

### 2.1. `GET /api/v1/auth/csrf`
* **Mục đích:** Lấy mã CSRF token và thiết lập cookie `XSRF-TOKEN`.
* **Phân quyền:** Public (Bất kỳ ai cũng có thể gọi).
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "CSRF token generated successfully",
  "data": {
    "token": "4a7f23a1-...",
    "headerName": "X-XSRF-TOKEN",
    "parameterName": "_csrf"
  },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```

---

### 2.2. `POST /api/v1/auth/register` (UC1.1)
* **Mục đích:** Đăng ký tài khoản mới ở trạng thái `PENDING_VERIFICATION` và gửi email chứa token xác thực.
* **Phân quyền:** Guest (Chưa đăng nhập).
* **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123@",
  "fullName": "Nguyen Van A",
  "role": "STUDENT" // Chỉ cho phép "STUDENT", "PARENT", "TUTOR". Cấm tuyệt đối "ADMIN".
}
```
* **Phản hồi 201 Created:**
```json
{
  "success": true,
  "message": "Đăng ký thành công. Vui lòng kiểm tra email để kích hoạt tài khoản.",
  "data": {
    "accountId": "a3b12345-...",
    "email": "user@example.com",
    "role": "STUDENT",
    "status": "PENDING_VERIFICATION"
  },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi có thể xảy ra:**
  * `400 BAD_REQUEST` + `VALIDATION_FAILED`: Dữ liệu đầu vào sai định dạng (mật khẩu yếu, email không đúng, role cấm).
  * `409 CONFLICT` + `USER_ALREADY_EXISTS`: Email đã được đăng ký bởi tài khoản khác trong hệ thống.

---

### 2.3. `POST /api/v1/auth/verify-email` (UC1.1)
* **Mục đích:** Xác thực tài khoản bằng token nhận được từ liên kết email và chuyển trạng thái sang `ACTIVE`.
* **Phân quyền:** Guest.
* **Request Body:**
```json
{
  "token": "raw-256-bit-token-string"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Kích hoạt tài khoản thành công. Bạn có thể đăng nhập ngay bây giờ.",
  "data": {
    "email": "user@example.com",
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `400 BAD_REQUEST` + `INVALID_TOKEN`: Token không tồn tại, sai định dạng.
  * `400 BAD_REQUEST` + `TOKEN_EXPIRED`: Token đã quá hạn 24 giờ.
  * `400 BAD_REQUEST` + `TOKEN_ALREADY_USED`: Token đã được sử dụng trước đó.

---

### 2.4. `POST /api/v1/auth/resend-verification` (UC1.1)
* **Mục đích:** Vô hiệu hóa token cũ, sinh token mới và gửi lại email kích hoạt. Cooldown tối thiểu 60 giây giữa các lần yêu cầu.
* **Phân quyền:** Guest.
* **Request Body:**
```json
{
  "email": "user@example.com"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Nếu email tồn tại và chưa kích hoạt, email xác thực mới đã được gửi.",
  "data": null,
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `429 TOO_MANY_REQUESTS` + `RATE_LIMIT_EXCEEDED`: Yêu cầu gửi lại quá nhanh (trong vòng 60 giây).

---

### 2.5. `POST /api/v1/auth/login` (UC1.2)
* **Mục đích:** Xác thực danh tính qua email/mật khẩu, sinh session mới và ghi nhận cookie `EDUFIT_SESSION`.
* **Phân quyền:** Guest.
* **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123@"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "id": "a3b12345-...",
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "role": "STUDENT",
    "redirectUrl": "/student"
  },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `401 UNAUTHORIZED` + `INVALID_CREDENTIALS`: Sai email hoặc mật khẩu (không phân biệt rõ để chống enumeration).
  * `403 FORBIDDEN` + `USER_NOT_ACTIVE`: Tài khoản đang ở trạng thái `PENDING_VERIFICATION` hoặc `DEACTIVATED`.
  * `423 LOCKED` + `ACCOUNT_LOCKED`: Tài khoản bị khóa tạm thời 15 phút do nhập sai quá 5 lần.

---

### 2.6. `GET /api/v1/auth/me`
* **Mục đích:** Đọc thông tin phiên hiện tại của người dùng đang đăng nhập.
* **Phân quyền:** Authenticated (Phải có session hợp lệ).
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Thông tin phiên hiện tại",
  "data": {
    "id": "a3b12345-...",
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "role": "STUDENT"
  },
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `401 UNAUTHORIZED` + `UNAUTHORIZED`: Phiên không hợp lệ hoặc đã hết hạn.

---

### 2.7. `POST /api/v1/auth/logout` (UC1.3)
* **Mục đích:** Hủy phiên hiện tại trên server và xóa cookie `EDUFIT_SESSION` trên trình duyệt.
* **Phân quyền:** Authenticated hoặc Expired-safe (nếu phiên đã hết hạn vẫn trả về 200 và xóa cookie).
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Đăng xuất thành công",
  "data": null,
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```

---

### 2.8. `POST /api/v1/auth/forgot-password` (UC1.4)
* **Mục đích:** Yêu cầu gửi email đặt lại mật khẩu. Luôn trả về phản hồi trung tính để ngăn chặn dò quét email (Account Enumeration).
* **Phân quyền:** Guest.
* **Request Body:**
```json
{
  "email": "user@example.com"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hòm thư của bạn.",
  "data": null,
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```

---

### 2.9. `POST /api/v1/auth/reset-password` (UC1.4)
* **Mục đích:** Đặt mật khẩu mới bằng reset token (hạn 30 phút). Khi thành công, vô hiệu hóa token và hủy toàn bộ các session đang hoạt động.
* **Phân quyền:** Guest.
* **Request Body:**
```json
{
  "token": "raw-reset-token-string",
  "newPassword": "NewStrongPassword123@"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới.",
  "data": null,
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `400 BAD_REQUEST` + `INVALID_TOKEN`: Token không hợp lệ.
  * `400 BAD_REQUEST` + `TOKEN_EXPIRED`: Token đã quá hạn 30 phút.
  * `400 BAD_REQUEST` + `TOKEN_ALREADY_USED`: Token đã được sử dụng.
  * `400 BAD_REQUEST` + `VALIDATION_FAILED`: Mật khẩu mới không đáp ứng độ phức tạp.

---

### 2.10. `POST /api/v1/auth/change-password` (UC1.4)
* **Mục đích:** Đổi mật khẩu khi đang đăng nhập. Yêu cầu nhập đúng mật khẩu hiện tại, không cho phép trùng mật khẩu cũ, duy trì phiên hiện tại và hủy toàn bộ các phiên khác.
* **Phân quyền:** Authenticated.
* **Request Body:**
```json
{
  "currentPassword": "OldPassword123@",
  "newPassword": "NewStrongPassword123@"
}
```
* **Phản hồi 200 OK:**
```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công. Các phiên đăng nhập trên thiết bị khác đã được đăng xuất an toàn.",
  "data": null,
  "timestamp": "2026-09-29T15:00:00.000Z"
}
```
* **Mã lỗi:**
  * `400 BAD_REQUEST` + `INVALID_CURRENT_PASSWORD`: Mật khẩu hiện tại không đúng.
  * `400 BAD_REQUEST` + `PASSWORD_REUSE_FORBIDDEN`: Mật khẩu mới trùng với mật khẩu cũ.
  * `401 UNAUTHORIZED` + `UNAUTHORIZED`: Chưa đăng nhập hoặc phiên đã hết hạn.

---

## 3. Mô hình Đe dọa & Cơ chế Phòng thủ (Threat Model & Mitigations)

| Nguy cơ tấn công (Threat) | Mức độ rủi ro | Cơ chế phòng thủ kỹ thuật đã chốt |
|---|:---:|---|
| **Dò quét tài khoản (Account Enumeration)** | Cao | 1. Khi đăng nhập sai: luôn trả về mã `INVALID_CREDENTIALS` chung cho cả sai email và sai mật khẩu.<br>2. Khi yêu cầu quên mật khẩu (`forgot-password`): luôn trả về cùng thông điệp thành công 200 OK dù email có trong hệ thống hay không. |
| **Tấn công dò mật khẩu (Brute Force / Credential Stuffing)** | Cao | 1. Sau 5 lần nhập sai liên tiếp, khóa tài khoản 15 phút (`failed_login_attempts >= 5` -> `locked_until`).<br>2. Rate limiting theo IP đối với các endpoint nhạy cảm (`/login`, `/register`, `/forgot-password`). |
| **Đánh cắp hoặc Tái sử dụng Token (Token Replay / Leakage)** | Rất cao | 1. **Tuyệt đối không lưu raw token vào DB**: Token ngẫu nhiên 256-bit được băm SHA-256 trước khi lưu vào bảng `account_token`.<br>2. Tiêu thụ token nguyên tử (`UPDATE account_token SET used_at = NOW() WHERE token_hash = :hash AND used_at IS NULL AND expires_at > NOW()`). Nếu không có dòng nào được cập nhật -> từ chối ngay lập tức. |
| **Cố định phiên (Session Fixation)** | Trung bình | Spring Security tự động đổi `sessionId` mới ngay sau khi xác thực đăng nhập thành công (`sessionFixation().migrateSession()`). |
| **Tấn công giả mạo yêu cầu (CSRF)** | Cao | Bắt buộc kiểm tra token CSRF qua Header `X-XSRF-TOKEN` cho toàn bộ các phương thức mutating `POST`/`PUT`/`DELETE`. |
| **Chuyển hướng độc hại (Open Redirect)** | Trung bình | Tham số `redirectUrl` hoặc `returnUrl` chỉ chấp nhận các đường dẫn tương đối an toàn nằm trong danh sách cho phép (`/student`, `/parent`, `/tutor`, `/admin`, hoặc bắt đầu bằng `/` đơn và không chứa `//` hay `\`). |
| **Lộ thông tin nhạy cảm qua Log / Response** | Cao | 1. Cấu hình Jackson không serialize trường password/token.<br>2. Bộ lọc Logging chặn không bao giờ in giá trị mật khẩu thô, raw token hoặc session ID ra console/log files. |
| **Thuật toán băm mật khẩu (Password Hashing)** | Nghiêm ngặt | Sử dụng thuật toán hiện đại **Argon2id** (thay vì MD5/SHA thông thường) để chống tấn công GPU/ASIC brute-force. |

---

## 4. Các Đường dẫn Chuyển hướng Mặc định theo Vai trò (Role Landing Paths)

Khi đăng nhập thành công mà không có `returnUrl`, người dùng sẽ được chuyển hướng về trang tổng quan tương ứng:

* `STUDENT` ➔ `/student`
* `PARENT`  ➔ `/parent`
* `TUTOR`   ➔ `/tutor`
* `ADMIN`   ➔ `/admin`
