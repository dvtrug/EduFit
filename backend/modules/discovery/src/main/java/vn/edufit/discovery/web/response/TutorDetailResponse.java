package vn.edufit.discovery.web.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSubjectDto;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;

public record TutorDetailResponse(
    UUID tutorId,
    String displayName,
    String headline,
    String bio,
    String teachingMethod,
    String teachingMode,
    String area,
    Long pricePerSession,
    Short experienceYears,
    BigDecimal ratingAvg,
    Integer reviewCount,
    boolean verified,
    List<TutorSubjectDto> subjects,
    List<WeeklyAvailabilityDto> availabilitySlots
) {

  public static TutorDetailResponse from(TutorDiscoveryProfileDto profile) {
    var tutor = profile.tutor();
    return new TutorDetailResponse(
        tutor.tutorId(), tutor.displayName(), tutor.headline(), tutor.bio(), tutor.teachingMethod(),
        tutor.teachingMode(), tutor.area(), tutor.pricePerSession(), tutor.experienceYears(),
        tutor.ratingAvg(), tutor.reviewCount(), "VERIFIED".equals(tutor.status()),
        profile.subjects(), profile.availabilitySlots()
    );
  }
}
