package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.application.service.ConnectionQueryService;
import vn.edufit.connection.infra.persistence.entity.*;
import vn.edufit.connection.infra.persistence.repository.*;
import vn.edufit.iam.api.*;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.*;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.*;

@ExtendWith(MockitoExtension.class)
class ConnectionQueryServiceTest {
  @Mock LinkInvitationRepository invitations;
  @Mock ParentStudentLinkRepository links;
  @Mock ConnectionRequestRepository requests;
  @Mock ProfileFacade profile;
  @Mock IamFacade iam;
  @Mock ConnectionFacade facade;
  ConnectionQueryService service;

  @BeforeEach void setup() {
    service = new ConnectionQueryService(invitations, links, requests, profile, iam, facade, Clock.systemUTC());
  }

  private CurrentUser actor(UUID id, String role) {
    CurrentUser actor = mock(CurrentUser.class);
    lenient().when(actor.getUserId()).thenReturn(id);
    when(actor.hasRole(anyString())).thenAnswer(i -> role.equals(i.getArgument(0)));
    return actor;
  }

  @ParameterizedTest @ValueSource(longs = {-1, 0, 1})
  void invitationExpiryUsesInjectedClockAtDeadline(long offset) {
    Instant deadline = Instant.parse("2026-10-25T00:00:00Z");
    UUID user = UUID.randomUUID();
    var invitation = new LinkInvitation(user, UUID.randomUUID(), deadline.minusSeconds(14 * 86400));
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(deadline.plusNanos(offset));
    service = new ConnectionQueryService(invitations, links, requests, profile, iam, facade, clock);
    when(invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(user, user))
        .thenReturn(List.of(invitation));
    service.invitations(actor(user, "PARENT"));
    assertEquals(offset < 0 ? "PENDING" : "EXPIRED", invitation.getStatus());
    verify(clock, times(1)).instant();
  }

  @Test void studentListIgnoresForeignScopeAndReturnsOnlyOwnedStudent() {
    UUID user = UUID.randomUUID(), own = UUID.randomUUID(), foreign = UUID.randomUUID();
    when(profile.getStudentIdByUserId(user)).thenReturn(own);
    when(profile.findStudentById(own)).thenReturn(Optional.of(new StudentSummaryDto(own, user, 4, null, true)));
    assertTrue(service.requests(actor(user, "STUDENT"), foreign).isEmpty());
    verify(requests).findByStudentIdOrderByCreatedAtDesc(own);
    verify(requests, never()).findByStudentIdOrderByCreatedAtDesc(foreign);
  }

  @Test void revokedParentCannotReadRequests() {
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID();
    when(facade.hasConfirmedParentLink(parent, student)).thenReturn(false);
    assertThrows(ForbiddenOperationException.class, () -> service.requests(actor(parent, "PARENT"), student));
    verifyNoInteractions(requests);
  }

  @Test void confirmedParentReadsOnlyRequestedStudent() {
    UUID parent = UUID.randomUUID(), student = UUID.randomUUID();
    when(facade.hasConfirmedParentLink(parent, student)).thenReturn(true);
    assertTrue(service.requests(actor(parent, "PARENT"), student).isEmpty());
    verify(requests).findByStudentIdOrderByCreatedAtDesc(student);
  }

  @Test void parentMustSupplyStudentScope() {
    assertThrows(InvalidOperationException.class, () -> service.requests(actor(UUID.randomUUID(), "PARENT"), null));
    verifyNoInteractions(requests, facade);
  }

  @ParameterizedTest @ValueSource(strings = {"ADMIN", "TUTOR"})
  void learnerQueryRejectsOtherRoles(String role) {
    assertThrows(ForbiddenOperationException.class, () -> service.requests(actor(UUID.randomUUID(), role), UUID.randomUUID()));
    verifyNoInteractions(requests, profile, facade);
  }

  @Test void tutorInboxRequiresTutorRole() {
    assertThrows(ForbiddenOperationException.class, () -> service.tutorInbox(actor(UUID.randomUUID(), "PARENT")));
    verifyNoInteractions(requests, profile, iam);
  }

  @ParameterizedTest @ValueSource(strings = {"PARENT", "STUDENT", "MISSING"})
  void tutorInboxScopesExpiresAndMapsLegacyMetadata(String sender) {
    UUID user = UUID.randomUUID(), tutor = UUID.randomUUID(), student = UUID.randomUUID(), goal = UUID.randomUUID(), sentBy = UUID.randomUUID();
    var request = new ConnectionRequest(student, tutor, goal, sentBy, List.of("Tue 19:00"), "hello", Instant.now().minusSeconds(15 * 86400));
    when(profile.getTutorIdByUserId(user)).thenReturn(tutor);
    when(requests.findByTutorIdOrderByCreatedAtDesc(tutor)).thenReturn(List.of(request));
    when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(new ConnectionGoalDto(goal, student, UUID.randomUUID(), 1, 4, "EXAM", null, "ACTIVE")));
    when(iam.findUserSummaryById(sentBy)).thenReturn(sender.equals("MISSING") ? Optional.empty() : Optional.of(new UserSummaryView(sentBy, "hidden@test.invalid", "Hidden name", AccountRole.valueOf(sender), AccountStatus.ACTIVE)));
    var result = service.tutorInbox(actor(user, "TUTOR"));
    assertEquals(1, result.size());
    var item = result.getFirst();
    assertEquals("EXPIRED", item.status());
    assertEquals("EXPIRED", request.getStatus());
    assertEquals("EXAM", item.goalType());
    assertEquals(1, item.subjectId());
    assertEquals(4, item.educationLevelId());
    assertEquals(List.of("Tue 19:00"), item.desiredSlots());
    assertEquals("hello", item.message());
    assertEquals(sender.equals("PARENT") ? "PARENT" : "STUDENT", item.senderRole());
    verify(requests).findByTutorIdOrderByCreatedAtDesc(tutor);
    verify(requests, never()).findByStudentIdOrderByCreatedAtDesc(any());
  }

  @Test void invitationsUseOnlyActorAsBothParticipants() {
    UUID user = UUID.randomUUID();
    var invitation = new LinkInvitation(user, UUID.randomUUID(), Instant.now().minusSeconds(15 * 86400));
    when(invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(user, user)).thenReturn(List.of(invitation));
    assertEquals("EXPIRED", service.invitations(actor(user, "PARENT")).getFirst().status());
    verify(invitations).findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(user, user);
  }

  @ParameterizedTest @ValueSource(strings = {"PARENT", "STUDENT"})
  void linksAlwaysUseConfirmedOwnedScope(String role) {
    UUID user = UUID.randomUUID(), student = UUID.randomUUID();
    if (role.equals("STUDENT")) when(profile.getStudentIdByUserId(user)).thenReturn(student);
    assertTrue(service.links(actor(user, role)).isEmpty());
    if (role.equals("PARENT")) verify(links).findByParentUserIdAndStatus(user, "CONFIRMED");
    else verify(links).findByStudentIdAndStatus(student, "CONFIRMED");
  }
}
