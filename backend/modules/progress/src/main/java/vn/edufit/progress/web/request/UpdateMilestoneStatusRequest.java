package vn.edufit.progress.web.request;

import jakarta.validation.constraints.NotNull;
import vn.edufit.progress.domain.model.MilestoneStatus;

import java.time.LocalDate;

public record UpdateMilestoneStatusRequest(
    @NotNull(message = "version không được để trống")
    Integer version,

    @NotNull(message = "status không được để trống")
    MilestoneStatus status,

    LocalDate achievedDate
) {}
