package vn.edufit.connection.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TutoringClassResult(
    UUID classId,
    UUID studentId,
    UUID tutorId,
    UUID goalId,
    String status,
    String studentDisplayName,
    String tutorDisplayName,
    String subjectName,
    String educationLevelName,
    String scheduleStatus,
    int sessionCount,
    Instant startedAt,
    Instant endedAt
) {}
