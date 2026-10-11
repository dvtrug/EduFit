package vn.edufit.connection.web.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.edufit.connection.application.dto.TutorInboxResult;

public record TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
    Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
    String status, Instant expiresAt) {
  public static TutorInboxResponse of(TutorInboxResult result) {
    return new TutorInboxResponse(result.requestId(), result.goalType(), result.subjectId(),
        result.educationLevelId(), result.desiredSlots(), result.message(), result.senderRole(),
        result.status(), result.expiresAt());
  }
}
