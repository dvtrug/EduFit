package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.application.service.ProfileFacadeImpl;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.shared.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProfileFacadeImplTest {

  @Mock
  private TutorProfileRepository tutorProfileRepository;

  @Mock
  private StudentProfileRepository studentProfileRepository;

  @Mock private LearningGoalRepository learningGoalRepository;
  @Mock private TutorSubjectRepository tutorSubjectRepository;

  private ProfileFacadeImpl profileFacade;

  @BeforeEach
  void setUp() {
    profileFacade = new ProfileFacadeImpl(tutorProfileRepository, studentProfileRepository,
        learningGoalRepository, tutorSubjectRepository);
  }

  @Test
  @DisplayName("Tìm kiếm gia sư theo userId thành công")
  void shouldFindTutorByUserId() {
    UUID userId = UUID.randomUUID();
    TutorProfile profile = new TutorProfile(userId, "Gia sư A", TeachingMode.ONLINE, 200_000L);

    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

    Optional<TutorSummaryDto> dtoOpt = profileFacade.findTutorByUserId(userId);
    assertTrue(dtoOpt.isPresent());
    assertEquals("Gia sư A", dtoOpt.get().displayName());
  }

  @Test
  @DisplayName("Ném ngoại lệ khi getTutorIdByUserId không tìm thấy")
  void shouldThrowWhenTutorNotFoundByUserId() {
    UUID userId = UUID.randomUUID();
    when(tutorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> profileFacade.getTutorIdByUserId(userId));
  }

  @Test
  @DisplayName("Cập nhật trạng thái gia sư sang PENDING_VERIFICATION")
  void shouldMarkTutorPendingVerification() {
    UUID tutorId = UUID.randomUUID();
    TutorProfile profile = new TutorProfile(UUID.randomUUID(), "Gia sư B", TeachingMode.BOTH, 150_000L);

    when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(profile));
    when(tutorProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    profileFacade.markTutorPendingVerification(tutorId);

    assertEquals(TutorStatus.PENDING_VERIFICATION, profile.getStatus());
    verify(tutorProfileRepository).save(profile);
  }

  @Test
  @DisplayName("Cập nhật trạng thái gia sư sang VERIFIED kèm thời gian duyệt")
  void shouldMarkTutorVerified() {
    UUID tutorId = UUID.randomUUID();
    TutorProfile profile = new TutorProfile(UUID.randomUUID(), "Gia sư C", TeachingMode.OFFLINE, 300_000L);
    Instant now = Instant.now();

    when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(profile));
    when(tutorProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    profileFacade.markTutorVerified(tutorId, now);

    assertEquals(TutorStatus.VERIFIED, profile.getStatus());
    assertNotNull(profile.getVerifiedAt());
    verify(tutorProfileRepository).save(profile);
  }

  @Test
  @DisplayName("Cập nhật trạng thái gia sư sang UNVERIFIED khi từ chối")
  void shouldMarkTutorRejected() {
    UUID tutorId = UUID.randomUUID();
    TutorProfile profile = new TutorProfile(UUID.randomUUID(), "Gia sư D", TeachingMode.ONLINE, 180_000L);

    when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(profile));
    when(tutorProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    profileFacade.markTutorRejected(tutorId);

    assertEquals(TutorStatus.UNVERIFIED, profile.getStatus());
    verify(tutorProfileRepository).save(profile);
  }

  @Test
  @DisplayName("Tìm kiếm học sinh theo userId thành công")
  void shouldFindStudentByUserId() {
    UUID userId = UUID.randomUUID();
    StudentProfile profile = new StudentProfile(userId);
    profile.setArea("Hà Nội");

    when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

    Optional<StudentSummaryDto> dtoOpt = profileFacade.findStudentByUserId(userId);
    assertTrue(dtoOpt.isPresent());
    assertEquals("Hà Nội", dtoOpt.get().area());
  }

  @Test
  @DisplayName("Ném ngoại lệ khi getStudentIdByUserId không tìm thấy")
  void shouldThrowWhenStudentNotFoundByUserId() {
    UUID userId = UUID.randomUUID();
    when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> profileFacade.getStudentIdByUserId(userId));
  }
}
