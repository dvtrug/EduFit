package vn.edufit.shared.exception;

/**
 * Ngoại lệ phát sinh khi xảy ra xung đột dữ liệu, trùng lặp tài nguyên hoặc vi phạm ràng buộc duy nhất.
 *
 * <p>Ví dụ sử dụng trong các Use Cases & Business Rules:
 * <ul>
 *   <li><b>UC1.1 (Register):</b> Email đăng ký đã tồn tại trong hệ thống (ErrorCode: {@link ErrorCode#USER_ALREADY_EXISTS}).</li>
 *   <li><b>UC4.1 / BR-42 (Scheduling):</b> Đề xuất buổi học bị trùng lịch với lịch khác (ErrorCode: {@link ErrorCode#SCHEDULE_OVERLAP}).</li>
 *   <li><b>UC6.1 / NFR-24 (Review):</b> Học sinh đã đánh giá gia sư này rồi (ErrorCode: {@link ErrorCode#REVIEW_ALREADY_EXISTS}).</li>
 * </ul>
 *
 * <p>Khi ngoại lệ này bắn ra, tầng {@code app} sẽ tự động bắt lại và chuyển thành HTTP 409 (Conflict).
 */
public class ResourceConflictException extends BaseBusinessException {

  /**
   * Khởi tạo ngoại lệ với mã lỗi cụ thể và thông điệp.
   *
   * @param errorCode Mã lỗi định danh (ví dụ: USER_ALREADY_EXISTS, SCHEDULE_OVERLAP).
   * @param message   Thông điệp mô tả chi tiết xung đột.
   */
  public ResourceConflictException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  /**
   * Khởi tạo ngoại lệ với mã lỗi mặc định là {@link ErrorCode#CONFLICT_DETECTED}.
   *
   * @param message Thông điệp mô tả xung đột.
   */
  public ResourceConflictException(String message) {
    super(ErrorCode.CONFLICT_DETECTED, message);
  }
}
