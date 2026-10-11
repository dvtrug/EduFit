package vn.edufit.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;

/** Internal mandatory writer; never starts an independent notification transaction. */
@Service
class ConnectionNotificationWriter {
  private final NotificationRepository repository;
  ConnectionNotificationWriter(NotificationRepository repository) { this.repository = repository; }

  @Transactional(propagation = Propagation.MANDATORY)
  public void invitationCreated(LinkInvitationChangedEvent event) {
    repository.save(new Notification(event.invitedUserId(), event.studentId(), "LINK_INVITED",
        "Lời mời liên kết", "Bạn có lời mời liên kết mới", "LINK_INVITATION", event.aggregateId(), event.occurredAt()));
  }
}
