package vn.edufit.profile.api.dto;

import java.util.List;
import java.util.UUID;

public record LearningGoalDiscoveryDto(
    UUID goalId,
    UUID studentId,
    UUID studentUserId,
    Integer subjectId,
    Integer educationLevelId,
    String mode,
    String area,
    Long budgetMin,
    Long budgetMax,
    String status,
    List<WeeklyAvailabilityDto> availabilitySlots
) {

  public LearningGoalDiscoveryDto {
    availabilitySlots = availabilitySlots == null ? List.of() : List.copyOf(availabilitySlots);
  }
}
