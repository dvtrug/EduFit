package vn.edufit.connection.web.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edufit.connection.application.dto.TutorInboxResult;

public record TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
    Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
    String status, Instant expiresAt, @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate deadline) {

  public TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, null);
  }

  public static TutorInboxResponse of(TutorInboxResult result) {
    return new TutorInboxResponse(result.requestId(), result.goalType(), result.subjectId(),
        result.educationLevelId(), result.desiredSlots(), result.message(), result.senderRole(),
        result.status(), result.expiresAt(), result.deadline());
  }
}
