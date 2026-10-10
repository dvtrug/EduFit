package vn.edufit.profile.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.EducationLevelOrderDto;
import vn.edufit.profile.api.dto.LearningGoalDiscoveryDto;
import vn.edufit.profile.api.dto.TutorDiscoveryProfileDto;
import vn.edufit.profile.api.dto.TutorSearchCriteria;
import vn.edufit.profile.api.dto.TutorSummaryDto;

/**
 * Public Facade Interface của module Profile cung cấp cho các module khác (Verification, Discovery, IAM...) tương tác (ADR-002).
 */
public interface ProfileFacade {

  Optional<TutorSummaryDto> findTutorByUserId(UUID userId);

  Optional<TutorSummaryDto> findTutorById(UUID tutorId);

  Page<TutorSummaryDto> searchTutors(TutorSearchCriteria criteria, Pageable pageable);

  Optional<TutorDiscoveryProfileDto> findVerifiedTutorDetail(UUID tutorId);

  List<TutorDiscoveryProfileDto> findVerifiedTutorDetails(List<UUID> tutorIds);

  List<TutorDiscoveryProfileDto> findVerifiedTutorsForMatching();

  /**
   * Returns the complete level catalog in business order, including retired levels so
   * deactivation does not make previously non-adjacent levels adjacent.
   * Missing or duplicate sort orders are rejected as ambiguous catalog data.
   */
  List<EducationLevelOrderDto> findEducationLevelsForMatching();

  Optional<LearningGoalDiscoveryDto> findLearningGoalForDiscovery(UUID goalId);

  UUID getTutorIdByUserId(UUID userId);

  void markTutorPendingVerification(UUID tutorId);

  void markTutorVerified(UUID tutorId, Instant verifiedAt);

  void markTutorRejected(UUID tutorId);

  Optional<StudentSummaryDto> findStudentByUserId(UUID userId);

  Optional<StudentSummaryDto> findStudentById(UUID studentId);

  UUID getStudentIdByUserId(UUID userId);

  boolean existsTutorByUserId(UUID userId);

  boolean existsStudentByUserId(UUID userId);
}
