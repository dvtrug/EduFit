package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edufit.connection.application.service.ConnectionRequestService;
import vn.edufit.connection.application.policy.ConnectionRequestInput;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.application.dto.TransitionResult;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.infra.persistence.entity.*;
import vn.edufit.connection.infra.persistence.repository.*;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.*;
import vn.edufit.iam.api.*;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.notification.NotificationSink;
import vn.edufit.shared.exception.*;

class ConnectionRequestRulesTest {
  final UUID student=UUID.randomUUID(), studentUser=UUID.randomUUID(), tutor=UUID.randomUUID(), tutorUser=UUID.randomUUID(), goal=UUID.randomUUID(), id=UUID.randomUUID();
  final Instant now=Instant.parse("2026-10-11T00:00:00Z");
  final ConnectionRequestRepository rows=mock(ConnectionRequestRepository.class);
  final TutoringClassRepository classes=mock(TutoringClassRepository.class);
  final ProfileFacade profile=mock(ProfileFacade.class);
  final IamFacade iam=mock(IamFacade.class);
  final ConnectionFacade facade=mock(ConnectionFacade.class);
  final ConnectionLock lock=mock(ConnectionLock.class);
  final NotificationSink sink=mock(NotificationSink.class);
  final org.springframework.context.ApplicationEventPublisher events=mock(org.springframework.context.ApplicationEventPublisher.class);
  ConnectionRequestService service;
  @BeforeEach void setup() {
    when(profile.findStudentById(student)).thenReturn(Optional.of(new StudentSummaryDto(student,studentUser,4,null,true)));
    when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(new ConnectionGoalDto(goal,student,studentUser,1,4,"EXAM",null,"ACTIVE")));
    tutorStatus("VERIFIED");account(tutorUser,"TUTOR",AccountStatus.ACTIVE);
    when(profile.tutorTeaches(tutor,1,4)).thenReturn(true);
    when(rows.save(any())).thenAnswer(call -> {var row=(ConnectionRequest)call.getArgument(0);org.springframework.test.util.ReflectionTestUtils.setField(row,"id",id);return row;});
    service=new ConnectionRequestService(rows,classes,profile,iam,lock,sink,facade,new ConnectionRequestInput("legacy-compatible"),events,Clock.fixed(now,ZoneOffset.UTC));
  }
  private void account(UUID user,String role,AccountStatus status) {
    when(iam.findUserSummaryById(user)).thenReturn(Optional.of(new UserSummaryView(user,"actor@test.invalid","Actor",AccountRole.valueOf(role),status)));
  }
  CurrentUser actor(UUID user,String role) {
    var actor=mock(CurrentUser.class);when(actor.getUserId()).thenReturn(user);
    when(actor.hasRole(anyString())).thenAnswer(call -> role.equals(call.getArgument(0)));
    account(user,role,AccountStatus.ACTIVE);return actor;
  }
  private void tutorStatus(String status) {
    when(profile.findTutorById(tutor)).thenReturn(Optional.of(new TutorSummaryDto(tutor,tutorUser,"Tutor",null,null,null,null,null,null,null,status,now,null,0)));
  }
  private TransitionResult<?> create(CurrentUser actor) { return service.request(actor,goal,tutor,List.of("Tue 19:00"),null); }
  private ConnectionRequest pending(UUID sender) {
    var row=new ConnectionRequest(student,tutor,goal,sender,List.of("Tue 19:00"),null,now);
    org.springframework.test.util.ReflectionTestUtils.setField(row,"id",id);
    when(rows.findById(id)).thenReturn(Optional.of(row));when(rows.lockById(id)).thenReturn(Optional.of(row));return row;
  }
  @ParameterizedTest @ValueSource(strings={"STUDENT","PARENT"}) void eligibleCurrentActorCanCreate(String role) {
    UUID user=role.equals("STUDENT")?studentUser:UUID.randomUUID();
    when(facade.hasConfirmedParentLink(user,student)).thenReturn(true);
    assertInstanceOf(TransitionResult.Success.class,create(actor(user,role)));
    verify(events).publishEvent(argThat((vn.edufit.connection.api.event.ConnectionRequestChangedEvent e) ->
        e.aggregateId().equals(id) && e.actorUserId().equals(user) && e.studentId().equals(student)
            && e.oldStatus() == null && "PENDING".equals(e.newStatus()) && e.classId() == null));
    verifyNoInteractions(sink);
  }
  @ParameterizedTest @ValueSource(strings={"STUDENT","PARENT"}) void inactiveActorCannotCreateOrCancel(String role) {
    UUID user=role.equals("STUDENT")?studentUser:UUID.randomUUID();var actor=actor(user,role);
    when(facade.hasConfirmedParentLink(user,student)).thenReturn(true);pending(user);
    account(user,role,AccountStatus.DEACTIVATED);
    assertThrows(ForbiddenOperationException.class,()->create(actor));
    assertThrows(ForbiddenOperationException.class,()->service.cancelRequest(actor,id));
    verify(rows,never()).save(any());verifyNoInteractions(sink);verifyNoInteractions(events);
  }
  @Test void inactiveTutorAccountCannotReceiveRequest() {
    account(tutorUser,"TUTOR",AccountStatus.DEACTIVATED);
    assertThrows(InvalidOperationException.class,()->create(actor(studentUser,"STUDENT")));
    verify(rows,never()).save(any());verifyNoInteractions(events);
  }
  @Test void unverifiedTutorCannotReceiveRequest() {
    tutorStatus("PENDING");assertThrows(InvalidOperationException.class,()->create(actor(studentUser,"STUDENT")));
    verifyNoInteractions(events);
  }
  @Test void changedGoalOwnerCannotCreateForDifferentAccount() {
    when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(new ConnectionGoalDto(goal,student,UUID.randomUUID(),1,4,"EXAM",null,"ACTIVE")));
    assertThrows(InvalidOperationException.class,()->create(actor(studentUser,"STUDENT")));verify(rows,never()).save(any());
    verifyNoInteractions(events);
  }
  @Test void inactiveGoalCannotCreate() {
    when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(new ConnectionGoalDto(goal,student,studentUser,1,4,"EXAM",null,"ARCHIVED")));
    assertThrows(InvalidOperationException.class,()->create(actor(studentUser,"STUDENT")));
    verifyNoInteractions(events);
  }
  @Test void expertiseMustMatchCurrentGoal() {
    when(profile.tutorTeaches(tutor,1,4)).thenReturn(false);assertThrows(InvalidOperationException.class,()->create(actor(studentUser,"STUDENT")));
    verifyNoInteractions(events);
  }
  @ParameterizedTest @ValueSource(strings={"duplicate","limit","class"}) void conflictsDoNotWriteRequest(String kind) {
    if(kind.equals("duplicate")) when(rows.existsByStudentIdAndTutorIdAndStatus(student,tutor,"PENDING")).thenReturn(true);
    if(kind.equals("class")) when(classes.existsByStudentIdAndTutorIdAndStatus(student,tutor,"ACTIVE")).thenReturn(true);
    if(kind.equals("limit")) {
      when(rows.countByStudentIdAndStatus(student,"PENDING")).thenReturn(5L);
      when(rows.findByStudentIdAndStatus(student,"PENDING")).thenReturn(Collections.nCopies(5,new ConnectionRequest(student,tutor,goal,studentUser,List.of("Tue 19:00"),null,now)));
    }
    assertInstanceOf(TransitionResult.Conflict.class,create(actor(studentUser,"STUDENT")));verify(rows,never()).save(any());verifyNoInteractions(sink);
    verifyNoInteractions(events);
  }
  @Test void revokedParentCannotCreateOrCancel() {
    var actor=actor(UUID.randomUUID(),"PARENT");pending(actor.getUserId());
    assertThrows(ForbiddenOperationException.class,()->create(actor));assertThrows(ForbiddenOperationException.class,()->service.cancelRequest(actor,id));
    verifyNoInteractions(events);
  }
  @Test void studentCanCancelAnOlderParentRequest() {
    var row=pending(UUID.randomUUID());
    when(profile.findTutorById(tutor)).thenReturn(Optional.of(new TutorSummaryDto(tutor,tutorUser,"Tutor",null,null,null,null,null,null,null,"VERIFIED",now,null,0)));
    assertInstanceOf(TransitionResult.Success.class,service.cancelRequest(actor(studentUser,"STUDENT"),id));
    assertEquals("CANCELLED",row.getStatus());
    verify(events).publishEvent(argThat((vn.edufit.connection.api.event.ConnectionRequestChangedEvent e) ->
        e.aggregateId().equals(id) && e.actorUserId().equals(studentUser) && e.studentId().equals(student)
            && "PENDING".equals(e.oldStatus()) && "CANCELLED".equals(e.newStatus()) && e.classId() == null));
  }
}
