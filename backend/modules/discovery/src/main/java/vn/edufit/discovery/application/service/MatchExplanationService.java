package vn.edufit.discovery.application.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edufit.ai.api.AiFeature;
import vn.edufit.ai.api.AiGateway;
import vn.edufit.ai.api.AiRequest;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;

@Service
public class MatchExplanationService {

  private static final Logger log = LoggerFactory.getLogger(MatchExplanationService.class);
  private final AiGateway aiGateway;

  public MatchExplanationService(AiGateway aiGateway) {
    this.aiGateway = aiGateway;
  }

  public Explanation explainTopMatch(
      UUID userId, MatchScore score, TutorDiscoveryProfileDto tutor) {
    String fallback = ruleBased(score);
    try {
      String prompt =
          """
          Hãy giải thích ngắn gọn bằng tiếng Việt, tối đa 2 câu, vì sao gia sư phù hợp.
          Chỉ được dùng các dữ kiện sau, không suy đoán thêm:
          - Hình thức dạy: %s
          - Học phí mỗi buổi: %d
          - Điểm môn học: %s/100
          - Điểm cấp học: %s/100
          - Điểm lịch phù hợp: %s/100
          - Điểm đánh giá: %s/100
          - Điểm ngân sách: %s/100
          - Điểm tổng: %s/100
          """
              .formatted(
                  tutor.tutor().teachingMode(),
                  tutor.tutor().pricePerSession(),
                  score.subjectFit(),
                  score.levelFit(),
                  score.scheduleFit(),
                  score.ratingFit(),
                  score.budgetFit(),
                  score.total());
      var response =
          aiGateway.complete(
              AiRequest.of(
                  userId,
                  AiFeature.MATCH_EXPLANATION,
                  "Bạn là trợ lý giải thích kết quả ghép đôi minh bạch của EduFit.",
                  prompt));
      if (!response.generatedByModel() || response.text().isBlank()) {
        return new Explanation(fallback, false);
      }
      return new Explanation(response.text(), true);
    } catch (RuntimeException ex) {
      log.warn(
          "Không thể tạo giải thích AI cho matching, dùng bản theo quy tắc [type: {}]",
          ex.getClass().getSimpleName());
      return new Explanation(fallback, false);
    }
  }

  public String ruleBased(MatchScore score) {
    return "Phù hợp môn %s%%, cấp học %s%%, lịch %s%%, đánh giá %s%% và ngân sách %s%%; có %d khung giờ giao nhau."
        .formatted(
            score.subjectFit(),
            score.levelFit(),
            score.scheduleFit(),
            score.ratingFit(),
            score.budgetFit(),
            score.overlappingSlots());
  }

  public record Explanation(String text, boolean aiGenerated) {}
}
