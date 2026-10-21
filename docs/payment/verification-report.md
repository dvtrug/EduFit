# Payment verification

- P00: `435da1b` — plan/checklist đã push.
- P01: domain state machine: RED testCompile thiếu SessionCharge; GREEN 7 tests SessionChargeTest, Maven reactor SUCCESS. Test mốc 1h/24h, uncompleted session, double confirm, báo cáo lệch, partial và full refund.

- P02: clean Maven reactor PASS: SessionChargeTest (7), ArchitectureTest (3), ArchitectureBoundaryTest (28), FlywayMigrationScriptTest (2). Lần đầu ArchitectureBoundaryTest lỗi do stale target từ branch cũ; clean đã xác minh hết lỗi. PostgreSQL flow test đang bổ sung, không coi naming test là test migration thực thi.

- P03: module Payment 17 tests PASS (domain 7, payOS 4, access 2, validation 1, wallet webhook 3); package app reactor SUCCESS. HMAC verifier khởi tạo HTTP client lazy để chạy offline; không dùng constructor failure làm test chữ ký.
- PostgreSQL 17 PaymentFlowIT: 10/10 PASS, 16 migrations trên schema mới; validate/rerun no-op. Concurrent confirm, insufficient-funds rollback, parent payer vs student confirmer, 1h auto, 24h timeout, matching reports chờ Admin, partial fee, cancelled-session rejection, duplicate ref/whitespace, withdrawal rejection.
- Independent review: sửa chặn Admin charge buổi chưa COMPLETED, bắt buộc receiving account payOS, trim reference và lưu manual QR để mở lại. Review lại không còn defect trong fixes. User chọn không thêm cross-model review.

- Startup smoke phát hiện Clock bean mới gây ambiguity ở IAM. Đã thay bằng PaymentTime wrapper, không đăng ký Clock toàn cục; thêm PaymentApplicationIT khởi động cả app và kiểm tra HTTP health 200 / anonymous wallet 401. Gate cuối: 17 Payment unit tests + 44 app/PG/architecture tests = 61 PASS, package SUCCESS.
- FE: 24 tests / 8 files PASS sau tách test ví và cập nhật nhãn; type-check PASS; lint Payment 0 errors, 0 warnings. Production build Webpack PASS bằng CA Windows (không disable TLS); default Turbopack có lỗi spawn CSS riêng máy.

- Browser phát hiện lỗi `ORDER BY created_at is ambiguous` ở topup/withdrawal history. Regression test tái hiện RED trên PostgreSQL; sửa projection thành danh sách cột duy nhất và sort theo timestamp gốc + id. Bao phủ ví từng chủ sở hữu, danh sách Admin, dữ liệu rỗng/có dữ liệu và offset.
- Sau sửa history: PaymentFlowIT 11/11 + PaymentApplicationIT 1/1 PASS, package reactor SUCCESS; không thay migration.

- History hotfix `3f5b4d7` đã push.
- P04a: SDK typed + WalletPanel + 2 tests cấu hình kênh nạp/không tự credit. Nằm trong FE gate 24 PASS, type-check/lint PASS.
- Browser Admin phát hiện thiếu bind `offset` ở danh sách tranh chấp. Regression RED tái hiện syntax error; GREEN 11 PaymentFlowIT PASS sau sửa, gồm hàng đợi rỗng/có báo cáo và phân trang.

- P04a `c32f518` và Admin pagination hotfix `2c5c105` đã push.
- P04b: SessionPaymentCard + 6 tests: xác nhận chủ động, chặn trước COMPLETED, mở tranh chấp, báo cáo mức tiền một phần, hết hạn/phân quyền, xác nhận giá trước cấp tiền. FE gate PASS.
