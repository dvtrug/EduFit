package vn.edufit.discovery.domain.model;

import java.util.List;

public record MatchCriteria(
    int subjectId,
    int educationLevelId,
    String mode,
    String area,
    Long budgetMin,
    long budgetMax,
    List<AvailabilitySlot> availabilitySlots
) {

  public MatchCriteria {
    availabilitySlots = availabilitySlots == null ? List.of() : List.copyOf(availabilitySlots);
  }
}
