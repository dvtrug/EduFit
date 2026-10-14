package vn.edufit.connection.infra.persistence.repository;

import vn.edufit.connection.infra.persistence.entity.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, UUID>, FreshConnectionRowRepository {
  boolean existsByParentUserIdAndStudentIdAndStatus(UUID parentUserId, UUID studentId, String status);
  List<ParentStudentLink> findByParentUserIdAndStatus(UUID parentUserId, String status);
  List<ParentStudentLink> findByStudentIdAndStatus(UUID studentId, String status);

  default Optional<ParentStudentLink> lockById(UUID id) {
    return lockFresh(ParentStudentLink.class, id);
  }
}
