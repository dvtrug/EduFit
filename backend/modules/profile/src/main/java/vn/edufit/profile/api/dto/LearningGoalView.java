package vn.edufit.profile.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Minimal goal selection contract. Contains no family contact or account identity. */
public record LearningGoalView(
    UUID goalId,
    UUID studentId,
    Integer subjectId,
    Integer educationLevelId,
    Short currentLevel,
    Short desiredLevel,
    String goalType,
    LocalDate deadline,
    String mode,
    String area,
    Long budgetMin,
    Long budgetMax,
    String status,
    Instant createdAt,
    Instant updatedAt,
    List<WeeklyAvailabilityDto> availabilitySlots,
    boolean readyForMatching,
    List<String> readinessReasons) {
  public LearningGoalView {
    availabilitySlots = List.copyOf(availabilitySlots);
    readinessReasons = List.copyOf(readinessReasons);
  }
}
