package vn.edufit.profile.web.request;

import jakarta.validation.constraints.NotNull;

public record AddTutorSubjectRequest(
    @NotNull(message = "Môn học không được để trống")
    Integer subjectId,

    @NotNull(message = "Cấp học không được để trống")
    Integer educationLevelId
) {}
