package vn.edufit.verification.application.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.verification.api.VerificationFacade;
import vn.edufit.verification.api.dto.VerificationStatusDto;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;
import vn.edufit.verification.infra.persistence.repository.VerificationRequestRepository;

@Service
public class VerificationFacadeImpl implements VerificationFacade {

  private final VerificationRequestRepository verificationRequestRepository;

  public VerificationFacadeImpl(VerificationRequestRepository verificationRequestRepository) {
    this.verificationRequestRepository = verificationRequestRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerificationStatusDto> getLatestStatusByTutorId(UUID tutorId) {
    return verificationRequestRepository.findFirstByTutorIdOrderBySubmittedAtDesc(tutorId)
        .map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasPendingVerification(UUID tutorId) {
    return verificationRequestRepository.existsByTutorIdAndStatus(tutorId, VerificationRequest.Status.PENDING);
  }

  private VerificationStatusDto toDto(VerificationRequest request) {
    return new VerificationStatusDto(
        request.getRequestId(),
        request.getTutorId(),
        request.getStatus().name(),
        request.getRejectionReason(),
        request.getSubmittedAt(),
        request.getReviewedAt()
    );
  }
}

