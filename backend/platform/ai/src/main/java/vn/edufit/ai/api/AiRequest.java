package vn.edufit.ai.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Đối tượng truyền dữ liệu (DTO / Value Object) đại diện cho một yêu cầu xử lý AI.
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Thiết kế Generic (Zero Domain Leakage):</b> Phân hệ {@code platform/ai} đóng vai trò
 *       là một động cơ thực thi hoàn tất văn bản thuần túy. Đối tượng này không chứa các trường
 *       nghiệp vụ đặc thù của Gia sư hay Học viên (không chứa môn học, học phí, điểm số...).</li>
 *   <li><b>Tuân thủ NFR-09 (Data Minimisation):</b> Module nghiệp vụ gọi tới (như {@code profile} hay
 *       {@code discovery}) có toàn quyền sở hữu dữ liệu và có trách nhiệm gọt sạch mọi thông tin
 *       định danh cá nhân (PII như SĐT, Email, CCCD, địa chỉ chi tiết) trước khi đóng gói vào {@link #userPrompt()}.</li>
 *   <li><b>Tính bất biến (Immutability):</b> Được định nghĩa dưới dạng Java Record, an toàn tuyệt đối
 *       khi chia sẻ giữa các luồng (Thread-safe) trên nền tảng Java 21 Virtual Threads.</li>
 * </ul>
 *
 * @param userId        Định danh tài khoản người dùng yêu cầu (dùng để kiểm tra quota và ghi log kiểm toán FR-05).
 * @param feature       Loại tính năng AI đang yêu cầu (TUTOR_BIO hoặc MATCH_EXPLANATION).
 * @param systemPrompt  Chỉ thị hệ thống định hình vai trò của mô hình ngôn ngữ (System Instruction). Có thể null.
 * @param userPrompt    Nội dung prompt chính chứa các sự thật cần xử lý (bắt buộc, không được để trống).
 * @param temperature   Độ ngẫu nhiên/sáng tạo của mô hình (thường từ 0.0 đến 1.0). Nếu null, mặc định là 0.7.
 */
public record AiRequest(
    UUID userId,
    AiFeature feature,
    String systemPrompt,
    String userPrompt,
    Double temperature
) {

  /**
   * Compact constructor kiểm tra tính hợp lệ và thiết lập giá trị mặc định.
   */
  public AiRequest {
    Objects.requireNonNull(userId, "userId không được phép null");
    Objects.requireNonNull(feature, "feature không được phép null");
    Objects.requireNonNull(userPrompt, "userPrompt không được phép null");

    if (userPrompt.isBlank()) {
      throw new IllegalArgumentException("userPrompt không được là chuỗi rỗng");
    }

    if (temperature == null) {
      temperature = 0.7;
    } else if (temperature < 0.0 || temperature > 2.0) {
      throw new IllegalArgumentException("temperature phải nằm trong khoảng từ 0.0 đến 2.0");
    }
  }

  /**
   * Phương thức tạo nhanh (Static Factory Method) với temperature mặc định (0.7).
   *
   * @param userId       ID tài khoản người dùng
   * @param feature      Tính năng AI
   * @param systemPrompt Chỉ thị hệ thống
   * @param userPrompt   Nội dung prompt người dùng
   * @return Đối tượng {@link AiRequest} mới
   */
  public static AiRequest of(UUID userId, AiFeature feature, String systemPrompt, String userPrompt) {
    return new AiRequest(userId, feature, systemPrompt, userPrompt, 0.7);
  }
}
