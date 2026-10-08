package vn.edufit.ai.api;

import vn.edufit.shared.exception.BaseBusinessException;
import vn.edufit.shared.exception.ErrorCode;

/**
 * Ngoại lệ phát sinh khi dịch vụ AI bên ngoài không thể truy cập hoặc gặp sự cố kỹ thuật.
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Kế thừa {@link BaseBusinessException}:</b> Tích hợp liền mạch với hệ thống mã lỗi chuẩn
 *       ({@link ErrorCode#AI_SERVICE_UNAVAILABLE}), được {@code GlobalExceptionHandler} tự động
 *       ánh xạ thành HTTP 503 Service Unavailable khi rơi ra ngoài Controller.</li>
 *   <li><b>Cách ly sự cố (Failure Isolation - NFR-10):</b> Ngoại lệ này cho phép các module nghiệp vụ
 *       ({@code profile}, {@code discovery}) dễ dàng bắt (catch) và xử lý rẽ nhánh an toàn:
 *       chuyển hướng sang Non-AI Fallback (FR-06) hoặc thông báo cho người dùng tự nhập tay (UC1.7 flow E2),
 *       ngăn chặn sự cố sụp đổ dây chuyền (Cascading Failure).</li>
 * </ul>
 */
public class AiUnavailableException extends BaseBusinessException {

  /**
   * Khởi tạo ngoại lệ với thông điệp giải thích.
   *
   * @param message Thông điệp mô tả lỗi phát sinh
   */
  public AiUnavailableException(String message) {
    super(ErrorCode.AI_SERVICE_UNAVAILABLE, message);
  }

  /**
   * Khởi tạo ngoại lệ với thông điệp và nguyên nhân gốc (cause).
   *
   * @param message Thông điệp mô tả lỗi phát sinh
   * @param cause   Ngoại lệ gốc (ví dụ lỗi I/O mạng hoặc lỗi HTTP 5xx từ nhà cung cấp)
   */
  public AiUnavailableException(String message, Throwable cause) {
    super(ErrorCode.AI_SERVICE_UNAVAILABLE, message, cause);
  }

  /**
   * Constructor hỗ trợ cho các lớp con kế thừa có thể định nghĩa ErrorCode chuyên biệt.
   *
   * @param errorCode Mã lỗi chuẩn
   * @param message   Thông điệp mô tả lỗi
   */
  protected AiUnavailableException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  /**
   * Constructor hỗ trợ cho các lớp con kế thừa có thể định nghĩa ErrorCode chuyên biệt kèm nguyên nhân.
   *
   * @param errorCode Mã lỗi chuẩn
   * @param message   Thông điệp mô tả lỗi
   * @param cause     Ngoại lệ gốc
   */
  protected AiUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
