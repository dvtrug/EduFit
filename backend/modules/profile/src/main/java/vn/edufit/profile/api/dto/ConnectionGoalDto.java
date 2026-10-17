package vn.edufit.profile.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConnectionGoalDto(
    UUID goalId,
    UUID studentId,
    UUID studentUserId,
    Integer subjectId,
    Integer educationLevelId,
    String goalType,
    LocalDate deadline,
    String status,
    Integer currentLevel,
    Integer desiredLevel,
    Long budgetMin,
    Long budgetMax,
    String area,
    String subjectName,
    String educationLevelName
) {
  public ConnectionGoalDto(
      UUID goalId,
      UUID studentId,
      UUID studentUserId,
      Integer subjectId,
      Integer educationLevelId,
      String goalType,
      LocalDate deadline,
      String status
  ) {
    this(goalId, studentId, studentUserId, subjectId, educationLevelId, goalType, deadline, status,
        null, null, null, null, null, null, null);
  }
}
