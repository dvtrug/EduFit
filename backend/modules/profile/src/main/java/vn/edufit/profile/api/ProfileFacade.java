package vn.edufit.profile.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.edufit.profile.api.dto.TutorProfileDto;

/**
 * Public Facade Interface của module Profile cho các module khác tương tác (ADR-002).
 */
public interface ProfileFacade {

  Optional<TutorProfileDto> findTutorByUserId(UUID userId);

  Optional<TutorProfileDto> findTutorById(UUID tutorId);

  UUID getTutorIdByUserId(UUID userId);

  void markTutorPendingVerification(UUID tutorId);

  void markTutorVerified(UUID tutorId, Instant verifiedAt);

  void markTutorRejected(UUID tutorId);
}

