# Vận hành và kiểm thử Payment

## Kênh nạp tiền

Không tạo số dư demo ở runtime. Thiếu cấu hình: FE báo chưa bật kênh nạp, không cho tạo QR.

Chuyển khoản QR thủ công: cấu hình `PAYMENT_BANK_BIN` (BIN 6 số), `PAYMENT_BANK_ACCOUNT`, `PAYMENT_BANK_HOLDER`. Student/Parent tạo yêu cầu, chuyển đúng tiền/nội dung trên QR. Admin vào `/admin/payments`, kiểm tra tài khoản ngân hàng thật rồi nhập mã giao dịch để credit. QR được lưu theo yêu cầu và mở lại từ lịch sử. Chỉ xác nhận giao dịch duy nhất; không dùng mã ví dụ.

payOS: cấu hình `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, **`PAYMENT_BANK_ACCOUNT`** (đúng tài khoản nhận) và `PAYMENT_FRONTEND_URL` (origin FE). Cấu hình webhook HTTPS public tới `/api/v1/payment/webhooks/payos`. Chữ ký sai, sai tiền, sai tài khoản, sai link/currency: không cộng ví. Mất webhook: đối soát và yêu cầu gửi lại webhook từ nhà cung cấp, không tự sửa balance SQL. Không in secret trong log/docs.

Tài liệu chính thức đã đối chiếu: https://payos.vn/docs/, https://payos.vn/docs/api/, https://payos.vn/docs/moi-truong-test. payOS ghi không có sandbox riêng; test tự động dùng dữ liệu test/mock và PostgreSQL riêng, không gửi tiền thật.

## Buổi học

Cấp tiền trong `/student/payments`, `/parent/billing` hoặc chi tiết lịch học. Payer kiểm tra giá trước khi reserve; đủ available mới được cấp. Ví chung của phụ huynh không tự chuyển sở hữu sang học sinh. Tutor ghi nhận kết quả COMPLETED theo Scheduling rồi xác nhận thanh toán trong cùng khối học phí hoặc trang thu nhập.

Student hoặc Parent payer còn link xác nhận. Khi đủ hai xác nhận, sổ ví tất toán tự động. Job 30 giây kiểm tra autoaccept learner endAt+1h và timeout dispute +24h. Độ trễ thực thi phụ thuộc hàng đợi; API vẫn kiểm hạn bằng server clock. Source scheduling được kiểm lại khi quyết toán; hủy/vắng mặt hoàn tiền không phạt.

Tranh chấp: hai phía gửi text và số tiền đã thống nhất trong hạn. Admin xem cả hai báo cáo tại `/admin/disputes`, duyệt tất toán hoặc từ chối trả hết khoản giữ. Fee 10% chỉ trên phần thực trả; Admin không thay đổi số tiền hai phía đã đồng ý. Chưa có appeal sau tất toán trong bản đầu.

## Rút tiền

Gia sư gửi yêu cầu với ngân hàng/số tài khoản/chủ tài khoản. Tiền chuyển sang held của gia sư. Admin kiểm tra người thụ hưởng và thực hiện chuyển tiền ngoài app; chỉ bấm đã chuyển sau khi xác minh kết quả cuối, nhập reference thật. Từ chối sẽ giải phóng held. Không tự thử lại chuyển ngân hàng khi chưa biết giao dịch cũ thành công hay thất bại. Bản đầu tự động chuyển vào ví gia sư, chưa tự động payout qua ngân hàng.

## Kiểm thử

- Domain/provider/access/validation: `backend/mvnw.cmd -f backend/pom.xml -pl modules/payment -am test -Dtest=SessionChargeTest,PayosGatewayTest,PaymentAccessTest,PaymentValidationTest,WalletServiceTest -Dsurefire.failIfNoSpecifiedTests=false`.
- PostgreSQL: `PaymentFlowIT` và `PaymentApplicationIT` mặc định dùng Testcontainers postgres:17-alpine; Docker bắt buộc, không silently skip. Hoặc cung cấp `PAYMENT_TEST_JDBC`, `PAYMENT_TEST_USER`, `PAYMENT_TEST_PASSWORD` tới **DB kiểm thử riêng** có quyền tạo schema. Test tạo schema ngẫu nhiên; không dùng DB sản xuất.
- Chạy cả flow + khởi động HTTP: `backend/mvnw.cmd -f backend/pom.xml -pl app -am test -Dtest=PaymentFlowIT,PaymentApplicationIT -Dsurefire.failIfNoSpecifiedTests=false`.
- FE: `npm test -- src/__tests__/features/payment src/__tests__/shared/SidebarNavigation.test.tsx src/__tests__/features/dashboard`, type-check, lint, build.

Windows ở phiên triển khai: Docker lỗi startup riêng của máy; đã dùng PostgreSQL 17 portable trong ignored `.codex-doc-review`, cổng 55439 cho IT. Nếu Next/Turbopack lỗi spawn CSS, có thể kiểm chứng `npm run build -- --webpack`; dùng CA Windows cho Node khi môi trường có TLS proxy, không tắt TLS verification.
