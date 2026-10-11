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
@SpringJUnitConfig(ConnectionLockPostgresTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConnectionLockPostgresTest {
  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired ConnectionLock lock;
  @Autowired InvitationQuotaService quota;
  @Autowired JpaTransactionManager transactionManager;
  @jakarta.persistence.PersistenceContext EntityManager em;

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
