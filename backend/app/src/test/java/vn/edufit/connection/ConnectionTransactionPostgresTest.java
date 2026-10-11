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
    doAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      throw new IllegalStateException("injected audit failure");
    }).when(audit).record(admin, "LINK_REVOKED", "PARENT_STUDENT_LINK", link, "reason");
    assertThrows(IllegalStateException.class, () -> links.revokeLink(actor(admin, "ADMIN"), link, "reason"));
    assertEquals("CONFIRMED", jdbc.queryForObject("select status from parent_student_link where link_id = ?", String.class, link));
    verifyNoInteractions(notifications);
    reset(audit);
    doAnswer(call -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      return null;
    }).when(notifications).notify(eq(parent), eq(student), eq("LINK_REVOKED"), anyString(), anyString(), eq("PARENT_STUDENT_LINK"), eq(link));
    links.revokeLink(actor(admin, "ADMIN"), link, "reason");
    assertEquals("REVOKED", jdbc.queryForObject("select status from parent_student_link where link_id = ?", String.class, link));
    verify(audit).record(admin, "LINK_REVOKED", "PARENT_STUDENT_LINK", link, "reason");
    verify(notifications).notify(eq(parent), eq(student), eq("LINK_REVOKED"), anyString(), anyString(), eq("PARENT_STUDENT_LINK"), eq(link));
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
    var created = requests.respondRequest(actor, request, true, null);
    assertNotNull(created.classId());
    assertEquals("ACCEPTED", jdbc.queryForObject("select status from connection_request where request_id = ?", String.class, request));
    assertEquals("ACTIVE", jdbc.queryForObject("select status from class where class_id = ?", String.class, created.classId()));
    assertEquals(1, jdbc.queryForObject("select count(*) from class where connection_request_id = ?", Integer.class, request));
    verify(notifications).notify(eq(studentUser), eq(student), eq("CONNECTION_ACCEPTED"), anyString(), anyString(), eq("CLASS"), eq(created.classId()));
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
