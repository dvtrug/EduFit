package vn.edufit.ai.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only statistics from completed audit logs, including success/error/timeout.
 * This is NOT atomic quota admission and excludes in-flight reservations or failed audit writes.
 * MATCH_EXPLANATION admission is enforced by AiGateway using durable AiMatchQuota reservations.
 * TUTOR_BIO callers may still query audit counts for their separate editing-session policy.
 */
public interface AiUsageQuery {
  long countRequestsSince(UUID userId, AiFeature feature, Instant since);
}
