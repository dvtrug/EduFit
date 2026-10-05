package vn.edufit.profile.web.response;

import java.util.Set;
import java.util.UUID;

public record MyProfileResponse(
    UUID userId,
    String email,
    Set<String> roles,
    StudentProfileResponse studentProfile,
    TutorProfileResponse tutorProfile
) {}
