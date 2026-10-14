package vn.edufit.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;
import vn.edufit.connection.api.event.ParentStudentLinkChangedEvent;

/** Mandatory notification transitions are consumed before business commit.
 * Class-created events never produce an additional accepted notification.
 */
@Component
class ConnectionTransitionListener {
  private final ConnectionNotificationWriter writer;
  ConnectionTransitionListener(ConnectionNotificationWriter writer) { this.writer = writer; }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = false)
  public void onInvitationChanged(LinkInvitationChangedEvent event) {
    switch (event.newStatus()) {
      case "PENDING" -> { if (event.oldStatus() == null) writer.invitationCreated(event); }
      case "ACCEPTED" -> writer.invitationAccepted(event);
      case "DECLINED" -> writer.invitationDeclined(event);
      default -> { } // Withdraw/system closure unchanged; expiry dispatch follows its publisher task.
    }
  }
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = false)
  public void onLinkChanged(ParentStudentLinkChangedEvent event) {
    if ("REVOKED".equals(event.newStatus())) writer.linkRevoked(event);
  }
}
