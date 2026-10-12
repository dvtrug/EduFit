package vn.edufit.profile.application.service;

import java.util.ArrayList;
import java.util.List;
import vn.edufit.profile.api.dto.LearningGoalView;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;

final class LearningGoalViews {
  private LearningGoalViews() {}

  static LearningGoalView from(
      LearningGoal goal, StudentProfile student, List<WeeklyAvailabilityDto> slots) {
    var reasons = new ArrayList<String>();
    if (!"ACTIVE".equals(goal.getStatus().name())) reasons.add("GOAL_ARCHIVED");
    if (student.getEducationLevelId() == null) reasons.add("EDUCATION_LEVEL_REQUIRED");
    if (goal.getBudgetMax() == null || goal.getBudgetMax() <= 0) reasons.add("BUDGET_MAX_REQUIRED");
    if (!"ONLINE".equals(goal.getMode().name())
        && (goal.getArea() == null || goal.getArea().isBlank())) reasons.add("AREA_REQUIRED");
    if (slots.isEmpty()) reasons.add("AVAILABILITY_REQUIRED");
    return new LearningGoalView(
        goal.getGoalId(),
        goal.getStudentId(),
        goal.getSubjectId(),
        student.getEducationLevelId(),
        goal.getCurrentLevel(),
        goal.getDesiredLevel(),
        goal.getGoalType().name(),
        goal.getDeadline(),
        goal.getMode().name(),
        goal.getArea(),
        goal.getBudgetMin(),
        goal.getBudgetMax(),
        goal.getStatus().name(),
        goal.getCreatedAt(),
        goal.getUpdatedAt(),
        slots,
        reasons.isEmpty(),
        reasons);
  }
}
