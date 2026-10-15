package vn.edufit.connection.web.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edufit.connection.application.dto.TutorInboxResult;

public record TutorInboxResponse(
    UUID requestId,
    String goalType,
    Integer subjectId,
    Integer educationLevelId,
    List<String> desiredSlots,
    String message,
    String senderRole,
    String status,
    Instant expiresAt,
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate deadline,
    @JsonInclude(JsonInclude.Include.NON_NULL) String studentDisplayName,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer currentLevel,
    @JsonInclude(JsonInclude.Include.NON_NULL) Integer desiredLevel,
    @JsonInclude(JsonInclude.Include.NON_NULL) String currentGrade,
    @JsonInclude(JsonInclude.Include.NON_NULL) String targetScore,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long budgetMin,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long budgetMax,
    @JsonInclude(JsonInclude.Include.NON_NULL) String area,
    @JsonInclude(JsonInclude.Include.NON_NULL) String district,
    @JsonInclude(JsonInclude.Include.NON_NULL) String city,
    @JsonInclude(JsonInclude.Include.NON_NULL) String subjectName,
    @JsonInclude(JsonInclude.Include.NON_NULL) String educationLevelName,
    @JsonInclude(JsonInclude.Include.NON_NULL) String gradeLevelName,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean hasSlotConflict,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> conflictingSlots
) {

  public TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  public TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt, LocalDate deadline) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, deadline,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  public static TutorInboxResponse of(TutorInboxResult result) {
    return new TutorInboxResponse(
        result.requestId(),
        result.goalType(),
        result.subjectId(),
        result.educationLevelId(),
        result.desiredSlots(),
        result.message(),
        result.senderRole(),
        result.status(),
        result.expiresAt(),
        result.deadline(),
        result.studentDisplayName(),
        result.currentLevel(),
        result.desiredLevel(),
        result.currentGrade(),
        result.targetScore(),
        result.budgetMin(),
        result.budgetMax(),
        result.area(),
        result.district(),
        result.city(),
        result.subjectName(),
        result.educationLevelName(),
        result.gradeLevelName(),
        result.hasSlotConflict(),
        result.conflictingSlots()
    );
  }
}
