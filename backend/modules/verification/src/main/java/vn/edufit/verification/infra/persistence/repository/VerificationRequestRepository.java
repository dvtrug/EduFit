package vn.edufit.verification.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

  boolean existsByTutorIdAndStatus(UUID tutorId, VerificationRequest.Status status);

  Optional<VerificationRequest> findFirstByTutorIdOrderBySubmittedAtDesc(UUID tutorId);

  @EntityGraph(attributePaths = {"credentials", "credentials.files"})
  Optional<VerificationRequest> findWithCredentialsByRequestId(UUID requestId);

  Page<VerificationRequest> findByStatusOrderBySubmittedAtAsc(
      VerificationRequest.Status status,
      Pageable pageable
  );
}
