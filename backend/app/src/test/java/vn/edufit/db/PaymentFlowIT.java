package vn.edufit.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.payment.application.*;
import vn.edufit.payment.infra.PaymentStore;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.*;
import vn.edufit.scheduling.api.SessionFacade;
import vn.edufit.scheduling.api.dto.SessionSummaryView;
import vn.edufit.shared.auth.CurrentUser;

class PaymentFlowIT {
  static PostgreSQLContainer<?> container;
  static DriverManagerDataSource datasource;
  JdbcTemplate db;
  PaymentStore store;
  SessionPaymentService service;
  ProfileFacade profiles;
  ConnectionFacade connections;
  SessionFacade sessions;
  UUID studentUser, studentId, tutorUser, tutorId, parentUser, adminUser, classId, sessionId;
  Instant end = Instant.parse("2026-10-21T12:00:00Z");
  AtomicReference<Instant> now = new AtomicReference<>();

  @BeforeAll
  static void database() {
    String url = System.getenv("PAYMENT_TEST_JDBC");
    String user = System.getenv().getOrDefault("PAYMENT_TEST_USER", "payment_test");
    String pass = System.getenv().getOrDefault("PAYMENT_TEST_PASSWORD", "");
    if (url == null) {
      container = new PostgreSQLContainer<>("postgres:17-alpine");
      container.start();
      url = container.getJdbcUrl();
      user = container.getUsername();
      pass = container.getPassword();
    }
    String schema = "payment_it_" + UUID.randomUUID().toString().replace("-", "");
    datasource =
        new DriverManagerDataSource(
            url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, pass);
    Flyway flyway =
        Flyway.configure()
            .dataSource(datasource)
            .schemas(schema)
            .locations("classpath:db/migration")
            .load();
    assertEquals(16, flyway.migrate().migrationsExecuted);
    flyway.validate();
    assertEquals(0, flyway.migrate().migrationsExecuted);
  }

  @AfterAll
  static void close() {
    if (container != null) container.stop();
  }

  @BeforeEach
  void setup() {
    db = new JdbcTemplate(datasource);
    store = new PaymentStore(db, new DataSourceTransactionManager(datasource));
    db.execute(
        "truncate"
            + " payment_entry,payment_audit,payment_charge,payment_topup,payment_withdrawal,payment_wallet");
    if (db.queryForObject("select count(*) from class", Long.class) == 0) {
      UUID student = db.queryForObject("select student_id from learning_goal limit 1", UUID.class);
      UUID tutor = db.queryForObject("select tutor_id from tutor_profile limit 1", UUID.class);
      UUID goal =
          db.queryForObject(
              "select goal_id from learning_goal where student_id=? limit 1", UUID.class, student);
      UUID sender =
          db.queryForObject(
              "select user_id from student_profile where student_id=?", UUID.class, student);
      UUID request = UUID.randomUUID();
      db.update(
          "insert into"
              + " connection_request(request_id,student_id,tutor_id,goal_id,sent_by_user_id,status,expires_at)"
              + " values (?,?,?,?,?,'ACCEPTED',now())",
          request,
          student,
          tutor,
          goal,
          sender);
      db.update(
          "insert into class(connection_request_id,student_id,tutor_id,goal_id) values (?,?,?,?)",
          request,
          student,
          tutor,
          goal);
    }
    var cls =
        db.queryForMap(
            "select c.*,s.user_id student_user,t.user_id tutor_user from class c join"
                + " student_profile s using(student_id) join tutor_profile t using(tutor_id) limit"
                + " 1");
    classId = (UUID) cls.get("class_id");
    studentId = (UUID) cls.get("student_id");
    tutorId = (UUID) cls.get("tutor_id");
    studentUser = (UUID) cls.get("student_user");
    tutorUser = (UUID) cls.get("tutor_user");
    parentUser =
        db.queryForObject("select user_id from account where role='PARENT' limit 1", UUID.class);
    adminUser =
        db.queryForObject("select user_id from account where role='ADMIN' limit 1", UUID.class);
    sessionId = UUID.randomUUID();
    db.update(
        "insert into"
            + " tutoring_session(session_id,class_id,tutor_id,student_id,start_at,end_at,mode,status,proposed_by,expires_at)"
            + " values (?,?,?,?,?,?,'ONLINE','COMPLETED',?,?)",
        sessionId,
        classId,
        tutorId,
        studentId,
        Timestamp.from(end.minusSeconds(5400)),
        Timestamp.from(end),
        tutorUser,
        Timestamp.from(end));
    profiles = mock(ProfileFacade.class);
    connections = mock(ConnectionFacade.class);
    sessions = mock(SessionFacade.class);
    when(profiles.findStudentByUserId(studentUser))
        .thenReturn(Optional.of(new StudentSummaryDto(studentId, studentUser, null, null, true)));
    when(profiles.findTutorById(tutorId))
        .thenReturn(
            Optional.of(
                new TutorSummaryDto(
                    tutorId,
                    tutorUser,
                    "Tutor",
                    null,
                    null,
                    "ONLINE",
                    null,
                    300000L,
                    null,
                    null,
                    "VERIFIED",
                    null,
                    null,
                    0)));
    when(connections.hasConfirmedParentLink(parentUser, studentId)).thenReturn(true);
    when(sessions.findSessionById(sessionId))
        .thenReturn(
            Optional.of(
                new SessionSummaryView(
                    sessionId,
                    classId,
                    tutorId,
                    studentId,
                    end.minusSeconds(5400),
                    end,
                    "ONLINE",
                    "COMPLETED",
                    false,
                    end.minusSeconds(7200))));
    now.set(end.minusSeconds(3600));
    Clock clock =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId z) {
            return this;
          }

