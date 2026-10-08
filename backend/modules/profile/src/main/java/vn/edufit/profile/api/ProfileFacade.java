package vn.edufit.profile.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.api.dto.ConnectionGoalDto;

/**
 * Public Facade Interface của module Profile cung cấp cho các module khác (Verification, Discovery, IAM...) tương tác (ADR-002).
 */
public interface ProfileFacade {

  Optional<TutorSummaryDto> findTutorByUserId(UUID userId);

  Optional<TutorSummaryDto> findTutorById(UUID tutorId);

  UUID getTutorIdByUserId(UUID userId);

  void markTutorPendingVerification(UUID tutorId);

  void markTutorVerified(UUID tutorId, Instant verifiedAt);

  void markTutorRejected(UUID tutorId);

  Optional<StudentSummaryDto> findStudentByUserId(UUID userId);

  Optional<StudentSummaryDto> findStudentById(UUID studentId);

  UUID getStudentIdByUserId(UUID userId);

  boolean existsTutorByUserId(UUID userId);

  boolean existsStudentByUserId(UUID userId);

  Optional<ConnectionGoalDto> findGoalForConnection(UUID goalId);

  boolean tutorTeaches(UUID tutorId, Integer subjectId, Integer educationLevelId);
}
