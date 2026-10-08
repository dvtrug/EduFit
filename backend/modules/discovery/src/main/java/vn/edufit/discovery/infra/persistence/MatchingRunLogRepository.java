package vn.edufit.discovery.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchingRunLogRepository extends JpaRepository<MatchingRunLog, UUID> {}
