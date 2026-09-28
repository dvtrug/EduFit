package vn.edufit.shared.response;

/**
 * Chi tiết lỗi kiểm thực (Validation Error) trên từng trường dữ liệu cụ thể trong yêu cầu gửi lên.
 *
 * <p>Mục đích thiết kế và liên kết giao diện Frontend:
 * <ul>
 *   <li><b>Ánh xạ lỗi chi tiết:</b> Khi người dùng gửi biểu mẫu (form) có trường không thỏa mãn ràng buộc
 *       (như email sai định dạng, mật khẩu ngắn hơn 8 ký tự), đối tượng này chứa chính xác trường bị sai.</li>
 *   <li><b>Hỗ trợ Form Validation ở Next.js:</b> Giúp Frontend ánh xạ trực tiếp trường {@link #field()}
 *       vào ô nhập liệu tương ứng trên giao diện để bôi đỏ và hiển thị câu báo lỗi ngay dưới chân ô nhập.</li>
 * </ul>
 *
 * @param field         Tên trường dữ liệu bị vi phạm ràng buộc (ví dụ: "email", "hourlyRate", "startTime").
 * @param message       Nội dung thông điệp mô tả chi tiết lỗi kiểm thực (ví dụ: "Email không đúng định dạng chuẩn").
 * @param rejectedValue Giá trị người dùng đã gửi lên bị từ chối (có thể là null nếu không truyền hoặc là dữ liệu nhạy cảm).
 */
public record FieldErrorItem(
    String field,
    String message,
    Object rejectedValue
) {}
