package vn.edufit.discovery.web.response;

import java.math.BigDecimal;

public record MatchScoreBreakdownResponse(
    BigDecimal scheduleFit,
    BigDecimal ratingFit,
    BigDecimal budgetFit,
    int overlappingSlots
) {}
