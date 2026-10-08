package vn.edufit.connection.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ConnectionRequest r where r.id = :id")
  Optional<ConnectionRequest> lockById(UUID id);

  List<ConnectionRequest> findByStudentIdAndStatus(UUID studentId, String status);
  List<ConnectionRequest> findByTutorIdAndStatus(UUID tutorId, String status);
  boolean existsByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
  List<ConnectionRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
  List<ConnectionRequest> findByTutorIdOrderByCreatedAtDesc(UUID tutorId);
}
