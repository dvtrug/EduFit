package vn.edufit.profile.application.service;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.event.ProfileUpdatedEvent;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;
import vn.edufit.profile.infra.persistence.entity.StudentProfile;
import vn.edufit.profile.infra.persistence.repository.EducationLevelRepository;
import vn.edufit.profile.infra.persistence.repository.StudentProfileRepository;
import vn.edufit.profile.web.request.UpdateStudentProfileRequest;
import vn.edufit.profile.web.response.StudentProfileResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ForbiddenOperationException;

@Service
public class StudentProfileService {

  private final StudentProfileRepository studentProfileRepository;
  private final EducationLevelRepository educationLevelRepository;
  private final ApplicationEventPublisher eventPublisher;

  public StudentProfileService(
      StudentProfileRepository studentProfileRepository,
      EducationLevelRepository educationLevelRepository,
      ApplicationEventPublisher eventPublisher
  ) {
    this.studentProfileRepository = studentProfileRepository;
    this.educationLevelRepository = educationLevelRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public StudentProfile getOrCreateProfile(UUID userId) {
    return studentProfileRepository.findByUserId(userId)
        .orElseGet(() -> studentProfileRepository.save(new StudentProfile(userId)));
  }

  @Transactional
  public StudentProfileResponse getProfileResponse(UUID userId) {
    StudentProfile profile = getOrCreateProfile(userId);
    return toResponse(profile);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<StudentProfileResponse> findProfileResponseByUserId(UUID userId) {
    return studentProfileRepository.findByUserId(userId).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public StudentProfileResponse getProfileResponseById(UUID studentId) {
    StudentProfile profile = studentProfileRepository.findById(studentId)
        .orElseThrow(() -> EntityNotFoundException.of("StudentProfile", studentId));
    return toResponse(profile);
  }

  @Transactional
  public StudentProfileResponse updateProfile(CurrentUser currentUser, UpdateStudentProfileRequest request) {
    ensureAuthenticated(currentUser);
    UUID userId = currentUser.getUserId();

    StudentProfile profile = studentProfileRepository.findByUserId(userId)
        .orElseGet(() -> new StudentProfile(userId));

    if (request.educationLevelId() != null) {
      if (!educationLevelRepository.existsById(request.educationLevelId())) {
        throw EntityNotFoundException.of("EducationLevel", request.educationLevelId());
      }
      profile.setEducationLevelId(request.educationLevelId());
    } else {
      profile.setEducationLevelId(null);
    }

    String area = request.area() != null ? request.area().trim() : null;
    profile.setArea(area);

    boolean isComplete = profile.getEducationLevelId() != null
        && profile.getArea() != null
        && !profile.getArea().isBlank();
    profile.setProfileComplete(isComplete);

    StudentProfile saved = studentProfileRepository.save(profile);
    eventPublisher.publishEvent(ProfileUpdatedEvent.of(userId, "STUDENT", saved.getStudentId()));

    return toResponse(saved);
  }

  public StudentProfileResponse toResponse(StudentProfile profile) {
    String levelName = null;
    if (profile.getEducationLevelId() != null) {
      levelName = educationLevelRepository.findById(profile.getEducationLevelId())
          .map(EducationLevel::getName)
          .orElse(null);
    }

    return new StudentProfileResponse(
        profile.getStudentId(),
        profile.getUserId(),
        profile.getEducationLevelId(),
        levelName,
        profile.getArea(),
        profile.isProfileComplete(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }

  private void ensureAuthenticated(CurrentUser currentUser) {
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new ForbiddenOperationException("Yêu cầu cần được xác thực danh tính người dùng.");
    }
  }
}
