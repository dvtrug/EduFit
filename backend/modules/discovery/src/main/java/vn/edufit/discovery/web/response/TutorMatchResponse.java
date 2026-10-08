package vn.edufit.discovery.web.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorMatchResponse(
    UUID tutorId,
    BigDecimal matchScore,
    MatchScoreBreakdownResponse scoreBreakdown,
    String explanation,
    boolean aiGenerated,
    TutorDiscoveryCardResponse tutorInfo
) {}
