package vn.edufit.connection.infra.persistence.repository;

import vn.edufit.connection.infra.persistence.entity.*;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringClassRepository extends JpaRepository<TutoringClass, UUID> {
  boolean existsByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
  Optional<TutoringClass> findByConnectionRequestId(UUID requestId);
  Optional<TutoringClass> findByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
}
