package vn.edufit.profile.web.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record LearningGoalResponse(
    UUID goalId,
    Integer subjectId,
    String subjectName,
    Integer educationLevelId,
    String educationLevelName,
    Short currentLevel,
    Short desiredLevel,
    String goalType,
    LocalDate deadline,
    String mode,
    String area,
    Long budgetMin,
    Long budgetMax,
    String status,
    List<AvailabilitySlotItem> availabilitySlots
) {
  public record AvailabilitySlotItem(
      Short dayOfWeek,
      LocalTime startTime,
      LocalTime endTime
  ) {}
}
