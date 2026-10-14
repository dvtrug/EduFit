package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.connection.application.service.ConnectionRequestService;
import vn.edufit.connection.application.service.LinkInvitationService;
import vn.edufit.connection.application.service.ParentStudentLinkService;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.application.service.InvitationQuotaService;
import vn.edufit.connection.application.service.ConnectionFacadeImpl;
import vn.edufit.connection.infra.persistence.entity.*;
import vn.edufit.connection.infra.persistence.repository.*;
import vn.edufit.iam.api.*;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.*;
import vn.edufit.shared.audit.AuditSink;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.*;
import vn.edufit.shared.notification.NotificationSink;

@ExtendWith(MockitoExtension.class)
class ConnectionUseCaseTest {
  @Mock LinkInvitationRepository invitations;
  @Mock ParentStudentLinkRepository links;
  @Mock ConnectionRequestRepository requests;
  @Mock TutoringClassRepository classes;
  @Mock IamFacade iam;
  @Mock ProfileFacade profile;
  @Mock ConnectionLock lock;
  @Mock AuditSink audit;
  @Mock NotificationSink notifications;
  @Mock org.springframework.context.ApplicationEventPublisher events;
  @Mock ConnectionInviteQuotaRepository quotas;
  ConnectionRequestService service;
  LinkInvitationService invitationService;
  ParentStudentLinkService linkService;

  @BeforeEach void setup() {
    service = new ConnectionRequestService(requests, classes, profile, lock, notifications, new ConnectionFacadeImpl(links, classes), Clock.systemUTC());
    invitationService = new LinkInvitationService(invitations, links, iam, profile, lock, new InvitationQuotaService(quotas, lock, Clock.systemUTC()), events, Clock.systemUTC());
    linkService = new ParentStudentLinkService(links, profile, lock, audit, notifications, iam, Clock.systemUTC());
  }

  private CurrentUser actor(UUID id, String role) {
    CurrentUser user = mock(CurrentUser.class);
    lenient().when(user.getUserId()).thenReturn(id);
    when(user.hasRole(anyString())).thenAnswer(i -> role.equals(i.getArgument(0)));
    lenient().when(iam.findUserSummaryById(id)).thenReturn(Optional.of(new UserSummaryView(id,
        "actor@test.invalid", "Fixture", AccountRole.valueOf(role), AccountStatus.ACTIVE)));
    return user;
  }

  @Test void unknownEmailDoesNotRevealAccountAndDoesNotWrite() {
    CurrentUser parent = actor(UUID.randomUUID(), "PARENT");
    when(iam.findUserSummaryByEmail("missing@example.com")).thenReturn(Optional.empty());
    assertDoesNotThrow(() -> invitationService.invite(parent, "missing@example.com"));
    verify(invitations, never()).save(any());
  }

  @Test void invitationCanOnlyBeAcceptedByRecipient() {
    UUID id = UUID.randomUUID(), student = UUID.randomUUID();
    LinkInvitation invitation = new LinkInvitation(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    when(invitations.findById(id)).thenReturn(Optional.of(invitation));
    assertThrows(ForbiddenOperationException.class,
        () -> invitationService.respondInvitation(actor(student, "STUDENT"), id, true));
    verify(links, never()).save(any());
  }

  @Test void expiredInvitationCannotCreateLink() {
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID(), id = UUID.randomUUID();
    LinkInvitation invitation = new LinkInvitation(parent, student, Instant.now().minusSeconds(15 * 86400));
    when(invitations.findById(id)).thenReturn(Optional.of(invitation));
    when(invitations.lockById(id)).thenReturn(Optional.of(invitation));
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
        invitationService.respondInvitation(actor(student, "STUDENT"), id, true));
    assertEquals("EXPIRED", invitation.getStatus());
    verify(links, never()).save(any());
  }

  @Test void revokedLinkImmediatelyLosesParentAccess() {
    UUID student = UUID.randomUUID(), parent = UUID.randomUUID(), id = UUID.randomUUID();
    ParentStudentLink link = new ParentStudentLink(parent, student, Instant.now());
    when(links.findById(id)).thenReturn(Optional.of(link));
    when(links.lockById(id)).thenReturn(Optional.of(link));
    CurrentUser admin = actor(UUID.randomUUID(), "ADMIN");
    assertThrows(InvalidOperationException.class, () -> linkService.revokeLink(admin, id, " "));
    when(profile.findStudentById(student)).thenReturn(Optional.of(new StudentSummaryDto(student, UUID.randomUUID(), 1, null, true)));
    linkService.revokeLink(admin, id, "violation");
    assertEquals("REVOKED", link.getStatus());
    verify(audit).record(argThat((vn.edufit.shared.audit.AuditRecord record) -> record.actorUserId().equals(admin.getUserId()) && record.targetId().equals(id) && "violation".equals(record.reason())));
    when(links.existsByParentUserIdAndStudentIdAndStatus(parent, student, "CONFIRMED")).thenReturn(false);
    assertFalse(new ConnectionFacadeImpl(links, classes).hasConfirmedParentLink(parent, student));
  }

