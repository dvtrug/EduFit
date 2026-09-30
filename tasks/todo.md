# Checklist RBL Authentication — IAM

## Phase 0 — Hiệu chỉnh nền tảng

- [ ] RBL-00 Chuẩn hóa mã BR/NFR trong code, test và báo cáo.
- [ ] RBL-00 Đổi tuyên bố chưa có code sang `Not Implemented`/`Not Run`.
- [ ] RBL-01 Sửa test lockout hết hạn và mô tả reset counter.
- [ ] RBL-01 Thêm boundary test tại đúng `expiresAt`.
- [ ] RBL-01 Validate/test SHA-256 token hash 64 ký tự hex.
- [ ] Checkpoint A: chạy toàn bộ IAM tests.

## Phase 1 — Password và Registration

- [x] RBL-02 Test và triển khai `PasswordPolicy`.
- [x] RBL-03 Test register success, role, duplicate, normalize, weak password, safe result.
- [x] RBL-03 Triển khai register bằng repository/password-hasher ports.
- [x] Checkpoint B: BR-01 đến BR-04 và NFR-01 xanh.

## Phase 2 — Password reset token

- [x] RBL-04 Test/triển khai secure random token và SHA-256 hash.
- [x] RBL-04 Test TTL, single-use và không lưu raw token.
- [x] Checkpoint C: password reset token có TTL 30 phút và dùng một lần.

## Phase 3 — Login

- [x] RBL-06 Test login thành công và safe identity.
- [x] RBL-06 Test enumeration resistance.
- [x] RBL-06 Test trạng thái account và lockout đầy đủ.
- [x] Checkpoint D: register -> authenticate không cần HTTP.

## Phase 4 — Password management

- [x] RBL-07 Test/triển khai forgot password neutral response.
- [x] RBL-07 Test/triển khai reset password và revoke toàn bộ session.
- [x] RBL-08 Test/triển khai change password và revoke session khác.

## Phase 5 — Persistence & Web Layer

- [x] RBL-09 Triển khai đầy đủ REST Controllers và Request/Response DTOs tại `vn.edufit.iam.web`.
- [x] RBL-09 Kết nối Spring Security Session Context (`EDUFIT_SESSION` cookie).
- [x] RBL-09 Hoàn thành `IamFacadeImpl` cho giao tiếp liên module.

## Ngoài phạm vi hiện tại

- [ ] SMTP/Mailpit adapter thật.
- [ ] Frontend Authentication UI.
- [ ] OAuth/JWT/social login.
