package vn.edufit.discovery.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Matching summary shared with backend modules; matchScore uses the 0..100 scale. */
public record TutorMatchSummaryDto(
    UUID tutorId,
    String displayName,
    BigDecimal matchScore,
    String explanation,
    boolean aiGenerated
) {}