          public Instant instant() {
            return now.get();
          }
        };
    service =
        new SessionPaymentService(
            store,
            new PaymentAccess(profiles, connections),
            sessions,
            profiles,
            connections,
            clock);
    store.locked(
        () -> {
          store.move(parentUser, 1000000, 0, "seed:" + sessionId, "TOPUP", sessionId);
          return null;
        });
  }

  CurrentUser actor(UUID id, String role) {
    var a = mock(CurrentUser.class);
    when(a.getUserId()).thenReturn(id);
    when(a.hasRole(anyString())).thenAnswer(i -> role.equals(i.getArgument(0)));
    return a;
  }

  void fund() {
    service.fund(actor(parentUser, "PARENT"), sessionId, 300000);
    now.set(end);
  }

  void confirmTutor() {
    service.act(actor(tutorUser, "TUTOR"), sessionId, "confirm", null, null);
  }

  void dispute() {
    service.act(actor(studentUser, "STUDENT"), sessionId, "dispute", "Thiếu giờ", null);
  }

  @Test
  void parentFundsStudentConfirmsAndConcurrentConfirmPaysTutorOnce() throws Exception {
    fund();
    confirmTutor();
    try (var pool = Executors.newFixedThreadPool(4)) {
      var jobs = new ArrayList<Future<?>>();
      for (int i = 0; i < 4; i++)
        jobs.add(
            pool.submit(
                () ->
                    service.act(actor(studentUser, "STUDENT"), sessionId, "confirm", null, null)));
      for (var f : jobs) f.get(15, TimeUnit.SECONDS);
    }
    assertEquals(700000, store.wallet(parentUser).available());
    assertEquals(0, store.wallet(parentUser).held());
    assertEquals(270000, store.wallet(tutorUser).available());
    assertEquals(
        30000,
        db.queryForObject(
            "select sum(available_delta) from payment_entry where kind='COMMISSION'", Long.class));
  }

  @Test
  void expiredDisputeReturnsExactlyToParentAndNeverPaysTutor() {
    fund();
    confirmTutor();
    dispute();
    now.set(end.plusSeconds(86400));
    service.processDeadlines();
    service.processDeadlines();
    assertEquals("EXPIRED", store.find(sessionId).orElseThrow().state().status);
    assertEquals(1000000, store.wallet(parentUser).available());
    assertEquals(0, store.wallet(parentUser).held());
    assertEquals(0, store.wallet(studentUser).available());
    assertEquals(0, store.wallet(tutorUser).available());
  }

  @Test
  void matchingReportsWaitForAdminAndPartialSettlementIsAtomic() {
    fund();
    dispute();
    service.act(actor(studentUser, "STUDENT"), sessionId, "report", "Đồng ý 100k", 100000L);
    service.act(actor(tutorUser, "TUTOR"), sessionId, "report", "Đã thống nhất", 100000L);
    now.set(end.plusSeconds(200000));
    service.processDeadlines();
    assertEquals(300000, store.wallet(parentUser).held());
    assertEquals(0, store.wallet(tutorUser).available());
    service.review(actor(adminUser, "ADMIN"), sessionId, true, "Đã kiểm tra hai báo cáo");
    assertEquals(900000, store.wallet(parentUser).available());
    assertEquals(90000, store.wallet(tutorUser).available());
    assertEquals(0, store.wallet(parentUser).held());
    assertThrows(
        RuntimeException.class,
        () -> service.review(actor(adminUser, "ADMIN"), sessionId, true, "Duyệt lại"));
  }

  @Test
  void autoAcceptAtOneHourDoesNotOverrideDispute() {
    fund();
    confirmTutor();
    dispute();
    now.set(end.plusSeconds(3600));
    service.processDeadlines();
    assertEquals("DISPUTED", store.find(sessionId).orElseThrow().state().status);
    assertEquals(0, store.wallet(tutorUser).available());
  }

  @Test
  void autoAcceptAtOneHourSettlesWhenTutorConfirmed() {
    fund();
    confirmTutor();
    now.set(end.plusSeconds(3600));
    service.processDeadlines();
    assertEquals(270000, store.wallet(tutorUser).available());
    assertTrue(store.find(sessionId).orElseThrow().state().autoAccepted);
  }

  @Test
  void revokedParentAndUnrelatedAccountCannotSpendOrConfirm() {
    when(connections.hasConfirmedParentLink(parentUser, studentId)).thenReturn(false);
    assertThrows(
        RuntimeException.class, () -> service.fund(actor(parentUser, "PARENT"), sessionId, 300000));
    assertEquals(1000000, store.wallet(parentUser).available());
  }

  @Test
  void insufficientFundsRollsBackChargeAndReservation() {
    store.locked(
        () -> {
          store.move(parentUser, -900000, 0, "remove:" + sessionId, "TEST", sessionId);
          return null;
        });
    assertThrows(RuntimeException.class, this::fund);
    assertTrue(store.find(sessionId).isEmpty());
    assertEquals(100000, store.wallet(parentUser).available());
  }

  @Test
  void duplicateDepositCannotCreditTwiceOrReuseBankReference() {
    var first = store.locked(() -> store.createTopup(parentUser, 100000, "MANUAL", end));
    UUID id = (UUID) first.get("id");
    store.locked(
        () -> {
          store.creditTopup(id, "REALBANKREF1", adminUser);
          store.creditTopup(id, "REALBANKREF1", adminUser);
          return null;
        });
    assertEquals(1100000, store.wallet(parentUser).available());
    var second = store.locked(() -> store.createTopup(parentUser, 100000, "MANUAL", end));
    assertThrows(
        RuntimeException.class,
        () ->
            store.locked(
                () -> {
                  store.creditTopup((UUID) second.get("id"), " REALBANKREF1 ", adminUser);
                  return null;
                }));
    assertEquals(1100000, store.wallet(parentUser).available());
  }

  @Test
  void adminCannotChargeACancelledSessionEvenWithMatchingReports() {
    fund();
    dispute();
    service.act(actor(studentUser, "STUDENT"), sessionId, "report", "Đồng ý", 100000L);
    service.act(actor(tutorUser, "TUTOR"), sessionId, "report", "Đồng ý", 100000L);
    when(sessions.findSessionById(sessionId))
        .thenReturn(
            Optional.of(
                new SessionSummaryView(
                    sessionId,
                    classId,
                    tutorId,
                    studentId,
                    end.minusSeconds(5400),
                    end,
                    "ONLINE",
                    "CANCELLED",
                    false,
                    end.minusSeconds(7200))));
    assertThrows(
        RuntimeException.class,
        () -> service.review(actor(adminUser, "ADMIN"), sessionId, true, "Duyệt"));
    assertEquals(300000, store.wallet(parentUser).held());
    service.review(actor(adminUser, "ADMIN"), sessionId, false, "Hủy buổi, trả ví");
    assertEquals(1000000, store.wallet(parentUser).available());
  }

  @Test
  void withdrawalRejectionReturnsFundsAndCannotBeProcessedTwice() {
    store.locked(
        () -> {
          store.move(tutorUser, 270000, 0, "tutor-seed:" + sessionId, "TEST", sessionId);
          return null;
        });
    var w =
        store.locked(() -> store.createWithdrawal(tutorUser, 200000, "Bank", "12345678", "Tutor"));
    assertEquals(70000, store.wallet(tutorUser).available());
    store.locked(
        () -> {
          store.reviewWithdrawal((UUID) w.get("id"), false, null, "Sai tài khoản", adminUser);
          return null;
        });
    assertEquals(270000, store.wallet(tutorUser).available());
    assertEquals(0, store.wallet(tutorUser).held());
    assertThrows(
        RuntimeException.class,
        () ->
            store.locked(
                () -> {
                  store.reviewWithdrawal(
                      (UUID) w.get("id"), true, "REFBANK2", "Duyệt lại", adminUser);
                  return null;
                }));
  }
}
