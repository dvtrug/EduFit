package vn.edufit.progress.web.response;

import java.time.Instant;
import java.util.UUID;

public record SessionNoteTimelineResponse(
    UUID sessionId,
    UUID classId,
    Instant startAt,
    Instant endAt,
    String status,
    String noteText,
    Instant recordedAt
) {}
