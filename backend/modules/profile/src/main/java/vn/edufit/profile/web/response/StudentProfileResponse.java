package vn.edufit.profile.web.response;

import java.time.Instant;
import java.util.UUID;

public record StudentProfileResponse(
    UUID studentId,
    UUID userId,
    Integer educationLevelId,
    String educationLevelName,
    String area,
    Boolean profileComplete,
    Instant createdAt,
    Instant updatedAt
) {}
