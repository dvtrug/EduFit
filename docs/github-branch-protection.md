# Thiết lập bảo vệ branch trên GitHub

Các quy tắc này không thể được áp dụng chỉ bằng file trên máy. Sau khi repository được push, quản trị viên repository cần tạo ruleset cho branch mặc định với các thiết lập sau:

1. Bắt buộc mở pull request trước khi merge.
2. Bắt buộc có ít nhất một lượt phê duyệt.
3. Hủy các lượt phê duyệt cũ khi có commit mới được push lên pull request.
4. Bắt buộc status check `Lint, test, and build` của workflow `Backend CI` thành công.
5. Bắt buộc branch phải được cập nhật với branch đích trước khi merge.
6. Chặn force push và xóa branch được bảo vệ.
7. Không cho phép bỏ qua ruleset, trừ khi nhóm chủ động chỉ định một quản trị viên phát hành.

Thay `OWNER/REPOSITORY` trong badge ở `README.md` bằng đường dẫn GitHub thật. Sau đó badge sẽ hiển thị trạng thái CI mới nhất ngay trên trang chủ repository.

## Cách kiểm tra sau khi cấu hình

1. Tạo một branch thử nghiệm và thêm lỗi định dạng hoặc lỗi biên dịch có chủ đích.
2. Mở pull request và xác nhận check `Lint, test, and build` thất bại.
3. Xác nhận nút merge bị chặn khi CI thất bại hoặc pull request chưa có lượt phê duyệt.
4. Sửa lỗi, push lại và xác nhận CI thành công trong tối đa năm phút.
5. Xác nhận badge trên `README.md` hiển thị trạng thái mới nhất mà không cần mở pull request.
