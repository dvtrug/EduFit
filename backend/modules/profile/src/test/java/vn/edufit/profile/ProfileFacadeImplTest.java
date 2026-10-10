package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edufit.profile.api.dto.EducationLevelOrderDto;
import vn.edufit.profile.api.dto.TutorCandidateCriteria;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorSearchCriteria;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.application.service.ProfileFacadeImpl;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.profile.infra.persistence.repository.TutorAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.GoalAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.InvalidOperationException;

@ExtendWith(MockitoExtension.class)
class ProfileFacadeImplTest {

  @Mock
  private TutorProfileRepository tutorProfileRepository;

  @Mock
  private StudentProfileRepository studentProfileRepository;

  @Mock
  private TutorSubjectRepository tutorSubjectRepository;

  @Mock
  private TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository;

  @Mock
  private LearningGoalRepository learningGoalRepository;

  @Mock
  private GoalAvailabilitySlotRepository goalAvailabilitySlotRepository;

  @Mock
  private EducationLevelRepository educationLevelRepository;

  private ProfileFacadeImpl profileFacade;

  @ParameterizedTest
  @CsvSource({"online, HANOI, true,false,hanoi", "OFFLINE, Hanoi,false,true,hanoi", "BOTH,,true,true,"})
  void candidateQueryNormalizesCriteriaAndLimitsBeforeHydration(
      String mode, String area, boolean online, boolean offline, String normalizedArea
  ) {
    when(tutorProfileRepository.findVerifiedCandidatesBySubject(
        TutorStatus.VERIFIED, 1, online, offline, normalizedArea, PageRequest.of(0, 200)
    )).thenReturn(List.of());

    assertEquals(List.of(), profileFacade.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(1, mode, area)));
    verify(tutorProfileRepository).findVerifiedCandidatesBySubject(
        TutorStatus.VERIFIED, 1, online, offline, normalizedArea, PageRequest.of(0, 200));
    verifyNoInteractions(tutorSubjectRepository, tutorAvailabilitySlotRepository);
  }

  @Test
  void offlineWithoutAreaCannotProduceCandidates() {
    assertEquals(List.of(), profileFacade.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(1, "OFFLINE", " ")));
    verifyNoInteractions(tutorProfileRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @org.junit.jupiter.params.provider.ValueSource(strings = {"invalid"})
  void matchingRequiresValidMode(String mode) {
    assertThrows(InvalidOperationException.class,
        () -> profileFacade.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(1, mode, null)));
    verifyNoInteractions(tutorProfileRepository);
  }

  @Test
  void matchingRejectsMissingSubjectAndSupportsConfiguredLimit() {
    assertThrows(InvalidOperationException.class, () -> profileFacade.findVerifiedCandidatesBySubject(null));
    assertThrows(InvalidOperationException.class,
        () -> profileFacade.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(null, "ONLINE", null)));
    assertThrows(InvalidOperationException.class,
        () -> profileFacade.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(0, "ONLINE", null)));
    var limited = new ProfileFacadeImpl(tutorProfileRepository, studentProfileRepository, tutorSubjectRepository,
        tutorAvailabilitySlotRepository, learningGoalRepository, goalAvailabilitySlotRepository,
        educationLevelRepository, 2);
    limited.findVerifiedCandidatesBySubject(new TutorCandidateCriteria(1, "ONLINE", null));
    verify(tutorProfileRepository).findVerifiedCandidatesBySubject(
        TutorStatus.VERIFIED, 1, true, false, null, PageRequest.of(0, 2));
    assertThrows(IllegalArgumentException.class, () -> new ProfileFacadeImpl(
        tutorProfileRepository, studentProfileRepository, tutorSubjectRepository,
        tutorAvailabilitySlotRepository, learningGoalRepository, goalAvailabilitySlotRepository,
        educationLevelRepository, 0));
  }

  @Test
  void shouldExposeBusinessOrderingWithoutChangingLevelIds() {
    var first = level(80, 10, true);
    var retired = level(3, 30, false);
    var last = level(41, 60, true);
    when(educationLevelRepository.findAllByOrderBySortOrderAscLevelIdAsc())
        .thenReturn(List.of(first, retired, last));

    assertEquals(List.of(new EducationLevelOrderDto(80, 10), new EducationLevelOrderDto(3, 30),
        new EducationLevelOrderDto(41, 60)), profileFacade.findEducationLevelsForMatching());
  }

  @Test
  void shouldRejectAmbiguousLevelOrdering() {
    when(educationLevelRepository.findAllByOrderBySortOrderAscLevelIdAsc())
        .thenReturn(List.of(level(80, 10, true), level(3, 10, true)));
    assertThrows(InvalidOperationException.class, profileFacade::findEducationLevelsForMatching);
  }

  @Test
  void shouldRejectMissingLevelOrdering() {
    var level = level(80, 10, true);
    level.setSortOrder(null);
    when(educationLevelRepository.findAllByOrderBySortOrderAscLevelIdAsc()).thenReturn(List.of(level));
    assertThrows(InvalidOperationException.class, profileFacade::findEducationLevelsForMatching);
  }

  private EducationLevel level(int id, int sortOrder, boolean active) {
    var level = new EducationLevel("Level " + id, sortOrder, active);
    ReflectionTestUtils.setField(level, "levelId", id);
    return level;
  }

  @BeforeEach
  void setUp() {
    profileFacade = new ProfileFacadeImpl(
        tutorProfileRepository,
        studentProfileRepository,
        tutorSubjectRepository,
        tutorAvailabilitySlotRepository,
        learningGoalRepository,
        goalAvailabilitySlotRepository,
        educationLevelRepository,
        200
    );
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
  @DisplayName("Tìm kiếm discovery chỉ trả hồ sơ gia sư đã VERIFIED theo bộ lọc")
  void shouldSearchVerifiedTutorsForDiscovery() {
    TutorProfile profile = new TutorProfile(UUID.randomUUID(), "Gia sư Search", TeachingMode.BOTH, 220_000L);
    profile.markVerified(Instant.now());
    TutorSearchCriteria criteria = new TutorSearchCriteria(1, 2, "Hà Nội", "ONLINE", 100_000L, 300_000L, null);
    PageRequest pageable = PageRequest.of(0, 20);

    when(tutorProfileRepository.searchVerifiedTutors(
        TutorStatus.VERIFIED,
        criteria.subjectId(),
        criteria.levelId(),
        "Hà Nội",
        TeachingMode.ONLINE,
        true,
        TeachingMode.BOTH,
        criteria.minPrice(),
        criteria.maxPrice(),
        criteria.minRating(),
        null,
        null,
        null,
        null,
        pageable
    )).thenReturn(new PageImpl<>(List.of(profile)));

    var result = profileFacade.searchTutors(criteria, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals("Gia sư Search", result.getContent().getFirst().displayName());
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
