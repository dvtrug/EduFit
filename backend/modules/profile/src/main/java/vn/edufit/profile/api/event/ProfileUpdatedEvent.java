package vn.edufit.profile.api.event;

import java.time.Instant;
import java.util.UUID;
import vn.edufit.shared.domain.BaseDomainEvent;

/**
 * Sự kiện miền được phát ra khi hồ sơ gia sư hoặc học sinh được cập nhật.
 */
public record ProfileUpdatedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID userId,
    String profileType,
    UUID profileId
) implements BaseDomainEvent {

  public static ProfileUpdatedEvent of(UUID userId, String profileType, UUID profileId) {
    return new ProfileUpdatedEvent(
        UUID.randomUUID(),
        Instant.now(),
        userId,
        profileType,
        profileId
    );
  }

  @Override
  public UUID getEventId() {
    return eventId;
  }

  @Override
  public Instant getOccurredAt() {
    return occurredAt;
  }
}
