package vn.edufit.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;

/** Foundation for incremental publisher migration. No Connection publisher emits these yet.
 * Expand recipient/transition dispatch with its publisher task; retain existing sinks until then.
 * Class-created events intentionally do not produce an accepted notification.
 */
@Component
class ConnectionTransitionListener {
  private final ConnectionNotificationWriter writer;
  ConnectionTransitionListener(ConnectionNotificationWriter writer) { this.writer = writer; }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = false)
  public void onInvitationChanged(LinkInvitationChangedEvent event) {
    if (event.oldStatus() == null && "PENDING".equals(event.newStatus())) writer.invitationCreated(event);
  }
}
