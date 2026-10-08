package vn.edufit.profile.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSearchCriteria;
import vn.edufit.profile.api.dto.TutorSubjectDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.api.dto.WeeklyAvailabilityDto;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.Subject;
import vn.edufit.profile.infra.persistence.entity.TeachingMode;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.entity.TutorStatus;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.GoalAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.SubjectRepository;
import vn.edufit.profile.infra.persistence.repository.TutorAvailabilitySlotRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class ProfileFacadeImpl implements ProfileFacade {

  private final TutorProfileRepository tutorProfileRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final TutorSubjectRepository tutorSubjectRepository;
  private final TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository;
  private final LearningGoalRepository learningGoalRepository;
  private final GoalAvailabilitySlotRepository goalAvailabilitySlotRepository;
  private final SubjectRepository subjectRepository;
  private final EducationLevelRepository educationLevelRepository;

  public ProfileFacadeImpl(
      TutorProfileRepository tutorProfileRepository,
      StudentProfileRepository studentProfileRepository,
      TutorSubjectRepository tutorSubjectRepository,
      TutorAvailabilitySlotRepository tutorAvailabilitySlotRepository,
      LearningGoalRepository learningGoalRepository,
      GoalAvailabilitySlotRepository goalAvailabilitySlotRepository,
      SubjectRepository subjectRepository,
      EducationLevelRepository educationLevelRepository
  ) {
    this.tutorProfileRepository = tutorProfileRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.tutorSubjectRepository = tutorSubjectRepository;
    this.tutorAvailabilitySlotRepository = tutorAvailabilitySlotRepository;
    this.learningGoalRepository = learningGoalRepository;
    this.goalAvailabilitySlotRepository = goalAvailabilitySlotRepository;
    this.subjectRepository = subjectRepository;
    this.educationLevelRepository = educationLevelRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TutorSummaryDto> findTutorByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId).map(this::toTutorDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TutorSummaryDto> findTutorById(UUID tutorId) {
    return tutorProfileRepository.findById(tutorId).map(this::toTutorDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TutorSummaryDto> searchTutors(TutorSearchCriteria criteria, Pageable pageable) {
    TutorSearchCriteria safeCriteria = criteria != null
        ? criteria
        : new TutorSearchCriteria(null, null, null, null, null, null, null);
    TeachingMode mode = parseTeachingMode(safeCriteria.mode());
    String area = normalizeBlank(safeCriteria.area());
    boolean includeBothMode = mode == TeachingMode.ONLINE || mode == TeachingMode.OFFLINE;

    return tutorProfileRepository.searchVerifiedTutors(
        TutorStatus.VERIFIED,
        safeCriteria.subjectId(),
        safeCriteria.levelId(),
        area,
        mode,
        includeBothMode,
        TeachingMode.BOTH,
        safeCriteria.minPrice(),
        safeCriteria.maxPrice(),
        safeCriteria.minRating(),
        normalizeBlank(safeCriteria.keyword()),
        safeCriteria.dayOfWeek(),
        safeCriteria.availableFrom(),
        safeCriteria.availableTo(),
        pageable
    ).map(this::toTutorDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TutorDiscoveryProfileDto> findVerifiedTutorDetail(UUID tutorId) {
    return tutorProfileRepository.findById(tutorId)
        .filter(tutor -> tutor.getStatus() == TutorStatus.VERIFIED)
        .map(tutor -> toDiscoveryProfiles(List.of(tutor)).getFirst());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TutorDiscoveryProfileDto> findVerifiedTutorDetails(List<UUID> tutorIds) {
    if (tutorIds == null || tutorIds.isEmpty()) {
      return List.of();
    }
    return toDiscoveryProfiles(
        tutorProfileRepository.findByTutorIdInAndStatus(tutorIds, TutorStatus.VERIFIED)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<TutorDiscoveryProfileDto> findVerifiedTutorsForMatching() {
    return toDiscoveryProfiles(tutorProfileRepository.findByStatus(TutorStatus.VERIFIED));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LearningGoalDiscoveryDto> findLearningGoalForDiscovery(UUID goalId) {
    return learningGoalRepository.findById(goalId).flatMap(goal ->
        studentProfileRepository.findById(goal.getStudentId())
            .map(student -> toLearningGoalDto(goal, student))
    );
  }

  @Override
  @Transactional(readOnly = true)
  public UUID getTutorIdByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId)
        .map(TutorProfile::getTutorId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ gia sư cho người dùng này"));
  }

  @Override
  @Transactional
  public void markTutorPendingVerification(UUID tutorId) {
    TutorProfile tutor = tutorProfileRepository.findById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
    tutor.markPendingVerification();
    tutorProfileRepository.save(tutor);
  }

  @Override
  @Transactional
  public void markTutorVerified(UUID tutorId, Instant verifiedAt) {
    TutorProfile tutor = tutorProfileRepository.findById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
    tutor.markVerified(verifiedAt);
    tutorProfileRepository.save(tutor);
  }

  @Override
  @Transactional
  public void markTutorRejected(UUID tutorId) {
    TutorProfile tutor = tutorProfileRepository.findById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("TutorProfile", tutorId));
    tutor.markRejected();
    tutorProfileRepository.save(tutor);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StudentSummaryDto> findStudentByUserId(UUID userId) {
    return studentProfileRepository.findByUserId(userId).map(this::toStudentDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StudentSummaryDto> findStudentById(UUID studentId) {
    return studentProfileRepository.findById(studentId).map(this::toStudentDto);
  }

  @Override
  @Transactional(readOnly = true)
  public UUID getStudentIdByUserId(UUID userId) {
    return studentProfileRepository.findByUserId(userId)
        .map(StudentProfile::getStudentId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ học sinh cho người dùng này"));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsTutorByUserId(UUID userId) {
    return tutorProfileRepository.existsByUserId(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsStudentByUserId(UUID userId) {
    return studentProfileRepository.existsByUserId(userId);
  }

  private TutorSummaryDto toTutorDto(TutorProfile profile) {
    return new TutorSummaryDto(
        profile.getTutorId(),
        profile.getUserId(),
        profile.getDisplayName(),
        profile.getHeadline(),
        profile.getBio(),
        profile.getTeachingMode() != null ? profile.getTeachingMode().name() : null,
        profile.getArea(),
        profile.getPricePerSession(),
        profile.getExperienceYears(),
        profile.getTeachingMethod(),
        profile.getStatus() != null ? profile.getStatus().name() : null,
        profile.getVerifiedAt(),
        profile.getRatingAvg(),
        profile.getReviewCount()
    );
  }

  private StudentSummaryDto toStudentDto(StudentProfile profile) {
    return new StudentSummaryDto(
        profile.getStudentId(),
        profile.getUserId(),
        profile.getEducationLevelId(),
        profile.getArea(),
        profile.isProfileComplete()
    );
  }

  private List<TutorDiscoveryProfileDto> toDiscoveryProfiles(List<TutorProfile> profiles) {
    if (profiles.isEmpty()) {
      return List.of();
    }
    Set<UUID> tutorIds = profiles.stream().map(TutorProfile::getTutorId).collect(Collectors.toSet());
    var tutorSubjects = tutorSubjectRepository.findByTutorIdIn(tutorIds);
    Map<Integer, String> subjectNames = subjectRepository.findAllById(
        tutorSubjects.stream().map(item -> item.getSubjectId()).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(Subject::getSubjectId, Subject::getName));
    Map<Integer, String> levelNames = educationLevelRepository.findAllById(
        tutorSubjects.stream().map(item -> item.getEducationLevelId()).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(EducationLevel::getLevelId, EducationLevel::getName));
    Map<UUID, List<TutorSubjectDto>> subjectsByTutor = tutorSubjects.stream()
        .collect(Collectors.groupingBy(
            item -> item.getTutorId(),
            Collectors.mapping(item -> new TutorSubjectDto(
                item.getSubjectId(),
                subjectNames.get(item.getSubjectId()),
                item.getEducationLevelId(),
                levelNames.get(item.getEducationLevelId())
            ), Collectors.toList())
        ));
    Map<UUID, List<WeeklyAvailabilityDto>> slotsByTutor = tutorAvailabilitySlotRepository
        .findByTutorIdInOrderByDayOfWeekAscStartTimeAsc(tutorIds).stream()
        .collect(Collectors.groupingBy(
            slot -> slot.getTutorId(),
            Collectors.mapping(
                slot -> new WeeklyAvailabilityDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()),
                Collectors.toList()
            )
        ));

    return profiles.stream()
        .map(profile -> new TutorDiscoveryProfileDto(
            toTutorDto(profile),
            subjectsByTutor.getOrDefault(profile.getTutorId(), List.of()),
            slotsByTutor.getOrDefault(profile.getTutorId(), List.of()),
            profile.getCreatedAt()
        ))
        .toList();
  }

  private LearningGoalDiscoveryDto toLearningGoalDto(LearningGoal goal, StudentProfile student) {
    List<WeeklyAvailabilityDto> slots = goalAvailabilitySlotRepository
        .findByGoalIdOrderByDayOfWeekAscStartTimeAsc(goal.getGoalId()).stream()
        .map(slot -> new WeeklyAvailabilityDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()))
        .toList();

    return new LearningGoalDiscoveryDto(
        goal.getGoalId(),
        goal.getStudentId(),
        student.getUserId(),
        goal.getSubjectId(),
        student.getEducationLevelId(),
        goal.getMode().name(),
        goal.getArea(),
        goal.getBudgetMin(),
        goal.getBudgetMax(),
        goal.getStatus().name(),
        slots
    );
  }

  private TeachingMode parseTeachingMode(String rawMode) {
    String normalized = normalizeBlank(rawMode);
    if (normalized == null) {
      return null;
    }

    try {
      return TeachingMode.valueOf(normalized.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new InvalidOperationException(
          ErrorCode.VALIDATION_FAILED,
          "Hình thức dạy không hợp lệ. Giá trị hợp lệ: ONLINE, OFFLINE, BOTH"
      );
    }
  }

  private String normalizeBlank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
