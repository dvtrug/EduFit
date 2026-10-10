package vn.edufit.discovery.web.response;

import java.math.BigDecimal;
import java.util.UUID;
import vn.edufit.discovery.application.dto.TutorMatchResult;

public record TutorMatchResponse(
    UUID tutorId,
    BigDecimal matchScore,
    MatchScoreBreakdownResponse scoreBreakdown,
    String explanation,
    boolean aiGenerated,
    TutorDiscoveryCardResponse tutorInfo
) {

  public static TutorMatchResponse from(TutorMatchResult result) {
    var score = result.score();
    return new TutorMatchResponse(
        score.tutorId(), score.total(),
        new MatchScoreBreakdownResponse(
            score.scheduleFit(), score.ratingFit(), score.budgetFit(), score.overlappingSlots()
        ),
        result.explanation(), result.aiGenerated(), TutorDiscoveryCardResponse.from(result.profile())
    );
  }
}
