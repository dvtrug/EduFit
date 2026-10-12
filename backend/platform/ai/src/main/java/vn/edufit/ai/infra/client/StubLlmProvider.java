package vn.edufit.ai.infra.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Hiện thực {@link LlmProvider} giả lập (Offline Stub / Mock Provider) cho môi trường phát triển và kiểm thử.
 *
 * <p>Mục đích thiết kế & Giá trị thực tế:
 * <ul>
 *   <li><b>Local-first & Offline Development (ADR-001):</b> Cho phép các thành viên trong nhóm phát triển,
 *       chạy toàn bộ hệ thống cục bộ mà không bắt buộc phải có API key, không phụ thuộc kết nối internet.</li>
 *   <li><b>CI/CD Pipeline không tốn chi phí:</b> Đảm bảo quy trình build tự động trên GitHub Actions
 *       chạy toàn bộ test suite thành công mà không tiêu tốn hạn ngạch token hay rò rỉ secret key.</li>
 *   <li><b>Tính tất định (Deterministic Outputs):</b> Trả về kết quả mẫu chuẩn mực, cố định,
 *       giúp các bài kiểm thử tự động (Unit / Integration Tests) chạy ổn định 100% không bị flaky.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "edufit.ai.provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmProvider implements LlmProvider {

  private static final Logger log = LoggerFactory.getLogger(StubLlmProvider.class);

  @Override
  public LlmResult call(String systemPrompt, String userPrompt, Double temperature) {
    log.info("StubLlmProvider đang thực thi (Chế độ giả lập an toàn, không tốn token)");

    if (userPrompt != null) {
      String lowerPrompt = userPrompt.toLowerCase();

      // Trường hợp 1: Tính năng gợi ý tạo Bio (UC1.7)
      if (lowerPrompt.contains("bio") || lowerPrompt.contains("giới thiệu") || lowerPrompt.contains("gia sư")) {
        String mockBio = generateDynamicTutorBio(userPrompt);
        return new LlmResult(mockBio, 70);
      }

      // Trường hợp 2: Tính năng giải thích kết quả Ghép đôi (UC2.4)
      if (lowerPrompt.contains("khớp") || lowerPrompt.contains("giải thích") || lowerPrompt.contains("đề xuất")) {
        String mockExplanation = "Gia sư này là lựa chọn tối ưu nhất cho bạn vì hoàn toàn trùng khớp 100% lịch học mong muốn "
            + "(tối thứ 2, 4, 6), mức học phí nằm trọn vẹn trong khoảng ngân sách dự kiến, "
            + "đồng thời sở hữu đánh giá xuất sắc 4.9/5 sao từ các phụ huynh và học sinh trước đó.";
        return new LlmResult(mockExplanation, 58);
      }
    }

    // Phản hồi mẫu mặc định
    return new LlmResult("Nội dung phản hồi hoàn tất thử nghiệm từ EduFit Stub AI Provider.", 25);
  }

  private String generateDynamicTutorBio(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return "Tôi là gia sư tâm huyết với nhiều năm kinh nghiệm giảng dạy, cam kết đồng hành cùng học sinh tiến bộ.";
    }

    String name = "Gia sư";
    String subjects = "các môn học";
    String levels = "các khối lớp";
    String exp = "nhiều năm";
    boolean isProfessional = prompt.toLowerCase().contains("chuyên nghiệp") || prompt.toLowerCase().contains("professional");
    boolean isConcise = prompt.toLowerCase().contains("ngắn gọn") || prompt.toLowerCase().contains("concise");
    boolean isFoundation = prompt.toLowerCase().contains("mất gốc") || prompt.toLowerCase().contains("căn bản") || prompt.toLowerCase().contains("foundation");

    String[] lines = prompt.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith("- Tên gia sư:") || trimmed.startsWith("- Tên:")) {
        name = trimmed.substring(trimmed.indexOf(":") + 1).trim();
      } else if (trimmed.startsWith("- Môn học")) {
        subjects = trimmed.substring(trimmed.indexOf(":") + 1).trim();
      } else if (trimmed.startsWith("- Khối lớp")) {
        levels = trimmed.substring(trimmed.indexOf(":") + 1).trim();
      } else if (trimmed.startsWith("- Kinh nghiệm")) {
        exp = trimmed.substring(trimmed.indexOf(":") + 1).trim();
      }
    }

    if (isProfessional) {
      return String.format(
          "Cử nhân chuyên ngành Sư phạm với %s kinh nghiệm chuyên sâu môn %s cấp %s. Giáo trình giảng dạy chuẩn hóa, bám sát cấu trúc đề thi chính thức, chú trọng rèn tư duy logic giải bài tập nâng cao và cam kết mục tiêu bứt phá điểm số cao.",
          exp, subjects, levels
      );
    } else if (isConcise) {
      return String.format(
          "Gia sư %s (%s) với %s kinh nghiệm. Phương pháp giảng dạy trọng tâm, bám sát đề thi, củng cố kiến thức nhanh và tiến bộ rõ rệt qua từng tháng học.",
          subjects, levels, exp
      );
    } else if (isFoundation) {
      return String.format(
          "Chào các em và quý phụ huynh! Tôi là %s, chuyên giảng dạy môn %s (%s). Với %s kinh nghiệm, phong cách giảng dạy kiên nhẫn, gần gũi, giúp các em xóa bỏ nỗi sợ mất gốc và tự tin nắm vững kiến thức nền tảng!",
          name, subjects, levels, exp
      );
    } else {
      return String.format(
          "Chào các em học sinh và quý phụ huynh! Tôi là %s, đồng hành cùng học sinh khối %s môn %s. Với %s kinh nghiệm, tôi luôn kiên nhẫn giảng dạy trực quan, biến bài học phức tạp thành dễ hiểu, giúp học sinh tự tin chinh phục điểm số cao nhất!",
          name, levels, subjects, exp
      );
    }
  }
}
