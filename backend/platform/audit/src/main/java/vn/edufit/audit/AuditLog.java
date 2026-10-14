package vn.edufit.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edufit.shared.audit.AuditRecord;

@Entity
@Table(name = "audit_log")
class AuditLog {
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "audit_id") private UUID id;
  @Column(name = "actor_user_id", nullable = false) private UUID actorUserId;
  @Column(name = "action", nullable = false, length = 60) private String action;
  @Column(name = "target_type", nullable = false, length = 40) private String targetType;
  @Column(name = "target_id", nullable = false) private UUID targetId;
  @Column(name = "reason") private String reason;
  @Column(name = "created_at", nullable = false) private Instant createdAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> metadata;

  protected AuditLog() {}
  AuditLog(UUID actor, String action, String targetType, UUID targetId, String reason) {
    this(new AuditRecord(actor, action, targetType, targetId, reason, Instant.now(), Map.of()));
  }
  AuditLog(AuditRecord record) {
    actorUserId = record.actorUserId();
    action = record.action();
    targetType = record.targetType();
    targetId = record.targetId();
    reason = record.reason();
    createdAt = record.occurredAt();
    metadata = record.metadata();
  }
}
