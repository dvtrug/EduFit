package vn.edufit.discovery.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.discovery.domain.model.SubjectLevel;
import vn.edufit.discovery.domain.model.TutorCandidate;

public class MatchScorer {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  public Optional<MatchScore> score(MatchCriteria criteria, TutorCandidate tutor) {
    int overlaps = countOverlappingRequestedSlots(criteria, tutor);
    if (!isEligible(criteria, tutor, overlaps)) {
      return Optional.empty();
    }

    BigDecimal scheduleFit = percentage(overlaps, criteria.availabilitySlots().size());
    BigDecimal ratingFit = ratingFit(tutor);
    BigDecimal budgetFit = budgetFit(criteria.budgetMax(), tutor.pricePerSession());
    BigDecimal total = scheduleFit.multiply(BigDecimal.valueOf(0.4))
        .add(ratingFit.multiply(BigDecimal.valueOf(0.3)))
        .add(budgetFit.multiply(BigDecimal.valueOf(0.3)))
        .setScale(2, RoundingMode.HALF_UP);

    return Optional.of(new MatchScore(
        tutor.tutorId(),
        total,
        scheduleFit,
        ratingFit,
        budgetFit,
        overlaps,
        tutor.reviewCount(),
        tutor.registeredAt()
    ));
  }

  private boolean isEligible(MatchCriteria criteria, TutorCandidate tutor, int overlaps) {
    return tutor.subjects().contains(new SubjectLevel(criteria.subjectId(), criteria.educationLevelId()))
        && modeMatches(criteria, tutor)
        && overlaps > 0;
  }

  private boolean modeMatches(MatchCriteria criteria, TutorCandidate tutor) {
    String requested = normalize(criteria.mode());
    String offered = normalize(tutor.mode());
    if ("ONLINE".equals(requested)) {
      return "ONLINE".equals(offered) || "BOTH".equals(offered);
    }
    if ("OFFLINE".equals(requested)) {
      return ("OFFLINE".equals(offered) || "BOTH".equals(offered))
          && sameArea(criteria.area(), tutor.area());
    }
    if ("BOTH".equals(requested)) {
      return "ONLINE".equals(offered) || sameArea(criteria.area(), tutor.area());
    }
    return false;
  }

  private int countOverlappingRequestedSlots(MatchCriteria criteria, TutorCandidate tutor) {
    return (int) criteria.availabilitySlots().stream()
        .filter(requested -> tutor.availabilitySlots().stream().anyMatch(requested::overlaps))
        .count();
  }

  private BigDecimal ratingFit(TutorCandidate tutor) {
    if (tutor.reviewCount() <= 0 || tutor.ratingAvg() == null) {
      return BigDecimal.valueOf(50).setScale(2);
    }
    return tutor.ratingAvg()
        .max(BigDecimal.ZERO)
        .min(BigDecimal.valueOf(5))
        .multiply(BigDecimal.valueOf(20))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal budgetFit(long budgetMax, long price) {
    if (price <= budgetMax) {
      return HUNDRED.setScale(2);
    }
    BigDecimal upperLimit = BigDecimal.valueOf(budgetMax).multiply(BigDecimal.valueOf(1.2));
    if (BigDecimal.valueOf(price).compareTo(upperLimit) >= 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    BigDecimal overBudget = BigDecimal.valueOf(price - budgetMax);
    BigDecimal tolerance = BigDecimal.valueOf(budgetMax).multiply(BigDecimal.valueOf(0.2));
    return HUNDRED.subtract(overBudget.multiply(HUNDRED).divide(tolerance, 6, RoundingMode.HALF_UP))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal percentage(int numerator, int denominator) {
    if (denominator == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BigDecimal.valueOf(numerator)
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private boolean sameArea(String left, String right) {
    return !normalize(left).isBlank() && normalize(left).equals(normalize(right));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
