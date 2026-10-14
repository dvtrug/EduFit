package vn.edufit.connection.infra.persistence.repository;

import vn.edufit.connection.infra.persistence.entity.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkInvitationRepository extends JpaRepository<LinkInvitation, UUID>, FreshConnectionRowRepository {
  default Optional<LinkInvitation> lockById(UUID id) {
    return lockFresh(LinkInvitation.class, id);
  }

  @Query("select i from LinkInvitation i where (i.inviterUserId = :a and i.invitedUserId = :b) or (i.inviterUserId = :b and i.invitedUserId = :a) order by i.createdAt desc")
  List<LinkInvitation> findPair(UUID a, UUID b);

  List<LinkInvitation> findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(UUID inviter, UUID invited);
}
