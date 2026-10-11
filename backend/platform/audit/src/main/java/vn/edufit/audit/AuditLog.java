package vn.edufit.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

  protected AuditLog() {}
  AuditLog(UUID actor, String action, String targetType, UUID targetId, String reason) {
    actorUserId = actor;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    createdAt = Instant.now();
  }
}
