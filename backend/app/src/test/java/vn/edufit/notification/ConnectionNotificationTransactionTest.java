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
  @PersistenceContext EntityManager em;

  @BeforeEach void resetProbe() { probe.gate = null; probe.sawTransaction.set(false); }
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
  @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackageClasses = NotificationRepository.class)
  @Import({ConnectionTransitionListener.class, ConnectionNotificationWriter.class})
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
    @Bean CommitProbe probe() { return new CommitProbe(); }
  }
}
