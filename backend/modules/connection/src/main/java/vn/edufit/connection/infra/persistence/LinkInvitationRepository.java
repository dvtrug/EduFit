package vn.edufit.connection.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface LinkInvitationRepository extends JpaRepository<LinkInvitation, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from LinkInvitation i where i.id = :id")
  Optional<LinkInvitation> lockById(UUID id);

  @Query("select i from LinkInvitation i where (i.inviterUserId = :a and i.invitedUserId = :b) or (i.inviterUserId = :b and i.invitedUserId = :a) order by i.createdAt desc")
  List<LinkInvitation> findPair(UUID a, UUID b);

  List<LinkInvitation> findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(UUID inviter, UUID invited);
}
