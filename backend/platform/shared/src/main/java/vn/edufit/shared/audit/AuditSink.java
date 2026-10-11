package vn.edufit.shared.audit;

import java.util.UUID;

public interface AuditSink {
  void record(UUID actorUserId, String action, String targetType, UUID targetId, String reason);
}
