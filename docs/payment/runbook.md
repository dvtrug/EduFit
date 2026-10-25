# Vận hành và đối soát Payment

> Cập nhật 25/10/2026: [quy tắc 24h xác nhận / 48h giải quyết và tài khoản nhận tiền đã lưu](payment-policy-update.md) là nguồn hiện hành. Nội dung 1h/24h tranh chấp ở bằng chứng/phase cũ là lịch sử.

Cập nhật 24/10/2026 theo code `3966d54`. Đọc [cơ chế](payment_implementation_plan.md), [API](api-contract.md) và [cấu hình payOS](payos-setup.md) trước khi vận hành. Không chỉnh balance bằng SQL hoặc tạo receipt giả để xử lý sự cố.

## 1. Kiểm tra trước khi bật kênh thanh toán

1. Đúng repository/branch và môi trường DB; migration V1.16 đến V1.20 áp dụng bằng Flyway. V1.18 chưa được thực thi trong đợt sửa tài khoản ảo, cần kiểm tra lúc nâng cấp.
2. Env thu/chi được truyền vào process/container; restart sau thay đổi env theo setup.
3. Admin mở /admin/payments, kiểm readiness; đây không phải phép thử khóa/ngân hàng live.
4. Kênh thu có ngân hàng nhận đúng, webhook HTTPS public đúng path.
5. Nếu chi: credentials/quyền dịch vụ/nguồn tiền chi riêng; không mặc định tiền thu tự chuyển sang nguồn chi.
6. Khóa chỉ backend, không log/commit/copy vào ảnh trao đổi.

## 2. Nạp qua payOS

Student/Parent tạo yêu cầu trong ví; kiểm đúng số tiền/tài khoản trên checkout trước chuyển. Ví chỉ tăng khi webhook hoặc signed lookup hợp lệ, không theo ảnh biên lai/return URL.

Khi đã chuyển mà ví chưa tăng:

1. Mở đúng yêu cầu nạp, bấm Kiểm tra tiền đã chuyển; owner/Admin mới được refresh.
2. Job cũng query theo batch; lease mỗi khoản 30s, không bấm liên tục.
3. Đối chiếu order, số tiền, tài khoản nhận, link/reference và trạng thái provider.
4. Kiểm public webhook, log lỗi không secret và liên kết đúng kênh thu.
5. Nếu nhiều lần chuyển/sai tổng tiền/mismatch: giữ trạng thái chờ, hỗ trợ đối soát với provider. Không manual-confirm khoản PAYOS.
6. Nếu đã credit, gọi lặp không được tăng lần hai; kiểm entry và notification cùng reference.

Có thời hạn checkout 30 phút không đồng nghĩa đã hủy giao dịch ngân hàng. Code hiện chưa đồng bộ mọi trạng thái hết hạn/hủy thành topup CANCELLED. Không hứa tự hoàn ngân hàng khi QR hết hạn.

## 3. Nạp QR thủ công

Cấu hình `PAYMENT_BANK_BIN`, `PAYMENT_BANK_ACCOUNT`, `PAYMENT_BANK_HOLDER`. Tạo yêu cầu MANUAL, chuyển đúng tiền/nội dung, Admin kiểm sao kê thật rồi nhập transaction reference duy nhất tại /admin/payments.

Chỉ bấm xác nhận khi đã nhận được tiền; tạo QR không credit. Không dùng endpoint confirm để từ chối yêu cầu, vì endpoint này chỉ xác nhận. Bản hiện tại chưa có luồng Admin cancel topup riêng.

## 4. Học phí từng buổi

- Student/confirmed Parent kiểm giá rồi cấp tiền; available sang held của payer. Chưa có gate chặn bắt đầu học nếu chưa cấp tiền.
- Tutor cập nhật buổi COMPLETED qua Scheduling, sau đó xác nhận thanh toán.
- Student hoặc Parent payer còn link xác nhận learner; parent khác payer không được thay xác nhận.
- Đủ hai phía thì tự tất toán trong ví. Sau endAt+24h không khiếu nại thì tự tất toán khi COMPLETED, kể cả thiếu xác nhận.
- Hủy/vắng mặt: deadline job giải phóng khoản giữ, không tự phạt.
- Dời lịch: dùng endAt hiện hành cho kiểm hạn, không đổi giá snapshot.
- Job mặc định fixed delay 30s; batch có thể gây trễ thực thi. Không coi quá mốc vài giây là đã mất dữ liệu.

## 5. Tranh chấp

1. Một phía mở dispute khi khoản còn chờ xác nhận, sau endAt.
2. Hệ thống lưu thời điểm mở/deadline 48h; tiền vẫn held.
3. Learner và tutor gửi riêng báo cáo và mức tiền thống nhất trước deadline.
4. Hai mức khớp → AWAITING_ADMIN, tiếp tục giữ tiền; không tự hết hạn lúc chờ Admin.
5. Admin tại /admin/disputes xem cả hai báo cáo, duyệt đúng mức hoặc từ chối với lý do.
6. Chưa khớp khi đủ 48h → EXPIRED/hoàn toàn bộ held về payer, không thu phí.

Mức thực tất toán S: commission=floor(S*10%), tutor nhận S-commission, phần chưa tính phí hoàn payer. Hoàn là về ví nội bộ, không trả ngân hàng. Khoản đã terminal chưa có appeal/reopen trong bản hiện tại.

## 6. Rút thủ công

