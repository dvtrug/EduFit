package vn.edufit.connection.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, UUID> {
  boolean existsByParentUserIdAndStudentIdAndStatus(UUID parentUserId, UUID studentId, String status);
  List<ParentStudentLink> findByParentUserIdAndStatus(UUID parentUserId, String status);
  List<ParentStudentLink> findByStudentIdAndStatus(UUID studentId, String status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from ParentStudentLink l where l.id = :id")
  Optional<ParentStudentLink> lockById(UUID id);
}
