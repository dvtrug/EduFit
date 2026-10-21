# Payment verification

- P00: `435da1b` — plan/checklist đã push.
- P01: domain state machine: RED testCompile thiếu SessionCharge; GREEN 7 tests SessionChargeTest, Maven reactor SUCCESS. Test mốc 1h/24h, uncompleted session, double confirm, báo cáo lệch, partial và full refund.

- P02: clean Maven reactor PASS: SessionChargeTest (7), ArchitectureTest (3), ArchitectureBoundaryTest (28), FlywayMigrationScriptTest (2). Lần đầu ArchitectureBoundaryTest lỗi do stale target từ branch cũ; clean đã xác minh hết lỗi. PostgreSQL flow test đang bổ sung, không coi naming test là test migration thực thi.

- P03: module Payment 17 tests PASS (domain 7, payOS 4, access 2, validation 1, wallet webhook 3); package app reactor SUCCESS. HMAC verifier khởi tạo HTTP client lazy để chạy offline; không dùng constructor failure làm test chữ ký.
- PostgreSQL 17 PaymentFlowIT: 10/10 PASS, 16 migrations trên schema mới; validate/rerun no-op. Concurrent confirm, insufficient-funds rollback, parent payer vs student confirmer, 1h auto, 24h timeout, matching reports chờ Admin, partial fee, cancelled-session rejection, duplicate ref/whitespace, withdrawal rejection.
- Independent review: sửa chặn Admin charge buổi chưa COMPLETED, bắt buộc receiving account payOS, trim reference và lưu manual QR để mở lại. Review lại không còn defect trong fixes. User chọn không thêm cross-model review.
