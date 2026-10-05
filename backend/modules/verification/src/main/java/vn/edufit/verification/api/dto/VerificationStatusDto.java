package vn.edufit.verification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificationStatusDto(
    UUID tutorId,
    boolean verified,
    String latestRequestStatus,
    Instant latestSubmittedAt,
    Instant verifiedAt
) {
}
