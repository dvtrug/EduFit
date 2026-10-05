package vn.edufit.profile.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.TutorProfileDto;
import vn.edufit.profile.infra.persistence.entity.TutorProfile;
import vn.edufit.profile.infra.persistence.repository.TutorProfileRepository;
import vn.edufit.shared.exception.EntityNotFoundException;

@Service
public class ProfileFacadeImpl implements ProfileFacade {

  private final TutorProfileRepository tutorProfileRepository;

  public ProfileFacadeImpl(TutorProfileRepository tutorProfileRepository) {
    this.tutorProfileRepository = tutorProfileRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TutorProfileDto> findTutorByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId).map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TutorProfileDto> findTutorById(UUID tutorId) {
    return tutorProfileRepository.findById(tutorId).map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public UUID getTutorIdByUserId(UUID userId) {
    return tutorProfileRepository.findByUserId(userId)
        .map(TutorProfile::getTutorId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ gia sư"));
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

  private TutorProfileDto toDto(TutorProfile profile) {
    return new TutorProfileDto(
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
}

