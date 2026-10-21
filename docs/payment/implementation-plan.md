# Payment: ví học phí và tất toán theo buổi

Ngày: 2026-10-21. Nhánh: `feat/payment`, base `develop@b1ea5cd`.
Yêu cầu mới của người dùng thay thế giới hạn D5 trong hai tài liệu nghiên cứu `docs/tuition`.

## Quy tắc đã chốt

- Student/Parent nạp tiền vào ví cá nhân. Không có API cho người dùng tự cộng số dư.
- Cấp tiền cho từng buổi từ ví của người thanh toán; chuyển available sang held. Hiển thị rõ giá theo buổi lấy từ hồ sơ gia sư, payer chấp nhận giá và snapshot giá/hoa hồng khi cấp tiền. Chưa có thỏa thuận giá riêng theo lớp, không suy diễn giá lịch sử.
- Chỉ buổi COMPLETED và đã qua giờ kết thúc mới được tất toán. Gia sư phải xác nhận. Student hoặc Parent cấp tiền còn liên kết có thể xác nhận phía người học.
- Sau endAt + 1 giờ, nếu không có tranh chấp, hệ thống tự xác nhận phía người học. Tác vụ không tự xác nhận thay gia sư. Gia sư xác nhận muộn không được khiến hệ thống trừ tiền trước khi COMPLETED.
- Một bên mở tranh chấp thì chặn tự tất toán. Thời hạn 24 giờ tính từ lúc mở tranh chấp theo thời gian server.
- Hai phía gửi báo cáo riêng trong hạn, cùng số tiền đề nghị và nội dung riêng: chuyển chờ Admin; giữ tiền chờ duyệt, không hết hạn theo thời gian Admin xử lý.
- Nếu hết 24 giờ chưa có hai báo cáo thống nhất: giải phóng toàn bộ held về available của đúng payer, kết thúc tranh chấp, không còn quyền tất toán khoản cũ.
- Admin chỉ duyệt đúng số tiền hai bên đã thống nhất; từ chối thì trả toàn bộ held về payer, ghi lý do. Không tự chỉnh số tiền sau khi hai phía báo cáo.
- Hoa hồng **10%** trên số tiền thực tất toán (VND nguyên, làm tròn xuống); phần còn lại vào ví gia sư; phần không tính phí trả ví payer. Ghi sổ nguyên tử, chỉ một lần.
- Hủy/vắng mặt: trả khoản giữ, không tự phạt. Dời lịch lấy endAt hiện hành khi kiểm tra hạn; báo cáo và giá vẫn giữ lịch sử.
- Ví thuộc tài khoản cấp tiền, không thuộc hồ sơ học sinh. Revoked Parent mất quyền truy cập buổi học; vẫn xem được sổ ví của chính mình. Deadline job hoàn tiền không phụ thuộc quyền hiện tại.

## Cấu trúc và tích hợp

Module `payment` phụ thuộc public facades Profile, Connection, Scheduling; không truy cập entity/repository module khác. Migration mới sau V1.15, không sửa SQL đã phát hành. Số tiền bigint VND; DB constraints và sổ giao dịch lưu bất biến. Mọi nghiệp vụ tài chính dùng transaction và khóa DB, hoạt động trên nhiều instance.

Kênh nạp: tích hợp payOS tạo checkout và webhook ký HMAC; chỉ xác minh đúng mã/số tiền/tài khoản/currency/reference mới cộng ví. Có kênh chuyển khoản thủ công cấu hình tài khoản nhận và Admin xác minh mã giao dịch thực tế; không dùng nút thành công giả. Thiếu cấu hình thì UI báo chưa sẵn sàng. API key chỉ ở backend.

Thu nhập gia sư chuyển vào số dư nội bộ khi tất toán. Rút tiền: gia sư yêu cầu, giữ số tiền rút; Admin xác nhận giao dịch ngân hàng đã thực hiện hoặc từ chối trả lại available. Không coi tạo yêu cầu rút là đã chuyển tiền. Tự chuyển ngân hàng qua payout API ngoài phạm vi bản đầu.

API cookie session/CSRF như hệ thống hiện tại. Ngoại lệ duy nhất là webhook công khai, bắt buộc xác minh chữ ký. Actor lấy từ CurrentUser, không lấy userId do client cung cấp.

## Giao diện

Thay ba trang finance stub bằng ví, nạp tiền, sổ ví và danh sách học phí theo buổi. Có cấp tiền, xác nhận, mở tranh chấp, gửi báo cáo giải quyết. Gia sư xem thu nhập/rút tiền; Admin có nạp thủ công, rút tiền và duyệt báo cáo hai bên. Dùng dữ liệu backend, loading/error/empty và nút đang xử lý; tiền không ở localStorage. Bỏ badge sắp ra mắt và KPI thanh toán giả.

## Thứ tự commit và kiểm chứng

Theo `tasks/payment/todo.md`. Mỗi phần kiểm chứng trước khi commit/push `origin feat/payment`. Không merge vào develop/main. Test thời điểm đúng biên 1h/24h, idempotence, báo cáo bất đồng, partial settlement, phân quyền và race bằng PostgreSQL. FE test hành vi, type-check, lint, build; API/migration smoke và browser nếu môi trường hỗ trợ. Ghi riêng giới hạn kiểm thử hoặc cấu hình thật còn thiếu.

## Giới hạn cần theo dõi

Khóa tài chính dùng một hàng điều phối trong DB cho MVP: tránh deadlock/race, đánh đổi throughput; không gọi API bên ngoài khi giữ khóa. Hàng đợi tự động xử lý theo batch, lỗi từng khoản không làm dừng tất cả. Nhà cung cấp phải cho phép mô hình nạp số dư/chi trả này trước khi vận hành tiền thật. Tài liệu payOS ngày 21/10/2026 ghi không có sandbox riêng; kiểm thử offline không thực hiện chuyển tiền thật.