Tutor yêu cầu rút, available sang held. Admin kiểm người thụ hưởng, chuyển tiền ngoài app và xác minh kết quả cuối. Chỉ sau đó dùng review approve=true với reference thật và note. Nếu chưa chuyển và từ chối, approve=false trả held về available.

Chỉ xử lý manual khi PENDING; không chuyển ngoài app cho yêu cầu đã PROCESSING. Legacy thiếu BIN dùng manual hoặc từ chối để gửi lại, không đoán BIN từ tên tự do.

## 7. Rút qua kênh chi payOS

Admin chọn Duyệt và chuyển qua payOS, ghi note. Backend lưu PROCESSING trước gửi, một UUID/reference/key cố định; UI chỉ cho đối soát sau dispatch.

| Kết quả | Xử lý |
|---|---|
| Signed SUCCEEDED và khớp transaction | PAID, trừ held một lần |
| Signed FAILED/CANCELLED/REVERSED và khớp | FAILED, hoàn held một lần |
| PROCESSING/RECEIVED/ON_HOLD | Giữ tiền, tiếp tục lookup |
| Timeout/mismatch/chữ ký sai/chưa tìm thấy | Giữ PROCESSING, không hoàn, không gửi lệnh mới |

API refresh và job dùng lease 60s. Nếu crash giữa lưu PROCESSING và provider nhận request, có thể không tìm thấy lệnh: lưu withdrawal UUID/provider ID, đối chiếu với hỗ trợ provider. Không reset trạng thái/retry create/chi thủ công trước khi xác minh chắc chắn không phát sinh chi; chưa có UI xử lý tự động trường hợp này.

## 8. Sự cố thường gặp

| Dấu hiệu | Kiểm tra |
|---|---|
| Flyway SQLState 28P01 | Sai DB endpoint/credentials; kiểm process/container/port trước, không sửa migration |
| Local 5433 trỏ DB dự án khác | Phiên triển khai đã dùng EduFit 5434; đọc env hiện tại, không đổi mật khẩu/kill DB dự án khác |
| Validate checksum mismatch | Xác định migration bị đổi/nhánh/schema; giữ dữ liệu, không tự repair/clean/reset |
| UI báo liên hệ Admin | Backend thiếu config kênh thu và manual; kiểm env process và readiness |
| Readiness true nhưng tạo checkout lỗi | Kiểm khóa/quyền/bank/kết nối provider; readiness chỉ kiểm đủ biến |
| Dữ liệu checkout/tài khoản nhận không hợp lệ ở bản cũ | Kiểm phiên bản; V1.18 hỗ trợ tài khoản ảo theo checkout đã ký. Không đổi PAYMENT_BANK_ACCOUNT thành tài khoản ảo; restart backend để Flyway thêm cột mới |
| Webhook không vào local | Dùng HTTPS public/tunnel, cập nhật khi URL đổi; không đăng ký localhost |
| Có tiền ngân hàng nhưng ví chờ | Làm quy trình reconcile mục 2, không cộng tay |
| Rút PROCESSING lâu | Lookup/hỗ trợ provider mục 7, không hoàn/chi thêm |
| Notification sink lỗi | Transaction tiền rollback; kiểm DB/log và source receipt rồi thử lại đúng nghiệp vụ |

Không in khóa hoặc payload chứa thông tin nhạy cảm khi báo lỗi. Ghi ID/order/reference, thời điểm và kết quả cần đối soát.

V1.18 lưu tài khoản nhận theo từng topup; receipt phải khớp snapshot qua accountNumber/virtualAccountNumber. Hàng cũ NULL fallback tài khoản gốc. Nếu chưa chuyển tiền cho order lỗi có thể tạo yêu cầu mới sau khi nâng cấp. Nếu đã chuyển, giữ order/reference và đối soát hỗ trợ; không tạo/chuyển thêm hoặc cập nhật snapshot bằng tài khoản do webhook cung cấp. Bản sửa này chưa chạy migration/test/live; xem [báo cáo](verification-report.md).

## 9. Tái kiểm thử trong môi trường riêng

Từ repository root:

```powershell
& .\backend\mvnw.cmd -f backend/pom.xml -pl modules/payment -am test
& .\backend\mvnw.cmd -f backend/pom.xml -pl app -am test '-Dtest=PaymentFlowIT,PaymentApplicationIT' '-Dsurefire.failIfNoSpecifiedTests=false'
```

Integration tests dùng Testcontainers postgres:17-alpine, hoặc `PAYMENT_TEST_JDBC`, `PAYMENT_TEST_USER`, `PAYMENT_TEST_PASSWORD` tới DB test riêng có quyền tạo schema. Không dùng DB vận hành. Tests không được coi skip vì thiếu Docker là pass.

Từ frontend:

```powershell
npm test -- src/__tests__/features/payment src/__tests__/shared/DashboardShell.test.tsx
npm run type-check
npm run build -- --webpack
```

Chạy thêm sidebar/dashboard tests khi đổi navigation. Windows/Turbopack từng lỗi spawn CSS; gate đã dùng Webpack. Môi trường TLS proxy dùng CA tin cậy, không tắt xác minh TLS.

[Báo cáo](verification-report.md) ghi test đã chạy; provider dùng mock, chưa live. Khả năng/môi trường test payOS cần đối chiếu tài liệu chính thức lúc sử dụng; không coi thông tin sandbox cũ trong lịch sử là điều kiện đã xác nhận cho merchant hiện tại.
