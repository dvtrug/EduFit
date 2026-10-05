package vn.edufit.verification.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edufit.verification.infra.persistence.entity.VerificationRequest;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

  boolean existsByTutorIdAndStatus(UUID tutorId, VerificationRequest.Status status);

  Optional<VerificationRequest> findFirstByTutorIdOrderBySubmittedAtDesc(UUID tutorId);

  Page<VerificationRequest> findByStatusOrderBySubmittedAtAsc(
      VerificationRequest.Status status,
      Pageable pageable
  );

  @Query("SELECT r FROM VerificationRequest r LEFT JOIN FETCH r.credentials c LEFT JOIN FETCH c.files WHERE r.requestId = :requestId")
  Optional<VerificationRequest> findWithCredentialsByRequestId(@Param("requestId") UUID requestId);
}

