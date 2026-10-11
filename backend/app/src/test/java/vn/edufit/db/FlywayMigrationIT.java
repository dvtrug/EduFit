package vn.edufit.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Kiểm thử tích hợp (Integration Test) xác minh toàn bộ kịch bản migration của Flyway trên CSDL
 * thật.
 *
 * <p>Mục đích thiết kế và tuân thủ quyết định kiến trúc:
 *
 * <ul>
 *   <li><b>ADR-002 Decision 3b (Mục 6):</b> <i>"CI bắt buộc chạy flyway migrate trên Testcontainers
 *       Postgres sạch mỗi PR - bắt lỗi thứ tự version sớm, trước khi merge."</i>
 *   <li><b>Docker bắt buộc:</b> Test phải fail khi thiếu Docker, không bỏ qua bằng skip. Máy phát
 *       triển local và CI cần có Docker để kiểm chứng migration PostgreSQL thật.
 *   <li><b>Kiểm tra thực tế:</b> Xác nhận các câu lệnh SQL trong {@code db/migration/**} có cú pháp
 *       hợp lệ với engine PostgreSQL 17; schema nền được mô tả trong {@code
 *       docs/edufit-schema.dbml}.
 * </ul>
 */
@Testcontainers
@DisplayName("Kiểm thử tích hợp Flyway Migration trên PostgreSQL thật (Testcontainers)")
class FlywayMigrationIT {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("edufit_test")
          .withUsername("edufit")
          .withPassword("edufit-local-only");

  @Test
  @DisplayName(
      "Nâng cấp DB main đã seed V1.09: giữ history/dữ liệu, chạy V1.10/V1.11/V1.12 và rerun no-op")
  void shouldUpgradeMainSeedHistoryWithoutRepair() throws Exception {
    String schema = "main_seed_upgrade";
    Flyway previous =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations(
                "classpath:db/migration/iam",
                "classpath:db/migration/profile",
                "classpath:db/migration/verification",
                "classpath:db/migration/scheduling",
                "classpath:db/migration/progress",
                "classpath:db/migration/review",
                "classpath:db/legacy-main")
            .schemas(schema)
            .target(MigrationVersion.fromVersion("1.09"))
            .load();
    assertEquals(9, previous.migrate().migrationsExecuted);
    try (Connection connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schema);
      statement.executeUpdate(
          "UPDATE account SET full_name = 'User edited name' WHERE user_id ="
              + " '11111111-1111-1111-1111-111111111111'");
      statement.executeUpdate(
          "INSERT INTO parent_student_link(link_id,parent_user_id,student_id,confirmed_at) VALUES"
              + " ('99999999-1111-1111-1111-111111111111','33333333-3333-3333-3333-333333333333','11111111-1111-1111-1111-000000000001',now())");
    }
    Flyway upgraded =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .schemas(schema)
            .outOfOrder(false)
            .validateOnMigrate(true)
            .baselineOnMigrate(false)
            .load();
    assertEquals(3, upgraded.migrate().migrationsExecuted);
    upgraded.validate();
    assertEquals(0, upgraded.migrate().migrationsExecuted, "Rerun không thực thi lại migration");
    try (Connection connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + schema);
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT checksum FROM flyway_schema_history WHERE version = '1.09'")) {
        assertTrue(rs.next());
        assertEquals(-1594466614, rs.getInt(1));
      }
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT full_name FROM account WHERE user_id ="
                  + " '11111111-1111-1111-1111-111111111111'")) {
        assertTrue(rs.next());
        assertEquals("User edited name", rs.getString(1));
      }
      try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM account")) {
        assertTrue(rs.next());
        assertEquals(4, rs.getInt(1));
      }
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT status,version FROM parent_student_link WHERE link_id ="
                  + " '99999999-1111-1111-1111-111111111111'")) {
        assertTrue(rs.next());
        assertEquals("CONFIRMED", rs.getString(1));
        assertEquals(0, rs.getLong(2));
      }
      statement
          .executeQuery("SELECT user_id,window_started_at,attempts FROM connection_invite_quota")
          .close();
    }
  }

  @Test
  @DisplayName(
      "Thực thi Flyway migrate trên CSDL sạch và xác minh bảng account, email_token và các bảng"
          + " SRS")
  void shouldMigrateCleanDatabaseSuccessfully() throws Exception {
    assertTrue(postgres.isRunning(), "Container PostgreSQL phải đang chạy để thực hiện kiểm thử");

    // 1. Cấu hình Flyway trỏ trực tiếp vào Testcontainers PostgreSQL
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .outOfOrder(false)
            .validateOnMigrate(true)
            .baselineOnMigrate(false)
            .load();

    // 2. Kích hoạt toàn bộ các file migration từ đầu đến cuối
    MigrateResult result = flyway.migrate();

    // 3. Kiểm chứng kết quả thực thi của Flyway
    assertTrue(result.success, "Quá trình migrate của Flyway phải thành công 100%");
    assertEquals(
        12, result.migrationsExecuted, "Phải thực thi đúng 12 file migration V1.01 -> V1.12");

    // 4. Kiểm tra trực tiếp các bảng và ràng buộc đã được tạo trong PostgreSQL
    try (Connection connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement()) {

      // Kiểm tra bảng account tồn tại và có thể truy vấn
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM account")) {
        assertTrue(rs.next(), "Bảng account phải tồn tại và cho phép truy vấn");
        assertEquals(4, rs.getInt(1), "Bảng account có 4 bản ghi demo khởi tạo từ V1.10");
      }

      // Kiểm tra bảng email_token tồn tại và có thể truy vấn
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM email_token")) {
        assertTrue(rs.next(), "Bảng email_token phải tồn tại và cho phép truy vấn");
        assertEquals(0, rs.getInt(1), "Bảng email_token mới khởi tạo phải có 0 bản ghi");
      }

      // Kiểm tra bảng class tồn tại
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM class")) {
        assertTrue(rs.next(), "Bảng class phải tồn tại và cho phép truy vấn");
      }

      // Kiểm tra bảng tutoring_session tồn tại
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM tutoring_session")) {
        assertTrue(rs.next(), "Bảng tutoring_session phải tồn tại và cho phép truy vấn");
      }

      // Kiểm tra bảng flyway_schema_history ghi nhận cả 12 version ở trạng thái thành công
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
        assertTrue(rs.next());
        assertEquals(
            12, rs.getInt(1), "Lịch sử migration phải ghi nhận đúng 12 bản ghi thành công");
      }
    }
  }

  @Test
  void quotaUpgradePreservesHistoricalAttemptsAndIsIdempotent() throws Exception {
    String schema = "ai_quota_upgrade";
    Flyway previous =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .schemas(schema)
            .target(MigrationVersion.fromVersion("1.11"))
            .load();
    previous.migrate();
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var statement = connection.createStatement()) {
      statement.execute("set search_path to " + schema);
      statement.execute(
          "insert into ai_request_log(user_id,feature,status,created_at) select"
              + " '11111111-1111-1111-1111-111111111111'::uuid,'MATCH_EXPLANATION',s,now()-interval"
              + " '1 hour' from unnest(array['SUCCESS','ERROR','TIMEOUT']) s");
      statement.execute(
          "insert into ai_request_log(user_id,feature,status)"
              + " values('11111111-1111-1111-1111-111111111111','TUTOR_BIO','SUCCESS')");
    }
    Flyway upgraded =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .schemas(schema)
            .load();
    assertEquals(1, upgraded.migrate().migrationsExecuted);
    upgraded.validate();
    assertEquals(0, upgraded.migrate().migrationsExecuted);
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var statement = connection.createStatement()) {
      statement.execute("set search_path to " + schema);
      try (var rows =
          statement.executeQuery(
              "select count(*) from ai_match_reservation r join ai_request_log l on"
                  + " l.log_id=r.reservation_id and l.created_at=r.reserved_at and"
                  + " l.status=r.status")) {
        assertTrue(rows.next());
        assertEquals(3, rows.getInt(1));
      }
      try (var rows = statement.executeQuery("select count(*) from ai_match_quota_actor")) {
        assertTrue(rows.next());
        assertEquals(1, rows.getInt(1));
      }
    }
  }
}
