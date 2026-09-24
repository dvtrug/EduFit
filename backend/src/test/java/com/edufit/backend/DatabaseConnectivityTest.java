package com.edufit.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseConnectivityTest {

  @Autowired private DataSource dataSource;

  @Autowired private Flyway flyway;

  @Test
  void applicationCanOpenAValidatedDatabaseConnection() throws SQLException {
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.isValid(2)).isTrue();
    }
  }

  @Test
  void baselineMigrationIsApplied() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
  }
}
