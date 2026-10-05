package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
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
import vn.edufit.profile.application.service.TutorProfileService;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;
import vn.edufit.profile.infra.persistence.entity.TutorSubject;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.infra.persistence.repository.TutorAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.profile.web.request.AddTutorSubjectRequest;
import vn.edufit.profile.web.request.SetAvailabilitySlotsRequest;
import vn.edufit.profile.web.request.UpdateTutorProfileRequest;
import vn.edufit.profile.web.response.TutorProfileResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;

@ExtendWith(MockitoExtension.class)
class TutorProfileServiceTest {

  @Mock
  private TutorProfileRepository tutorProfileRepository;

  @Mock
  private TutorSubjectRepository tutorSubjectRepository;

  @Mock
  private TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository;

  @Mock
  private SubjectRepository subjectRepository;

  @Mock
  private EducationLevelRepository educationLevelRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private TutorProfileService tutorProfileService;

  @BeforeEach
  void setUp() {
    tutorProfileService = new TutorProfileService(
        tutorProfileRepository,
        tutorSubjectRepository,
        tutorAvailabilitySlotRepository,
        subjectRepository,
        educationLevelRepository,
        eventPublisher
    );
  }

  @Test
  @DisplayName("BR-07: Ném lỗi khi hình thức dạy OFFLINE hoặc BOTH mà không có thông tin khu vực")
  void shouldThrowWhenOfflineModeWithoutArea() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);

    UpdateTutorProfileRequest req = new UpdateTutorProfileRequest(
        "Gia sư Toán",
        "Chuyên luyện thi",
        "Bio",
        TeachingMode.OFFLINE,
        null, // area is missing
        250_000L,
        (short) 3,
        "Phương pháp chủ động"
    );

    assertThrows(InvalidOperationException.class, () -> tutorProfileService.updateProfile(currentUser, req));
  }

  @Test
  @DisplayName("Cập nhật hồ sơ gia sư thành công và publish Domain Event")
  void shouldUpdateTutorProfileSuccessfully() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    TutorProfile existing = new TutorProfile(userId, "Gia sư", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateTutorProfileRequest req = new UpdateTutorProfileRequest(
        "Thầy Nam",
        "Gia sư Lý 10 năm kinh nghiệm",
        "Giới thiệu chi tiết",
        TeachingMode.ONLINE,
        null,
        300_000L,
        (short) 10,
        "Học đi đôi với hành"
    );

    TutorProfileResponse resp = tutorProfileService.updateProfile(currentUser, req);

    assertNotNull(resp);
    assertEquals("Thầy Nam", resp.displayName());
    assertEquals(300_000L, resp.pricePerSession());

    ArgumentCaptor<ProfileUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ProfileUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertEquals("TUTOR", eventCaptor.getValue().profileType());
    assertEquals(userId, eventCaptor.getValue().userId());
  }

  @Test
  @DisplayName("Thêm môn học giảng dạy thành công")
  void shouldAddTutorSubjectSuccessfully() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    TutorProfile profile = new TutorProfile(userId, "Gia sư A", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    when(subjectRepository.existsById(1)).thenReturn(true);
    when(educationLevelRepository.existsById(2)).thenReturn(true);
    when(tutorSubjectRepository.existsByTutorIdAndSubjectIdAndEducationLevelId(profile.getTutorId(), 1, 2))
        .thenReturn(false);

    AddTutorSubjectRequest req = new AddTutorSubjectRequest(1, 2);
    TutorProfileResponse resp = tutorProfileService.addSubject(currentUser, req);

    assertNotNull(resp);
    verify(tutorSubjectRepository).save(any(TutorSubject.class));
    verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
  }

  @Test
  @DisplayName("Ném xung đột khi thêm môn học và cấp học đã tồn tại trong hồ sơ")
  void shouldThrowWhenAddingDuplicateSubject() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    TutorProfile profile = new TutorProfile(userId, "Gia sư B", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    when(subjectRepository.existsById(1)).thenReturn(true);
    when(educationLevelRepository.existsById(2)).thenReturn(true);
    when(tutorSubjectRepository.existsByTutorIdAndSubjectIdAndEducationLevelId(profile.getTutorId(), 1, 2))
        .thenReturn(true);

    AddTutorSubjectRequest req = new AddTutorSubjectRequest(1, 2);
    assertThrows(ResourceConflictException.class, () -> tutorProfileService.addSubject(currentUser, req));
  }

  @Test
  @DisplayName("Ném lỗi khi đặt khung giờ kết thúc trước hoặc bằng giờ bắt đầu")
  void shouldThrowWhenSlotEndTimeBeforeStartTime() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    TutorProfile profile = new TutorProfile(userId, "Gia sư C", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

    SetAvailabilitySlotsRequest req = new SetAvailabilitySlotsRequest(List.of(
        new SetAvailabilitySlotsRequest.SlotItem((short) 1, LocalTime.of(10, 0), LocalTime.of(9, 0))
    ));

    assertThrows(InvalidOperationException.class, () -> tutorProfileService.setAvailabilitySlots(currentUser, req));
  }

  @Test
  @DisplayName("NFR-17: Ném lỗi khi các khung giờ rảnh trong cùng ngày bị trùng lấn")
  void shouldThrowWhenAvailabilitySlotsOverlapOnSameDay() {
    UUID userId = UUID.randomUUID();
    CurrentUser currentUser = mockCurrentUser(userId);
    TutorProfile profile = new TutorProfile(userId, "Gia sư D", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

    SetAvailabilitySlotsRequest req = new SetAvailabilitySlotsRequest(List.of(
        new SetAvailabilitySlotsRequest.SlotItem((short) 1, LocalTime.of(8, 0), LocalTime.of(10, 0)),
        new SetAvailabilitySlotsRequest.SlotItem((short) 1, LocalTime.of(9, 30), LocalTime.of(11, 30))
    ));

    assertThrows(InvalidOperationException.class, () -> tutorProfileService.setAvailabilitySlots(currentUser, req));
  }

  private CurrentUser mockCurrentUser(UUID userId) {
    return new CurrentUser() {
      @Override
      public UUID getUserId() {
        return userId;
      }

      @Override
      public String getEmail() {
        return "tutor@edufit.vn";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("TUTOR");
      }

      @Override
      public boolean hasRole(String role) {
        return "TUTOR".equals(role);
      }
    };
  }
}
