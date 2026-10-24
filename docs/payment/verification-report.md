# Payment verification

Đây là lịch sử bằng chứng theo từng đợt, không phải một lần chạy tất cả test. Bản code tham chiếu khi chuẩn hóa docs 24/10/2026 là `3966d54`: V1.17, có reconcile nạp và payout sau Admin duyệt; 74 case BE khác nhau qua các gate, gate FE cuối 25 tests/8 files. Bản sửa tài khoản ảo V1.18 sau đó chỉ được biên dịch, chưa chạy test/migration/provider theo yêu cầu người dùng; xem mục cuối. Chưa kiểm chứng giao dịch payOS/merchant thật. Các mô tả payout thủ công và số lượng test ít hơn bên dưới thuộc bản đầu, trước P07–P11.

Tài liệu đọc kèm: [giải trình kiến trúc](payment_implementation_plan.md), [task từng phase](../../tasks/payment/plan.md), [API](api-contract.md), [setup](payos-setup.md), [runbook](runbook.md).

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

## Tổng hợp kiểm chứng bản đầu (trước P07–P11)

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

P08/P09 `710b9af` đã push; bổ sung header-signature + stable idempotency test `354cfbb` (3/3 PayosTransportTest PASS). Tổng 74 test case BE riêng biệt qua các gate (73 trong gate package + 1 case chữ ký bổ sung), không tuyên bố toàn repository được chạy lại.

P10: FE/notification 24 tests PASS, sau phát hiện mobile sidebar chiếm chiều ngang đã thêm native dialog navigation và RED/GREEN test. Gate cuối 25 tests / 8 files PASS; type-check/lint Payment + DashboardShell PASS, production Webpack build PASS. Chrome dùng backend thật + schema PG test riêng: 4 vai trò, manual topup không tự credit, Admin bank-confirm cập nhật đúng ví, persisted PAYMENT_TOPUP notification đúng một lần mỗi lần credit, readiness Admin-only, unsigned webhook bị từ chối, 390px không overflow và menu mở/đóng bằng Escape; zero page errors. Đã xem ảnh mobile và desktop. Test payout API bên ngoài dùng HTTP/provider mock; không chuyển tiền thật.

P10 mobile fix `a3dfd37` và FE provider `31ccac8` đã push. P11: hướng dẫn `payos-setup.md`, README, API và DBML đã đồng bộ V1.17. Kênh thu 5 biến để người dùng điền; kênh chi có bộ khóa riêng và PAYOS_PAYOUT_ENABLED=false mặc định. Không có secret trong commit; .env local ignored. Các tiến trình FE/BE/PG dành riêng cho kiểm thử đã dừng. Toàn bộ thay đổi trên feat/payment, không merge main/develop.

## Chuẩn hóa docs theo mẫu Discovery — 24/10/2026

- Đọc bản Discovery gốc, tasks/plan.md và tasks/todo.md; đối chiếu Payment domain/application/store/controllers, V1.16/V1.17, FE SDK và lịch sử Git tại code `3966d54`.
- Bổ sung payment_implementation_plan.md theo mục 0–9: audit delta, mục tiêu, cơ chế tiền/state/time/fee, boundary/workflows Mermaid, từng file/schema, quyết định/việc mở, phase, verification và Q&A.
- Bổ sung tasks/payment/plan.md, tổ chức lại checklist theo 12 phase và 14 task lịch sử; giữ SHA thực tế, tách checkpoint khỏi task và việc vận hành còn chờ.
- Đồng bộ API/runbook/setup và chỉ mục; implementation-plan.md cũ tiếp tục dẫn tới nguồn chính. Ghi đúng khác biệt webhook/lookup, endpoint confirm/payout, trạng thái topup và commission snapshot; không tuyên bố chức năng chưa có.
- Kiểm tra tài liệu: 9 Markdown trong docs/payment và tasks/payment, 53 link nội bộ, 6 khối Mermaid có fence cân bằng, 21 commit tham chiếu, thứ tự mục 0–9/phase 0–11 và 14 ID task đều PASS. File/test được tham chiếu đã đối chiếu trong repository.
- Đây là thay đổi Markdown; không thay code/SQL/env, không chạy lại gate BE/FE hay chuyển tiền thật. Số test ở các phần trước là bằng chứng lịch sử, không phải test mới của đợt docs.

## Giải thích nghiệp vụ và ảnh diagram — 24/10/2026

