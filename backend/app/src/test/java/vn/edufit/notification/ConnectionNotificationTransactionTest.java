package vn.edufit.notification;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.persistence.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.*;
import org.springframework.transaction.support.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import vn.edufit.connection.api.event.*;
import vn.edufit.connection.infra.persistence.entity.LinkInvitation;

@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig(ConnectionNotificationTransactionTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConnectionNotificationTransactionTest {
  private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired DataSource source;
  @Autowired JpaTransactionManager manager;
  @Autowired ApplicationEventPublisher events;
  @Autowired ConnectionNotificationWriter writer;
  @Autowired CommitProbe probe;
  @Autowired vn.edufit.connection.application.service.LinkInvitationService invitationService;
  @Autowired(required = false) vn.edufit.connection.application.service.ConnectionRequestService requestService;
  @Autowired vn.edufit.iam.api.IamFacade iam;
  @Autowired vn.edufit.profile.api.ProfileFacade profile;
  @Autowired vn.edufit.shared.notification.NotificationSink oldSink;
  @PersistenceContext EntityManager em;

  @BeforeEach void resetProbe() {
    probe.gate = null; probe.sawTransaction.set(false);
    org.mockito.Mockito.reset(iam, profile, oldSink);
  }
  private record Fixture(UUID inviter, UUID invited, UUID student, UUID invitation) {}
  private Fixture fixture() {
    var jdbc = new JdbcTemplate(source);
    UUID parent = UUID.randomUUID(), studentUser = UUID.randomUUID(), student = UUID.randomUUID();
    for (UUID user : java.util.List.of(parent, studentUser))
      jdbc.update("insert into account(user_id,email,password_hash,full_name,role) values (?,?,?,?,?)",
          user, user + "@test.invalid", "test-only", "Event fixture", user.equals(parent) ? "PARENT" : "STUDENT");
    jdbc.update("insert into student_profile(student_id,user_id) values (?,?)", student, studentUser);
    return new Fixture(parent, studentUser, student, UUID.randomUUID());
  }
  private LinkInvitationChangedEvent created(Fixture fixture, UUID recipient) {
    return new LinkInvitationChangedEvent(UUID.randomUUID(), fixture.invitation(), fixture.inviter(), fixture.student(),
        null, "PENDING", NOW, fixture.inviter(), recipient, null, LinkInvitationChangedEvent.Cause.USER_ACTION);
  }
  private void insertInvitation(Fixture fixture) {
    new JdbcTemplate(source).update("insert into link_invitation(invitation_id,inviter_user_id,invited_user_id,created_at,expires_at) values (?,?,?,?,?)",
        fixture.invitation(), fixture.inviter(), fixture.invited(), java.sql.Timestamp.from(NOW), java.sql.Timestamp.from(NOW.plusSeconds(14 * 86400)));
  }
  private int notifications(Fixture fixture) {
    return new JdbcTemplate(source).queryForObject("select count(*) from notification where target_id=?", Integer.class, fixture.invitation());
  }
  private int invitations(Fixture fixture) {
    return new JdbcTemplate(source).queryForObject("select count(*) from link_invitation where invitation_id=?", Integer.class, fixture.invitation());
  }

  @Test void notificationAndBusinessRowCommitTogetherBeforeCommit() {
    var fixture = fixture();
    new TransactionTemplate(manager).executeWithoutResult(status -> {
      insertInvitation(fixture); events.publishEvent(created(fixture, fixture.invited()));
      assertEquals(0, notifications(fixture), "BEFORE_COMMIT must not execute at publication time");
    });
    assertTrue(probe.sawTransaction.get());
    assertEquals(1, invitations(fixture)); assertEquals(1, notifications(fixture));
    var jdbc = new JdbcTemplate(source);
    assertEquals(fixture.invited(), jdbc.queryForObject("select user_id from notification where target_id=?", UUID.class, fixture.invitation()));
    assertEquals(NOW, jdbc.queryForObject("select created_at from notification where target_id=?", java.sql.Timestamp.class, fixture.invitation()).toInstant());
    assertEquals("LINK_INVITED", jdbc.queryForObject("select type from notification where target_id=?", String.class, fixture.invitation()));
  }

  @Test void outsideTransactionPublicationHasNoFallbackAndWriterRequiresTransaction() {
    var fixture = fixture(); events.publishEvent(created(fixture, fixture.invited()));
    assertEquals(0, notifications(fixture)); assertFalse(probe.sawTransaction.get());
    assertThrows(IllegalTransactionStateException.class, () -> writer.invitationCreated(created(fixture, fixture.invited())));
  }

  @Test void outerRollbackDiscardsBusinessAndNotification() {
    var fixture = fixture();
    new TransactionTemplate(manager).executeWithoutResult(status -> {
      insertInvitation(fixture); events.publishEvent(created(fixture, fixture.invited())); status.setRollbackOnly();
    });
    assertEquals(0, invitations(fixture)); assertEquals(0, notifications(fixture));
    assertFalse(probe.sawTransaction.get(), "BEFORE_COMMIT does not run for an outer rollback");
  }

  @Test void notificationForeignKeyFailureRollsBackBusinessWrite() {
    var fixture = fixture();
    var failure = assertThrows(RuntimeException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
      insertInvitation(fixture); events.publishEvent(created(fixture, UUID.randomUUID()));
    }));
    assertEquals("23503", sqlState(failure));
    assertEquals(0, invitations(fixture)); assertEquals(0, notifications(fixture));
  }

  @Test void deferredNotificationConstraintFailureRollsBackBusinessWrite() {
    var fixture = fixture(); var jdbc = new JdbcTemplate(source);
    jdbc.execute("create function connection_test_fail_notification() returns trigger language plpgsql as $$ begin raise exception 'injected notification commit constraint' using errcode='23514'; end; $$");
    jdbc.execute("create constraint trigger connection_test_notification_failure after insert on notification deferrable initially deferred for each row execute function connection_test_fail_notification()");
    try {
      var failure = assertThrows(RuntimeException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
        insertInvitation(fixture); events.publishEvent(created(fixture, fixture.invited()));
      }));
      assertEquals("23514", sqlState(failure));
      assertEquals(0, invitations(fixture)); assertEquals(0, notifications(fixture));
    } finally {
      jdbc.execute("drop trigger connection_test_notification_failure on notification");
      jdbc.execute("drop function connection_test_fail_notification()");
    }
  }

  @Test void delayedBeforeCommitListenerKeepsBusinessAndNotificationInvisibleUntilCommit() throws Exception {
    var fixture = fixture(); var gate = new Gate(); probe.gate = gate;
    try (var executor = Executors.newSingleThreadExecutor()) {
      var future = executor.submit(() -> new TransactionTemplate(manager).executeWithoutResult(status -> {
        insertInvitation(fixture); events.publishEvent(created(fixture, fixture.invited()));
      }));
      try {
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS));
        assertTrue(probe.sawTransaction.get());
        assertEquals(0, invitations(fixture)); assertEquals(0, notifications(fixture));
        assertFalse(future.isDone());
      } finally { gate.release.countDown(); }
      future.get(20, TimeUnit.SECONDS);
    } finally { probe.gate = null; }
    assertEquals(1, invitations(fixture)); assertEquals(1, notifications(fixture));
  }

  @Test void classCreatedDoesNotDuplicateNotificationAndUnmigratedTransitionsRemainDormant() {
    var fixture = fixture();
    new TransactionTemplate(manager).executeWithoutResult(status -> {
      insertInvitation(fixture); events.publishEvent(created(fixture, fixture.invited()));
      events.publishEvent(new TutoringClassCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), fixture.inviter(), fixture.student(),
          null, "ACTIVE", NOW, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
      events.publishEvent(new LinkInvitationChangedEvent(UUID.randomUUID(), fixture.invitation(), fixture.invited(), fixture.student(),
          "PENDING", "ACCEPTED", NOW, fixture.inviter(), fixture.invited(), UUID.randomUUID(), LinkInvitationChangedEvent.Cause.USER_ACTION));
    });
    assertEquals(1, notifications(fixture));
  }

  private vn.edufit.shared.auth.CurrentUser actor(UUID userId, String role) {
    var user = org.mockito.Mockito.mock(vn.edufit.shared.auth.CurrentUser.class);
    org.mockito.Mockito.when(user.getUserId()).thenReturn(userId);
    org.mockito.Mockito.when(user.hasRole(org.mockito.ArgumentMatchers.anyString())).thenAnswer(call -> role.equals(call.getArgument(0)));
    return user;
  }
  private void accountMetadata(Fixture fixture) {
    org.mockito.Mockito.when(iam.findUserSummaryById(fixture.inviter())).thenReturn(java.util.Optional.of(
        new vn.edufit.iam.api.dto.UserSummaryView(fixture.inviter(), "parent@test.invalid", "Parent", vn.edufit.iam.api.AccountRole.PARENT, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    var student = new vn.edufit.iam.api.dto.UserSummaryView(fixture.invited(), "student@test.invalid", "Student", vn.edufit.iam.api.AccountRole.STUDENT, vn.edufit.iam.api.AccountStatus.ACTIVE);
    org.mockito.Mockito.when(iam.findUserSummaryById(fixture.invited())).thenReturn(java.util.Optional.of(student));
    org.mockito.Mockito.when(iam.findUserSummaryByEmail("student@test.invalid")).thenReturn(java.util.Optional.of(student));
    org.mockito.Mockito.when(profile.findStudentByUserId(fixture.invited())).thenReturn(java.util.Optional.of(
        new vn.edufit.profile.api.dto.StudentSummaryDto(fixture.student(), fixture.invited(), 4, null, false)));
  }
  @Test void realInvitationEntryPointsDeliverCreatedAcceptedExactlyOnce() {
    var fixture = fixture(); accountMetadata(fixture); var jdbc = new JdbcTemplate(source);
    assertTrue(invitationService.invite(actor(fixture.inviter(), "PARENT"), "student@test.invalid"));
    UUID invitation = jdbc.queryForObject("select invitation_id from link_invitation where inviter_user_id=? and invited_user_id=?", UUID.class, fixture.inviter(), fixture.invited());
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and type='LINK_INVITED'", Integer.class, invitation));
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
        invitationService.respondInvitation(actor(fixture.invited(), "STUDENT"), invitation, true));
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
        invitationService.respondInvitation(actor(fixture.invited(), "STUDENT"), invitation, true));
    assertEquals(1, jdbc.queryForObject("select count(*) from parent_student_link where student_id=? and parent_user_id=?", Integer.class, fixture.student(), fixture.inviter()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where user_id=? and about_student_id=? and type='LINK_CONFIRMED'", Integer.class, fixture.inviter(), fixture.student()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }
  @Test void realInvitationDeclineDeliversOnceAndCooldownIsRetained() {
    var fixture = fixture(); accountMetadata(fixture); insertInvitation(fixture);
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
        invitationService.respondInvitation(actor(fixture.invited(), "STUDENT"), fixture.invitation(), false));
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
        invitationService.respondInvitation(actor(fixture.invited(), "STUDENT"), fixture.invitation(), false));
    assertEquals(1, notifications(fixture));
    var jdbc = new JdbcTemplate(source);
    assertEquals("LINK_DECLINED", jdbc.queryForObject("select type from notification where target_id=?", String.class, fixture.invitation()));
    assertEquals(fixture.inviter(), jdbc.queryForObject("select user_id from notification where target_id=?", UUID.class, fixture.invitation()));
    assertEquals(NOW.plusSeconds(7 * 86400), jdbc.queryForObject("select reinvite_after from link_invitation where invitation_id=?", java.sql.Timestamp.class, fixture.invitation()).toInstant());
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }
  @Test void realAcceptRollsBackInvitationAndLinkWhenListenerInsertFails() {
    var fixture = fixture(); accountMetadata(fixture); insertInvitation(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.execute("create function connection_test_reject_confirmed_notification() returns trigger language plpgsql as $$ begin if new.type='LINK_CONFIRMED' then raise exception 'notification rejected' using errcode='23514'; end if; return new; end; $$");
    jdbc.execute("create trigger connection_test_reject_confirmed_notification before insert on notification for each row execute function connection_test_reject_confirmed_notification()");
    try {
      assertThrows(RuntimeException.class, () -> invitationService.respondInvitation(actor(fixture.invited(), "STUDENT"), fixture.invitation(), true));
      assertEquals("PENDING", jdbc.queryForObject("select status from link_invitation where invitation_id=?", String.class, fixture.invitation()));
      assertEquals(0, jdbc.queryForObject("select count(*) from parent_student_link where student_id=? and parent_user_id=?", Integer.class, fixture.student(), fixture.inviter()));
    } finally {
      jdbc.execute("drop trigger connection_test_reject_confirmed_notification on notification");
      jdbc.execute("drop function connection_test_reject_confirmed_notification()");
    }
  }

  private record RequestFixture(UUID parent, UUID studentUser, UUID student, UUID tutorUser, UUID tutor, UUID goal, UUID request) {}
  private RequestFixture requestFixture() {
    var jdbc = new JdbcTemplate(source);
    UUID parent = UUID.randomUUID(), studentUser = UUID.randomUUID(), student = UUID.randomUUID();
    UUID tutorUser = UUID.randomUUID(), tutor = UUID.randomUUID(), goal = UUID.randomUUID(), request = UUID.randomUUID();
    for (UUID user : java.util.List.of(parent, studentUser, tutorUser))
      jdbc.update("insert into account(user_id,email,password_hash,full_name,role) values (?,?,?,?,?)",
          user, user + "@test.invalid", "test-only", "Request fixture",
          user.equals(parent) ? "PARENT" : user.equals(tutorUser) ? "TUTOR" : "STUDENT");
    jdbc.update("insert into student_profile(student_id,user_id,education_level_id) values (?,?,4)", student, studentUser);
    jdbc.update("insert into tutor_profile(tutor_id,user_id,display_name,teaching_mode,price_per_session,status) values (?,?,'Tutor','ONLINE',100000,'VERIFIED')", tutor, tutorUser);
    jdbc.update("insert into learning_goal(goal_id,student_id,subject_id,current_level,desired_level,goal_type,deadline,mode,budget_max,status) values (?,?,1,1,2,'EXAM_PREPARATION',current_date + 30,'ONLINE',100000,'ACTIVE')", goal, student);
    return new RequestFixture(parent, studentUser, student, tutorUser, tutor, goal, request);
  }
  private void mockRequestMetadata(RequestFixture fixture) {
    org.mockito.Mockito.when(iam.findUserSummaryById(fixture.studentUser())).thenReturn(java.util.Optional.of(
        new vn.edufit.iam.api.dto.UserSummaryView(fixture.studentUser(), "student@test.invalid", "Student", vn.edufit.iam.api.AccountRole.STUDENT, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    org.mockito.Mockito.when(iam.findUserSummaryById(fixture.parent())).thenReturn(java.util.Optional.of(
        new vn.edufit.iam.api.dto.UserSummaryView(fixture.parent(), "parent@test.invalid", "Parent", vn.edufit.iam.api.AccountRole.PARENT, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    org.mockito.Mockito.when(iam.findUserSummaryById(fixture.tutorUser())).thenReturn(java.util.Optional.of(
        new vn.edufit.iam.api.dto.UserSummaryView(fixture.tutorUser(), "tutor@test.invalid", "Tutor", vn.edufit.iam.api.AccountRole.TUTOR, vn.edufit.iam.api.AccountStatus.ACTIVE)));
    org.mockito.Mockito.when(profile.findStudentById(fixture.student())).thenReturn(java.util.Optional.of(
        new vn.edufit.profile.api.dto.StudentSummaryDto(fixture.student(), fixture.studentUser(), 4, null, true)));
    org.mockito.Mockito.when(profile.findTutorById(fixture.tutor())).thenReturn(java.util.Optional.of(
        new vn.edufit.profile.api.dto.TutorSummaryDto(fixture.tutor(), fixture.tutorUser(), "Tutor", null, null, null, null, null, null, null, "VERIFIED", NOW, null, 0)));
    org.mockito.Mockito.when(profile.findGoalForConnection(fixture.goal())).thenReturn(java.util.Optional.of(
        new vn.edufit.profile.api.dto.ConnectionGoalDto(fixture.goal(), fixture.student(), fixture.studentUser(), 1, 4, "EXAM_PREPARATION", null, "ACTIVE")));
    org.mockito.Mockito.when(profile.tutorTeaches(fixture.tutor(), 1, 4)).thenReturn(true);
  }

  @Test void realRequestCreationDeliversToTutorOnlyWhenStudentSends() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    var result = requestService.request(actor(fixture.studentUser(), "STUDENT"), fixture.goal(), fixture.tutor(), java.util.List.of("Tue 19:00"), null);
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class, result);
    var created = ((vn.edufit.connection.application.dto.TransitionResult.Success<vn.edufit.connection.application.dto.ConnectionRequestResult>) result).value();
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and type='CONNECTION_REQUESTED'", Integer.class, created.requestId()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=? and about_student_id=?", Integer.class, created.requestId(), fixture.tutorUser(), fixture.student()));
    assertEquals(0, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=?", Integer.class, created.requestId(), fixture.studentUser()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }

  @Test void realRequestCreationDeliversToBothTutorAndStudentWhenParentSends() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())", UUID.randomUUID(), fixture.parent(), fixture.student());
    var result = requestService.request(actor(fixture.parent(), "PARENT"), fixture.goal(), fixture.tutor(), java.util.List.of("Tue 19:00"), null);
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class, result);
    var created = ((vn.edufit.connection.application.dto.TransitionResult.Success<vn.edufit.connection.application.dto.ConnectionRequestResult>) result).value();
    assertEquals(2, jdbc.queryForObject("select count(*) from notification where target_id=? and type='CONNECTION_REQUESTED'", Integer.class, created.requestId()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=? and about_student_id=?", Integer.class, created.requestId(), fixture.tutorUser(), fixture.student()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=? and about_student_id=?", Integer.class, created.requestId(), fixture.studentUser(), fixture.student()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }

  @Test void realRequestCancellationByParentDeliversToTutorAndStudent() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())", UUID.randomUUID(), fixture.parent(), fixture.student());
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now()+interval '7 days')",
        fixture.request(), fixture.student(), fixture.tutor(), fixture.goal(), fixture.parent(), "[]");
    var result = requestService.cancelRequest(actor(fixture.parent(), "PARENT"), fixture.request());
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class, result);
    assertEquals("CANCELLED", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
    assertEquals(2, jdbc.queryForObject("select count(*) from notification where target_id=? and type='CONNECTION_CANCELLED'", Integer.class, fixture.request()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=? and about_student_id=?", Integer.class, fixture.request(), fixture.tutorUser(), fixture.student()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=? and about_student_id=?", Integer.class, fixture.request(), fixture.studentUser(), fixture.student()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }

  @Test void realRequestCancellationByStudentDeliversToTutorOnlyEvenIfParentSent() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now()+interval '7 days')",
        fixture.request(), fixture.student(), fixture.tutor(), fixture.goal(), fixture.parent(), "[]");
    var result = requestService.cancelRequest(actor(fixture.studentUser(), "STUDENT"), fixture.request());
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class, result);
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and type='CONNECTION_CANCELLED'", Integer.class, fixture.request()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=?", Integer.class, fixture.request(), fixture.tutorUser()));
    assertEquals(0, jdbc.queryForObject("select count(*) from notification where target_id=? and user_id=?", Integer.class, fixture.request(), fixture.studentUser()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }

  @Test void realRequestCancellationTerminalRetryDoesNotDuplicateNotification() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now()+interval '7 days')",
        fixture.request(), fixture.student(), fixture.tutor(), fixture.goal(), fixture.studentUser(), "[]");
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Success.class,
        requestService.cancelRequest(actor(fixture.studentUser(), "STUDENT"), fixture.request()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=?", Integer.class, fixture.request()));
    assertInstanceOf(vn.edufit.connection.application.dto.TransitionResult.Conflict.class,
        requestService.cancelRequest(actor(fixture.studentUser(), "STUDENT"), fixture.request()));
    assertEquals(1, jdbc.queryForObject("select count(*) from notification where target_id=?", Integer.class, fixture.request()));
    org.mockito.Mockito.verifyNoInteractions(oldSink);
  }

  @Test void realRequestCreationRollsBackBothNotificationsAndRequestWhenSecondNotificationFails() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())", UUID.randomUUID(), fixture.parent(), fixture.student());
    jdbc.execute("create function connection_test_reject_student_request_notification() returns trigger language plpgsql as $$ begin if new.type='CONNECTION_REQUESTED' and new.user_id='" + fixture.studentUser() + "' then raise exception 'second notification rejected' using errcode='23514'; end if; return new; end; $$");
    jdbc.execute("create trigger connection_test_reject_student_request_notification before insert on notification for each row execute function connection_test_reject_student_request_notification()");
    try {
      assertThrows(RuntimeException.class, () -> requestService.request(actor(fixture.parent(), "PARENT"), fixture.goal(), fixture.tutor(), java.util.List.of("Tue 19:00"), null));
      assertEquals(0, jdbc.queryForObject("select count(*) from connection_request where student_id=? and tutor_id=?", Integer.class, fixture.student(), fixture.tutor()));
      assertEquals(0, jdbc.queryForObject("select count(*) from notification where about_student_id=?", Integer.class, fixture.student()));
    } finally {
      jdbc.execute("drop trigger connection_test_reject_student_request_notification on notification");
      jdbc.execute("drop function connection_test_reject_student_request_notification()");
    }
  }

  @Test void realRequestCancellationRollsBackWhenNotificationFails() {
    var fixture = requestFixture(); mockRequestMetadata(fixture); var jdbc = new JdbcTemplate(source);
    jdbc.update("insert into connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,desired_slots,expires_at) values (?,?,?,?,?,cast(? as jsonb),now()+interval '7 days')",
        fixture.request(), fixture.student(), fixture.tutor(), fixture.goal(), fixture.studentUser(), "[]");
    jdbc.execute("create function connection_test_reject_cancel_notification() returns trigger language plpgsql as $$ begin if new.type='CONNECTION_CANCELLED' then raise exception 'cancel notification rejected' using errcode='23514'; end if; return new; end; $$");
    jdbc.execute("create trigger connection_test_reject_cancel_notification before insert on notification for each row execute function connection_test_reject_cancel_notification()");
    try {
      assertThrows(RuntimeException.class, () -> requestService.cancelRequest(actor(fixture.studentUser(), "STUDENT"), fixture.request()));
      assertEquals("PENDING", jdbc.queryForObject("select status from connection_request where request_id=?", String.class, fixture.request()));
      assertEquals(0, jdbc.queryForObject("select count(*) from notification where target_id=?", Integer.class, fixture.request()));
    } finally {
      jdbc.execute("drop trigger connection_test_reject_cancel_notification on notification");
      jdbc.execute("drop function connection_test_reject_cancel_notification()");
    }
  }

  private String sqlState(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause())
      if (cause instanceof SQLException sql) return sql.getSQLState();
    throw new AssertionError("Expected actual PostgreSQL constraint failure", failure);
  }
  static class Gate {
    final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
  }
  static class CommitProbe {
    volatile Gate gate;
    final AtomicBoolean sawTransaction = new AtomicBoolean();
    @Order(-100)
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = false)
    public void observe(LinkInvitationChangedEvent event) {
      sawTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
      Gate current = gate;
      if (current == null) return;
      current.entered.countDown();
      try { if (!current.release.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("Listener gate timed out"); }
      catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
    }
  }

  @Configuration
  @EnableTransactionManagement
  @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackageClasses = {
      NotificationRepository.class,
      vn.edufit.connection.infra.persistence.repository.LinkInvitationRepository.class,
      vn.edufit.connection.infra.persistence.repository.ConnectionRequestRepository.class,
      vn.edufit.connection.infra.persistence.repository.TutoringClassRepository.class,
      vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository.class
  })
  @Import({ConnectionTransitionListener.class, ConnectionNotificationWriter.class,
      vn.edufit.connection.application.service.LinkInvitationService.class,
      vn.edufit.connection.application.service.InvitationQuotaService.class,
      vn.edufit.connection.infra.locking.PostgresConnectionLock.class,
      vn.edufit.connection.application.service.ConnectionRequestService.class,
      vn.edufit.connection.application.service.ConnectionFacadeImpl.class})
  static class Config {
    @Bean DataSource dataSource() {
      var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate(); return source;
    }
    @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(source);
      factory.setPackagesToScan("vn.edufit.notification", "vn.edufit.connection.infra.persistence.entity");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate")); return factory;
    }
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
    @Bean java.time.Clock clock() { return java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC); }
    @Bean @Primary EntityManager entityManager(EntityManagerFactory factory) { return SharedEntityManagerCreator.createSharedEntityManager(factory); }
    @Bean vn.edufit.iam.api.IamFacade iam() { return org.mockito.Mockito.mock(vn.edufit.iam.api.IamFacade.class); }
    @Bean vn.edufit.profile.api.ProfileFacade profile() { return org.mockito.Mockito.mock(vn.edufit.profile.api.ProfileFacade.class); }
    @Bean vn.edufit.shared.notification.NotificationSink oldSink() { return org.mockito.Mockito.mock(vn.edufit.shared.notification.NotificationSink.class); }
    @Bean vn.edufit.connection.application.policy.ConnectionRequestInput connectionRequestInput() {
      return new vn.edufit.connection.application.policy.ConnectionRequestInput("legacy-compatible");
    }
    @Bean CommitProbe probe() { return new CommitProbe(); }
  }
}
