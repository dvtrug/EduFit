package vn.edufit.discovery.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import vn.edufit.discovery.domain.model.AvailabilitySlot;
import vn.edufit.discovery.domain.model.EducationLevelOrder;
import vn.edufit.discovery.domain.model.MatchCriteria;
import vn.edufit.discovery.domain.model.MatchScore;
import vn.edufit.discovery.domain.model.TutorCandidate;

public class MatchScorer {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private final EducationLevelOrder levelOrder;
  private final MatchWeights weights;

  public MatchScorer(EducationLevelOrder levelOrder) {
    this(levelOrder, MatchWeights.standard());
  }

  public MatchScorer(EducationLevelOrder levelOrder, MatchWeights weights) {
    this.levelOrder = Objects.requireNonNull(levelOrder);
    this.weights = Objects.requireNonNull(weights);
  }

  public Optional<MatchScore> score(MatchCriteria criteria, TutorCandidate tutor) {
    if (criteria.budgetMax() <= 0 || tutor.pricePerSession() < 0) {
      throw new IllegalArgumentException("Matching requires a positive budget and non-negative price.");
    }
    boolean teachesSubject = tutor.subjects().stream().anyMatch(item -> item.subjectId() == criteria.subjectId());
    if (!teachesSubject || !modeMatches(criteria, tutor)) {
      return Optional.empty();
    }
    BigDecimal level = levelFit(criteria, tutor);
    var requested = merge(criteria.availabilitySlots());
    var offered = merge(tutor.availabilitySlots());
    BigDecimal time = timeFit(requested, offered);
    // Keep the legacy field as the number of original requested slots with any overlap.
    int overlappingSlots = (int) criteria.availabilitySlots().stream()
        .filter(slot -> offered.stream().anyMatch(slot::overlaps)).count();
    BigDecimal price = budgetFit(criteria.budgetMax(), tutor.pricePerSession());
    BigDecimal rating = ratingFit(tutor);
    BigDecimal total = HUNDRED.multiply(weights.subject()).add(level.multiply(weights.level()))
        .add(price.multiply(weights.price())).add(time.multiply(weights.time())).add(rating.multiply(weights.rating()));
    return Optional.of(new MatchScore(tutor.tutorId(), rounded(total), rounded(HUNDRED), rounded(level),
        rounded(time), rounded(rating), rounded(price), overlappingSlots, tutor.reviewCount(), tutor.registeredAt()));
  }

  private BigDecimal levelFit(MatchCriteria criteria, TutorCandidate tutor) {
    BigDecimal best = BigDecimal.ZERO;
    for (var subject : tutor.subjects()) {
      if (subject.subjectId() != criteria.subjectId()) {
        continue;
      }
      if (subject.educationLevelId() == criteria.educationLevelId()) {
        return HUNDRED;
      }
      if (levelOrder.areAdjacent(subject.educationLevelId(), criteria.educationLevelId())) {
        best = BigDecimal.valueOf(50);
      }
    }
    return best;
  }

  private BigDecimal timeFit(List<AvailabilitySlot> requested, List<AvailabilitySlot> offered) {
    long desiredDuration = requested.stream().mapToLong(this::duration).sum();
    long overlapDuration = 0;
    for (var goalSlot : requested) {
      for (var tutorSlot : offered) {
        if (goalSlot.overlaps(tutorSlot)) {
          var start = goalSlot.startTime().isAfter(tutorSlot.startTime()) ? goalSlot.startTime() : tutorSlot.startTime();
          var end = goalSlot.endTime().isBefore(tutorSlot.endTime()) ? goalSlot.endTime() : tutorSlot.endTime();
          overlapDuration += Duration.between(start, end).toNanos();
        }
      }
    }
    return desiredDuration == 0 ? BigDecimal.ZERO
        : BigDecimal.valueOf(overlapDuration).multiply(HUNDRED)
            .divide(BigDecimal.valueOf(desiredDuration), 12, RoundingMode.HALF_UP);
  }

  private List<AvailabilitySlot> merge(List<AvailabilitySlot> slots) {
    var sorted = slots.stream().sorted(Comparator.comparingInt(AvailabilitySlot::dayOfWeek)
        .thenComparing(AvailabilitySlot::startTime).thenComparing(AvailabilitySlot::endTime)).toList();
    var merged = new ArrayList<AvailabilitySlot>();
    for (var slot : sorted) {
      if (!slot.startTime().isBefore(slot.endTime())) {
        throw new IllegalArgumentException("Availability slots must have a positive duration.");
      }
      if (!merged.isEmpty()) {
        var last = merged.getLast();
        if (last.dayOfWeek() == slot.dayOfWeek() && !slot.startTime().isAfter(last.endTime())) {
          merged.set(merged.size() - 1, new AvailabilitySlot(last.dayOfWeek(), last.startTime(),
              last.endTime().isAfter(slot.endTime()) ? last.endTime() : slot.endTime()));
          continue;
        }
      }
      merged.add(slot);
    }
    return merged;
  }

  private long duration(AvailabilitySlot slot) {
    return Duration.between(slot.startTime(), slot.endTime()).toNanos();
  }

  private BigDecimal ratingFit(TutorCandidate tutor) {
    if (tutor.reviewCount() <= 0 || tutor.ratingAvg() == null) {
      return BigDecimal.valueOf(35);
    }
    BigDecimal confidence = new BigDecimal("0.5").add(
        BigDecimal.valueOf(Math.min(5, tutor.reviewCount())).multiply(new BigDecimal("0.1")));
    return tutor.ratingAvg().max(BigDecimal.ZERO).min(BigDecimal.valueOf(5))
        .multiply(BigDecimal.valueOf(20)).multiply(confidence);
  }

  private BigDecimal budgetFit(long budgetMax, long price) {
    BigDecimal budget = BigDecimal.valueOf(budgetMax);
    BigDecimal amount = BigDecimal.valueOf(price);
    if (price <= budgetMax) {
      return HUNDRED;
    }
    BigDecimal breakpoint = budget.multiply(new BigDecimal("1.3"));
    if (amount.compareTo(breakpoint) <= 0) {
      return HUNDRED.subtract(amount.subtract(budget).multiply(BigDecimal.valueOf(50))
          .divide(budget.multiply(new BigDecimal("0.3")), 12, RoundingMode.HALF_UP));
    }
    return BigDecimal.valueOf(50).subtract(amount.subtract(breakpoint).multiply(BigDecimal.valueOf(50))
        .divide(budget.multiply(new BigDecimal("0.2")), 12, RoundingMode.HALF_UP)).max(BigDecimal.ZERO);
  }

  private boolean modeMatches(MatchCriteria criteria, TutorCandidate tutor) {
    String requested = normalize(criteria.mode());
    String offered = normalize(tutor.mode());
    boolean online = "ONLINE".equals(offered) || "BOTH".equals(offered);
    boolean offline = ("OFFLINE".equals(offered) || "BOTH".equals(offered))
        && !normalize(criteria.area()).isBlank() && normalize(criteria.area()).equals(normalize(tutor.area()));
    return switch (requested) {
      case "ONLINE" -> online;
      case "OFFLINE" -> offline;
      case "BOTH" -> online || offline;
      default -> false;
    };
  }

  private BigDecimal rounded(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
