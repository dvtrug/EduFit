package vn.edufit.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị kiểm tra quy chuẩn các file script migration của Flyway.
 *
 * <p>Mục đích kiểm thử:
 * <ul>
 *   <li><b>Quy chuẩn đặt tên:</b> Mọi file migration bắt buộc tuân thủ
 *       định dạng: {@code V1.{seq}__{description}.sql} hoặc {@code V{yyyy}.{MM}.{dd}.{seq}__{description}.sql}.</li>
 *   <li><b>Tính toàn vẹn dữ liệu:</b> Cấm tuyệt đối các file rỗng 0 bytes hoặc file không có nội dung SQL.</li>
 *   <li><b>Chạy nhanh độc lập:</b> Bài test này chạy trong vài mili-giây mà không cần Docker.</li>
 * </ul>
 */
@DisplayName("Kiểm thử quy chuẩn các file Flyway Migration (Convention & Naming)")
class FlywayMigrationScriptTest {

  /**
   * Pattern biểu thức chính quy kiểm tra định dạng tên file migration:
   * Chấp nhận: V1.01__create_account_and_session_tables.sql hoặc V2026.09.30.01__*.sql
   */
  private static final Pattern MIGRATION_NAME_PATTERN =
      Pattern.compile("^V(?:1\\.\\d{2}|\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2})__[a-z0-9_]+\\.sql$");

  private final Path migrationRootDir =
      Paths.get("src", "main", "resources", "db", "migration");

  @Test
  @DisplayName("Tất cả file SQL migration phải tuân thủ chuẩn đặt tên và không được để trống")
  void allMigrationFilesShouldFollowNamingConvention() throws IOException {
    assertTrue(Files.exists(migrationRootDir), "Thư mục migration phải tồn tại");

    try (Stream<Path> paths = Files.walk(migrationRootDir)) {
      List<Path> sqlFiles = paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".sql"))
          .sorted()
          .toList();

      assertFalse(sqlFiles.isEmpty(), "Phải có ít nhất một file migration SQL trong db/migration");
      assertEquals(8, sqlFiles.size(), "Phải có đúng 8 file migration tương ứng 8 module V1.01 -> V1.08");

      for (Path file : sqlFiles) {
        String fileName = file.getFileName().toString();
        assertTrue(
            MIGRATION_NAME_PATTERN.matcher(fileName).matches(),
            "File migration [" + fileName + "] không đúng định dạng V1.{seq}__*.sql hoặc V{yyyy}.{MM}.{dd}.{seq}__*.sql"
        );

        long fileSize = Files.size(file);
        assertTrue(
            fileSize > 100,
            "File migration [" + fileName + "] không được để trống (kích thước: " + fileSize + " bytes)"
        );
      }
    }
  }

  @Test
  @DisplayName("File migration phải chứa các bảng cốt lõi theo docs/edufit-schema.dbml")
  void migrationFilesShouldContainCoreTables() throws IOException {
    Path iamDir = migrationRootDir.resolve("iam");
    assertTrue(Files.exists(iamDir), "Thư mục migration/iam phải tồn tại");

    Path iamMigration = iamDir.resolve("V1.01__create_account_and_session_tables.sql");
    assertTrue(Files.exists(iamMigration), "Script V1.01 phải tồn tại");
    String iamContent = Files.readString(iamMigration);
    assertTrue(iamContent.contains("CREATE TABLE IF NOT EXISTS account"), "Phải chứa bảng account");
    assertTrue(iamContent.contains("CREATE TABLE IF NOT EXISTS email_token"), "Phải chứa bảng email_token");
    assertTrue(iamContent.contains("CREATE TABLE IF NOT EXISTS SPRING_SESSION"), "Phải chứa bảng SPRING_SESSION");

    Path schedulingDir = migrationRootDir.resolve("scheduling");
    Path schedulingMigration = schedulingDir.resolve("V1.05__create_tutoring_session_tables.sql");
    assertTrue(Files.exists(schedulingMigration), "Script V1.05 phải tồn tại");
    String schedulingContent = Files.readString(schedulingMigration);
    assertTrue(schedulingContent.contains("CREATE TABLE IF NOT EXISTS tutoring_session"), "Phải chứa bảng tutoring_session");
    assertTrue(schedulingContent.contains("no_tutor_overlap"), "Phải chứa exclusion constraint no_tutor_overlap");
  }
}
