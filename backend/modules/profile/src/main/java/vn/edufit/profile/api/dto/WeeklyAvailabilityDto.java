package vn.edufit.profile.api.dto;

import java.time.LocalTime;

public record WeeklyAvailabilityDto(
    Short dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {}
