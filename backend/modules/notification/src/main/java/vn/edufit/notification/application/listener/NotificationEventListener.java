package vn.edufit.notification.application.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.edufit.notification.application.service.NotificationService;
import vn.edufit.shared.domain.BaseDomainEvent;

/**
 * Lắng nghe các Sự kiện Miền (Domain Events) phát tán qua {@code ApplicationEventPublisher}.
 *
 * <p>Theo thiết kế SADS và ADR-002:
 * <ul>
 *   <li>Sự kiện được xử lý đồng bộ trong cùng Transaction (@Transactional) của hành động gốc.</li>
 *   <li>Notification module chỉ đóng vai trò Consumer thụ động đón nhận event từ các module khác.</li>
 * </ul>
 */
@Component
public class NotificationEventListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

  private final NotificationService notificationService;

  public NotificationEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener
  public void handleDomainEvent(BaseDomainEvent event) {
    log.debug(
        "NotificationEventListener received event: {} [eventId: {}, occurredAt: {}]",
        event.getClass().getSimpleName(),
        event.getEventId(),
        event.getOccurredAt()
    );
  }
}
