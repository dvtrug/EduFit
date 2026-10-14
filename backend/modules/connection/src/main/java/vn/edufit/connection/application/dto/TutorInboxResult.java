package vn.edufit.connection.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TutorInboxResult(UUID requestId, String goalType, Integer subjectId,
    Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
    String status, Instant expiresAt, LocalDate deadline) {

  public TutorInboxResult(UUID requestId, String goalType, Integer subjectId,
      Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
      String status, Instant expiresAt) {
    this(requestId, goalType, subjectId, educationLevelId, desiredSlots, message, senderRole, status, expiresAt, null);
  }
}
