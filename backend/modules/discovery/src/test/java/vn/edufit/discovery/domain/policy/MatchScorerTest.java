package vn.edufit.discovery.domain.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.EducationLevelOrder;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.SubjectLevel;
import vn.edufit.discovery.domain.model.TutorCandidate;

class MatchScorerTest {

  private final MatchScorer scorer = new MatchScorer(new EducationLevelOrder(List.of(80, 3, 41, 12)));
  private final AvailabilitySlot monday = slot(1, "18:00", "20:00");

  @ParameterizedTest
  @CsvSource({"3,100.00,100.00", "80,50.00,90.00", "41,50.00,90.00", "12,0.00,80.00", "4,0.00,80.00"})
  void levelUsesCatalogOrderNotNumericIdAndDoesNotExclude(int level, String fit, String total) {
    var result = scorer.score(criteria(List.of(monday)), tutor(level, 200000, "5", 10, List.of(monday))).orElseThrow();
    assertEquals(new BigDecimal("100.00"), result.subjectFit());
    assertEquals(new BigDecimal(fit), result.levelFit());
    assertEquals(new BigDecimal(total), result.total());
  }

  @Test
  void levelMustBelongToRequestedSubjectAndBestLevelWins() {
    var base = tutor(12, 200000, "5", 10, List.of(monday));
    var otherSubjectExact = new TutorCandidate(base.tutorId(), Set.of(new SubjectLevel(1, 12), new SubjectLevel(2, 3)),
        base.mode(), base.area(), base.pricePerSession(), base.ratingAvg(), base.reviewCount(), base.registeredAt(), base.availabilitySlots());
    assertEquals(new BigDecimal("0.00"), scorer.score(criteria(List.of(monday)), otherSubjectExact).orElseThrow().levelFit());
    var exact = new TutorCandidate(base.tutorId(), Set.of(new SubjectLevel(1, 80), new SubjectLevel(1, 3)),
        base.mode(), base.area(), base.pricePerSession(), base.ratingAvg(), base.reviewCount(), base.registeredAt(), base.availabilitySlots());
    assertEquals(new BigDecimal("100.00"), scorer.score(criteria(List.of(monday)), exact).orElseThrow().levelFit());
    var wrongSubject = new TutorCandidate(base.tutorId(), Set.of(new SubjectLevel(2, 3)), base.mode(), base.area(),
        base.pricePerSession(), base.ratingAvg(), base.reviewCount(), base.registeredAt(), base.availabilitySlots());
    assertTrue(scorer.score(criteria(List.of(monday)), wrongSubject).isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"ONLINE,ONLINE,Other,true", "ONLINE,BOTH,Other,true", "ONLINE,OFFLINE,Hanoi,false",
      "OFFLINE,ONLINE,Hanoi,false", "OFFLINE,BOTH,Hanoi,true", "OFFLINE,OFFLINE,Other,false",
      "BOTH,BOTH,Other,true", "BOTH,ONLINE,Other,true", "BOTH,OFFLINE,Hanoi,true",
      "BOTH,OFFLINE,Other,false", "BOTH,INVALID,Hanoi,false", "INVALID,ONLINE,Hanoi,false"})
  void modeAndAreaRemainHardFilters(String requestedMode, String tutorMode, String area, boolean eligible) {
    var goal = new MatchCriteria(1, 3, requestedMode, " Hanoi ", null, 300000, List.of(monday));
    var base = tutor(3, 200000, "5", 10, List.of(monday));
    var candidate = new TutorCandidate(base.tutorId(), base.subjects(), tutorMode, area, base.pricePerSession(),
        base.ratingAvg(), base.reviewCount(), base.registeredAt(), base.availabilitySlots());
    assertEquals(eligible, scorer.score(goal, candidate).isPresent());
  }

  @Test
  void weightedTotalUsesAllFiveComponents() {
    var result = scorer.score(criteria(List.of(slot(1, "18:00", "21:00"))),
        tutor(80, 345000, "5", 1, List.of(slot(1, "18:00", "19:00")))).orElseThrow();
    assertEquals(new BigDecimal("69.00"), result.total());
    assertEquals(new BigDecimal("33.33"), result.scheduleFit());
    assertEquals(new BigDecimal("60.00"), result.ratingFit());
  }

