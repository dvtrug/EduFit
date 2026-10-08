package vn.edufit.discovery.web.response;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSubjectDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;

public record TutorDiscoveryCardResponse(
    UUID tutorId,
    String displayName,
    String headline,
    String shortBio,
    String teachingMode,
    String area,
    Long pricePerSession,
    BigDecimal ratingAvg,
    Integer reviewCount,
    boolean verified,
    List<TutorSubjectDto> subjects
) {

  public static TutorDiscoveryCardResponse from(TutorSummaryDto tutor) {
    return new TutorDiscoveryCardResponse(
        tutor.tutorId(),
        tutor.displayName(),
        tutor.headline(),
        shorten(tutor.bio()),
        tutor.teachingMode(),
        tutor.area(),
        tutor.pricePerSession(),
        tutor.ratingAvg(),
        tutor.reviewCount(),
        "VERIFIED".equals(tutor.status()),
        List.of()
    );
  }

  public static TutorDiscoveryCardResponse from(TutorDiscoveryProfileDto profile) {
    TutorSummaryDto tutor = profile.tutor();
    return new TutorDiscoveryCardResponse(
        tutor.tutorId(), tutor.displayName(), tutor.headline(), shorten(tutor.bio()),
        tutor.teachingMode(), tutor.area(), tutor.pricePerSession(), tutor.ratingAvg(),
        tutor.reviewCount(), "VERIFIED".equals(tutor.status()), profile.subjects()
    );
  }

  private static String shorten(String value) {
    if (value == null || value.length() <= 180) {
      return value;
    }
    return value.substring(0, 177) + "...";
  }
}
