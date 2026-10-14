package vn.edufit.connection.infra.persistence.repository;

import vn.edufit.connection.infra.persistence.entity.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID>, FreshConnectionRowRepository {
  default Optional<ConnectionRequest> lockById(UUID id) {
    return lockFresh(ConnectionRequest.class, id);
  }

  List<ConnectionRequest> findByStudentIdAndStatus(UUID studentId, String status);
  List<ConnectionRequest> findByTutorIdAndStatus(UUID tutorId, String status);
  boolean existsByStudentIdAndTutorIdAndStatus(UUID studentId, UUID tutorId, String status);
  List<ConnectionRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
  List<ConnectionRequest> findByTutorIdOrderByCreatedAtDesc(UUID tutorId);
}
