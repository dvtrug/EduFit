package vn.edufit.verification.application.service;

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
  public boolean isTutorVerified(UUID tutorId) {
    return verificationRequestRepository
        .findFirstByTutorIdOrderBySubmittedAtDesc(tutorId)
        .map(request -> request.getStatus() == VerificationRequest.Status.APPROVED)
        .orElse(false);
  }

  @Override
  @Transactional(readOnly = true)
  public VerificationStatusDto getTutorVerificationStatus(UUID tutorId) {
    return verificationRequestRepository
        .findFirstByTutorIdOrderBySubmittedAtDesc(tutorId)
        .map(request -> new VerificationStatusDto(
            tutorId,
            request.getStatus() == VerificationRequest.Status.APPROVED,
            request.getStatus().name(),
            request.getSubmittedAt(),
            request.getReviewedAt()
        ))
        .orElse(new VerificationStatusDto(tutorId, false, null, null, null));
  }
}
