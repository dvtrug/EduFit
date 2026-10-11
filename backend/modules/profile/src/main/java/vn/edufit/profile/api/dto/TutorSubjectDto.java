package vn.edufit.profile.api.dto;

public record TutorSubjectDto(
    Integer subjectId,
    String subjectName,
    Integer educationLevelId,
    String educationLevelName
) {}
