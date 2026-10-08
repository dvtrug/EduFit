package vn.edufit.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.ai.api.AiFeature;
import vn.edufit.ai.api.AiGateway;
import vn.edufit.ai.api.AiRequest;
import vn.edufit.ai.api.AiUsageQuery;
import vn.edufit.discovery.application.service.MatchExplanationService;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;

@ExtendWith(MockitoExtension.class)
class MatchExplanationServiceTest {

  @Mock private AiGateway aiGateway;
  @Mock private AiUsageQuery aiUsageQuery;

  @Test
  void shouldUseRuleBasedExplanationWhenQuotaIsExhausted() {
    UUID userId = UUID.randomUUID();
    when(aiUsageQuery.countRequestsSince(eq(userId), eq(AiFeature.MATCH_EXPLANATION), any(Instant.class)))
        .thenReturn(10L);

    var explanation = service().explainTopMatch(userId, score(), tutor());

    assertFalse(explanation.aiGenerated());
    assertEquals("Phù hợp lịch 100.00%, đánh giá 80.00% và ngân sách 100.00%; có 1 khung giờ giao nhau.", explanation.text());
    verifyNoInteractions(aiGateway);
  }

  @Test
  void shouldUseRuleBasedExplanationWhenAiFails() {
    UUID userId = UUID.randomUUID();
    when(aiUsageQuery.countRequestsSince(eq(userId), eq(AiFeature.MATCH_EXPLANATION), any(Instant.class)))
        .thenReturn(0L);
    when(aiGateway.complete(any(AiRequest.class))).thenThrow(new IllegalStateException("AI unavailable"));

    var explanation = service().explainTopMatch(userId, score(), tutor());

    assertFalse(explanation.aiGenerated());
    assertEquals("Phù hợp lịch 100.00%, đánh giá 80.00% và ngân sách 100.00%; có 1 khung giờ giao nhau.", explanation.text());
  }

  private MatchExplanationService service() {
    return new MatchExplanationService(aiGateway, aiUsageQuery);
  }

  private MatchScore score() {
    return new MatchScore(UUID.randomUUID(), new BigDecimal("94.00"), new BigDecimal("100.00"),
        new BigDecimal("80.00"), new BigDecimal("100.00"), 1, 8, Instant.now());
  }

  private TutorDiscoveryProfileDto tutor() {
    TutorSummaryDto summary = new TutorSummaryDto(UUID.randomUUID(), UUID.randomUUID(), "Gia sư A",
        "Toán", "Bio", "ONLINE", "Hà Nội", 200_000L, (short) 5, "Thực hành", "VERIFIED",
        Instant.now(), new BigDecimal("4.00"), 8);
    return new TutorDiscoveryProfileDto(summary, List.of(), List.of(), Instant.now());
  }
}
