package vn.edufit.notification.application.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.notification.application.service.NotificationService;
import vn.edufit.shared.domain.BaseDomainEvent;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @Mock
  private NotificationService notificationService;

  private NotificationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new NotificationEventListener(notificationService);
  }

  @Test
  @DisplayName("handleDomainEvent: Tiếp nhận BaseDomainEvent an toàn không ném ngoại lệ")
  void handleDomainEvent_processesEventSafely() {
    BaseDomainEvent dummyEvent = new BaseDomainEvent() {
      private final UUID eventId = UUID.randomUUID();
      private final Instant occurredAt = Instant.now();

      @Override
      public UUID getEventId() {
        return eventId;
      }

      @Override
      public Instant getOccurredAt() {
        return occurredAt;
      }
    };

    assertDoesNotThrow(() -> listener.handleDomainEvent(dummyEvent));
  }
}
