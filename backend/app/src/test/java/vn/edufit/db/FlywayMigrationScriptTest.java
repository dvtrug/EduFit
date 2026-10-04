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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử đơn vị kiểm tra quy chuẩn các file script migration của Flyway.
 *
 * <p>Mục đích kiểm thử:
 * <ul>
 *   <li><b>Quy chuẩn đặt tên (ADR-002 Decision 3b):</b> Mọi file migration bắt buộc tuân thủ
 *       định dạng Timestamp Versioning: {@code V{yyyy}.{MM}.{dd}.{seq}__{description}.sql}.</li>
 *   <li><b>Tính toàn vẹn dữ liệu:</b> Cấm tuyệt đối các file rỗng 0 bytes hoặc file không có nội dung SQL.</li>
 *   <li><b>Chạy nhanh độc lập:</b> Bài test này chạy trong vài mili-giây mà không cần Docker.</li>
 * </ul>
 */
@DisplayName("Kiểm thử quy chuẩn các file Flyway Migration (Convention & Naming)")
class FlywayMigrationScriptTest {

  /**
   * Pattern biểu thức chính quy kiểm tra định dạng tên file migration:
   * Ví dụ hợp lệ: V2026.09.30.01__create_users_table.sql
   */
  private static final Pattern MIGRATION_NAME_PATTERN =
      Pattern.compile("^V\\d{4}\\.\\d{2}\\.\\d{2}\\.\\d{2}__[a-z0-9_]+\\.sql$");

  private final Path migrationRootDir =
      Paths.get("src", "main", "resources", "db", "migration");

  @Test
  @DisplayName("Tất cả file SQL migration phải tuân thủ chuẩn đặt tên V{yyyy}.{MM}.{dd}.{seq}__*.sql")
  void allMigrationFilesShouldFollowNamingConvention() throws IOException {
    assertTrue(Files.exists(migrationRootDir), "Thư mục migration phải tồn tại");

    try (Stream<Path> paths = Files.walk(migrationRootDir)) {
      List<Path> sqlFiles = paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".sql"))
          .toList();

      assertFalse(sqlFiles.isEmpty(), "Phải có ít nhất một file migration SQL trong db/migration");

      for (Path file : sqlFiles) {
        String fileName = file.getFileName().toString();
        assertTrue(
            MIGRATION_NAME_PATTERN.matcher(fileName).matches(),
            "File migration [" + fileName + "] không đúng định dạng timestamp V{yyyy}.{MM}.{dd}.{seq}__*.sql"
        );

        long fileSize = Files.size(file);
        assertTrue(
            fileSize > 50,
            "File migration [" + fileName + "] không được để trống (kích thước quá nhỏ: " + fileSize + " bytes)"
        );
      }
    }
  }

  @Test
  @DisplayName("File migration phải chứa lệnh CREATE TABLE và chú thích hợp lệ")
  void migrationFilesShouldContainValidSqlStatements() throws IOException {
    Path iamDir = migrationRootDir.resolve("iam");
    assertTrue(Files.exists(iamDir), "Thư mục migration/iam phải tồn tại");

    Path usersMigration = iamDir.resolve("V2026.09.30.01__create_users_table.sql");
    assertTrue(Files.exists(usersMigration), "Script tạo bảng users phải tồn tại");

    String usersContent = Files.readString(usersMigration);
    assertTrue(usersContent.contains("CREATE TABLE IF NOT EXISTS users"), "Script phải chứa lệnh tạo bảng users");
    assertTrue(usersContent.contains("CONSTRAINT uq_users_email UNIQUE"), "Script phải chứa ràng buộc unique email");
    assertTrue(usersContent.contains("chk_users_role CHECK"), "Script phải chứa check constraint cho vai trò");

    Path tokensMigration = iamDir.resolve("V2026.09.30.02__create_account_tokens_table.sql");
    assertTrue(Files.exists(tokensMigration), "Script tạo bảng account_tokens phải tồn tại");

    String tokensContent = Files.readString(tokensMigration);
    assertTrue(tokensContent.contains("CREATE TABLE IF NOT EXISTS account_tokens"), "Script phải chứa lệnh tạo bảng account_tokens");
    assertTrue(tokensContent.contains("token_hash VARCHAR(64)"), "Script phải lưu chuỗi băm SHA-256 64 ký tự");
    assertTrue(tokensContent.contains("used_at TIMESTAMPTZ"), "Script phải chứa cột used_at chống Replay");
  }
}
