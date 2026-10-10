package vn.edufit.discovery.domain.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.SubjectLevel;
import vn.edufit.discovery.domain.model.TutorCandidate;
import vn.edufit.discovery.domain.policy.MatchScorer;

class MatchScorerTest {

  private final MatchScorer scorer = new MatchScorer();
  private final AvailabilitySlot monday = new AvailabilitySlot((short) 1, LocalTime.of(18, 0), LocalTime.of(20, 0));

  @Test
  @DisplayName("Tính điểm theo trọng số lịch 40%, rating 30%, ngân sách 30%")
  void shouldCalculateWeightedScore() {
    MatchCriteria criteria = criteria(List.of(monday));
    TutorCandidate tutor = tutor(BigDecimal.valueOf(5), 10, 200_000L, List.of(monday));

    var result = scorer.score(criteria, tutor).orElseThrow();

    assertEquals(new BigDecimal("100.00"), result.total());
    assertEquals(new BigDecimal("100.00"), result.scheduleFit());
    assertEquals(new BigDecimal("100.00"), result.ratingFit());
    assertEquals(new BigDecimal("100.00"), result.budgetFit());
  }

  @Test
  @DisplayName("Gia sư chưa có đánh giá nhận điểm rating trung lập 50")
  void shouldUseNeutralRatingForNewTutor() {
    var result = scorer.score(criteria(List.of(monday)), tutor(null, 0, 200_000L, List.of(monday))).orElseThrow();

    assertEquals(new BigDecimal("50.00"), result.ratingFit());
    assertEquals(new BigDecimal("85.00"), result.total());
  }

  @Test
  @DisplayName("Loại gia sư không cùng môn và cấp học")
  void shouldRejectSubjectLevelMismatch() {
    TutorCandidate tutor = new TutorCandidate(
        UUID.randomUUID(), Set.of(new SubjectLevel(1, 3)), "ONLINE", "Hà Nội", 200_000L,
        BigDecimal.valueOf(5), 10, Instant.parse("2025-01-01T00:00:00Z"), List.of(monday)
    );

    assertTrue(scorer.score(criteria(List.of(monday)), tutor).isEmpty());
  }

  @Test
  @DisplayName("Loại gia sư không có lịch giao nhau")
  void shouldRejectScheduleMismatch() {
    AvailabilitySlot tuesday = new AvailabilitySlot((short) 2, LocalTime.of(18, 0), LocalTime.of(20, 0));

    assertTrue(scorer.score(criteria(List.of(monday)), tutor(BigDecimal.valueOf(5), 10, 200_000L, List.of(tuesday))).isEmpty());
  }

  @Test
  @DisplayName("Giảm tuyến tính điểm ngân sách và về 0 khi vượt trần 20%")
  void shouldReduceBudgetFitLinearly() {
    var tenPercentOver = scorer.score(criteria(List.of(monday)), tutor(BigDecimal.valueOf(5), 10, 330_000L, List.of(monday))).orElseThrow();
    var twentyPercentOver = scorer.score(criteria(List.of(monday)), tutor(BigDecimal.valueOf(5), 10, 360_000L, List.of(monday))).orElseThrow();

    assertEquals(new BigDecimal("50.00"), tenPercentOver.budgetFit());
    assertEquals(new BigDecimal("0.00"), twentyPercentOver.budgetFit());
  }

  private MatchCriteria criteria(List<AvailabilitySlot> slots) {
    return new MatchCriteria(1, 2, "ONLINE", "Hà Nội", 100_000L, 300_000L, slots);
  }

  private TutorCandidate tutor(BigDecimal rating, int reviews, long price, List<AvailabilitySlot> slots) {
    return new TutorCandidate(
        UUID.randomUUID(), Set.of(new SubjectLevel(1, 2)), "ONLINE", "Hà Nội", price,
        rating, reviews, Instant.parse("2025-01-01T00:00:00Z"), slots
    );
  }
}
