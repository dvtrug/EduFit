package vn.edufit.profile.api.dto;

import java.time.Instant;
import java.util.List;

public record TutorDiscoveryProfileDto(
    TutorSummaryDto tutor,
    List<TutorSubjectDto> subjects,
    List<WeeklyAvailabilityDto> availabilitySlots,
    Instant registeredAt
) {

  public TutorDiscoveryProfileDto {
    subjects = subjects == null ? List.of() : List.copyOf(subjects);
    availabilitySlots = availabilitySlots == null ? List.of() : List.copyOf(availabilitySlots);
  }
}
