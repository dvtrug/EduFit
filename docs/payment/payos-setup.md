# Cấu hình payOS cho EduFit

Code hoàn thiện trên `feat/payment`. Secrets trong `.env.example` và `.env` local được để trống; không điền `...` làm giá trị thật. Giữ nguyên các cấu hình DB/Gemini đang có và chỉ bổ sung khối Payment. Không đưa khóa vào frontend hoặc commit Git.

## 1. Kích hoạt nạp tiền tự động

Tạo tài khoản tại https://my.payos.vn, xác thực cá nhân/doanh nghiệp, liên kết ngân hàng và tạo kênh thu được phép dùng cho nghiệp vụ EduFit. Lấy Client ID, API Key, Checksum Key của **đúng kênh thu**.

Điền vào `.env` ở thư mục gốc repository (cùng `compose.yaml`):

```dotenv
PAYOS_CLIENT_ID=
PAYOS_API_KEY=
PAYOS_CHECKSUM_KEY=
PAYMENT_BANK_ACCOUNT=
PAYMENT_FRONTEND_URL=http://localhost:3000
```

`PAYMENT_BANK_ACCOUNT` là số tài khoản nhận liên kết với kênh thu, không phải tài khoản người nạp/gia sư. `PAYMENT_FRONTEND_URL` là origin frontend đang dùng, không kèm Markdown, không phải URL webhook. Production thay bằng origin HTTPS của frontend.

## 2. Cho backend nhận webhook

Webhook URL cấu hình trên kênh thu payOS:

```text
https://<backend-public>/api/v1/payment/webhooks/payos
```

Khi chạy local, tạo tunnel HTTPS trỏ vào `http://localhost:8080`, rồi dùng URL public của tunnel với path trên. Không đăng ký `localhost` trên payOS vì máy chủ payOS không truy cập được máy bạn. Khi tunnel đổi địa chỉ, cập nhật webhook trên payOS. Endpoint nhận POST, xác minh HMAC, không dùng cookie/CSRF của người dùng.

payOS có thể gửi giao dịch mẫu đã ký khi đăng ký webhook. EduFit ACK mẫu hợp lệ không khớp yêu cầu nạp và không cộng tiền cho bất kỳ ví nào. Khi thiếu khóa hoặc chữ ký sai, endpoint từ chối.

Không cần một biến `WEBHOOK_URL` trong code hiện tại: URL này được nhập trên payOS; API Admin hiển thị path để cấu hình.

## 3. Nạp cấu hình và khởi động lại

- IntelliJ: dùng run configuration `EduFitApplication - Gemini (.env)` đang import `.env`; restart backend sau khi sửa file. Nếu dùng configuration khác, thêm VM option `-Dspring.config.import=file:<absolute-repo-path>/.env[.properties]` hoặc khai báo các biến môi trường tương ứng. Giữ `.env` ở đúng repository đang chạy.
- Docker: `docker compose up -d --build backend`. Compose đã truyền các biến Payment từ `.env` vào container; không chỉ restart container cũ sau khi sửa environment.
- Chạy JAR: truyền các biến qua môi trường hoặc dùng `--spring.config.import=file:<absolute-repo-path>/.env[.properties]`.
- Admin mở **Đối soát thanh toán** (`/admin/payments`): xem trạng thái kênh thu/QR thủ công/kênh chi. Đây là kiểm tra đủ cấu hình, không khẳng định khóa còn hiệu lực hay ngân hàng đã kết nối thành công.

## 4. Kiểm tra nạp tiền sau cấu hình

1. Đăng nhập Student/Parent, tạo yêu cầu nạp trong trang ví. Nút mở thanh toán phải trỏ tới checkout HTTPS của payOS.
2. Kiểm tra đúng ngân hàng, tài khoản, số tiền và mã yêu cầu trên checkout trước khi chuyển.
3. Với giao dịch kiểm chứng đã được chủ tài khoản cho phép, backend chỉ cộng ví sau webhook hợp lệ hoặc truy vấn đối soát payOS có chữ ký. Không tin ảnh chuyển khoản, return URL hay nút trên frontend.
4. Nếu webhook đến chậm, bấm **Kiểm tra tiền đã chuyển**; job cũng đối soát mỗi 30 giây theo batch. Mỗi yêu cầu được giới hạn tần suất đối soát. Giao dịch nhiều lần hoặc sai tổng tiền/tài khoản cần hỗ trợ đối soát, không tự cộng sai số dư.
5. Kiểm tra sổ ví, thông báo nạp thành công và ví tài khoản khác không bị cộng. Refresh/webhook lặp lại không được cộng lần hai.

Thiếu khóa: hệ thống vẫn chạy, kênh payOS bị tắt. Không có số dư giả khi tạo QR.

