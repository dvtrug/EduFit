package vn.edufit.connection.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionInviteQuotaRepository extends JpaRepository<ConnectionInviteQuota, UUID> {}
