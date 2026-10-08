package vn.edufit.profile.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConnectionGoalDto(UUID goalId, UUID studentId, UUID studentUserId,
    Integer subjectId, Integer educationLevelId, String goalType, LocalDate deadline,
    String status) {}
