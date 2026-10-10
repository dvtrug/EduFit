package vn.edufit.discovery.infra.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.discovery.infra.persistence.entity.MatchingRunLogEntity;

@Repository
public interface MatchingRunLogRepository extends JpaRepository<MatchingRunLogEntity, UUID> {}