- Bổ sung [luồng, rule và cơ chế Payment](payment-flow-and-rules.md): ba vòng đời tiền, available/held, QR checkout hiện tại, receipt/lookup/manual, fund/confirmation/1h, dispute/report/24h/Admin, fee/partial/refund, withdrawal/manual/payout và 30 rule có nguồn thực thi.
- Thêm 7 PNG nhúng trực tiếp vào tài liệu nghiệp vụ và kiến trúc, kèm 7 SVG chỉnh sửa được. Có script Python/Pillow để tái tạo; không gọi provider hoặc thay đổi dữ liệu ứng dụng.
- Render cả 7 ảnh PASS với kiểm khung chữ/box không chồng; đã xem toàn bộ qua contact sheet và xem riêng ảnh tranh chấp/rút tiền ở kích thước chi tiết. Nội dung tiền/thời gian/nhánh xử lý đã đối chiếu code hiện tại.
- Kiểm tra tài liệu: link nội bộ và image reference, thứ tự đủ PAY-01..PAY-30, PNG đọc được/không trống, SVG XML hợp lệ, AST Python và git diff --check PASS. Các file/link cũ vẫn tồn tại.
- Không thay code FE/BE, schema, env hoặc chạy giao dịch live. Không chạy lại các gate test nghiệp vụ chỉ vì bổ sung tài liệu/ảnh; các số test trước đây giữ nguyên phạm vi lịch sử.

## Sửa kiểm tra tài khoản ảo khi tạo checkout — 24/10/2026

- Bằng chứng người dùng: POST `/api/v1/payment/topups` trả 400 `INVALID_OPERATION`, message `Dữ liệu checkout/tài khoản nhận không hợp lệ`. Message cũ gộp lỗi URL/link/tài khoản; chưa có response provider thật nên không khẳng định đã quan sát được field nào sai trong giao dịch đó.
- Đối chiếu code thấy `PayosGateway.create` buộc accountNumber từ response trùng tài khoản gốc trong cấu hình. [FAQ payOS mục 6](https://payos.vn/docs/cau-hoi-thuong-gap/) xác nhận VietQR Pro cấp tài khoản ảo riêng cho đơn, có thể khác tài khoản gốc; [API chính thức](https://payos.vn/docs/api/) mô tả response có chữ ký và accountNumber/virtualAccountNumber trên receipt. Điều kiện cũ từ chối trường hợp hợp lệ này.
- Sửa create: giữ xác minh chữ ký transport, kiểm URL bằng URI/HTTPS/host, kiểm link ID, order/amount/VND và tài khoản hợp lệ; trả message riêng theo nguyên nhân. Lưu tài khoản từ response có chữ ký theo từng topup. Không đổi env hoặc so tài khoản ảo với ngân hàng gốc.
- Sửa webhook và signed lookup cùng đối chiếu snapshot qua accountNumber hoặc virtualAccountNumber, giữ kiểm amount/link/reference/idempotency và transaction. Store không cho thay snapshot/link đã gắn. Không dùng webhook làm nguồn tạo snapshot.
- V1.18 chỉ thêm cột nullable `provider_account_number` và constraint định dạng; DBML đã đồng bộ. Giữ nguyên migration cũ và số dư. NULL giữ dữ liệu legacy/manual, fallback tài khoản gốc; đơn cũ dùng tài khoản ảo thiếu snapshot cần hỗ trợ đối soát nếu đã chuyển, không tự học tài khoản từ receipt.
- Viết nguồn regression: create với virtual account; sai order/amount/VND/URL/account bị từ chối; verify giữ virtualAccountNumber; webhook không bypass bằng tài khoản gốc khi snapshot khác; reconcile đúng/sai snapshot; PostgreSQL duplicate webhook một credit; upgrade V1.17 giữ dữ liệu và để snapshot NULL. Cập nhật các assertion số migration lên 18. **Các test này chưa chạy.**
- Đã chạy duy nhất Maven offline `test-compile -DskipTests` với Java 21, reactor 18 module: **BUILD SUCCESS**. Lệnh chỉ biên dịch main/test source, không thực thi test, không start backend, không migrate DB, không gọi payOS. Có warning deprecated/unchecked từ code hiện có; không lỗi biên dịch.
- Kiểm tĩnh tài liệu: 10 Markdown, 97 liên kết local không hỏng; 18 file migration gồm V1.18 mới, không sửa migration cũ; `git diff --check` không lỗi whitespace. `.env` vẫn ignored và không được theo dõi bởi Git.
- Tài liệu setup/API/runbook/luồng/plan/checklist được đồng bộ. Người dùng cần khởi động lại backend để Flyway áp dụng V1.18. Chỉ tạo yêu cầu mới khi chưa chuyển tiền cho yêu cầu lỗi; nếu đã chuyển, giữ order/reference để đối soát, không chuyển thêm.
