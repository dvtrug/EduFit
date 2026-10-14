package vn.edufit.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
  @Autowired vn.edufit.connection.application.service.ParentStudentLinkService links;
  @Autowired vn.edufit.profile.api.ProfileFacade profile;
  @Autowired vn.edufit.iam.api.IamFacade iam;
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
  private record LinkFixture(UUID link, UUID parent, UUID student, UUID studentUser, UUID admin) {}
  private LinkFixture linkFixture() {
    var fixture = new LinkFixture(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
    var jdbc = new JdbcTemplate(source);
    for (UUID id : java.util.List.of(fixture.parent(),fixture.studentUser(),fixture.admin()))
      jdbc.update("insert into account(user_id,email,password_hash,full_name,role) values (?,?,'test-only','Audit fixture',?)",id,id+"@test.invalid",id.equals(fixture.parent())?"PARENT":id.equals(fixture.admin())?"ADMIN":"STUDENT");
    jdbc.update("insert into student_profile(student_id,user_id) values (?,?)",fixture.student(),fixture.studentUser());
    jdbc.update("insert into parent_student_link(link_id,parent_user_id,student_id,confirmed_at) values (?,?,?,now())",fixture.link(),fixture.parent(),fixture.student());
    when(profile.findStudentById(fixture.student())).thenReturn(java.util.Optional.of(new vn.edufit.profile.api.dto.StudentSummaryDto(fixture.student(),fixture.studentUser(),4,null,true)));
    when(iam.findUserSummaryById(fixture.admin())).thenReturn(java.util.Optional.of(new vn.edufit.iam.api.dto.UserSummaryView(fixture.admin(),"admin@test.invalid","Admin",vn.edufit.iam.api.AccountRole.ADMIN,vn.edufit.iam.api.AccountStatus.ACTIVE)));
    return fixture;
  }
  private vn.edufit.shared.auth.CurrentUser admin(LinkFixture fixture) {
    var actor=mock(vn.edufit.shared.auth.CurrentUser.class);when(actor.getUserId()).thenReturn(fixture.admin());
    when(actor.hasRole(anyString())).thenAnswer(call -> "ADMIN".equals(call.getArgument(0)));return actor;
  }
  @Test void realRevokeAndSnapshotCommitTogetherWithoutDeletingStudentData() {
    var fixture=linkFixture();links.revokeLink(admin(fixture),fixture.link(),"private reason");
    var jdbc=new JdbcTemplate(source);
    assertEquals("REVOKED",jdbc.queryForObject("select status from parent_student_link where link_id=?",String.class,fixture.link()));
    assertEquals(fixture.studentUser().toString(),jdbc.queryForObject("select metadata ->> 'studentUserId' from audit_log where target_id=?",String.class,fixture.link()));
    assertEquals(1,jdbc.queryForObject("select count(*) from student_profile where student_id=?",Integer.class,fixture.student()));
  }
  @Test void databaseAuditFailureRollsBackRealRevocation() {
    var fixture=linkFixture();var jdbc=new JdbcTemplate(source);
    jdbc.execute("create function reject_connection_audit() returns trigger language plpgsql as $$ begin raise exception 'audit rejected' using errcode='23514'; end; $$");
    jdbc.execute("create trigger reject_connection_audit before insert on audit_log for each row execute function reject_connection_audit()");
    try {
      assertThrows(RuntimeException.class,()->links.revokeLink(admin(fixture),fixture.link(),"reason"));
      assertEquals("CONFIRMED",jdbc.queryForObject("select status from parent_student_link where link_id=?",String.class,fixture.link()));
      assertEquals(0,jdbc.queryForObject("select count(*) from audit_log where target_id=?",Integer.class,fixture.link()));
    } finally { jdbc.execute("drop trigger reject_connection_audit on audit_log");jdbc.execute("drop function reject_connection_audit()"); }
  }
  @Configuration @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = {AuditLogRepository.class, vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository.class})
  @Import({JpaAuditSink.class,vn.edufit.connection.application.service.ParentStudentLinkService.class,vn.edufit.connection.infra.locking.PostgresConnectionLock.class})
  static class Config {
    @Bean DataSource dataSource() {
      var source = new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();return source;
    }
    @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();factory.setDataSource(source);
      factory.setPackagesToScan("vn.edufit.audit","vn.edufit.connection.infra.persistence.entity");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));return factory;
    }
    @Bean java.time.Clock clock() { return java.time.Clock.systemUTC(); }
    @Bean @Primary jakarta.persistence.EntityManager entityManager(EntityManagerFactory factory) { return SharedEntityManagerCreator.createSharedEntityManager(factory); }
    @Bean vn.edufit.iam.api.IamFacade iam() { return mock(vn.edufit.iam.api.IamFacade.class); }
    @Bean vn.edufit.profile.api.ProfileFacade profile() { return mock(vn.edufit.profile.api.ProfileFacade.class); }
    @Bean vn.edufit.shared.notification.NotificationSink notifications() { return mock(vn.edufit.shared.notification.NotificationSink.class); }
    @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
  }
}
