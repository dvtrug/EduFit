package vn.edufit.discovery.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TutorCandidate(
    UUID tutorId,
    Set<SubjectLevel> subjects,
    String mode,
    String area,
    long pricePerSession,
    BigDecimal ratingAvg,
    int reviewCount,
    Instant registeredAt,
    List<AvailabilitySlot> availabilitySlots
) {

  public TutorCandidate {
    subjects = subjects == null ? Set.of() : Set.copyOf(subjects);
    availabilitySlots = availabilitySlots == null ? List.of() : List.copyOf(availabilitySlots);
  }
}
