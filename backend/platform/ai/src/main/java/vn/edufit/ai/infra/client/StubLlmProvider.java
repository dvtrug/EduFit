package vn.edufit.ai.infra.client;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Hiện thực {@link LlmProvider} giả lập (Offline Stub / Mock Provider) cho môi trường phát triển và
 * kiểm thử.
 *
 * <p>Mục đích thiết kế & Giá trị thực tế:
 *
 * <ul>
 *   <li><b>Local-first & Offline Development (ADR-001):</b> Cho phép các thành viên trong nhóm phát
 *       triển, chạy toàn bộ hệ thống cục bộ mà không bắt buộc phải có API key, không phụ thuộc kết
 *       nối internet.
 *   <li><b>CI/CD Pipeline không tốn chi phí:</b> Đảm bảo quy trình build tự động trên GitHub
 *       Actions chạy toàn bộ test suite thành công mà không tiêu tốn hạn ngạch token hay rò rỉ
 *       secret key.
 *   <li><b>Tính tất định (Deterministic Outputs):</b> Trả về kết quả mẫu chuẩn mực, cố định, giúp
 *       các bài kiểm thử tự động (Unit / Integration Tests) chạy ổn định 100% không bị flaky.
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
      String lowerPrompt = userPrompt.toLowerCase(Locale.ROOT);

      // Matching prompts also contain "gia sư"; recognize explanation before bio.
      if (lowerPrompt.contains("giải thích")
          || lowerPrompt.contains("ghép đôi")
          || lowerPrompt.contains("khớp")
          || lowerPrompt.contains("đề xuất")) {
        return new LlmResult(
            "Giải thích giả lập; dùng điểm theo quy tắc để hiển thị dữ kiện thực.", null, false);
      }

      // Trường hợp 1: Tính năng gợi ý tạo Bio (UC1.7)
      if (lowerPrompt.contains("bio")
          || lowerPrompt.contains("giới thiệu")
          || lowerPrompt.contains("gia sư")) {
        return new LlmResult(
            "Bản nháp giả lập; hãy tự điền môn dạy, kinh nghiệm và phương pháp đã xác minh.",
            null,
            false);
      }
    }

    // Phản hồi mẫu mặc định
    return new LlmResult("Nội dung giả lập từ EduFit Stub AI Provider.", null, false);
  }
}
