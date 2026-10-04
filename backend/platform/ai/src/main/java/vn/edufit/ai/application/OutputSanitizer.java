package vn.edufit.ai.application;

import org.springframework.stereotype.Component;
import vn.edufit.ai.api.AiUnavailableException;

/**
 * Thành phần kiểm duyệt và làm sạch dữ liệu văn bản trả về từ LLM Provider (Output Sanitization).
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Hàng rào an toàn đầu ra (Defensive Programming):</b> Kết quả sinh ra từ các mô hình AI
 *       luôn tiềm ẩn rủi ro trả về rỗng, chuỗi trắng, hoặc chuỗi có độ dài đột biến làm tràn bộ đệm giao diện.</li>
 *   <li><b>Kiểm tra tính hợp lệ cơ bản:</b> Chặn phản hồi rỗng hoặc chỉ toàn khoảng trắng,
 *       kích hoạt ngay ngoại lệ để tầng nghiệp vụ biết và xử lý.</li>
 *   <li><b>Khống chế độ dài tối đa:</b> Giới hạn văn bản ở ngưỡng an toàn (mặc định 4.000 ký tự),
 *       cắt bớt các ký tự thừa vượt ngưỡng để bảo vệ bộ nhớ và giao diện phía Frontend.</li>
 * </ul>
 */
@Component
public class OutputSanitizer {

  /** Ngưỡng ký tự tối đa cho phép một phản hồi từ AI Gateway (4000 ký tự). */
  public static final int MAX_ALLOWED_CHARS = 4000;

  /**
   * Làm sạch và kiểm duyệt chuỗi văn bản thô từ LLM.
   *
   * @param rawText Chuỗi văn bản thô do LLM Provider sinh ra
   * @return Chuỗi văn bản đã được cắt tỉa và kiểm tra độ dài
   * @throws AiUnavailableException Khi chuỗi văn bản là null hoặc rỗng
   */
  public String sanitize(String rawText) {
    if (rawText == null || rawText.isBlank()) {
      throw new AiUnavailableException("Nhà cung cấp AI trả về phản hồi rỗng (Empty content).");
    }

    String trimmed = rawText.trim();

    // Giới hạn độ dài tối đa nhằm tránh tràn giao diện người dùng
    if (trimmed.length() > MAX_ALLOWED_CHARS) {
      trimmed = trimmed.substring(0, MAX_ALLOWED_CHARS).trim();
    }

    return trimmed;
  }
}
