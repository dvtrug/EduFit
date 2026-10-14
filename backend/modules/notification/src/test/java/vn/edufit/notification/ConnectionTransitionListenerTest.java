package vn.edufit.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import vn.edufit.connection.api.event.LinkInvitationChangedEvent;

class ConnectionTransitionListenerTest {
  private LinkInvitationChangedEvent event(String oldStatus, String status) {
    return new LinkInvitationChangedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        oldStatus, status, Instant.parse("2026-10-11T00:00:00Z"), UUID.randomUUID(), UUID.randomUUID(), null,
        LinkInvitationChangedEvent.Cause.USER_ACTION);
  }
  @Test void createdNotificationRetainsRecipientStudentTargetAndBusinessTimestamp() throws Exception {
    var repository = mock(NotificationRepository.class);
    var event = event(null, "PENDING");
    new ConnectionTransitionListener(new ConnectionNotificationWriter(repository, mock(vn.edufit.connection.api.ConnectionFacade.class))).onInvitationChanged(event);
    var capture = ArgumentCaptor.forClass(Notification.class); verify(repository).save(capture.capture());
    Notification notification = capture.getValue();
    assertEquals(event.invitedUserId(), field(notification, "userId"));
    assertEquals(event.studentId(), field(notification, "studentId"));
    assertEquals(event.aggregateId(), field(notification, "targetId"));
    assertEquals(event.occurredAt(), field(notification, "createdAt"));
    assertEquals("LINK_INVITED", field(notification, "type"));
    assertEquals("LINK_INVITATION", field(notification, "targetType"));
  }
  @ParameterizedTest @ValueSource(strings = {"WITHDRAWN", "EXPIRED"})
  void unmigratedTransitionsCannotDuplicateExistingDirectNotifications(String status) {
    var writer = mock(ConnectionNotificationWriter.class);
    new ConnectionTransitionListener(writer).onInvitationChanged(event("PENDING", status));
    verifyNoInteractions(writer);
  }
  @ParameterizedTest @ValueSource(strings = {"ACCEPTED", "DECLINED"})
  void completedInvitationDispatchesExactlyOneWriter(String status) {
    var writer = mock(ConnectionNotificationWriter.class);
    new ConnectionTransitionListener(writer).onInvitationChanged(event("PENDING", status));
    if (status.equals("ACCEPTED")) verify(writer).invitationAccepted(any());
    else verify(writer).invitationDeclined(any());
    verifyNoMoreInteractions(writer);
  }
  @ParameterizedTest @ValueSource(strings = {"ACCEPTED", "REJECTED"})
  void completedRequestDispatchesExactlyOneWriter(String status) {
    var writer = mock(ConnectionNotificationWriter.class);
    var event = new vn.edufit.connection.api.event.ConnectionRequestChangedEvent(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "PENDING", status, Instant.parse("2026-10-11T00:00:00Z"), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), status.equals("ACCEPTED") ? UUID.randomUUID() : null);
    new ConnectionTransitionListener(writer).onRequestChanged(event);
    if (status.equals("ACCEPTED")) verify(writer).requestAccepted(event);
    else verify(writer).requestRejected(event);
    verifyNoMoreInteractions(writer);
  }
  @Test void pendingToPendingIsNotAnEvent() {
    assertThrows(IllegalArgumentException.class, () -> event("PENDING", "PENDING"));
  }
  private Object field(Notification notification, String name) throws Exception {
    var field = Notification.class.getDeclaredField(name); field.setAccessible(true); return field.get(notification);
  }
}
