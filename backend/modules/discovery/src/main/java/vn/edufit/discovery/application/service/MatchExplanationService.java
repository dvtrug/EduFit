package vn.edufit.discovery.application.service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.math.BigDecimal;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
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
  private static final java.util.regex.Pattern EXPLANATION_LINE =
      java.util.regex.Pattern.compile("^(\\d{1,2})\\s*\\|\\s*(.+)$");
  private static final int MAX_COMMENT_LENGTH = 180;
  private final AiGateway aiGateway;

  public MatchExplanationService(AiGateway aiGateway) {
    this.aiGateway = aiGateway;
  }

  public List<Explanation> explainMatches(UUID userId, LearningGoalDiscoveryDto goal,
      List<MatchScore> scores, List<TutorDiscoveryProfileDto> tutors) {
    if (scores.isEmpty()) return List.of();
    if (scores.size() != tutors.size()) throw new IllegalArgumentException("Mismatched candidates");
    var fallback = java.util.stream.IntStream.range(0, scores.size())
        .mapToObj(i -> new Explanation(personalizedFacts(goal, scores.get(i), tutors, i), false))
        .toList();
    String subjectName = tutors.stream().flatMap(t -> t.subjects().stream())
        .filter(s -> java.util.Objects.equals(s.subjectId(), goal.subjectId())).map(s -> s.subjectName())
        .findFirst().orElse("chưa có tên môn");
    String levelName = tutors.stream().flatMap(t -> t.subjects().stream())
        .filter(s -> java.util.Objects.equals(s.educationLevelId(), goal.educationLevelId())).map(s -> s.educationLevelName())
        .findFirst().orElse("chưa có tên cấp học");
    var facts = new StringBuilder("NHU CẦU HỌC: ")
        .append("môn=").append(subjectName).append(", cấp học=").append(levelName)
        .append(", mục tiêu=").append(goal.goalType()).append(", mức hiện tại=").append(goal.currentLevel())
        .append(", mức mong muốn=").append(goal.desiredLevel()).append(", hạn=").append(goal.deadline())
        .append(", hình thức=").append(goal.mode()).append(", ngân sách mỗi buổi=")
        .append(goal.budgetMin()).append(" đến ").append(goal.budgetMax())
        .append(", lịch mong muốn=").append(goal.availabilitySlots()).append('\n');
    for (int i = 0; i < scores.size(); i++) {
      var score = scores.get(i);
      var tutor = tutors.get(i).tutor();
      // Only structured facts cross the AI boundary; never bio, contact data, or names.
      facts.append("ỨNG VIÊN ").append(i + 1).append(": ")
          .append("nhu cầu học=").append(needFit(score)).append("/100")
          .append(", đúng cấp học=").append(score.levelFit().compareTo(BigDecimal.valueOf(100)) == 0)
          .append(", hồ sơ đã xác minh=").append(tutor.verifiedAt() != null)
          .append(", số năm kinh nghiệm tự khai=").append(tutor.experienceYears())
          .append(", đánh giá=").append(tutor.ratingAvg()).append("/5 từ ").append(tutor.reviewCount())
          .append(", điểm tin cậy phản hồi=").append(score.ratingFit())
          .append(", lịch đáp ứng=").append(score.scheduleFit()).append("/100, trùng ")
          .append(score.overlappingSlots()).append(" khung giờ, lịch gia sư=")
          .append(tutors.get(i).availabilitySlots())
          .append(", hình thức=").append(tutor.teachingMode())
          .append(", học phí mỗi buổi=").append(tutor.pricePerSession())
          .append(", điểm ngân sách=").append(score.budgetFit())
          .append(", điểm tổng=").append(score.total())
          .append(", điểm đáng chú ý đã kiểm tra=").append(fallback.get(i).text()).append('\n');
    }
    try {
      var response = aiGateway.complete(AiRequest.of(userId, AiFeature.MATCH_EXPLANATION,
          """
          Bạn tư vấn chọn gia sư bằng tiếng Việt dựa đúng dữ kiện được cung cấp.
          Đánh giá theo 4 tiêu chí: nhu cầu học 40%, năng lực/độ tin cậy 25%, lịch/hình thức 25%, ngân sách 10%.
          Thứ tự ứng viên đã được hệ thống chấm điểm; không thay đổi thứ tự.
          Với MỖI ứng viên, viết một nhận xét riêng 1-2 câu, khoảng 120-160 ký tự, tuyệt đối không quá 180 ký tự.
          Trả lời câu hỏi: 'Với nhu cầu học của bạn, vì sao nên cân nhắc người này?'
          Chọn 1 điểm khác biệt có ý nghĩa từ hồ sơ so với nhu cầu và các ứng viên khác;
          giải thích lợi ích thực tế cho người học. Nếu có bất lợi quan trọng thì nói ngắn gọn.
          Không bắt mọi nhận xét đi qua đủ 4 tiêu chí. Không liệt kê số liệu hoặc phần trăm.
          Mỗi người một góc đánh giá và cách mở câu riêng; không dùng cùng khung câu rồi thay số.
          Ví dụ về góc nhìn (không chép mẫu): giữ được lịch học đều, giảm áp lực chi phí,
          có nhiều phản hồi để tham khảo, phải thống nhất lại lịch, hoặc cần cân nhắc mức phí.
          Dùng 'điểm đáng chú ý đã kiểm tra' như một dữ kiện, không sao chép câu đó làm nhận xét.
          Nếu hai hồ sơ thực sự tương đương, thừa nhận chưa đủ dữ kiện phân biệt, không bịa điểm riêng.
          Không suy đoán thành tích, phương pháp, bằng cấp, khả năng đạt điểm hay phù hợp luyện thi
          khi không có dữ kiện; kinh nghiệm tự khai không phải kinh nghiệm đã xác minh.
          Môn đúng là điều kiện đầu vào; mục tiêu và hạn học chỉ là ngữ cảnh, chưa được chấm semantic.
          null là chưa có dữ liệu. Không gọi ai là tốt nhất về một tiêu chí nếu không có so sánh chứng minh.
          Chỉ trả đúng một dòng cho mỗi ứng viên dạng: 1|lý do riêng của ứng viên 1
          Không Markdown, không xuống dòng bên trong một lý do, không thêm tiêu đề.
          """, facts.toString()));
      if (!response.generatedByModel() || response.text().isBlank()) return fallback;
      Map<Integer, String> explanations = new HashMap<>();
      var invalid = new HashSet<Integer>();
      for (String line : response.text().split("\\R")) {
        var match = EXPLANATION_LINE.matcher(line.trim());
        if (!match.matches()) continue;
        int index = Integer.parseInt(match.group(1)) - 1;
        String text = match.group(2).trim();
        if (index < 0 || index >= scores.size()) continue;
        if (text.length() < 15 || text.length() > MAX_COMMENT_LENGTH || explanations.putIfAbsent(index, text) != null) {
          invalid.add(index);
        }
      }
      // Repeated generic output is not accepted as personalized model output.
      var counts = explanations.values().stream().collect(java.util.stream.Collectors.groupingBy(
          MatchExplanationService::normalizeExplanation, java.util.stream.Collectors.counting()));
      return java.util.stream.IntStream.range(0, scores.size()).mapToObj(i -> {
        String text = explanations.get(i);
        return text == null || invalid.contains(i) || counts.get(normalizeExplanation(text)) > 1
            ? fallback.get(i) : new Explanation(text, true);
      }).toList();
    } catch (RuntimeException ex) {
      log.warn("Không thể tạo giải thích matching theo nhóm [type: {}]", ex.getClass().getSimpleName());
      return fallback;
    }
  }

  private static String normalizeExplanation(String text) {
    // Same sentence with only different prices/ratings/years is still the same template.
    return text.toLowerCase(java.util.Locale.ROOT).replaceAll("\\d+(?:[.,]\\d+)*", "#")
        .replaceAll("[\\p{Punct}]+", " ").replaceAll("\\s+", " ").trim();
  }

  public static BigDecimal needFit(MatchScore score) {
    return score.subjectFit().add(score.levelFit()).divide(BigDecimal.valueOf(2));
  }

  private String personalizedFacts(LearningGoalDiscoveryDto goal, MatchScore score,
      List<TutorDiscoveryProfileDto> profiles, int index) {
    var tutor = profiles.get(index).tutor();
    if (tutor.pricePerSession() > goal.budgetMax()) {
      return "Học phí " + tutor.pricePerSession() + " đồng/buổi vượt ngân sách bạn đặt; cần cân nhắc chi phí trước khi chọn.";
    }
    if (score.levelFit().compareTo(BigDecimal.valueOf(100)) < 0) {
      return "Có dạy môn bạn cần nhưng chưa khớp cấp học; nên trao đổi nội dung học trước khi kết nối.";
    }
    if (score.scheduleFit().signum() == 0) {
      return "Chưa trùng lịch bạn chọn; chỉ nên cân nhắc nếu hai bên có thể sắp xếp giờ khác.";
    }
    long cheaperOrEqual = profiles.stream()
        .filter(p -> p.tutor().pricePerSession() <= tutor.pricePerSession()).count();
    if (profiles.size() > 1 && cheaperOrEqual == 1) {
      return "Mức phí " + tutor.pricePerSession() + " đồng/buổi thấp nhất trong các gợi ý, giúp giảm áp lực ngân sách của bạn.";
    }
    if (index == 0 && score.scheduleFit().compareTo(BigDecimal.valueOf(100)) == 0) {
      return "Đáp ứng trọn lịch học bạn chọn và đúng môn, cấp học; thuận lợi để duy trì việc học đều đặn.";
    }
    int reviews = tutor.reviewCount() == null ? 0 : tutor.reviewCount();
    if (reviews > 0 && profiles.stream()
        .filter(p -> p.tutor().reviewCount() != null && p.tutor().reviewCount() >= reviews).count() == 1) {
      return "Có " + reviews + " phản hồi học viên, nhiều nhất trong các gợi ý; bạn có thêm cơ sở tham khảo trước khi chọn.";
    }
    if (score.scheduleFit().compareTo(BigDecimal.valueOf(100)) < 0) {
      return "Trùng " + score.overlappingSlots() + " khung giờ nhưng chưa đủ thời lượng bạn muốn; cần thống nhất lại lịch học.";
    }
    if (reviews == 0) {
      return "Lịch và mức phí đáp ứng nhu cầu của bạn, nhưng chưa có phản hồi học viên để tham khảo thêm.";
    }
    return "Đúng môn, cấp học và trong ngân sách; bạn có thể so sánh hồ sơ, phản hồi học viên để chọn thêm.";
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
