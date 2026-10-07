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
        String mockBio = "Tôi là gia sư tâm huyết với hơn 4 năm kinh nghiệm giảng dạy môn Toán cấp 2 và cấp 3. "
            + "Phương pháp sư phạm của tôi tập trung vào việc khơi dậy niềm đam mê học tập, "
            + "giúp học sinh củng cố kiến thức nền tảng và rèn luyện kỹ năng tư duy giải bài tập một cách tự tin, hiệu quả.";
        return new LlmResult(mockBio, 65);
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
}
