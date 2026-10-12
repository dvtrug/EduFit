package vn.edufit.profile.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import vn.edufit.profile.infra.persistence.entity.GoalType;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;

public record CreateLearningGoalRequest(
    @NotNull Integer subjectId,
    @NotNull @Min(1) @Max(5) Short currentLevel,
    @NotNull @Min(1) @Max(5) Short desiredLevel,
    @NotNull GoalType goalType,
    @NotNull @Future LocalDate deadline,
    @NotNull TeachingMode mode,
    @Size(max = 100) String area,
    @Min(0) Long budgetMin,
    @NotNull @Min(1) Long budgetMax,
    @NotEmpty @Valid List<SlotItem> availabilitySlots
) {
  public record SlotItem(
      @NotNull @Min(1) @Max(7) Short dayOfWeek,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime
  ) {}
}
