package vn.edufit.ai.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Atomic BR-25 reservation: at most ten MATCH_EXPLANATION attempts per actor in (now-24h, now].
 * Empty means exhausted; store errors throw AiUnavailableException (never permit on error). A
 * committed reservation consumes an attempt even on provider failure, crash or lease expiry. The
 * gateway owns admission; consumers must not combine this with a usage-count precheck.
 */
public interface AiMatchQuota {
  Optional<UUID> tryReserve(UUID actorUserId);
}