## 5. Bật rút tiền qua API chi hộ (tùy chọn riêng)

Năm biến kênh thu ở trên chỉ bật nạp. Để Admin duyệt và hệ thống tự gửi tiền về ngân hàng gia sư, cần kênh chi đã được nhà cung cấp cấp quyền, cùng số dư/nguồn tiền chi trả tương ứng. Không mặc định tiền kênh thu tự được điều chuyển vào tài khoản chi.

```dotenv
PAYOS_PAYOUT_ENABLED=false
PAYOS_PAYOUT_CLIENT_ID=
PAYOS_PAYOUT_API_KEY=
PAYOS_PAYOUT_CHECKSUM_KEY=
```

Điền bộ khóa kênh chi, kiểm tra điều kiện sử dụng/số dư trên payOS rồi đổi `PAYOS_PAYOUT_ENABLED=true` và khởi động lại backend. Không tự dùng khóa kênh thu cho kênh chi trừ khi payOS xác nhận chúng được cấp quyền tương ứng. Chưa cấu hình kênh chi: vẫn có quy trình Admin chuyển khoản ngoài hệ thống và xác nhận sau đối soát.

Gia sư chọn tên ngân hàng, nhập số tài khoản và chủ tài khoản. Admin kiểm tra người thụ hưởng, chọn **Duyệt và chuyển qua payOS**, xác nhận ghi chú để gửi lệnh. UUID yêu cầu rút được lưu cố định làm `referenceId` và `x-idempotency-key`.

- `PENDING`: chưa gửi lệnh; có thể từ chối hoàn ví hoặc chuyển thủ công rồi xác nhận nếu sử dụng kênh thủ công.
- `PROCESSING`: đã ghi nhận gửi lệnh. Tiền được giữ; UI chỉ cho đối soát, không cho xác nhận thủ công/từ chối/nhấn gửi lại. Job truy vấn theo provider ID hoặc reference để phục hồi sau timeout/crash.
- Giao dịch `SUCCEEDED` xác minh đúng reference/số tiền/BIN/tài khoản: ghi `PAID`, trừ held đúng một lần.
- Giao dịch `FAILED`/`CANCELLED`/`REVERSED` đã xác minh: ghi `FAILED`, hoàn held về available đúng một lần.
- Không rõ kết quả, không tìm thấy lệnh hoặc sai chữ ký: tiếp tục giữ tiền, không coi timeout/HTTP 200 tạo lệnh là đã chuyển thành công. Liên hệ hỗ trợ nhà cung cấp để xác minh reference; không tự gửi lệnh khác/chỉnh balance SQL. Nếu crash xảy ra trước khi nhà cung cấp nhận request, yêu cầu có thể cần xử lý vận hành sau khi đã xác minh chắc chắn không phát sinh chi.

Yêu cầu rút trước V1.17 thiếu BIN tiếp tục xử lý thủ công hoặc từ chối để gia sư gửi lại; không tự đoán ngân hàng theo tên tự do cũ. Phí chi hộ là chi phí vận hành riêng, hiện không tự trừ thêm vào số tiền rút đã chấp nhận.

## 6. QR ngân hàng đối soát thủ công

Nếu chỉ muốn QR chuyển khoản và Admin xác minh sao kê:

```dotenv
PAYMENT_BANK_BIN=
PAYMENT_BANK_ACCOUNT=
PAYMENT_BANK_HOLDER=
```

Điền BIN 6 số, số tài khoản và tên chủ tài khoản nhận. Thiếu khóa payOS nhưng đủ ba giá trị này thì UI dùng QR thủ công. Nạp chỉ tăng sau Admin xác minh giao dịch ngân hàng thật và nhập reference duy nhất. Không thể chỉ dùng ảnh QR để tự nhận thông báo ngân hàng.

## Kiểm chứng đã thực hiện / nguồn

Kiểm thử tự động dùng PostgreSQL riêng và HTTP/provider mock, chưa thực hiện chuyển tiền thật hoặc kiểm chứng khóa merchant thật. Migration V1.17 đã được kiểm thử fresh và upgrade từ V1.16; không sửa checksum migration cũ. Thông báo được lưu qua Shared NotificationSink trong cùng transaction với sổ ví; UI poll/focus để cập nhật.

- API chính thức: https://payos.vn/docs/api/
- SDK Java và quy tắc ký request/response: https://github.com/payOSHQ/payos-lib-java
- Danh mục ngân hàng trên UI là snapshot 23/10/2026 của https://api.vietqr.io/v2/banks (ngân hàng có `transferSupported=1`); code gửi BIN, không gửi tên ngân hàng tự do làm mã chi.
