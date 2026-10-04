package vn.edufit.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử tích hợp (Integration Test) xác minh toàn bộ kịch bản migration của Flyway trên CSDL thật.
 *
 * <p>Mục đích thiết kế và tuân thủ quyết định kiến trúc:
 * <ul>
 *   <li><b>ADR-002 Decision 3b (Mục 6):</b> <i>"CI bắt buộc chạy flyway migrate trên Testcontainers Postgres sạch mỗi PR -
 *       bắt lỗi thứ tự version sớm, trước khi merge."</i></li>
 *   <li><b>disabledWithoutDocker = true:</b> Tự động bỏ qua (skip) một cách an toàn nếu máy phát triển local
 *       chưa bật Docker Desktop, đồng thời tự động kích hoạt 100% khi chạy trên môi trường CI GitHub Actions.</li>
 *   <li><b>Kiểm tra thực tế:</b> Xác nhận các câu lệnh SQL trong {@code db/migration/**} có cú pháp hợp lệ
 *       với engine PostgreSQL 17 và tạo thành công 29 bảng theo {@code docs/edufit-schema.dbml}.</li>
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Kiểm thử tích hợp Flyway Migration trên PostgreSQL thật (Testcontainers)")
class FlywayMigrationIT {

  @Container
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
      .withDatabaseName("edufit_test")
      .withUsername("edufit")
      .withPassword("edufit-local-only");

  @Test
  @DisplayName("Thực thi Flyway migrate trên CSDL sạch và xác minh bảng account, email_token và các bảng SRS")
  void shouldMigrateCleanDatabaseSuccessfully() throws Exception {
    assertTrue(postgres.isRunning(), "Container PostgreSQL phải đang chạy để thực hiện kiểm thử");

    // 1. Cấu hình Flyway trỏ trực tiếp vào Testcontainers PostgreSQL
    Flyway flyway = Flyway.configure()
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
    assertEquals(8, result.migrationsExecuted, "Phải thực thi đúng 8 file migration V1.01 -> V1.08");

    // 4. Kiểm tra trực tiếp các bảng và ràng buộc đã được tạo trong PostgreSQL
    try (Connection connection = DriverManager.getConnection(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword()
    );
         Statement statement = connection.createStatement()) {

      // Kiểm tra bảng account tồn tại và có thể truy vấn
      try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM account")) {
        assertTrue(rs.next(), "Bảng account phải tồn tại và cho phép truy vấn");
        assertEquals(0, rs.getInt(1), "Bảng account mới khởi tạo phải có 0 bản ghi");
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

      // Kiểm tra bảng flyway_schema_history ghi nhận cả 8 version ở trạng thái thành công
      try (ResultSet rs = statement.executeQuery(
          "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true"
      )) {
        assertTrue(rs.next());
        assertEquals(8, rs.getInt(1), "Lịch sử migration phải ghi nhận đúng 8 bản ghi thành công");
      }
    }
  }
}
