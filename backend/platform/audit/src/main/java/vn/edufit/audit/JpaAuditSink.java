package vn.edufit.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import vn.edufit.shared.audit.AuditSink;

interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {}

@Service
class JpaAuditSink implements AuditSink {
  private final AuditLogRepository repository;
  JpaAuditSink(AuditLogRepository repository) { this.repository = repository; }

  @Override
  public void record(UUID actorUserId, String action, String targetType, UUID targetId, String reason) {
    repository.save(new AuditLog(actorUserId, action, targetType, targetId, reason));
  }
}
