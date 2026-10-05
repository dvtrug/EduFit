package vn.edufit.profile.web.request;

import jakarta.validation.constraints.Size;

public record UpdateStudentProfileRequest(
    Integer educationLevelId,
    @Size(max = 100, message = "Khu vực không được vượt quá 100 ký tự")
    String area
) {}
