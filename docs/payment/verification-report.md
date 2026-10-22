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

- P04b `6fb85d9` đã push.
- P04c: workspace thật cho Student/Parent/Tutor, lọc tháng, sổ ví/lịch sử/phân trang và polling; tích hợp học phí vào modal buổi học. Type-check và production build PASS.

- P04c `0cb5c2a` đã push.
- P05: Admin nạp/rút/duyệt hai báo cáo; navigation mới, bỏ redirect cũ `/admin/disputes`, bỏ KPI tiền giả. 2 Admin form tests + 7 sidebar/CTA tests PASS (đã xử lý act warning trong test Parent).
- Browser Chrome production frontend + Spring Boot + PostgreSQL riêng: 4 vai trò PASS; QR không tự credit, Admin manual bank-confirm cập nhật đúng ví, ví Parent độc lập, Tutor không thể rút khi số dư 0, Student Admin API 403, disputes route không redirect, không pageerror. Không có chuyển tiền thật; bank confirmation được mô phỏng trên DB riêng.

- P05 `e80751b` đã push. P06: đồng bộ 7 bảng vào DBML, đánh dấu tài liệu tuition cũ được thay thế, bổ sung lệnh flow/startup tests trong runbook.

## Tổng hợp kiểm chứng cuối

- Backend gate ban đầu: 61 PASS (17 unit Payment + 33 architecture/migration naming + 10 PG flows + 1 app startup). Sau bổ sung regression history/Admin: 11 PG flows PASS; startup 1 PASS; package SUCCESS. Tổng 62 test case khác nhau qua các gate, không tuyên bố chạy lại toàn bộ repository.
- FE: 24 tests / 8 files PASS; Sidebar 7/7 chạy lại sau sửa act warning; type-check PASS; lint phần Payment 0 errors/0 warnings. Production Webpack build PASS. Các thay đổi sau build là test/format và backend query fixes.
- Browser kiểm tra 4 vai trò PASS trên Chrome với production frontend và API thật. Ảnh màn hình đã xem cho Student/Parent/Tutor/Admin; không lỗi hiển thị ở viewport kiểm tra 1440px.
- Flyway: thực thi 16 migrations trên schema PostgreSQL 17 mới, validate và migrate lại không áp dụng thêm. Giữ nguyên migration history trước Payment.
- Không kiểm chứng live payOS/tiền ngân hàng thật vì chưa có bộ cấu hình merchant thật. Test chữ ký và kiểm tra payload là offline; manual bank-confirm chỉ mô phỏng trên DB riêng. Payout ngân hàng vẫn do Admin thực hiện bên ngoài và xác nhận sau đối soát.
- Tất cả commit chỉ push `origin/feat/payment`; không merge develop/main. Danh sách commit có thể xem `git log b1ea5cd..feat/payment --oneline`.

## Sửa kết nối local khi Flyway báo 28P01

Backend đang trỏ cổng 5433 thuộc PostgreSQL dự án khác. Đã bật lại container EduFit với volume cũ tại cổng local 5434; `.env` local giữ credentials của container EduFit. Cấu hình backend hỗ trợ fallback `POSTGRES_*` cùng Docker Compose, vẫn ưu tiên `DB_*`. Maven package PASS; khởi động với `.env` thật, validate 16 migrations, apply V1.15/V1.16 thành công và HTTP health UP. Dữ liệu cũ được giữ nguyên.

## Hoàn thiện payOS 23/10/2026

P07: Compose truyền toàn bộ cấu hình Payment; .env.example để trống secrets; .env local bổ sung biến thiếu không thay giá trị đã có. API Admin configuration chỉ trả readiness/frontend URL/webhook path. PaymentSettingsTest và gateway/wallet tests PASS; placeholder `...` không bật kênh live.

P07 `de6b336` đã push. Gateway `d16e468` đã push: ký/xác minh BODY cho kênh thu, HEADER + URI encoding cho kênh chi; đối chiếu API và source SDK chính thức, không thực hiện request giao dịch thật.

P08/P09: PostgreSQL 17 fresh 17 migrations + upgrade V1.16 có số dư/rút chờ, rerun no-op PASS. Gate backend 73 PASS (23 Payment unit + 50 app/architecture/PG/startup). Payout concurrent approval chỉ gọi create một lần, timeout giữ tiền và không repost, processing không được manual paid/refund, signed success ghi sổ một lần, failure trả ví, mismatch/unauthorized không release. Notification failure rollback cùng ví; duplicate credit chỉ notify một lần. Topup reconcile owner-only, không credit khi sai amount/account hoặc timeout; lease chặn refresh storm. Package SUCCESS. Provider calls dùng mock/test fixtures, chưa xác minh merchant live.
