package vn.edufit.profile.web.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import vn.edufit.profile.infra.persistence.entity.GoalType;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;

/** Full replacement of editable goal fields; identity and status are server-owned. */
public record SaveLearningGoalRequest(
    @NotNull Integer subjectId,
    @NotNull Short currentLevel,
    @NotNull Short desiredLevel,
    @NotNull GoalType goalType,
    @NotNull @Future LocalDate deadline,
    @NotNull TeachingMode mode,
    @Size(max = 100) String area,
    @PositiveOrZero Long budgetMin,
    @NotNull @Positive Long budgetMax) {

  @AssertTrue(message = "desiredLevel phải lớn hơn currentLevel")
  public boolean isLevelOrderValid() {
    return currentLevel == null || desiredLevel == null || desiredLevel > currentLevel;
  }

  @AssertTrue(message = "budgetMin không được lớn hơn budgetMax")
  public boolean isBudgetOrderValid() {
    return budgetMin == null || budgetMax == null || budgetMin <= budgetMax;
  }

  @AssertTrue(message = "OFFLINE/BOTH cần area không trắng")
  public boolean isAreaValid() {
    return mode == null || mode == TeachingMode.ONLINE || (area != null && !area.isBlank());
  }
}
