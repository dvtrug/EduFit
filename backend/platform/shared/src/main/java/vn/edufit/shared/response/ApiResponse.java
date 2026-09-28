package vn.edufit.shared.response;

import java.time.Instant;

/**
 * Định dạng phong bì phản hồi chuẩn (Standard Response Envelope) cho các HTTP response thành công (2xx).
 *
 * <p>Mục đích thiết kế và lý do kiến trúc:
 * <ul>
 *   <li><b>Nằm tại Shared Kernel (platform/shared):</b> Được thiết kế bằng Java {@code record} thuần túy,
 *       không phụ thuộc vào bất kỳ thư viện framework bên ngoài nào. Nhờ đó, tất cả các Controller ở các module
 *       nghiệp vụ (như {@code scheduling}, {@code iam}, {@code profile}...) đều có thể dùng trực tiếp mà không
 *       gây ra lỗi phụ thuộc vòng tròn (Circular Dependency) với module {@code app}.</li>
 *   <li><b>Tính nhất quán cho Frontend (Next.js):</b> Mọi kết quả thành công gửi về máy khách đều có chung một
 *       cấu trúc {@code { success: true, data: ..., message: ..., timestamp: ... }}, giúp tầng HTTP Client phía
 *       Next.js dễ dàng bóc tách dữ liệu và hiển thị thông báo phản hồi (Toast).</li>
 *   <li><b>Chuẩn hóa thời gian (NFR-18):</b> Trường {@code timestamp} ghi nhận chính xác thời điểm máy chủ
 *       hoàn tất xử lý theo chuẩn ISO-8601 (Instant UTC).</li>
 * </ul>
 *
 * @param <T> Kiểu dữ liệu (Payload) của kết quả nghiệp vụ trả về.
 * @param success Cờ trạng thái thành công (luôn là {@code true} cho phản hồi này).
 * @param data Dữ liệu kết quả nghiệp vụ (có thể là đối tượng DTO, danh sách, hoặc null nếu không có payload).
 * @param message Thông điệp mô tả ngắn gọn kết quả thao tác cho người dùng.
 * @param timestamp Thời điểm tạo phản hồi theo giờ chuẩn quốc tế ISO-8601.
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp
) {

  /**
   * Tạo phản hồi thành công kèm dữ liệu kết quả và thông điệp mặc định "Thành công".
   *
   * @param data Dữ liệu kết quả nghiệp vụ.
   * @param <T>  Kiểu dữ liệu của payload.
   * @return Đối tượng {@link ApiResponse} đã được đóng gói chuẩn mực.
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, "Thành công", Instant.now());
  }

  /**
   * Tạo phản hồi thành công kèm dữ liệu kết quả và thông điệp tùy biến theo từng hành động người dùng.
   *
   * @param data    Dữ liệu kết quả nghiệp vụ.
   * @param message Thông điệp mô tả kết quả (ví dụ: "Đăng ký tài khoản thành công").
   * @param <T>     Kiểu dữ liệu của payload.
   * @return Đối tượng {@link ApiResponse} đã được đóng gói chuẩn mực.
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, data, message, Instant.now());
  }

  /**
   * Tạo phản hồi thành công không kèm dữ liệu (dành cho các tác vụ cập nhật/xóa trả về void).
   *
   * @param message Thông điệp mô tả kết quả thao tác.
   * @return Đối tượng {@link ApiResponse} với trường data mang giá trị null.
   */
  public static ApiResponse<Void> ok(String message) {
    return new ApiResponse<>(true, null, message, Instant.now());
  }

  /**
   * Tạo phản hồi thành công mặc định không kèm dữ liệu với thông điệp "Thành công".
   *
   * @return Đối tượng {@link ApiResponse} với data là null và message là "Thành công".
   */
  public static ApiResponse<Void> ok() {
    return ok("Thành công");
  }
}
