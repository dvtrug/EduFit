package vn.edufit.shared.response;

import java.time.Instant;
import java.util.List;

/**
 * Định dạng phong bì phản hồi lỗi chuẩn (Standard Error Envelope) cho các HTTP response thất bại (4xx, 5xx).
 *
 * <p>Mục đích thiết kế và bảo mật hệ thống:
 * <ul>
 *   <li><b>Chuẩn hóa định dạng lỗi:</b> Toàn bộ lỗi từ hệ thống (lỗi nghiệp vụ, lỗi kiểm thực dữ liệu,
 *       lỗi phân quyền hay lỗi máy chủ) đều được bao bọc chung bởi định dạng này.</li>
 *   <li><b>Không lộ thông tin nhạy cảm:</b> Ngăn chặn tuyệt đối việc đưa Exception Stack Trace, câu truy vấn SQL
 *       hay tên bảng cơ sở dữ liệu ra bên ngoài client.</li>
 *   <li><b>Mã lỗi nghiệp vụ bất biến:</b> Trường {@link #code()} mang mã định danh không đổi (tương ứng với
 *       {@link vn.edufit.shared.exception.ErrorCode}), giúp Frontend dễ dàng rẽ nhánh giao diện hoặc hỗ trợ đa ngôn ngữ.</li>
 * </ul>
 *
 * @param code      Mã định danh lỗi nghiệp vụ (ví dụ: "SCHEDULE_OVERLAP", "USER_ALREADY_EXISTS", "RESOURCE_NOT_FOUND").
 * @param message   Thông điệp mô tả lỗi dễ hiểu cho người dùng cuối hoặc lập trình viên frontend.
 * @param details   Danh sách lỗi chi tiết theo từng trường (đặc biệt hữu dụng cho lỗi kiểm thực Validation 400).
 * @param timestamp Thời điểm xảy ra lỗi theo chuẩn ISO-8601.
 */
public record ApiError(
    String code,
    String message,
    List<FieldErrorItem> details,
    Instant timestamp
) {

  /**
   * Tạo đối tượng ApiError đơn giản không kèm danh sách chi tiết lỗi trường.
   *
   * @param code    Mã định danh lỗi.
   * @param message Thông điệp mô tả lỗi.
   * @return Đối tượng {@link ApiError} với danh sách details rỗng.
   */
  public static ApiError of(String code, String message) {
    return new ApiError(code, message, List.of(), Instant.now());
  }

  /**
   * Tạo đối tượng ApiError kèm danh sách chi tiết lỗi kiểm thực theo từng trường dữ liệu.
   *
   * @param code    Mã định danh lỗi.
   * @param message Thông điệp mô tả lỗi.
   * @param details Danh sách đối tượng {@link FieldErrorItem}.
   * @return Đối tượng {@link ApiError}.
   */
  public static ApiError of(String code, String message, List<FieldErrorItem> details) {
    return new ApiError(code, message, details != null ? List.copyOf(details) : List.of(), Instant.now());
  }
}
