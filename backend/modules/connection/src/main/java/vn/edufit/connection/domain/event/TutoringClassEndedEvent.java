package vn.edufit.connection.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TutoringClassEndedEvent(
    UUID classId,
    UUID studentId,
    UUID tutorId,
    String reason,
    Instant endedAt
) {}
