package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vn.edufit.profile.api.event.ProfileUpdatedEvent;
import vn.edufit.profile.application.service.StudentProfileService;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.web.request.UpdateStudentProfileRequest;
import vn.edufit.profile.web.response.StudentProfileResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ForbiddenOperationException;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

  @Mock
  private StudentProfileRepository studentProfileRepository;

  @Mock
  private EducationLevelRepository educationLevelRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private StudentProfileService studentProfileService;

  @BeforeEach
  void setUp() {
    studentProfileService = new StudentProfileService(
        studentProfileRepository,
        educationLevelRepository,
        eventPublisher
    );
  }

  @Test
  @DisplayName("Lấy hồ sơ học viên tự động tạo mới nếu chưa tồn tại")
  void shouldCreateNewStudentProfileIfNotExists() {
    UUID userId = UUID.randomUUID();
    when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
    when(studentProfileRepository.save(any(StudentProfile.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    StudentProfileResponse response = studentProfileService.getProfileResponse(userId);

    assertNotNull(response);
    assertEquals(userId, response.userId());
    assertFalse(response.profileComplete());
  }

  @Test
  @DisplayName("Cập nhật hồ sơ học viên thành công và đánh dấu hoàn thành khi có đủ thông tin")
  void shouldUpdateProfileAndMarkComplete() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    StudentProfile existing = new StudentProfile(userId);

    when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(educationLevelRepository.existsById(1)).thenReturn(true);
    when(educationLevelRepository.findById(1)).thenReturn(Optional.of(new EducationLevel("Lớp 12", 12, true)));
    when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateStudentProfileRequest request = new UpdateStudentProfileRequest(1, "Hà Nội");
    StudentProfileResponse response = studentProfileService.updateProfile(currentUser, request);

    assertNotNull(response);
    assertTrue(response.profileComplete());
    assertEquals("Hà Nội", response.area());
    assertEquals("Lớp 12", response.educationLevelName());

    ArgumentCaptor<ProfileUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ProfileUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertEquals("STUDENT", eventCaptor.getValue().profileType());
    assertEquals(userId, eventCaptor.getValue().userId());
  }

  @Test
  @DisplayName("Ném lỗi khi educationLevelId không tồn tại")
  void shouldThrowWhenEducationLevelNotFound() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    StudentProfile existing = new StudentProfile(userId);

    when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(educationLevelRepository.existsById(999)).thenReturn(false);

    UpdateStudentProfileRequest request = new UpdateStudentProfileRequest(999, "TP.HCM");
    assertThrows(EntityNotFoundException.class, () -> studentProfileService.updateProfile(currentUser, request));
  }

  @Test
  @DisplayName("Ném lỗi khi chưa đăng nhập")
  void shouldThrowWhenUnauthenticated() {
    UpdateStudentProfileRequest request = new UpdateStudentProfileRequest(1, "TP.HCM");
    assertThrows(ForbiddenOperationException.class, () -> studentProfileService.updateProfile(null, request));
  }

  private CurrentUser mockCurrentUser(UUID userId) {
    return new CurrentUser() {
      @Override
      public UUID getUserId() {
        return userId;
      }

      @Override
      public String getEmail() {
        return "student@edufit.vn";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("STUDENT");
      }

      @Override
      public boolean hasRole(String role) {
        return "STUDENT".equals(role);
      }
    };
  }
}
