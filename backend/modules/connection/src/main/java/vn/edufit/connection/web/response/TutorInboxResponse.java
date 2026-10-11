package vn.edufit.connection.web.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TutorInboxResponse(UUID requestId, String goalType, Integer subjectId,
    Integer educationLevelId, List<String> desiredSlots, String message, String senderRole,
    String status, Instant expiresAt) {}
