package vn.edufit.profile.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO tương thích cho thông tin hồ sơ gia sư chia sẻ giữa các module.
 * Khuyến nghị sử dụng {@link TutorSummaryDto}.
 */
public record TutorProfileDto(
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
