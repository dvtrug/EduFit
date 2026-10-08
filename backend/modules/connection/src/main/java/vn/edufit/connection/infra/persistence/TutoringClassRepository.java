package vn.edufit.connection.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringClassRepository extends JpaRepository<TutoringClass, UUID> {
  boolean existsByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
  Optional<TutoringClass> findByConnectionRequestId(UUID requestId);
  Optional<TutoringClass> findByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
}
