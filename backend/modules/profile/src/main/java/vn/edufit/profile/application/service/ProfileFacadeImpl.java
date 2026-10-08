package vn.edufit.profile.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.profile.infra.persistence.repository.LearningGoalRepository;
import vn.edufit.profile.infra.persistence.repository.TutorSubjectRepository;
import vn.edufit.shared.exception.EntityNotFoundException;

@Service
public class ProfileFacadeImpl implements ProfileFacade {

  private final TutorProfileRepository tutorProfileRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final LearningGoalRepository learningGoalRepository;
  private final TutorSubjectRepository tutorSubjectRepository;

  public ProfileFacadeImpl(
      TutorProfileRepository tutorProfileRepository,
      StudentProfileRepository studentProfileRepository,
      LearningGoalRepository learningGoalRepository,
      TutorSubjectRepository tutorSubjectRepository
  ) {
    this.tutorProfileRepository = tutorProfileRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.learningGoalRepository = learningGoalRepository;
    this.tutorSubjectRepository = tutorSubjectRepository;
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

  @Override
  @Transactional(readOnly = true)
  public Optional<ConnectionGoalDto> findGoalForConnection(UUID goalId) {
    return learningGoalRepository.findById(goalId).flatMap(goal -> studentProfileRepository
        .findById(goal.getStudentId()).map(student -> new ConnectionGoalDto(
            goal.getGoalId(), goal.getStudentId(), student.getUserId(), goal.getSubjectId(),
            student.getEducationLevelId(), goal.getGoalType().name(), goal.getDeadline(),
            goal.getStatus().name())));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean tutorTeaches(UUID tutorId, Integer subjectId, Integer educationLevelId) {
    return tutorSubjectRepository.existsByTutorIdAndSubjectIdAndEducationLevelId(
        tutorId, subjectId, educationLevelId);
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
}
