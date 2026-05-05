package com.recruitingtransactionos.coreapi.industrypack.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class JdbcIndustryPackReadPortIntegrationTest {
  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;
  private static MigrateResult migrateResult;

  @BeforeAll
  static void migrate() {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
  }

  @Test
  void readsSeededSemiconductorPackOntologyAndRoleTemplate() {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(30);
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);

    var semiconductor = port.findByKey(new IndustryPackKey("semiconductor"));

    assertThat(semiconductor).isPresent();
    assertThat(semiconductor.orElseThrow().displayName()).isEqualTo("Semiconductor");
    assertThat(semiconductor.orElseThrow().maturity().wireValue()).isEqualTo("seeded");

    var ontology = port.findActiveOntologyVersion(semiconductor.orElseThrow().industryPackId(), Instant.parse("2026-05-03T00:00:00Z"));

    assertThat(ontology).isPresent();
    assertThat(ontology.orElseThrow().versionKey()).isEqualTo("ontology-semiconductor-v1");

    var template = port.findRoleFamilyTemplate(
        semiconductor.orElseThrow().industryPackId(),
        ontology.orElseThrow().ontologyVersionId(),
        "DV / Verification");

    assertThat(template).isPresent();
    assertThat(template.orElseThrow().requiredSkillKeys()).contains("systemverilog", "uvm");
    assertThat(template.orElseThrow().antiPatterns())
        .anyMatch(value -> value.contains("software QA") || value.contains("software testing"));
  }

  @Test
  void keepsGeneralPackCold() {
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);
    var general = port.findByKey(new IndustryPackKey("general"));
    assertThat(general).isPresent();
    assertThat(general.orElseThrow().maturity().wireValue()).isEqualTo("cold");
    var ontology = port.findActiveOntologyVersion(new IndustryPackId(general.orElseThrow().industryPackId().value()), Instant.parse("2026-05-03T00:00:00Z"));
    assertThat(ontology).isPresent();
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("unwrap not supported");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }

      @Override
      public java.io.PrintWriter getLogWriter() {
        return new java.io.PrintWriter(System.out);
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getGlobal();
      }
    };
  }
}
