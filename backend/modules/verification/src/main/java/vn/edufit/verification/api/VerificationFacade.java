package vn.edufit.verification.api;

import java.util.Optional;
import java.util.UUID;
import vn.edufit.verification.api.dto.VerificationStatusDto;

public interface VerificationFacade {

  Optional<VerificationStatusDto> getLatestStatusByTutorId(UUID tutorId);

  boolean hasPendingVerification(UUID tutorId);
}

