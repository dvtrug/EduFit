package vn.edufit.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edufit.connection.application.service.LinkInvitationService;
import vn.edufit.connection.application.service.ConnectionRequestService;
import vn.edufit.connection.application.service.ConnectionQueryService;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import java.time.Instant;
import vn.edufit.connection.application.service.ParentStudentLinkService;
import vn.edufit.shared.auth.CurrentUser;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.application.service.InvitationQuotaService;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.audit.AuditSink;
import vn.edufit.shared.notification.NotificationSink;

@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(ConnectionTransactionPostgresTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConnectionTransactionPostgresTest {
  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired vn.edufit.connection.infra.persistence.repository.LinkInvitationRepository invitationRows;
  @Autowired vn.edufit.connection.infra.persistence.repository.ConnectionRequestRepository requestRows;
  @Autowired vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository linkRows;
  @Autowired ConnectionRequestService requests;
  @Autowired ConnectionQueryService queries;
  @Autowired ProfileFacade profile;
  @Autowired LinkInvitationService invitations;
  @Autowired ParentStudentLinkService links;
  @Autowired DataSource source;
  @Autowired IamFacade iam;
  @Autowired AuditSink audit;
  @Autowired NotificationSink notifications;
  @Autowired ConnectionLock lock;
  @Autowired InvitationQuotaService quota;
  @Autowired JpaTransactionManager transactionManager;
  @jakarta.persistence.PersistenceContext EntityManager em;

  @BeforeEach void resetCollaborators() { reset(iam, profile, audit, notifications); }

  private CurrentUser actor(UUID id, String role) {
    CurrentUser actor = mock(CurrentUser.class);
    lenient().when(actor.getUserId()).thenReturn(id);
    when(actor.hasRole(anyString())).thenAnswer(i -> role.equals(i.getArgument(0)));
    lenient().when(iam.findUserSummaryById(id)).thenReturn(Optional.of(new vn.edufit.iam.api.dto.UserSummaryView(id,
        "actor@test.invalid", "Fixture", vn.edufit.iam.api.AccountRole.valueOf(role), vn.edufit.iam.api.AccountStatus.ACTIVE)));
    return actor;
  }

  private UUID account(String role) {
    UUID id = UUID.randomUUID();
    new JdbcTemplate(source).update("insert into account(user_id,email,password_hash,full_name,role) values (?,?,?,?,?)",
        id, id + "@test.invalid", "test-only", "Transaction fixture", role);
    return id;
  }

  @Test void invitationEntryPointCommitsQuotaInItsOwnTransaction() {
    UUID parent = account("PARENT");
    when(iam.findUserSummaryByEmail("missing@test.invalid")).thenAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return Optional.empty();
    });
    assertTrue(invitations.invite(actor(parent, "PARENT"), "missing@test.invalid"));
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertEquals(1, new JdbcTemplate(source).queryForObject(
        "select attempts from connection_invite_quota where user_id = ?", Integer.class, parent));
    verifyNoInteractions(notifications);
  }

  @Test void revokeRollsBackOnAuditFailureThenCommitsOnSuccess() {
    var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), studentUser = account("STUDENT"), admin = account("ADMIN");
    UUID student = UUID.randomUUID(), link = UUID.randomUUID();
    jdbc.update("insert into student_profile(student_id,user_id) values (?,?)", student, studentUser);
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())", link, parent, student);
    when(profile.findStudentById(student)).thenReturn(Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(student, studentUser, 4, null, true)));
    doAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      throw new IllegalStateException("injected audit failure");
    }).when(audit).record(any(vn.edufit.shared.audit.AuditRecord.class));
    assertThrows(IllegalStateException.class, () -> links.revokeLink(actor(admin, "ADMIN"), link, "reason"));
    assertEquals("CONFIRMED", jdbc.queryForObject("select status from parent_student_link where link_id = ?", String.class, link));
    verifyNoInteractions(notifications);
    reset(audit);
    doAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    }).when(notifications).notify(eq(parent), isNull(), eq("LINK_REVOKED"), anyString(), anyString(), eq("PARENT_STUDENT_LINK"), eq(link));
    links.revokeLink(actor(admin, "ADMIN"), link, "reason");
    assertEquals("REVOKED", jdbc.queryForObject("select status from parent_student_link where link_id = ?", String.class, link));
    verify(audit).record(argThat((vn.edufit.shared.audit.AuditRecord record) -> record.actorUserId().equals(admin) && record.targetId().equals(link) && record.metadata().get("studentUserId").equals(studentUser.toString())));
    verify(notifications).notify(eq(parent), isNull(), eq("LINK_REVOKED"), anyString(), anyString(), eq("PARENT_STUDENT_LINK"), eq(link));
  }

  @Test void acceptCreatesClassAtomicallyAndQueryHydratesInsideTransaction() {
    var jdbc = new JdbcTemplate(source);
    UUID studentUser = account("STUDENT"), tutorUser = account("TUTOR");
    UUID student = UUID.randomUUID(), tutor = UUID.randomUUID(), goal = UUID.randomUUID(), request = UUID.randomUUID();
    jdbc.update("insert into student_profile(student_id,user_id,education_level_id) values (?,?,4)", student, studentUser);
    jdbc.update("insert into tutor_profile(tutor_id,user_id,display_name,teaching_mode,price_per_session,status) values (?,?,'Tutor','ONLINE',100000,'VERIFIED')", tutor, tutorUser);
    jdbc.update("insert into learning_goal(goal_id,student_id,subject_id,current_level,desired_level,goal_type,deadline,mode,budget_max) values (?,?,1,1,2,'EXAM_PREPARATION',current_date + 30,'ONLINE',100000)", goal, student);
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now() + interval '7 days')",
        request, student, tutor, goal, studentUser, "[\"Tue 19:00\"]");
    when(profile.getTutorIdByUserId(tutorUser)).thenReturn(tutor);
    when(profile.findTutorById(tutor)).thenReturn(Optional.of(new TutorSummaryDto(tutor,
        tutorUser, "Tutor", null, null, null, null, null, null, null, "VERIFIED", Instant.now(), null, 0)));
    when(profile.findStudentById(student)).thenReturn(Optional.of(new StudentSummaryDto(student, studentUser, 4, null, true)));
    when(profile.findGoalForConnection(goal)).thenAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return Optional.of(new ConnectionGoalDto(goal, student, studentUser, 1, 4, "EXAM_PREPARATION", null, "ACTIVE"));
    });
    when(profile.tutorTeaches(tutor, 1, 4)).thenReturn(true);
    var actor = actor(tutorUser, "TUTOR");
    var inbox = queries.tutorInbox(actor);
    assertEquals(1, inbox.size());
    assertEquals(java.util.List.of("Tue 19:00"), inbox.getFirst().desiredSlots());
    assertEquals("STUDENT", inbox.getFirst().senderRole());
    doThrow(new IllegalStateException("injected notification failure")).when(notifications)
        .notify(eq(studentUser), eq(student), eq("CONNECTION_ACCEPTED"), anyString(), anyString(), eq("CLASS"), any(UUID.class));
    assertThrows(IllegalStateException.class, () -> requests.respondRequest(actor, request, true, null));
    assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id = ?", String.class, request));
    assertEquals(0, jdbc.queryForObject("select count(*) from class where connection_request_id = ?", Integer.class, request));
    reset(notifications);
    doAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    }).when(notifications).notify(eq(studentUser), eq(student), eq("CONNECTION_ACCEPTED"), anyString(), anyString(), eq("CLASS"), any(UUID.class));
    var result = requests.respondRequest(actor, request, true, null);
    var created = ((vn.edufit.connection.application.dto.TransitionResult.Success<vn.edufit.connection.application.dto.ClassResult>) result).value();
    assertNotNull(created.classId());
    assertEquals("ACCEPTED", jdbc.queryForObject("select status from connection_request where request_id = ?", String.class, request));
    assertEquals("ACTIVE", jdbc.queryForObject("select status from class where class_id = ?", String.class, created.classId()));
    assertEquals(1, jdbc.queryForObject("select count(*) from class where connection_request_id = ?", Integer.class, request));
    verify(notifications).notify(eq(studentUser), eq(student), eq("CONNECTION_ACCEPTED"), anyString(), anyString(), eq("CLASS"), eq(created.classId()));
  }

  private record RequestFixture(UUID request, UUID student, UUID tutor, UUID goal, UUID studentUser, UUID tutorUser) {}

  private RequestFixture pendingRequest() {
    var jdbc = new JdbcTemplate(source);
    UUID studentUser = account("STUDENT"), tutorUser = account("TUTOR");
    UUID student = UUID.randomUUID(), tutor = UUID.randomUUID(), goal = UUID.randomUUID(), request = UUID.randomUUID();
    jdbc.update("insert into student_profile(student_id,user_id,education_level_id) values (?,?,4)", student, studentUser);
    jdbc.update("insert into tutor_profile(tutor_id,user_id,display_name,teaching_mode,price_per_session,status) values (?,?,'Tutor','ONLINE',100000,'VERIFIED')", tutor, tutorUser);
    jdbc.update("insert into learning_goal(goal_id,student_id,subject_id,current_level,desired_level,goal_type,deadline,mode,budget_max) values (?,?,1,1,2,'EXAM_PREPARATION',current_date + 30,'ONLINE',100000)", goal, student);
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now() + interval '7 days')",
        request, student, tutor, goal, studentUser, "[]");
    lenient().when(profile.getTutorIdByUserId(tutorUser)).thenReturn(tutor);
    lenient().when(profile.findTutorById(tutor)).thenReturn(Optional.of(new TutorSummaryDto(tutor,
        tutorUser, "Tutor", null, null, null, null, null, null, null, "VERIFIED", Instant.now(), null, 0)));
    lenient().when(profile.findGoalForConnection(goal)).thenReturn(Optional.of(
        new ConnectionGoalDto(goal, student, studentUser, 1, 4, "EXAM_PREPARATION", null, "ACTIVE")));
    lenient().when(profile.tutorTeaches(tutor, 1, 4)).thenReturn(true);
    lenient().when(iam.findUserSummaryById(tutorUser)).thenReturn(Optional.of(new vn.edufit.iam.api.dto.UserSummaryView(tutorUser,
        "tutor@test.invalid", "Tutor", vn.edufit.iam.api.AccountRole.TUTOR, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    lenient().when(profile.findStudentById(student)).thenReturn(Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(student,studentUser,4,null,true)));
    return new RequestFixture(request, student, tutor, goal, studentUser, tutorUser);
  }

  @Test void sixthPendingRequestIsBlockedButExpiredRequestReleasesTheQuota() {
    var fixture=pendingRequest();var destination=pendingRequest();var jdbc=new JdbcTemplate(source);
    for(int i=0;i<4;i++) {
      UUID tutorUser=account("TUTOR"),tutor=UUID.randomUUID();
      jdbc.update("insert into tutor_profile(tutor_id,user_id,display_name,teaching_mode,price_per_session,status) values (?,?,'Tutor','ONLINE',100000,'VERIFIED')",tutor,tutorUser);
      jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,'[]'::jsonb,now()+interval '7 days')",UUID.randomUUID(),fixture.student(),tutor,fixture.goal(),fixture.studentUser());
    }
    var actor=actor(fixture.studentUser(),"STUDENT");
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
        requests.request(actor,fixture.goal(),destination.tutor(),java.util.List.of("Tue 19:00"),null));
    assertEquals(5L,requestRows.countByStudentIdAndStatus(fixture.student(),"PENDING"));
    jdbc.update("update connection_request set expires_at=now()-interval '1 day' where request_id=?",fixture.request());
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
        requests.request(actor,fixture.goal(),destination.tutor(),java.util.List.of("Tue 19:00"),null));
    assertEquals(5L,requestRows.countByStudentIdAndStatus(fixture.student(),"PENDING"));
    assertEquals("EXPIRED",jdbc.queryForObject("select status from connection_request where request_id=?",String.class,fixture.request()));
  }

  @Test void conflictExceptionFromRequiredWriteRollsBackAcceptedRequestAndClass() {
    var fixture = pendingRequest();
    doThrow(new vn.edufit.shared.exception.ResourceConflictException("injected writer conflict"))
        .when(notifications).notify(any(), any(), anyString(), anyString(), anyString(), anyString(), any());
    assertThrows(vn.edufit.shared.exception.ResourceConflictException.class, () ->
        requests.respondRequest(actor(fixture.tutorUser(), "TUTOR"), fixture.request(), true, null));
    var jdbc = new JdbcTemplate(source);
    assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
    assertEquals(0, jdbc.queryForObject("select count(*) from class where connection_request_id=?", Integer.class, fixture.request()));
  }

  private org.springframework.test.web.servlet.MockMvc http(CurrentUser actor) {
    @SuppressWarnings("unchecked") org.springframework.beans.factory.ObjectProvider<CurrentUser> users = mock(org.springframework.beans.factory.ObjectProvider.class);
    when(users.getObject()).thenReturn(actor);
    return org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
        new vn.edufit.connection.web.ConnectionController(requests, invitations, links, queries, users))
        .setControllerAdvice(new vn.edufit.connection.web.ConnectionWriteExceptionHandler(),
            new vn.edufit.web.advice.GlobalExceptionHandler()).build();
  }

  @Test void expiredInvitationCommitsBeforeHttpConflictAndOuterRollbackIsRespected() throws Exception {
    var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), studentUser = account("STUDENT"), invitation = UUID.randomUUID();
    jdbc.update("insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,expires_at) values (?,?,?,now()-interval '1 day')", invitation, parent, studentUser);
    var actor = actor(parent, "PARENT");
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      var result = invitations.withdrawInvitation(actor, invitation);
      assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class, result);
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      status.setRollbackOnly();
    });
    assertEquals("PENDING", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
    http(actor).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
        "/api/v1/connections/parent-student/invitations/" + invitation + "/withdraw"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("CONFLICT_DETECTED"));
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertEquals("EXPIRED", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
    verifyNoInteractions(notifications);
  }

  @Test void expiredRequestCommitsBeforeHttpConflictAndOuterRollbackIsRespected() throws Exception {
    var fixture = pendingRequest(); var jdbc = new JdbcTemplate(source);
    jdbc.update("update connection_request set expires_at=now()-interval '1 day' where request_id=?", fixture.request());
    var actor = actor(fixture.tutorUser(), "TUTOR");
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
          requests.respondRequest(actor, fixture.request(), true, null));
      status.setRollbackOnly();
    });
    assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
    http(actor).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
        "/api/v1/connections/requests/" + fixture.request() + "/respond")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
    assertEquals("EXPIRED", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
    assertEquals(0, jdbc.queryForObject("select count(*) from class where connection_request_id=?", Integer.class, fixture.request()));
    verifyNoInteractions(notifications);
  }

  @Test void createConflictCommitsOldRequestExpiry() {
    var expired = pendingRequest(); var other = pendingRequest(); var jdbc = new JdbcTemplate(source);
    jdbc.update("update connection_request set expires_at=now()-interval '1 day' where request_id=?", expired.request());
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now()+interval '7 days')",
        UUID.randomUUID(), expired.student(), other.tutor(), expired.goal(), expired.studentUser(), "[]");
    when(profile.findStudentById(expired.student())).thenReturn(Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(
        expired.student(), expired.studentUser(), 4, null, true)));
    var result = requests.request(actor(expired.studentUser(), "STUDENT"), expired.goal(), other.tutor(), java.util.List.of("Tue 19:00"), null);
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class, result);
    assertEquals("EXPIRED", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, expired.request()));
    verifyNoInteractions(notifications);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"flush", "deferred"})
  void databaseClassFailureRollsBackAcceptedRequestAndMapsAfterTransaction(String phase) throws Exception {
    var fixture = pendingRequest(); var jdbc = new JdbcTemplate(source);
    jdbc.execute("create function connection_test_fail_class() returns trigger language plpgsql as $$ begin raise exception 'injected class constraint' using errcode='23514'; end; $$");
    String trigger = phase.equals("deferred")
        ? "create constraint trigger connection_test_class_failure after insert on class deferrable initially deferred for each row execute function connection_test_fail_class()"
        : "create trigger connection_test_class_failure before insert on class for each row execute function connection_test_fail_class()";
    jdbc.execute(trigger);
    if (phase.equals("flush")) doAnswer(call -> { em.flush(); return null; }).when(notifications)
        .notify(any(), any(), anyString(), anyString(), anyString(), anyString(), any());
    try {
      http(actor(fixture.tutorUser(), "TUTOR")).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
          "/api/v1/connections/requests/" + fixture.request() + "/respond")
          .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Dữ liệu kết nối đã thay đổi hoặc không còn hợp lệ"));
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
      assertEquals(0, jdbc.queryForObject("select count(*) from class where connection_request_id=?", Integer.class, fixture.request()));
    } finally {
      jdbc.execute("drop trigger connection_test_class_failure on class");
      jdbc.execute("drop function connection_test_fail_class()");
    }
    reset(notifications);
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
        requests.respondRequest(actor(fixture.tutorUser(), "TUTOR"), fixture.request(), true, null));
    assertEquals(1, jdbc.queryForObject("select count(*) from class where connection_request_id=?", Integer.class, fixture.request()));
  }

  @Test void acceptedRequestStillRollsBackWithOuterTransaction() {
    var fixture = pendingRequest();
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
      assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
          requests.respondRequest(actor(fixture.tutorUser(), "TUTOR"), fixture.request(), true, null));
      status.setRollbackOnly();
    });
    var jdbc = new JdbcTemplate(source);
    assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
    assertEquals(0, jdbc.queryForObject("select count(*) from class where connection_request_id=?", Integer.class, fixture.request()));
  }

  @Test void invitationAndLinkRollbackTogetherWhenNotificationWriteFails() {
    var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), studentUser = account("STUDENT"), student = UUID.randomUUID(), invitation = UUID.randomUUID();
    jdbc.update("insert into student_profile(student_id,user_id) values (?,?)", student, studentUser);
    jdbc.update("insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,expires_at) values (?,?,?,now()+interval '14 days')", invitation, parent, studentUser);
    when(profile.findStudentByUserId(studentUser)).thenReturn(Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(student, studentUser, 4, null, true)));
    when(iam.findUserSummaryById(parent)).thenReturn(Optional.of(new vn.edufit.iam.api.dto.UserSummaryView(
        parent, "parent@test.invalid", "Parent", vn.edufit.iam.api.AccountRole.PARENT, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    doThrow(new vn.edufit.shared.exception.ResourceConflictException("injected notification write conflict"))
        .when(notifications).notify(any(), any(), anyString(), anyString(), anyString(), anyString(), any());
    assertThrows(vn.edufit.shared.exception.ResourceConflictException.class,
        () -> invitations.respondInvitation(actor(studentUser, "STUDENT"), invitation, true));
    assertEquals("PENDING", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
    assertEquals(0, jdbc.queryForObject("select count(*) from parent_student_link where student_id=? and parent_user_id=?", Integer.class, student, parent));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"invitation", "request", "link"})
  void rowLockReloadsStateCommittedAfterInitialRead(String kind) throws Exception {
    var fixture = pendingRequest(); var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), id = kind.equals("request") ? fixture.request() : UUID.randomUUID();
    if (kind.equals("invitation")) jdbc.update(
        "insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,expires_at) values (?,?,?,now()+interval '14 days')",
        id, parent, fixture.studentUser());
    if (kind.equals("link")) jdbc.update(
        "insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())",
        id, parent, fixture.student());
    Class<?> type = switch (kind) {
      case "invitation" -> vn.edufit.connection.infra.persistence.entity.LinkInvitation.class;
      case "request" -> vn.edufit.connection.infra.persistence.entity.ConnectionRequest.class;
      default -> vn.edufit.connection.infra.persistence.entity.ParentStudentLink.class;
    };
    String table = switch (kind) { case "invitation" -> "link_invitation"; case "request" -> "connection_request"; default -> "parent_student_link"; };
    String key = switch (kind) { case "invitation" -> "invitation_id"; case "request" -> "request_id"; default -> "link_id"; };
    String terminal = switch (kind) { case "invitation" -> "WITHDRAWN"; case "request" -> "CANCELLED"; default -> "REVOKED"; };
    try (var executor = Executors.newSingleThreadExecutor()) {
      new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
        Object cached = em.find(type, id);
        assertEquals(kind.equals("link") ? "CONFIRMED" : "PENDING", rowStatus(cached));
        var other = executor.submit(() -> jdbc.update("update " + table + " set status=?,version=version+1 where " + key + "=?", terminal, id));
        try { assertEquals(1, other.get(10, TimeUnit.SECONDS)); }
        catch (Exception failure) { throw new AssertionError("Concurrent update must commit before lock", failure); }
        Object locked = switch (kind) {
          case "invitation" -> invitationRows.lockById(id).orElseThrow();
          case "request" -> requestRows.lockById(id).orElseThrow();
          default -> linkRows.lockById(id).orElseThrow();
        };
        assertEquals(terminal, rowStatus(locked), "Lock must reload committed state, not reuse stale first-level cache");
      });
    }
  }

  private String rowStatus(Object row) {
    if (row instanceof vn.edufit.connection.infra.persistence.entity.LinkInvitation invitation) return invitation.getStatus();
    if (row instanceof vn.edufit.connection.infra.persistence.entity.ConnectionRequest request) return request.getStatus();
    return ((vn.edufit.connection.infra.persistence.entity.ParentStudentLink) row).getStatus();
  }

  @Test void rowReloadPreservesUnrelatedManagedTransition() {
    var fixture = pendingRequest(); var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), invitation = UUID.randomUUID();
    jdbc.update("insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,expires_at) values (?,?,?,now()+interval '14 days')",
        invitation, parent, fixture.studentUser());
    new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
      var other = invitationRows.findById(invitation).orElseThrow();
      other.decline(Instant.now());
      assertTrue(requestRows.lockById(fixture.request()).isPresent());
    });
    assertEquals("DECLINED", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
  }

  @Test void freshRowReadsRequireTransactionAndReturnEmptyForMissingRows() {
    UUID missing = UUID.randomUUID();
    assertThrows(IllegalTransactionStateException.class, () -> invitationRows.lockById(missing));
    assertThrows(IllegalTransactionStateException.class, () -> requestRows.lockById(missing));
    assertThrows(IllegalTransactionStateException.class, () -> linkRows.lockById(missing));
    new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
      assertTrue(invitationRows.lockById(missing).isEmpty());
      assertTrue(requestRows.lockById(missing).isEmpty());
      assertTrue(linkRows.lockById(missing).isEmpty());
    });
  }

  @Test void existingLinkClosureCommitsBeforeHttp409AndHonorsOuterRollback() throws Exception {
    var jdbc = new JdbcTemplate(source);
    UUID parent = account("PARENT"), studentUser = account("STUDENT"), student = UUID.randomUUID(), invitation = UUID.randomUUID();
    jdbc.update("insert into student_profile(student_id,user_id) values (?,?)", student, studentUser);
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())", UUID.randomUUID(), parent, student);
    jdbc.update("insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,expires_at) values (?,?,?,now()+interval '14 days')", invitation, parent, studentUser);
    when(profile.findStudentByUserId(studentUser)).thenReturn(Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(student, studentUser, 4, null, false)));
    when(iam.findUserSummaryById(parent)).thenReturn(Optional.of(new vn.edufit.iam.api.dto.UserSummaryView(parent, "parent@test.invalid", "Fixture", vn.edufit.iam.api.AccountRole.PARENT, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    var recipient = actor(studentUser, "STUDENT");
    new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
      assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class, invitations.respondInvitation(recipient, invitation, true));
      tx.setRollbackOnly();
    });
    assertEquals("PENDING", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
    http(recipient).perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/connections/parent-student/respond")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"invitationId\":\"" + invitation + "\",\"accept\":true}"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
    assertEquals("WITHDRAWN", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, invitation));
    assertEquals(1, jdbc.queryForObject("select count(*) from parent_student_link where student_id=?", Integer.class, student));
    verifyNoInteractions(notifications);
  }

  @Test void adaptersRequireAnExistingTransaction() {
    assertThrows(IllegalTransactionStateException.class, () -> lock.lockStudent(UUID.randomUUID()));
    assertThrows(IllegalTransactionStateException.class, () -> quota.consume(UUID.randomUUID()));
  }

  @Test void oppositeInvitationDirectionsBlockUntilTransactionEnds() throws Exception {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    var acquired = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var transactions = new TransactionTemplate(transactionManager);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var holder = executor.submit(() -> transactions.executeWithoutResult(status -> {
        lock.lockInvitationPair(a, b);
        acquired.countDown();
        try { assertTrue(release.await(20, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
      }));
      try {
        assertTrue(acquired.await(10, TimeUnit.SECONDS));
        RuntimeException blocked = assertThrows(RuntimeException.class, () -> transactions.executeWithoutResult(status -> {
          em.createNativeQuery("set local lock_timeout = '250ms'").executeUpdate();
          lock.lockInvitationPair(b, a);
        }));
        Throwable cause = blocked;
        while (cause.getCause() != null) cause = cause.getCause();
        assertInstanceOf(SQLException.class, cause);
        assertEquals("55P03", ((SQLException) cause).getSQLState(), "Must be a real PostgreSQL lock timeout");
      } finally { release.countDown(); }
      holder.get(10, TimeUnit.SECONDS);
      transactions.executeWithoutResult(status -> lock.lockInvitationPair(b, a));
    }
  }

  @Configuration
  @EnableTransactionManagement
  @ComponentScan(basePackages = {"vn.edufit.connection.application", "vn.edufit.connection.infra.locking"})
  @org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.edufit.connection.infra.persistence.repository")
  @org.springframework.context.annotation.Import({vn.edufit.iam.infra.config.IamClockConfig.class, vn.edufit.notification.NotificationSinkTestBridge.class})
  static class Config {
    @Bean DataSource dataSource() {
      var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
      return source;
    }
    @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan("vn.edufit.connection.infra.persistence.entity");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
      return factory;
    }
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
    @Bean EntityManager entityManager(EntityManagerFactory factory) { return SharedEntityManagerCreator.createSharedEntityManager(factory); }
    @Bean ProfileFacade profile() { return mock(ProfileFacade.class); }
    @Bean IamFacade iam() { return mock(IamFacade.class); }
    @Bean AuditSink audit() { return mock(AuditSink.class); }
    @Bean NotificationSink notifications() { return mock(NotificationSink.class); }
  }
}
