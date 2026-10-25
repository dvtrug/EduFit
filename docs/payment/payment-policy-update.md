# Payment: cập nhật xác nhận và tài khoản nhận tiền — 25/10/2026

Quy tắc trong tài liệu này thay thế autoaccept 1h và hạn tranh chấp 24h trong tài liệu lịch sử. Nhánh triển khai: `feat/payment`. Không phát sinh giao dịch ngân hàng thật khi phát triển/kiểm thử.

## Xác nhận và khiếu nại

- Sau giờ kết thúc trên lịch, gia sư và phía người học có nút **Xác nhận buổi học** và **Khiếu nại**. Phụ huynh chỉ thao tác theo quyền payer/liên kết hiện có.
- Hai bên xác nhận chủ động khi buổi học COMPLETED: tất toán ngay, hoa hồng 10% phần thực tất toán.
- Chưa đủ xác nhận, không khiếu nại: sau `endAt + 24h` hệ thống tự tất toán khi Scheduling ghi COMPLETED, kể cả chưa bên nào bấm xác nhận. Không coi sự im lặng là xác nhận chủ động; cờ `autoAccepted` ghi nguồn tất toán tự động.
- Không tự tất toán ở giờ thứ nhất. Các điều kiện cấp tiền cũng dùng cửa sổ 24h, giá/nguồn tiền/điều kiện trạng thái khác giữ nguyên.
- Khiếu nại chỉ được mở từ endAt đến trước endAt+24h và trước tất toán. Khi mở, tiền tiếp tục held và không chuyển sang ví gia sư.
- Hạn giải quyết là `disputeOpenedAt + 48h`. Trong thời hạn này hai bên gửi báo cáo và số tiền đề nghị. Hai mức khớp thì chờ Admin; chờ Admin không tự hết hạn.
- Hết 48h chưa thống nhất: EXPIRED, hoàn toàn bộ khoản giữ về available của payer, không thu phí. Đây là kế thừa cách xử lý hết hạn đã chốt trước đây; thời hạn đổi từ 24h sang 48h.
- Job mặc định 30s, có thể thực thi sau mốc hạn; deadline/authorization ở backend mới là điều kiện quyết định. Hủy/vắng mặt không bị tính học phí.
- V1.19 chỉ bỏ đồng ý tự suy diễn theo rule 1h trên khoản còn chờ xác nhận. Giữ xác nhận chủ động, khoản tranh chấp và lịch sử terminal; không sửa migration cũ hoặc hoàn tác giao dịch đã tất toán.

## Phạm vi tiếp theo

Lưu một tài khoản nhận tiền mặc định theo chủ ví; mỗi yêu cầu rút giữ snapshot người nhận. Lần rút sau chỉ nhập số tiền, xem tài khoản nhận và xác nhận. Không tự bật kênh chi payOS hoặc thay quy trình Admin duyệt.

## Kiểm chứng phần mốc thời gian

RED: test domain phát hiện autoaccept ở 1h và tranh chấp hết hạn ở 24h; test FE phát hiện thiếu nhãn/nút và hạn mới. GREEN: 31 Payment unit tests, 18 PaymentFlowIT và 2 migration convention tests PASS; 7 FE session decision tests PASS. PostgreSQL 17 Testcontainers riêng: fresh 19 migration và upgrade có dữ liệu/validate/no-op PASS. Provider dùng mock; không chạy backend với khóa thật hoặc chuyển tiền ngân hàng. Bộ FlywayMigrationIT tổng quát sẽ được chạy trong gate cuối sau khi bổ sung tài khoản nhận tiền.
