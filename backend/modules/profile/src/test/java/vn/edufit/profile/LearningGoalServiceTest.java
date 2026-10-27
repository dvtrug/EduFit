package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
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
import vn.edufit.profile.infra.persistence.entity.GoalStatus;
import vn.edufit.profile.infra.persistence.entity.GoalType;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.repository.GoalAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.web.request.SaveLearningGoalRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.InvalidOperationException;

@ExtendWith(MockitoExtension.class)
class LearningGoalServiceTest {

  @Mock
  private LearningGoalRepository goals;

  @Mock
  private StudentProfileRepository students;

  @Mock
  private SubjectRepository subjects;

  @Mock
  private GoalAvailabilitySlotRepository slots;

  private Validator validator;
  private LearningGoalService service;

  private final UUID testUserId = UUID.randomUUID();
  private final UUID testStudentId = UUID.randomUUID();
  private final UUID testGoalId = UUID.randomUUID();
  private CurrentUser actor;
  private StudentProfile student;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    service = new LearningGoalService(goals, students, subjects, slots, validator);
    actor = new CurrentUser() {
      @Override public UUID getUserId() { return testUserId; }
      @Override public String getEmail() { return "student@edufit.vn"; }
      @Override public Set<String> getRoles() { return Set.of("STUDENT"); }
      @Override public boolean hasRole(String role) { return "STUDENT".equals(role); }
    };
    student = new StudentProfile(testUserId);
    ReflectionTestUtils.setField(student, "studentId", testStudentId);
  }

  @Test
  @DisplayName("Chỉnh sửa mục tiêu: Ném lỗi khi cố ý thay đổi subjectId của mục tiêu đã tồn tại")
  void updateThrowsWhenChangingSubjectId() {
    LearningGoal existingGoal = new LearningGoal(
        testStudentId, 1, (short) 1, (short) 4, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, 300000L);
    ReflectionTestUtils.setField(existingGoal, "goalId", testGoalId);

    when(students.findByUserId(testUserId)).thenReturn(Optional.of(student));
    when(goals.findByGoalIdAndStudentId(testGoalId, testStudentId)).thenReturn(Optional.of(existingGoal));

    Subject subject2 = new Subject("Vật lý", true);
    ReflectionTestUtils.setField(subject2, "subjectId", 2);
    when(subjects.findById(2)).thenReturn(Optional.of(subject2));

    SaveLearningGoalRequest request = new SaveLearningGoalRequest(
        2, (short) 1, (short) 4, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, "Cầu Giấy", 150000L, 300000L);

    InvalidOperationException ex = assertThrows(
        InvalidOperationException.class,
        () -> service.update(actor, testGoalId, request)
    );
    assertEquals("Môn học của mục tiêu không được phép thay đổi. Hãy tạo mục tiêu mới cho môn học khác.", ex.getMessage());
  }

  @Test
  @DisplayName("Chỉnh sửa mục tiêu: Thành công khi giữ nguyên subjectId")
  void updateSucceedsWhenSubjectIdRemainsSame() {
    LearningGoal existingGoal = new LearningGoal(
        testStudentId, 1, (short) 1, (short) 4, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, 300000L);
    ReflectionTestUtils.setField(existingGoal, "goalId", testGoalId);

    when(students.findByUserId(testUserId)).thenReturn(Optional.of(student));
    when(goals.findByGoalIdAndStudentId(testGoalId, testStudentId)).thenReturn(Optional.of(existingGoal));

    Subject subject1 = new Subject("Toán", true);
    ReflectionTestUtils.setField(subject1, "subjectId", 1);
    when(subjects.findById(1)).thenReturn(Optional.of(subject1));
    when(slots.findByGoalIdOrderByDayOfWeekAscStartTimeAsc(testGoalId)).thenReturn(List.of());

    SaveLearningGoalRequest request = new SaveLearningGoalRequest(
        1, (short) 2, (short) 5, GoalType.ADVANCED_STUDY,
        LocalDate.now().plusMonths(8), TeachingMode.ONLINE, null, 200000L, 400000L);

    var view = service.update(actor, testGoalId, request);
    assertEquals(1, view.subjectId());
    assertEquals("ADVANCED_STUDY", view.goalType());
    assertEquals(400000L, view.budgetMax());
  }

  @Test
  @DisplayName("Lưu trữ mục tiêu: Cập nhật trạng thái thành ARCHIVED")
  void archiveMarksStatusAsArchived() {
    LearningGoal existingGoal = new LearningGoal(
        testStudentId, 1, (short) 1, (short) 4, GoalType.EXAM_PREPARATION,
        LocalDate.now().plusMonths(6), TeachingMode.BOTH, 300000L);
    ReflectionTestUtils.setField(existingGoal, "goalId", testGoalId);

    when(students.findByUserId(testUserId)).thenReturn(Optional.of(student));
    when(goals.findByGoalIdAndStudentId(testGoalId, testStudentId)).thenReturn(Optional.of(existingGoal));
    when(slots.findByGoalIdOrderByDayOfWeekAscStartTimeAsc(testGoalId)).thenReturn(List.of());

    var view = service.archive(actor, testGoalId);
    assertEquals("ARCHIVED", view.status());
    assertEquals(GoalStatus.ARCHIVED, existingGoal.getStatus());
  }
}
