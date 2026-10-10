package vn.edufit.discovery.api.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Describes a completed matching run, including runs with zero results.
 * Consumers with external side effects must handle this event after transaction commit.
 */
public record MatchingExecutedEvent(
    UUID userId,
    UUID studentId,
    UUID goalId,
    int resultCount,
    Instant executedAt
) {

  public MatchingExecutedEvent(UUID userId, UUID studentId, UUID goalId, int resultCount) {
    this(userId, studentId, goalId, resultCount, Instant.now());
  }
}
