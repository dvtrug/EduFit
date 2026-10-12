package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edufit.profile.application.service.LearningGoalService;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.GoalAvailabilitySlot;
import vn.edufit.profile.infra.persistence.entity.GoalStatus;
import vn.edufit.profile.infra.persistence.entity.GoalType;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.GoalAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.web.request.CreateLearningGoalRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.InvalidOperationException;

@ExtendWith(MockitoExtension.class)
class LearningGoalServiceTest {

  @Mock private StudentProfileRepository studentProfiles;
  @Mock private LearningGoalRepository goals;
  @Mock private GoalAvailabilitySlotRepository goalSlots;
  @Mock private SubjectRepository subjects;
  @Mock private EducationLevelRepository educationLevels;

  private LearningGoalService service;
  private UUID userId;
  private UUID studentId;
  private StudentProfile student;

  @BeforeEach
  void setUp() {
    service = new LearningGoalService(studentProfiles, goals, goalSlots, subjects, educationLevels);
    userId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    student = new StudentProfile(userId);
    ReflectionTestUtils.setField(student, "studentId", studentId);
    student.setEducationLevelId(4);
    student.setArea("Cầu Giấy, Hà Nội");
  }

  @Test
  @DisplayName("Liệt kê mục tiêu đang hoạt động bằng dữ liệu từ database")
  void listsActiveGoalsFromDatabase() {
    UUID goalId = UUID.randomUUID();
    LearningGoal goal = new LearningGoal(
        studentId, 1, (short) 1, (short) 3, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, 300_000L
    );
    ReflectionTestUtils.setField(goal, "goalId", goalId);
    goal.setArea("Cầu Giấy, Hà Nội");

    Subject subject = new Subject("Toán", true);
    ReflectionTestUtils.setField(subject, "subjectId", 1);
    EducationLevel level = new EducationLevel("Lớp 9", 4, true);
    ReflectionTestUtils.setField(level, "levelId", 4);

    when(studentProfiles.findByUserId(userId)).thenReturn(Optional.of(student));
    when(goals.findByStudentIdAndStatusOrderByUpdatedAtDesc(studentId, GoalStatus.ACTIVE))
        .thenReturn(List.of(goal));
    when(subjects.findById(1)).thenReturn(Optional.of(subject));
    when(educationLevels.findById(4)).thenReturn(Optional.of(level));
    when(goalSlots.findByGoalIdOrderByDayOfWeekAscStartTimeAsc(goalId))
        .thenReturn(List.of(new GoalAvailabilitySlot(goalId, (short) 2,
            LocalTime.of(19, 0), LocalTime.of(20, 30))));

    var result = service.listActiveGoals(currentUser());

    assertEquals(1, result.size());
    assertEquals("Toán", result.getFirst().subjectName());
    assertEquals("Lớp 9", result.getFirst().educationLevelName());
    assertEquals(1, result.getFirst().availabilitySlots().size());
  }

  @Test
  @DisplayName("Từ chối mục tiêu có mức mong muốn không cao hơn mức hiện tại")
  void rejectsInvalidProgression() {
    when(studentProfiles.findByUserId(userId)).thenReturn(Optional.of(student));

    var request = request((short) 3, (short) 3, List.of(
        new CreateLearningGoalRequest.SlotItem((short) 2, LocalTime.of(19, 0), LocalTime.of(20, 30))
    ));

    assertThrows(InvalidOperationException.class, () -> service.createGoal(currentUser(), request));
  }

  @Test
  @DisplayName("Từ chối các khung giờ học bị chồng lấn trong cùng ngày")
  void rejectsOverlappingSlots() {
    when(studentProfiles.findByUserId(userId)).thenReturn(Optional.of(student));

    var request = request((short) 1, (short) 3, List.of(
        new CreateLearningGoalRequest.SlotItem((short) 2, LocalTime.of(19, 0), LocalTime.of(20, 30)),
        new CreateLearningGoalRequest.SlotItem((short) 2, LocalTime.of(20, 0), LocalTime.of(21, 0))
    ));

    assertThrows(InvalidOperationException.class, () -> service.createGoal(currentUser(), request));
  }

  private CreateLearningGoalRequest request(
      short currentLevel,
      short desiredLevel,
      List<CreateLearningGoalRequest.SlotItem> slots
  ) {
    return new CreateLearningGoalRequest(
        1, currentLevel, desiredLevel, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, "Cầu Giấy, Hà Nội",
        150_000L, 300_000L, slots
    );
  }

  private CurrentUser currentUser() {
    return new CurrentUser() {
      @Override public UUID getUserId() { return userId; }
      @Override public String getEmail() { return "student@edufit.vn"; }
      @Override public Set<String> getRoles() { return Set.of("STUDENT"); }
      @Override public boolean hasRole(String role) { return "STUDENT".equals(role); }
    };
  }
}