  @Test void unlinkedParentCannotSendTutorRequest() {
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID(), goal = UUID.randomUUID();
    when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(new ConnectionGoalDto(
        goal, student, UUID.randomUUID(), 1, 1, "EXAM", null, "ACTIVE")));
    assertThrows(ForbiddenOperationException.class, () -> service.request(actor(parent, "PARENT"),
        goal, UUID.randomUUID(), List.of("Mon 18:00"), null));
    verify(requests, never()).save(any());
  }

  @Test void tutorCannotAcceptOtherTutorsRequest() {
    UUID tutorUser = UUID.randomUUID(), id = UUID.randomUUID();
    ConnectionRequest request = new ConnectionRequest(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), List.of("Tue 19:00"), null, Instant.now());
    when(requests.findById(id)).thenReturn(Optional.of(request));
    when(profile.getTutorIdByUserId(tutorUser)).thenReturn(UUID.randomUUID());
    assertThrows(ForbiddenOperationException.class,
        () -> service.respondRequest(actor(tutorUser, "TUTOR"), id, true, null));
    verify(classes, never()).save(any());
  }

  @Test void inviteQuotaCountsEvenUnknownEmails() {
    UUID id = UUID.randomUUID();
    ConnectionInviteQuota quota = new ConnectionInviteQuota(id, Instant.now());
    for (int i = 0; i < 10; i++) assertTrue(quota.consume(Instant.now()));
    when(quotas.findById(id)).thenReturn(Optional.of(quota));
    assertFalse(invitationService.invite(actor(id, "PARENT"), "nobody@example.com"));
    verify(iam, never()).findUserSummaryByEmail(anyString());
  }

  @Test void tutorAcceptCreatesOneActiveClass() {
    UUID tutorUser = UUID.randomUUID(), tutorId = UUID.randomUUID(), studentId = UUID.randomUUID();
    UUID goalId = UUID.randomUUID(), id = UUID.randomUUID();
    ConnectionRequest request = new ConnectionRequest(studentId, tutorId, goalId,
        UUID.randomUUID(), List.of("Tue 19:00"), null, Instant.now());
    when(requests.findById(id)).thenReturn(Optional.of(request));
    when(requests.lockById(id)).thenReturn(Optional.of(request));
    when(profile.getTutorIdByUserId(tutorUser)).thenReturn(tutorId);
    when(profile.findTutorById(tutorId)).thenReturn(Optional.of(new TutorSummaryDto(tutorId,
        tutorUser, "Tutor", null, null, null, null, null, null, null, "VERIFIED",
        Instant.now(), null, 0)));
    when(profile.findGoalForConnection(goalId)).thenReturn(Optional.of(new ConnectionGoalDto(
        goalId, studentId, UUID.randomUUID(), 1, 2, "EXAM", null, "ACTIVE")));
    when(profile.tutorTeaches(tutorId, 1, 2)).thenReturn(true);
    when(classes.save(any())).thenAnswer(i -> i.getArgument(0));
    assertNotNull(service.respondRequest(actor(tutorUser, "TUTOR"), id, true, null));
    assertEquals("ACCEPTED", request.getStatus());
    verify(classes, times(1)).save(any());
    verify(notifications).notify(eq(request.getSentByUserId()), eq(studentId),
        eq("CONNECTION_ACCEPTED"), anyString(), anyString(), eq("CLASS"), nullable(UUID.class));
  }

  @Test void acceptingInvitationLocksStudentBeforePairAndRow() {
    UUID parent = UUID.randomUUID(), studentUser = UUID.randomUUID(), student = UUID.randomUUID(), id = UUID.randomUUID();
    var invitation = new LinkInvitation(parent, studentUser, Instant.now());
    org.springframework.test.util.ReflectionTestUtils.setField(invitation, "id", id);
    when(invitations.findById(id)).thenReturn(Optional.of(invitation));
    when(invitations.lockById(id)).thenReturn(Optional.of(invitation));
    when(profile.findStudentByUserId(studentUser)).thenReturn(Optional.of(new StudentSummaryDto(student, studentUser, 1, null, true)));
    when(iam.findUserSummaryById(parent)).thenReturn(Optional.of(new UserSummaryView(parent, "parent@example.test", "Parent", AccountRole.PARENT, AccountStatus.ACTIVE)));
    when(links.save(any())).thenAnswer(i -> { var row = (ParentStudentLink) i.getArgument(0); org.springframework.test.util.ReflectionTestUtils.setField(row, "id", UUID.randomUUID()); return row; });
    assertNotNull(invitationService.respondInvitation(actor(studentUser, "STUDENT"), id, true));
    var order = inOrder(lock, invitations, links);
    order.verify(lock).lockStudent(student);
    order.verify(lock).lockInvitationPair(parent, studentUser);
    order.verify(invitations).lockById(id);
    order.verify(links).save(any());
  }
}