  @Test
  void budgetZeroAndNegativePricesFailClearlyWithoutDivisionByZero() {
    var goal = new MatchCriteria(1, 3, "ONLINE", "Hanoi", null, 0, List.of(monday));
    assertThrows(IllegalArgumentException.class, () -> scorer.score(goal, tutor(3, 200000, "5", 10, List.of(monday))));
    assertThrows(IllegalArgumentException.class, () -> scorer.score(criteria(List.of(monday)), tutor(3, -1, "5", 10, List.of(monday))));
  }

  @Test
  void duplicateNestedAndTouchingSlotsCannotInflateTimeFit() {
    var requested = List.of(monday, monday, slot(1, "19:00", "19:30"), slot(1, "20:00", "21:00"));
    var offered = List.of(slot(1, "18:00", "19:00"), slot(1, "18:30", "21:00"), slot(1, "19:00", "20:00"));
    var result = scorer.score(criteria(requested), tutor(3, 200000, "5", 10, offered)).orElseThrow();
    assertEquals(new BigDecimal("100.00"), result.scheduleFit());
    assertEquals(4, result.overlappingSlots());
  }

  @ParameterizedTest
  @CsvSource({"0,100.00", "300000,100.00", "345000,75.00", "390000,50.00",
      "420000,25.00", "450000,0.00", "900000,0.00"})
  void priceUsesTwoLinearSegments(long price, String expected) {
    var score = scorer.score(criteria(List.of(monday)), tutor(3, price, "5", 10, List.of(monday))).orElseThrow();
    assertEquals(new BigDecimal(expected), score.budgetFit());
  }

  @ParameterizedTest
  @CsvSource({"5,0,35.00", "5,1,60.00", "5,2,70.00", "5,5,100.00", "4,5,80.00",
      "5,50,100.00", ",5,35.00", "-1,5,0.00", "6,5,100.00"})
  void ratingUsesReviewConfidenceAndNewTutorDefault(String rating, int reviews, String expected) {
    var score = scorer.score(criteria(List.of(monday)), tutor(3, 200000, rating, reviews, List.of(monday))).orElseThrow();
    assertEquals(new BigDecimal(expected), score.ratingFit());
  }

  @Test
  void timeUsesMinutesInsteadOfSlotCounts() {
    var requested = List.of(slot(1, "18:00", "21:00"), slot(2, "18:00", "19:00"));
    var score = scorer.score(criteria(requested), tutor(3, 200000, "5", 10,
        List.of(slot(1, "18:00", "19:00")))).orElseThrow();
    assertEquals(new BigDecimal("25.00"), score.scheduleFit());
  }

  @Test
  void overlappingGoalAndTutorSlotsAreMergedBeforeCountingDuration() {
    var requested = List.of(slot(1, "18:00", "20:00"), slot(1, "19:00", "21:00"));
    var offered = List.of(slot(1, "18:00", "19:00"), slot(1, "18:30", "19:00"));
    var score = scorer.score(criteria(requested), tutor(3, 200000, "5", 10, offered)).orElseThrow();
    assertEquals(new BigDecimal("33.33"), score.scheduleFit());
    assertEquals(1, score.overlappingSlots());
  }

  @Test
  void noTimeOverlapIsSoftIncludingTouchingBoundaryAndDifferentDays() {
    for (var offered : List.of(List.<AvailabilitySlot>of(), List.of(slot(1, "20:00", "21:00")),
        List.of(slot(2, "18:00", "20:00")))) {
      var score = scorer.score(criteria(List.of(monday)), tutor(3, 200000, "5", 10, offered)).orElseThrow();
      assertEquals(new BigDecimal("0.00"), score.scheduleFit());
      assertEquals(new BigDecimal("85.00"), score.total());
    }
    assertEquals(new BigDecimal("0.00"), scorer.score(criteria(List.of()),
        tutor(3, 200000, "5", 10, List.of(monday))).orElseThrow().scheduleFit());
  }

  private MatchCriteria criteria(List<AvailabilitySlot> slots) {
    return new MatchCriteria(1, 3, "ONLINE", "Hanoi", 100000L, 300000L, slots);
  }

  private TutorCandidate tutor(int level, long price, String rating, int reviews, List<AvailabilitySlot> slots) {
    return new TutorCandidate(UUID.randomUUID(), Set.of(new SubjectLevel(1, level)), "ONLINE", "Hanoi",
        price, rating == null ? null : new BigDecimal(rating), reviews, Instant.parse("2025-01-01T00:00:00Z"), slots);
  }

  private AvailabilitySlot slot(int day, String start, String end) {
    return new AvailabilitySlot((short) day, LocalTime.parse(start), LocalTime.parse(end));
  }
}
