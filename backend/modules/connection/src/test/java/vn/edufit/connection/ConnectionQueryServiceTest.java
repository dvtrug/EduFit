package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;
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

  @ParameterizedTest @ValueSource(strings = {"PARENT", "STUDENT"})
  void tutorInboxScopesExpiresAndMapsBatchMetadata(String sender) {
    UUID user = UUID.randomUUID(), tutor = UUID.randomUUID(), student = UUID.randomUUID(), goal = UUID.randomUUID(), sentBy = UUID.randomUUID();
    var request = new ConnectionRequest(student, tutor, goal, sentBy, List.of("Tue 19:00"), "hello", Instant.now().minusSeconds(15 * 86400));
    when(profile.getTutorIdByUserId(user)).thenReturn(tutor);
    when(requests.findByTutorIdOrderByCreatedAtDesc(tutor)).thenReturn(List.of(request));
    when(profile.findGoalsForConnection(List.of(goal))).thenReturn(Map.of(
        goal, new ConnectionGoalDto(goal, student, UUID.randomUUID(), 1, 4, "EXAM", java.time.LocalDate.of(2027, 1, 1), "ACTIVE")));
    when(iam.findUserSummariesByIds(List.of(sentBy))).thenReturn(Map.of(
        sentBy, new UserSummaryView(sentBy, "hidden@test.invalid", "Hidden name", AccountRole.valueOf(sender), AccountStatus.ACTIVE)));

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
    assertEquals(sender, item.senderRole());
    assertEquals(java.time.LocalDate.of(2027, 1, 1), item.deadline());
    verify(requests).findByTutorIdOrderByCreatedAtDesc(tutor);
    verify(requests, never()).findByStudentIdOrderByCreatedAtDesc(any());
  }

  @Test void tutorInboxMissingGoalThrowsConflict409() {
    UUID user = UUID.randomUUID(), tutor = UUID.randomUUID(), student = UUID.randomUUID(), goal = UUID.randomUUID(), sentBy = UUID.randomUUID();
    var request = new ConnectionRequest(student, tutor, goal, sentBy, List.of("Tue 19:00"), "hello", Instant.now());
    when(profile.getTutorIdByUserId(user)).thenReturn(tutor);
    when(requests.findByTutorIdOrderByCreatedAtDesc(tutor)).thenReturn(List.of(request));
    when(profile.findGoalsForConnection(List.of(goal))).thenReturn(Map.of());
    when(iam.findUserSummariesByIds(List.of(sentBy))).thenReturn(Map.of(
        sentBy, new UserSummaryView(sentBy, "h@t.i", "Name", AccountRole.STUDENT, AccountStatus.ACTIVE)));

    var ex = assertThrows(ResourceConflictException.class, () -> service.tutorInbox(actor(user, "TUTOR")));
    assertEquals(ErrorCode.CONFLICT_DETECTED, ex.getErrorCode());
  }

  @Test void tutorInboxMissingSenderThrowsConflict409() {
    UUID user = UUID.randomUUID(), tutor = UUID.randomUUID(), student = UUID.randomUUID(), goal = UUID.randomUUID(), sentBy = UUID.randomUUID();
    var request = new ConnectionRequest(student, tutor, goal, sentBy, List.of("Tue 19:00"), "hello", Instant.now());
    when(profile.getTutorIdByUserId(user)).thenReturn(tutor);
    when(requests.findByTutorIdOrderByCreatedAtDesc(tutor)).thenReturn(List.of(request));
    when(profile.findGoalsForConnection(List.of(goal))).thenReturn(Map.of(
        goal, new ConnectionGoalDto(goal, student, UUID.randomUUID(), 1, 4, "EXAM", null, "ACTIVE")));
    when(iam.findUserSummariesByIds(List.of(sentBy))).thenReturn(Map.of());

    var ex = assertThrows(ResourceConflictException.class, () -> service.tutorInbox(actor(user, "TUTOR")));
    assertEquals(ErrorCode.CONFLICT_DETECTED, ex.getErrorCode());
  }

  @Test void tutorInboxBatchesDistinctIdsWithoutNPlusOne() {
    UUID user = UUID.randomUUID(), tutor = UUID.randomUUID(), student = UUID.randomUUID();
    UUID goal1 = UUID.randomUUID(), goal2 = UUID.randomUUID();
    UUID sender1 = UUID.randomUUID(), sender2 = UUID.randomUUID();
    var r1 = new ConnectionRequest(student, tutor, goal1, sender1, List.of("Tue 19:00"), null, Instant.now());
    var r2 = new ConnectionRequest(student, tutor, goal1, sender2, List.of("Wed 19:00"), null, Instant.now());
    var r3 = new ConnectionRequest(student, tutor, goal2, sender1, List.of("Thu 19:00"), null, Instant.now());

    when(profile.getTutorIdByUserId(user)).thenReturn(tutor);
    when(requests.findByTutorIdOrderByCreatedAtDesc(tutor)).thenReturn(List.of(r1, r2, r3));
    when(profile.findGoalsForConnection(anyList())).thenReturn(Map.of(
        goal1, new ConnectionGoalDto(goal1, student, UUID.randomUUID(), 1, 4, "EXAM", null, "ACTIVE"),
        goal2, new ConnectionGoalDto(goal2, student, UUID.randomUUID(), 2, 4, "COURSE", java.time.LocalDate.of(2027, 2, 1), "ACTIVE")));
    when(iam.findUserSummariesByIds(anyList())).thenReturn(Map.of(
        sender1, new UserSummaryView(sender1, "s1@test.invalid", "Sender One", AccountRole.STUDENT, AccountStatus.ACTIVE),
        sender2, new UserSummaryView(sender2, "s2@test.invalid", "Sender Two", AccountRole.PARENT, AccountStatus.ACTIVE)));

    var result = service.tutorInbox(actor(user, "TUTOR"));
    assertEquals(3, result.size());
    verify(profile, times(1)).findGoalsForConnection(argThat(list -> list.size() == 2 && list.contains(goal1) && list.contains(goal2)));
    verify(iam, times(1)).findUserSummariesByIds(argThat(list -> list.size() == 2 && list.contains(sender1) && list.contains(sender2)));
  }

  @Test void invitationsHydrateInviterNameAndRoleViaBatch() {
    UUID user = UUID.randomUUID(), inviter1 = UUID.randomUUID(), inviter2 = UUID.randomUUID();
    var inv1 = new LinkInvitation(inviter1, user, Instant.now().minusSeconds(3600));
    var inv2 = new LinkInvitation(inviter2, user, Instant.now().minusSeconds(7200));
    when(invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(user, user)).thenReturn(List.of(inv1, inv2));
    when(iam.findUserSummariesByIds(anyList())).thenReturn(Map.of(
        inviter1, new UserSummaryView(inviter1, "p@test.invalid", "Parent One", AccountRole.PARENT, AccountStatus.ACTIVE)));
    // inviter2 is deleted/missing in IAM

    var list = service.invitations(actor(user, "STUDENT"));
    assertEquals(2, list.size());
    assertEquals("Parent One", list.get(0).inviterName());
    assertEquals("PARENT", list.get(0).inviterRole());
    assertNull(list.get(1).inviterName());
    assertNull(list.get(1).inviterRole());
    verify(iam, times(1)).findUserSummariesByIds(argThat(ids -> ids.size() == 2 && ids.contains(inviter1) && ids.contains(inviter2)));
  }

  @Test void invitationsUseOnlyActorAsBothParticipants() {
    UUID user = UUID.randomUUID();
    var invitation = new LinkInvitation(user, UUID.randomUUID(), Instant.now().minusSeconds(15 * 86400));
    when(invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(user, user)).thenReturn(List.of(invitation));
    when(iam.findUserSummariesByIds(anyList())).thenReturn(Map.of(
        user, new UserSummaryView(user, "user@test.invalid", "User Name", AccountRole.PARENT, AccountStatus.ACTIVE)));
    var res = service.invitations(actor(user, "PARENT"));
    assertEquals("EXPIRED", res.getFirst().status());
    assertEquals("User Name", res.getFirst().inviterName());
    assertEquals("PARENT", res.getFirst().inviterRole());
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
