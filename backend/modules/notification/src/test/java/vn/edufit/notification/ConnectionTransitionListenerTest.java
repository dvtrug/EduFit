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
  @Test
  void unmigratedTransitionsCannotDuplicateExistingDirectNotifications() {
    var writer = mock(ConnectionNotificationWriter.class);
    new ConnectionTransitionListener(writer).onInvitationChanged(event("PENDING", "WITHDRAWN"));
    verifyNoInteractions(writer);
  }
  @ParameterizedTest @ValueSource(strings = {"ACCEPTED", "DECLINED", "EXPIRED"})
  void completedInvitationDispatchesExactlyOneWriter(String status) {
    var writer = mock(ConnectionNotificationWriter.class);
    var ev = event("PENDING", status);
    new ConnectionTransitionListener(writer).onInvitationChanged(ev);
    switch (status) {
      case "ACCEPTED" -> verify(writer).invitationAccepted(ev);
      case "DECLINED" -> verify(writer).invitationDeclined(ev);
      case "EXPIRED" -> verify(writer).invitationExpired(ev);
    }
    verifyNoMoreInteractions(writer);
  }
  @ParameterizedTest @ValueSource(strings = {"ACCEPTED", "REJECTED", "EXPIRED"})
  void completedRequestDispatchesExactlyOneWriter(String status) {
    var writer = mock(ConnectionNotificationWriter.class);
    var event = new vn.edufit.connection.api.event.ConnectionRequestChangedEvent(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "PENDING", status, Instant.parse("2026-10-11T00:00:00Z"), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), status.equals("ACCEPTED") ? UUID.randomUUID() : null);
    new ConnectionTransitionListener(writer).onRequestChanged(event);
    switch (status) {
      case "ACCEPTED" -> verify(writer).requestAccepted(event);
      case "REJECTED" -> verify(writer).requestRejected(event);
      case "EXPIRED" -> verify(writer).requestExpired(event);
    }
    verifyNoMoreInteractions(writer);
  }
  @Test void pendingToPendingIsNotAnEvent() {
    assertThrows(IllegalArgumentException.class, () -> event("PENDING", "PENDING"));
  }

  @Test void invitationExpiredNotifiesOnlyInviter() throws Exception {
    var repository = mock(NotificationRepository.class);
    var writer = new ConnectionNotificationWriter(repository, mock(vn.edufit.connection.api.ConnectionFacade.class));
    var ev = event("PENDING", "EXPIRED");
    writer.invitationExpired(ev);
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(repository).save(captor.capture());
    Notification notification = captor.getValue();
    assertEquals(ev.inviterUserId(), field(notification, "userId"));
    assertEquals(ev.studentId(), field(notification, "studentId"));
    assertEquals("LINK_EXPIRED", field(notification, "type"));
    assertEquals("LINK_INVITATION", field(notification, "targetType"));
    assertEquals(ev.aggregateId(), field(notification, "targetId"));
  }

  @Test void requestExpiredNotifiesTutorAndStudentWhenStudentSent() throws Exception {
    var repository = mock(NotificationRepository.class);
    var writer = new ConnectionNotificationWriter(repository, mock(vn.edufit.connection.api.ConnectionFacade.class));
    UUID reqId = UUID.randomUUID(), student = UUID.randomUUID(), studentUser = UUID.randomUUID(), tutorUser = UUID.randomUUID();
    var ev = new vn.edufit.connection.api.event.ConnectionRequestChangedEvent(
        UUID.randomUUID(), reqId, null, student, "PENDING", "EXPIRED",
        Instant.parse("2026-10-14T10:00:00Z"), studentUser, studentUser, tutorUser, null);
    writer.requestExpired(ev);
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(repository, times(2)).save(captor.capture());
    var list = captor.getAllValues();
    assertEquals(tutorUser, field(list.get(0), "userId"));
    assertEquals("CONNECTION_EXPIRED", field(list.get(0), "type"));
    assertEquals(studentUser, field(list.get(1), "userId"));
    assertEquals("CONNECTION_EXPIRED", field(list.get(1), "type"));
  }

  @Test void requestExpiredNotifiesTutorAndParentWhenParentConfirmed() throws Exception {
    var repository = mock(NotificationRepository.class);
    var facade = mock(vn.edufit.connection.api.ConnectionFacade.class);
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID(), studentUser = UUID.randomUUID(), tutorUser = UUID.randomUUID();
    when(facade.hasConfirmedParentLink(parent, student)).thenReturn(true);
    var writer = new ConnectionNotificationWriter(repository, facade);
    var ev = new vn.edufit.connection.api.event.ConnectionRequestChangedEvent(
        UUID.randomUUID(), UUID.randomUUID(), null, student, "PENDING", "EXPIRED",
        Instant.parse("2026-10-14T10:00:00Z"), parent, studentUser, tutorUser, null);
    writer.requestExpired(ev);
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(repository, times(2)).save(captor.capture());
    var list = captor.getAllValues();
    assertEquals(tutorUser, field(list.get(0), "userId"));
    assertEquals(parent, field(list.get(1), "userId"));
  }

  @Test void requestExpiredNotifiesTutorAndStudentWhenParentRevoked() throws Exception {
    var repository = mock(NotificationRepository.class);
    var facade = mock(vn.edufit.connection.api.ConnectionFacade.class);
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID(), studentUser = UUID.randomUUID(), tutorUser = UUID.randomUUID();
    when(facade.hasConfirmedParentLink(parent, student)).thenReturn(false);
    var writer = new ConnectionNotificationWriter(repository, facade);
    var ev = new vn.edufit.connection.api.event.ConnectionRequestChangedEvent(
        UUID.randomUUID(), UUID.randomUUID(), null, student, "PENDING", "EXPIRED",
        Instant.parse("2026-10-14T10:00:00Z"), parent, studentUser, tutorUser, null);
    writer.requestExpired(ev);
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(repository, times(2)).save(captor.capture());
    var list = captor.getAllValues();
    assertEquals(tutorUser, field(list.get(0), "userId"));
    assertEquals(studentUser, field(list.get(1), "userId"));
  }

  private Object field(Notification notification, String name) throws Exception {
    var field = Notification.class.getDeclaredField(name); field.setAccessible(true); return field.get(notification);
  }
}
