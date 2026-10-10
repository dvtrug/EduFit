package vn.edufit.discovery.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public record MatchScore(
    UUID tutorId,
    BigDecimal total,
    BigDecimal subjectFit,
    BigDecimal levelFit,
    BigDecimal scheduleFit,
    BigDecimal ratingFit,
    BigDecimal budgetFit,
    int overlappingSlots,
    int reviewCount,
    Instant registeredAt
) {

  public static Comparator<MatchScore> rankingOrder() {
    return Comparator.comparing(MatchScore::total).reversed()
        .thenComparing(MatchScore::scheduleFit, Comparator.reverseOrder())
        .thenComparing(MatchScore::reviewCount, Comparator.reverseOrder())
        .thenComparing(MatchScore::registeredAt, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(MatchScore::tutorId);
  }
}
