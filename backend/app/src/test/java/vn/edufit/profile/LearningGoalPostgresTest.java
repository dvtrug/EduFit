package vn.edufit.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.json.JsonMapper;
import vn.edufit.profile.web.LearningGoalController;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.web.advice.GlobalExceptionHandler;

@Testcontainers
@SpringJUnitConfig(LearningGoalPostgresTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LearningGoalPostgresTest {
  static final String ROOT = "/api/v1/profiles/students/me/goals";

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired ApplicationContext context;
  @Autowired DataSource source;
  @Autowired CurrentUser user;
  JdbcTemplate jdbc;
  MockMvc mvc;
  UUID accountId, studentId;

  @BeforeEach
  void setup() {
    jdbc = new JdbcTemplate(source);
    jdbc.execute("truncate account, subject, education_level cascade");
    jdbc.update("insert into subject(subject_id,name) values(1,'Math')");
    jdbc.update("insert into subject(subject_id,name,is_active) values(2,'Retired',false)");
    jdbc.update("insert into education_level(level_id,name,sort_order) values(1,'Grade',1)");
    accountId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    account(accountId, "STUDENT");
    jdbc.update(
        "insert into student_profile(student_id,user_id) values(?,?)", studentId, accountId);
    reset(user);
    actor(accountId, "STUDENT");
    mvc =
        MockMvcBuilders.standaloneSetup(
                context.getBean(LearningGoalController.class),
                context.getBean(vn.edufit.discovery.web.DiscoveryController.class))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createListsOwnGoalAndReportsMissingInputsWithoutCompletingProfile() throws Exception {
    UUID id = create();
    mvc.perform(get(ROOT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].goalId").value(id.toString()))
        .andExpect(jsonPath("$.data[0].readyForMatching").value(false))
        .andExpect(jsonPath("$.data[0].readinessReasons[0]").value("EDUCATION_LEVEL_REQUIRED"))
        .andExpect(jsonPath("$.data[0].availabilitySlots").isEmpty());
    assertEquals(
        studentId,
        jdbc.queryForObject(
            "select student_id from learning_goal where goal_id=?", UUID.class, id));
    assertFalse(
        jdbc.queryForObject(
            "select profile_complete from student_profile where student_id=?",
            Boolean.class,
            studentId));
  }

  @Test
  void updateAndArchivePreserveGoalAndExistingHistory() throws Exception {
    UUID id = create();
    jdbc.update(
        "insert into matching_run_log(user_id,student_id,goal_id,result_count) values(?,?,?,0)",
        accountId,
        studentId,
        id);
    mvc.perform(
            put(ROOT + "/" + id)
                .contentType("application/json")
                .content(payload().replace("200000", "350000")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.budgetMax").value(350000));
    mvc.perform(post(ROOT + "/" + id + "/archive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
        .andExpect(jsonPath("$.data.readyForMatching").value(false));
    mvc.perform(post(ROOT + "/" + id + "/archive")).andExpect(status().isOk());
    mvc.perform(put(ROOT + "/" + id).contentType("application/json").content(payload()))
        .andExpect(status().isBadRequest());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from matching_run_log where goal_id=?", Integer.class, id));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PARENT", "TUTOR", "ADMIN", "GUEST"})
  void otherRolesCannotReadOrWriteStudentGoals(String role) throws Exception {
    UUID id = create();
    actor(accountId, role);
    mvc.perform(get(ROOT)).andExpect(status().isForbidden());
    mvc.perform(post(ROOT).contentType("application/json").content(payload()))
        .andExpect(status().isForbidden());
    mvc.perform(put(ROOT + "/" + id).contentType("application/json").content(payload()))
        .andExpect(status().isForbidden());
    mvc.perform(post(ROOT + "/" + id + "/archive")).andExpect(status().isForbidden());
  }

  @Test
  void otherStudentCannotMutateGoalOrSeeItInOwnList() throws Exception {
    UUID id = create(), other = UUID.randomUUID(), otherStudent = UUID.randomUUID();
    account(other, "STUDENT");
    jdbc.update("insert into student_profile(student_id,user_id) values(?,?)", otherStudent, other);
    actor(other, "STUDENT");
    mvc.perform(get(ROOT)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    mvc.perform(put(ROOT + "/" + id).contentType("application/json").content(payload()))
        .andExpect(status().isNotFound());
    mvc.perform(post(ROOT + "/" + id + "/archive")).andExpect(status().isNotFound());
    assertEquals(
        "ACTIVE",
        jdbc.queryForObject("select status from learning_goal where goal_id=?", String.class, id));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"subjectId\":1|\"subjectId\":999", "\"subjectId\":1|\"subjectId\":2",
        "\"currentLevel\":1|\"currentLevel\":3", "\"budgetMax\":200000|\"budgetMax\":0",
        "\"budgetMin\":0|\"budgetMin\":-1", "\"budgetMin\":0|\"budgetMin\":300000",
        "\"mode\":\"ONLINE\"|\"mode\":\"OFFLINE\"", "\"mode\":\"ONLINE\"|\"mode\":\"BOTH\"",
        "\"subjectId\":1|\"subjectId\":null", "\"desiredLevel\":3|\"desiredLevel\":null"
      })
  void invalidFieldsCannotCreateOrOverwrite(String mutation) throws Exception {
    UUID id = create();
    String[] parts = mutation.split("\\|", 2);
    String invalid = payload().replace(parts[0], parts[1]);
    mvc.perform(post(ROOT).contentType("application/json").content(invalid))
        .andExpect(status().isBadRequest());
    mvc.perform(put(ROOT + "/" + id).contentType("application/json").content(invalid))
        .andExpect(status().isBadRequest());
    assertEquals(1, jdbc.queryForObject("select count(*) from learning_goal", Integer.class));
    assertEquals(
        200000L,
        jdbc.queryForObject(
            "select budget_max from learning_goal where goal_id=?", Long.class, id));
  }

  @Test
  void todayAndPastDeadlineRejectedButSmallintScaleIsNotInvented() throws Exception {
    for (LocalDate date : new LocalDate[] {LocalDate.now(), LocalDate.now().minusDays(1)}) {
      mvc.perform(
              post(ROOT)
                  .contentType("application/json")
                  .content(
                      payload().replace(LocalDate.now().plusDays(30).toString(), date.toString())))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post(ROOT)
                .contentType("application/json")
                .content(
                    payload()
                        .replace("\"currentLevel\":1", "\"currentLevel\":10")
                        .replace("\"desiredLevel\":3", "\"desiredLevel\":11")))
        .andExpect(status().isCreated());
  }

  @Test
  void missingProfileIsReportedInsteadOfCreatingImplicitOwner() throws Exception {
    jdbc.update("delete from student_profile where student_id=?", studentId);
    mvc.perform(post(ROOT).contentType("application/json").content(payload()))
        .andExpect(status().isNotFound());
    mvc.perform(get(ROOT)).andExpect(status().isNotFound());
  }

  @Test
  void offlineAreaIsTrimmedAndOptionalMinimumCanBeOmitted() throws Exception {
    mvc.perform(
            post(ROOT)
                .contentType("application/json")
                .content(
                    payload()
                        .replace("\"ONLINE\"", "\"BOTH\"")
                        .replace("\"area\":null", "\"area\":\" Hanoi \"")
                        .replace("\"budgetMin\":0", "\"budgetMin\":null")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.area").value("Hanoi"));
    assertEquals("Hanoi", jdbc.queryForObject("select area from learning_goal", String.class));
  }

  @Test
  void archivedGoalIsRejectedByRealMatchingAndConnectionServices() throws Exception {
    UUID id = create();
    mvc.perform(post(ROOT + "/" + id + "/archive")).andExpect(status().isOk());
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () ->
            context
                .getBean(vn.edufit.discovery.application.service.TutorMatchingService.class)
                .match(user, id, 5));
    assertThrows(
        vn.edufit.shared.exception.InvalidOperationException.class,
        () ->
            context
                .getBean(vn.edufit.connection.application.ConnectionService.class)
                .request(user, id, UUID.randomUUID(), java.util.List.of("MON 18:00"), null));
    assertEquals(0, jdbc.queryForObject("select count(*) from connection_request", Integer.class));
  }

  @Test
  void availabilityRoundTripEnablesMatchingWithoutSqlGoalOrSlotSeed() throws Exception {
    UUID id = create();
    jdbc.update("update student_profile set education_level_id=1 where student_id=?", studentId);
    mvc.perform(
            put(ROOT + "/" + id + "/availability")
                .contentType("application/json")
                .content(availability(1, "18:00", "20:00")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.readyForMatching").value(true))
        .andExpect(jsonPath("$.data.availabilitySlots[0].dayOfWeek").value(1));
    mvc.perform(get(ROOT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].readyForMatching").value(true));
    mvc.perform(
            post("/api/v1/discovery/match")
                .contentType("application/json")
                .content("{\"goalId\":\"" + id + "\",\"topN\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from matching_run_log where goal_id=?", Integer.class, id));
  }

  @Test
  void availabilityTouchingBoundariesAllowedAndEmptyPayloadClearsReadiness() throws Exception {
    UUID id = create();
    replace(
        id,
        "{\"slots\":[{\"dayOfWeek\":7,\"startTime\":\"19:00\",\"endTime\":\"20:00\"},{\"dayOfWeek\":7,\"startTime\":\"18:00\",\"endTime\":\"19:00\"}]}");
    mvc.perform(get(ROOT))
        .andExpect(jsonPath("$.data[0].availabilitySlots[0].startTime").value("18:00:00"));
    replace(id, "{\"slots\":[]}");
    mvc.perform(get(ROOT))
        .andExpect(jsonPath("$.data[0].availabilitySlots").isEmpty())
        .andExpect(jsonPath("$.data[0].readyForMatching").value(false));
    assertEquals(
        0, jdbc.queryForObject("select count(*) from goal_availability_slot", Integer.class));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "null",
        "{}",
        "{\"slots\":null}",
        "{\"slots\":[null]}",
        "{\"slots\":[{\"dayOfWeek\":0,\"startTime\":\"18:00\",\"endTime\":\"19:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":8,\"startTime\":\"18:00\",\"endTime\":\"19:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":\"20:00\",\"endTime\":\"19:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":\"19:00\",\"endTime\":\"19:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":\"23:00\",\"endTime\":\"01:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":\"18:00:01\",\"endTime\":\"19:00\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":\"18:00\",\"endTime\":\"19:00:00.001\"}]}",
        "{\"slots\":[{\"dayOfWeek\":1,\"startTime\":null,\"endTime\":\"19:00\"}]}"
      })
  void availabilityInvalidInputKeepsOldSlots(String invalid) throws Exception {
    UUID id = create();
    replace(id, availability(1, "18:00", "19:00"));
    UUID oldSlot = jdbc.queryForObject("select slot_id from goal_availability_slot", UUID.class);
    mvc.perform(
            put(ROOT + "/" + id + "/availability").contentType("application/json").content(invalid))
        .andExpect(status().isBadRequest());
    assertEquals(
        oldSlot, jdbc.queryForObject("select slot_id from goal_availability_slot", UUID.class));
  }

  @Test
  void availabilityDuplicateOverlapAndOversizedPayloadRejectedBeforeDelete() throws Exception {
    UUID id = create();
    replace(id, availability(1, "18:00", "19:00"));
    String item = "{\"dayOfWeek\":1,\"startTime\":\"18:00\",\"endTime\":\"19:00\"}";
    for (String invalid :
        java.util.List.of(
            "{\"slots\":[" + item + "," + item + "]}",
            "{\"slots\":["
                + item
                + ",{\"dayOfWeek\":1,\"startTime\":\"18:30\",\"endTime\":\"20:00\"}]}",
            "{\"slots\":[" + String.join(",", java.util.Collections.nCopies(101, item)) + "]}")) {
      mvc.perform(
              put(ROOT + "/" + id + "/availability")
                  .contentType("application/json")
                  .content(invalid))
          .andExpect(status().isBadRequest());
    }
    assertEquals(
        1, jdbc.queryForObject("select count(*) from goal_availability_slot", Integer.class));
  }

  @Test
  void availabilityCommitFailureRestoresDeletedSlots() throws Exception {
    UUID id = create();
    replace(id, availability(1, "18:00", "19:00"));
    UUID old = jdbc.queryForObject("select slot_id from goal_availability_slot", UUID.class);
    jdbc.execute(
        "create function reject_goal_slot() returns trigger language plpgsql as $$ begin raise"
            + " exception 'test deferred slot failure'; return new; end $$");
    jdbc.execute(
        "create constraint trigger reject_goal_slot after insert on goal_availability_slot"
            + " deferrable initially deferred for each row execute function reject_goal_slot()");
    try {
      mvc.perform(
              put(ROOT + "/" + id + "/availability")
                  .contentType("application/json")
                  .content(availability(2, "21:00", "22:00")))
          .andExpect(status().isInternalServerError());
      assertEquals(
          old, jdbc.queryForObject("select slot_id from goal_availability_slot", UUID.class));
    } finally {
      jdbc.execute("drop trigger reject_goal_slot on goal_availability_slot");
      jdbc.execute("drop function reject_goal_slot()");
    }
  }

  @Test
  void availabilityConcurrentReplacementsSerializeUntilTransactionCommit() throws Exception {
    UUID id = create();
    var held = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var first =
          executor.submit(
              () ->
                  new org.springframework.transaction.support.TransactionTemplate(
                          context.getBean(
                              org.springframework.transaction.PlatformTransactionManager.class))
                      .executeWithoutResult(
                          status -> {
                            replace(id, availability(1, "18:00", "19:00"));
                            held.countDown();
                            try {
                              if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                throw new AssertionError("release timeout");
                            } catch (InterruptedException ex) {
                              Thread.currentThread().interrupt();
                              throw new RuntimeException(ex);
                            }
                          }));
      try {
        assertTrue(held.await(5, java.util.concurrent.TimeUnit.SECONDS));
        var second = executor.submit(() -> replace(id, availability(2, "20:00", "21:00")));
        assertThrows(
            java.util.concurrent.TimeoutException.class,
            () -> second.get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
        release.countDown();
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        second.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(
            2,
            jdbc.queryForObject("select day_of_week from goal_availability_slot", Integer.class));
        assertEquals(
            1, jdbc.queryForObject("select count(*) from goal_availability_slot", Integer.class));
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void availabilityRequiresOwnerAndActiveGoal() throws Exception {
    UUID id = create();
    actor(accountId, "PARENT");
    mvc.perform(
            put(ROOT + "/" + id + "/availability")
                .contentType("application/json")
                .content(availability(1, "18:00", "19:00")))
        .andExpect(status().isForbidden());
    UUID other = UUID.randomUUID();
    account(other, "STUDENT");
    jdbc.update("insert into student_profile(user_id) values(?)", other);
    actor(other, "STUDENT");
    mvc.perform(
            put(ROOT + "/" + id + "/availability")
                .contentType("application/json")
                .content(availability(1, "18:00", "19:00")))
        .andExpect(status().isNotFound());
    actor(accountId, "STUDENT");
    mvc.perform(post(ROOT + "/" + id + "/archive")).andExpect(status().isOk());
    mvc.perform(
            put(ROOT + "/" + id + "/availability")
                .contentType("application/json")
                .content(availability(1, "18:00", "19:00")))
        .andExpect(status().isBadRequest());
  }

  String availability(int day, String start, String end) {
    return "{\"slots\":[{\"dayOfWeek\":"
        + day
        + ",\"startTime\":\""
        + start
        + "\",\"endTime\":\""
        + end
        + "\"}]}";
  }

  void replace(UUID goal, String body) {
    try {
      mvc.perform(
              put(ROOT + "/" + goal + "/availability")
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  UUID create() throws Exception {
    String body =
        mvc.perform(post(ROOT).contentType("application/json").content(payload()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(
        JsonMapper.builder().build().readTree(body).get("data").get("goalId").asText());
  }

  String payload() {
    return """
    {"subjectId":1,"currentLevel":1,"desiredLevel":3,"goalType":"IMPROVE_GRADES",
     "deadline":"%s","mode":"ONLINE","area":null,"budgetMin":0,"budgetMax":200000}
    """
        .formatted(LocalDate.now().plusDays(30));
  }

  void account(UUID id, String role) {
    jdbc.update(
        "insert into account(user_id,email,password_hash,full_name,role)"
            + " values(?,?,'test-only','Test',?)",
        id,
        id + "@example.test",
        role);
  }

  void actor(UUID id, String role) {
    when(user.getUserId()).thenReturn(role.equals("GUEST") ? null : id);
    when(user.hasRole(anyString())).thenAnswer(call -> role.equals(call.getArgument(0)));
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  @EnableJpaRepositories({
    "vn.edufit.profile.infra.persistence.repository",
    "vn.edufit.discovery.infra.persistence.repository",
    "vn.edufit.connection.infra.persistence"
  })
  @Import({
    LearningGoalController.class,
    vn.edufit.profile.application.service.LearningGoalService.class,
    vn.edufit.profile.application.service.ProfileFacadeImpl.class,
    vn.edufit.discovery.application.service.TutorMatchingService.class,
    vn.edufit.discovery.application.service.DiscoveryQueryService.class,
    vn.edufit.discovery.web.DiscoveryController.class,
    vn.edufit.connection.application.ConnectionService.class
  })
  static class Config {
    @Bean
    DataSource dataSource() {
      var source =
          new DriverManagerDataSource(
              postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
      Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
      return source;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan(
          "vn.edufit.profile.infra.persistence.entity",
          "vn.edufit.discovery.infra.persistence.entity",
          "vn.edufit.connection.infra.persistence");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
      return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }

    @Bean
    CurrentUser user() {
      return mock(CurrentUser.class);
    }

    @Bean
    org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator() {
      return new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
    }

    @Bean
    jakarta.persistence.EntityManager entityManager(EntityManagerFactory factory) {
      return SharedEntityManagerCreator.createSharedEntityManager(factory);
    }

    @Bean
    vn.edufit.discovery.application.service.MatchExplanationService explanations() {
      return mock(vn.edufit.discovery.application.service.MatchExplanationService.class);
    }

    @Bean
    vn.edufit.iam.api.IamFacade iam() {
      return mock(vn.edufit.iam.api.IamFacade.class);
    }

    @Bean
    vn.edufit.shared.audit.AuditSink audit() {
      return mock(vn.edufit.shared.audit.AuditSink.class);
    }

    @Bean
    vn.edufit.shared.notification.NotificationSink notifications() {
      return mock(vn.edufit.shared.notification.NotificationSink.class);
    }
  }
}
