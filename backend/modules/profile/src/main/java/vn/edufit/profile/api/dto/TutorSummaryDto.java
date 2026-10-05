package vn.edufit.profile.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public DTO chứa tóm tắt thông tin hồ sơ gia sư chia sẻ giữa các module (ADR-002).
 */
public record TutorSummaryDto(
    UUID tutorId,
    UUID userId,
    String displayName,
    String headline,
    String bio,
    String teachingMode,
    String area,
    Long pricePerSession,
    Short experienceYears,
    String teachingMethod,
    String status,
    Instant verifiedAt,
    BigDecimal ratingAvg,
    Integer reviewCount
) {}
