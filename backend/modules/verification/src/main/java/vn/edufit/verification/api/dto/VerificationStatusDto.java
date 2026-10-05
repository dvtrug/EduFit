package vn.edufit.verification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificationStatusDto(
    UUID requestId,
    UUID tutorId,
    String status,
    String rejectionReason,
    Instant submittedAt,
    Instant reviewedAt
) {}

