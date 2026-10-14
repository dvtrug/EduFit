package vn.edufit.audit;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import vn.edufit.shared.audit.*;

@Testcontainers
@SpringJUnitConfig(AuditPersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditPersistenceTest {
  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
  @Autowired AuditSink sink;
  @Autowired DataSource source;
  @Autowired JpaTransactionManager manager;
  private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test void structuredSnapshotRetainsAccountsReasonAndBusinessTime() {
    UUID target = UUID.randomUUID(); Instant time = Instant.parse("2026-10-11T00:00:00Z");
    var metadata = new java.util.HashMap<String,String>();
    metadata.put("parentUserId", UUID.randomUUID().toString());
    metadata.put("studentId", UUID.randomUUID().toString());
    metadata.put("studentUserId", ACTOR.toString());
    var record = new AuditRecord(ACTOR,"LINK_REVOKED","PARENT_STUDENT_LINK",target,"private reason",time,metadata);
    metadata.clear();
    new TransactionTemplate(manager).executeWithoutResult(status -> sink.record(record));
    var jdbc = new JdbcTemplate(source);
    assertEquals(time,jdbc.queryForObject("select created_at from audit_log where target_id=?",java.sql.Timestamp.class,target).toInstant());
    assertEquals("private reason",jdbc.queryForObject("select reason from audit_log where target_id=?",String.class,target));
    record.metadata().forEach((key,value) -> assertEquals(value,jdbc.queryForObject("select metadata ->> ? from audit_log where target_id=?",String.class,key,target)));
    assertThrows(UnsupportedOperationException.class, () -> record.metadata().put("changed","value"));
  }
  @Test void legacyApiStillPersistsWithEmptyMetadata() {
    UUID target = UUID.randomUUID();
    new TransactionTemplate(manager).executeWithoutResult(status -> sink.record(ACTOR,"LEGACY","ACCOUNT",target,null));
    assertEquals("{}",new JdbcTemplate(source).queryForObject("select metadata::text from audit_log where target_id=?",String.class,target));
  }
  @Test void outerRollbackDiscardsStructuredAudit() {
    UUID target = UUID.randomUUID();
    new TransactionTemplate(manager).executeWithoutResult(status -> {
      sink.record(new AuditRecord(ACTOR,"LINK_REVOKED","PARENT_STUDENT_LINK",target,null,Instant.now(),Map.of("studentUserId",ACTOR.toString())));
      status.setRollbackOnly();
    });
    assertEquals(0,new JdbcTemplate(source).queryForObject("select count(*) from audit_log where target_id=?",Integer.class,target));
  }
  @Configuration @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = AuditLogRepository.class)
  @Import(JpaAuditSink.class)
  static class Config {
    @Bean DataSource dataSource() {
      var source = new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();return source;
    }
    @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);
      factory.setPackagesToScan("vn.edufit.audit");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));return factory;
    }
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
  }
}
