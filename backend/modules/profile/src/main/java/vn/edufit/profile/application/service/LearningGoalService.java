package vn.edufit.profile.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.infra.persistence.entity.GoalAvailabilitySlot;
import vn.edufit.profile.infra.persistence.entity.GoalStatus;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.GoalAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.web.request.CreateLearningGoalRequest;
import vn.edufit.profile.web.response.LearningGoalResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class LearningGoalService {

  private final StudentProfileRepository studentProfiles;
  private final LearningGoalRepository goals;
  private final GoalAvailabilitySlotRepository goalSlots;
  private final SubjectRepository subjects;
  private final EducationLevelRepository educationLevels;

  public LearningGoalService(
      StudentProfileRepository studentProfiles,
      LearningGoalRepository goals,
      GoalAvailabilitySlotRepository goalSlots,
      SubjectRepository subjects,
      EducationLevelRepository educationLevels
  ) {
    this.studentProfiles = studentProfiles;
    this.goals = goals;
    this.goalSlots = goalSlots;
    this.subjects = subjects;
    this.educationLevels = educationLevels;
  }

  @Transactional(readOnly = true)
  public List<LearningGoalResponse> listActiveGoals(CurrentUser currentUser) {
    StudentProfile student = requireStudent(currentUser);
    return goals.findByStudentIdAndStatusOrderByUpdatedAtDesc(student.getStudentId(), GoalStatus.ACTIVE)
        .stream()
        .map(goal -> toResponse(goal, student))
        .toList();
  }

  @Transactional
  public LearningGoalResponse createGoal(CurrentUser currentUser, CreateLearningGoalRequest request) {
    StudentProfile student = requireStudent(currentUser);
    validate(request);
    if (!subjects.existsById(request.subjectId())) {
      throw EntityNotFoundException.of("Subject", request.subjectId());
    }

    LearningGoal goal = new LearningGoal(
        student.getStudentId(), request.subjectId(), request.currentLevel(), request.desiredLevel(),
        request.goalType(), request.deadline(), request.mode(), request.budgetMax()
    );
    goal.setArea(normalizeArea(request.area()));
    goal.setBudgetMin(request.budgetMin());
    LearningGoal saved = goals.save(goal);

    List<GoalAvailabilitySlot> slots = request.availabilitySlots().stream()
        .map(slot -> new GoalAvailabilitySlot(
            saved.getGoalId(), slot.dayOfWeek(), slot.startTime(), slot.endTime()
        ))
        .toList();
    goalSlots.saveAll(slots);
    return toResponse(saved, student, slots);
  }

  private StudentProfile requireStudent(CurrentUser currentUser) {
    if (currentUser == null || currentUser.getUserId() == null || !currentUser.hasRole("STUDENT")) {
      throw new ForbiddenOperationException("Chỉ học sinh được quản lý mục tiêu học tập của mình.");
    }
    return studentProfiles.findByUserId(currentUser.getUserId())
        .orElseThrow(() -> EntityNotFoundException.of("StudentProfile", currentUser.getUserId()));
  }

  private void validate(CreateLearningGoalRequest request) {
    if (request.desiredLevel() <= request.currentLevel()) {
      throw new InvalidOperationException("Mức mong muốn phải cao hơn mức hiện tại.");
    }
    if (request.budgetMin() != null && request.budgetMin() > request.budgetMax()) {
      throw new InvalidOperationException("Ngân sách tối thiểu không được lớn hơn ngân sách tối đa.");
    }
    if (request.mode() != TeachingMode.ONLINE
        && (request.area() == null || request.area().isBlank())) {
      throw new InvalidOperationException("Khu vực là bắt buộc khi có nhu cầu học trực tiếp.");
    }

    for (CreateLearningGoalRequest.SlotItem slot : request.availabilitySlots()) {
      if (!slot.endTime().isAfter(slot.startTime())) {
        throw new InvalidOperationException("Giờ kết thúc phải sau giờ bắt đầu.");
      }
    }
    Map<Short, List<CreateLearningGoalRequest.SlotItem>> slotsByDay = request.availabilitySlots()
        .stream().collect(Collectors.groupingBy(CreateLearningGoalRequest.SlotItem::dayOfWeek));
    for (List<CreateLearningGoalRequest.SlotItem> sameDaySlots : slotsByDay.values()) {
      List<CreateLearningGoalRequest.SlotItem> sorted = new ArrayList<>(sameDaySlots);
      sorted.sort(Comparator.comparing(CreateLearningGoalRequest.SlotItem::startTime));
      for (int i = 0; i < sorted.size() - 1; i++) {
        if (sorted.get(i).endTime().isAfter(sorted.get(i + 1).startTime())) {
          throw new InvalidOperationException("Các khung giờ học trong cùng ngày không được chồng lấn.");
        }
      }
    }
  }

  private LearningGoalResponse toResponse(LearningGoal goal, StudentProfile student) {
    return toResponse(
        goal,
        student,
        goalSlots.findByGoalIdOrderByDayOfWeekAscStartTimeAsc(goal.getGoalId())
    );
  }

  private LearningGoalResponse toResponse(
      LearningGoal goal,
      StudentProfile student,
      List<GoalAvailabilitySlot> slots
  ) {
    String subjectName = subjects.findById(goal.getSubjectId())
        .map(subject -> subject.getName()).orElse("Môn học");
    String educationLevelName = student.getEducationLevelId() == null ? null
        : educationLevels.findById(student.getEducationLevelId())
            .map(level -> level.getName()).orElse(null);
    return new LearningGoalResponse(
        goal.getGoalId(), goal.getSubjectId(), subjectName,
        student.getEducationLevelId(), educationLevelName,
        goal.getCurrentLevel(), goal.getDesiredLevel(), goal.getGoalType().name(),
        goal.getDeadline(), goal.getMode().name(), goal.getArea(),
        goal.getBudgetMin(), goal.getBudgetMax(), goal.getStatus().name(),
        slots.stream().map(slot -> new LearningGoalResponse.AvailabilitySlotItem(
            slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()
        )).toList()
    );
  }

  private String normalizeArea(String area) {
    return area == null || area.isBlank() ? null : area.trim();
  }
}
