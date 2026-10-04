package vn.edufit.ai.api;

import vn.edufit.shared.exception.ErrorCode;

/**
 * Ngoại lệ phát sinh khi cuộc gọi tới nhà cung cấp AI vượt quá thời hạn cho phép (15 giây).
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Tuân thủ nghiêm ngặt NFR-10 (AI Failure Isolation):</b> Yêu cầu phi chức năng NFR-10
 *       quy định: <i>"An AI call times out after 15 seconds. A failure or timeout never blocks
 *       core functions (profile, Search, Matching); they remain usable without the AI."</i></li>
 *   <li><b>Phân cấp kế thừa:</b> Là lớp con chuyên biệt của {@link AiUnavailableException},
 *       sử dụng mã lỗi {@link ErrorCode#AI_REQUEST_TIMEOUT} được {@code GlobalExceptionHandler}
 *       ánh xạ thành HTTP 504 Gateway Timeout.</li>
 *   <li><b>Kích hoạt cơ chế Fallback:</b> Giúp module gọi phân biệt rõ giữa lỗi mạng tức thời
 *       và việc nhà cung cấp AI xử lý quá lâu để chủ động kích hoạt cơ chế dự phòng không dùng AI.</li>
 * </ul>
 */
public class AiTimeoutException extends AiUnavailableException {

  /**
   * Khởi tạo ngoại lệ timeout với thông điệp giải thích.
   *
   * @param message Thông điệp mô tả lỗi quá thời gian chờ
   */
  public AiTimeoutException(String message) {
    super(ErrorCode.AI_REQUEST_TIMEOUT, message);
  }

  /**
   * Khởi tạo ngoại lệ timeout với thông điệp và nguyên nhân gốc.
   *
   * @param message Thông điệp mô tả lỗi quá thời gian chờ
   * @param cause   Ngoại lệ gốc (ví dụ: {@link java.net.SocketTimeoutException})
   */
  public AiTimeoutException(String message, Throwable cause) {
    super(ErrorCode.AI_REQUEST_TIMEOUT, message, cause);
  }
}
