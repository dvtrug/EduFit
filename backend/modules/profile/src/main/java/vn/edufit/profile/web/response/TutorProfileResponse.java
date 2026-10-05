package vn.edufit.profile.web.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TutorProfileResponse(
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
    Integer reviewCount,
    List<TutorSubjectResponse> subjects,
    List<TutorSlotResponse> availabilitySlots,
    Instant createdAt,
    Instant updatedAt
) {
  public record TutorSubjectResponse(
      UUID tutorSubjectId,
      Integer subjectId,
      String subjectName,
      Integer educationLevelId,
      String educationLevelName
  ) {}

  public record TutorSlotResponse(
      UUID slotId,
      Short dayOfWeek,
      LocalTime startTime,
      LocalTime endTime
  ) {}
}
