package vn.edufit.profile.application.service;

import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.dto.LearningGoalView;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.profile.infra.persistence.entity.*;
import vn.edufit.profile.infra.persistence.repository.*;
import vn.edufit.profile.web.request.SaveLearningGoalRequest;
import vn.edufit.profile.web.request.SetGoalAvailabilityRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.*;

@Service
public class LearningGoalService {
  private final LearningGoalRepository goals;
  private final StudentProfileRepository students;
  private final SubjectRepository subjects;
  private final GoalAvailabilitySlotRepository slots;
  private final Validator validator;

  public LearningGoalService(
      LearningGoalRepository goals,
      StudentProfileRepository students,
      SubjectRepository subjects,
      GoalAvailabilitySlotRepository slots,
      Validator validator) {
    this.goals = goals;
    this.students = students;
    this.subjects = subjects;
    this.slots = slots;
    this.validator = validator;
  }

  @Transactional
  public LearningGoalView create(CurrentUser actor, SaveLearningGoalRequest input) {
    StudentProfile student = owner(actor);
    validate(input);
    LearningGoal goal =
        new LearningGoal(
            student.getStudentId(),
            input.subjectId(),
            input.currentLevel(),
            input.desiredLevel(),
            input.goalType(),
            input.deadline(),
            input.mode(),
            input.budgetMax());
    apply(goal, input);
    return view(goals.save(goal), student);
  }

  @Transactional(readOnly = true)
  public List<LearningGoalView> list(CurrentUser actor) {
    StudentProfile student = owner(actor);
    var owned = goals.findByStudentIdOrderByCreatedAtAscGoalIdAsc(student.getStudentId());
    if (owned.isEmpty()) return List.of();
    var byGoal =
        slots
            .findByGoalIdInOrderByDayOfWeekAscStartTimeAsc(
                owned.stream().map(LearningGoal::getGoalId).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    GoalAvailabilitySlot::getGoalId,
                    Collectors.mapping(
                        slot ->
                            new WeeklyAvailabilityDto(
                                slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()),
                        Collectors.toList())));
    return owned.stream()
        .map(
            goal ->
                LearningGoalViews.from(
                    goal, student, byGoal.getOrDefault(goal.getGoalId(), List.of())))
        .toList();
  }

  @Transactional
  public LearningGoalView update(CurrentUser actor, UUID goalId, SaveLearningGoalRequest input) {
    StudentProfile student = owner(actor);
    LearningGoal goal = ownedForUpdate(goalId, student);
    ensureActive(goal);
    validate(input);
    apply(goal, input);
    return view(goal, student);
  }

  @Transactional
  public LearningGoalView archive(CurrentUser actor, UUID goalId) {
    StudentProfile student = owner(actor);
    LearningGoal goal = ownedForUpdate(goalId, student);
    if (goal.getStatus() != GoalStatus.ARCHIVED) goal.setStatus(GoalStatus.ARCHIVED);
    return view(goal, student);
  }

  @Transactional
  public LearningGoalView replaceAvailability(
      CurrentUser actor, UUID goalId, SetGoalAvailabilityRequest input) {
    StudentProfile student = owner(actor);
    LearningGoal goal = ownedForUpdate(goalId, student);
    ensureActive(goal);
    if (input == null || !validator.validate(input).isEmpty())
      throw new InvalidOperationException("Danh sách khung giờ không hợp lệ.");
    var ordered =
        input.slots().stream()
            .sorted(
                Comparator.comparing(SetGoalAvailabilityRequest.Slot::dayOfWeek)
                    .thenComparing(SetGoalAvailabilityRequest.Slot::startTime))
            .toList();
    for (int index = 1; index < ordered.size(); index++) {
      var previous = ordered.get(index - 1);
      var current = ordered.get(index);
      if (previous.dayOfWeek().equals(current.dayOfWeek())
          && current.startTime().isBefore(previous.endTime()))
        throw new InvalidOperationException("Khung giờ không được trùng hoặc giao nhau.");
    }
    // Bulk delete executes before inserts; the owner goal lock serializes replace/update/archive.
    slots.deleteForGoal(goalId);
    slots.saveAllAndFlush(
        ordered.stream()
            .map(
                slot ->
                    new GoalAvailabilitySlot(
                        goalId, slot.dayOfWeek(), slot.startTime(), slot.endTime()))
            .toList());
    goal.touch();
    return view(goal, student);
  }

  private StudentProfile owner(CurrentUser actor) {
    if (actor == null || actor.getUserId() == null || !actor.hasRole("STUDENT"))
      throw new ForbiddenOperationException("Chỉ học sinh được quản lý mục tiêu của mình.");
    return students
        .findByUserId(actor.getUserId())
        .orElseThrow(() -> EntityNotFoundException.of("StudentProfile", actor.getUserId()));
  }

  private LearningGoal ownedForUpdate(UUID goalId, StudentProfile student) {
    return goals
        .findByGoalIdAndStudentId(goalId, student.getStudentId())
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", goalId));
  }

  private void ensureActive(LearningGoal goal) {
    if (goal.getStatus() != GoalStatus.ACTIVE)
      throw new InvalidOperationException("Mục tiêu đã lưu trữ không được chỉnh sửa.");
  }

  private void validate(SaveLearningGoalRequest input) {
    if (input == null || !validator.validate(input).isEmpty())
      throw new InvalidOperationException("Dữ liệu mục tiêu không hợp lệ.");
    if (subjects
        .findById(input.subjectId())
        .filter(subject -> Boolean.TRUE.equals(subject.getIsActive()))
        .isEmpty()) throw new InvalidOperationException("Môn học phải tồn tại và đang hoạt động.");
  }

  private void apply(LearningGoal goal, SaveLearningGoalRequest input) {
    goal.setSubjectId(input.subjectId());
    goal.setCurrentLevel(input.currentLevel());
    goal.setDesiredLevel(input.desiredLevel());
    goal.setGoalType(input.goalType());
    goal.setDeadline(input.deadline());
    goal.setMode(input.mode());
    goal.setArea(input.area() == null || input.area().isBlank() ? null : input.area().trim());
    goal.setBudgetMin(input.budgetMin());
    goal.setBudgetMax(input.budgetMax());
    goal.touch();
  }

  private LearningGoalView view(LearningGoal goal, StudentProfile student) {
    var availability =
        slots.findByGoalIdOrderByDayOfWeekAscStartTimeAsc(goal.getGoalId()).stream()
            .map(
                slot ->
                    new WeeklyAvailabilityDto(
                        slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()))
            .toList();
    return LearningGoalViews.from(goal, student, availability);
  }
}
