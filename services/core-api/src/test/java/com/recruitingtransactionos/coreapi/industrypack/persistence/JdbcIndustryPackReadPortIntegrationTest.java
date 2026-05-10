package com.recruitingtransactionos.coreapi.industrypack.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.industrypack.IndustryPackCalibrationProfile;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
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
    assertThat(migrateResult.migrationsExecuted).isEqualTo(33);
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);

    var semiconductor = port.findByKey(new IndustryPackKey("semiconductor"));

    assertThat(semiconductor).isPresent();
    assertThat(semiconductor.orElseThrow().displayName()).isEqualTo("Semiconductor");
    assertThat(semiconductor.orElseThrow().maturity().wireValue()).isEqualTo("production");

    var ontology = port.findActiveOntologyVersion(semiconductor.orElseThrow().industryPackId(), Instant.parse("2026-05-03T00:00:00Z"));

    assertThat(ontology).isPresent();
    assertThat(ontology.orElseThrow().versionKey()).isEqualTo("ontology-semiconductor-v2");

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
    assertThat(general.orElseThrow().maturity().wireValue()).isEqualTo("seeded");
    var ontology = port.findActiveOntologyVersion(new IndustryPackId(general.orElseThrow().industryPackId().value()), Instant.parse("2026-05-03T00:00:00Z"));
    assertThat(ontology).isPresent();
  }

  @Test
  void task47SeedsAllV21PacksWithCalibrationMetadata() throws Exception {
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);
    IndustryPackService service = new IndustryPackService(port);

    var profiles = service.listCalibrationProfiles(Instant.parse("2026-05-09T00:00:00Z"));

    assertThat(profiles)
        .extracting(profile -> profile.industryPack().packKey().value())
        .containsExactly(
            "executive_search",
            "finance",
            "general",
            "healthcare",
            "internet_ai",
            "manufacturing",
            "sales",
            "semiconductor");
    assertThat(profiles).allSatisfy(profile -> {
      assertThat(profile.ontologyVersion().reviewBy()).isAfter(Instant.parse("2026-05-09T00:00:00Z"));
      assertThat(profile.goldCases()).isNotEmpty();
      assertThat(profile.negativeCases()).isNotEmpty();
      assertThat(profile.antiPatterns()).isNotEmpty();
      assertThat(profile.scoreCaps()).isNotEmpty();
    });
    assertThat(profiles)
        .filteredOn(profile -> profile.industryPack().packKey().value().equals("semiconductor"))
        .singleElement()
        .satisfies(profile -> {
          assertThat(profile.industryPack().maturity().wireValue()).isEqualTo("production");
          assertThat(profile.scoreCaps()).anyMatch(value -> value.contains("production"));
        });
    assertThat(profiles)
        .filteredOn(profile -> !profile.industryPack().packKey().value().equals("semiconductor"))
        .allSatisfy(profile -> {
          assertThat(profile.industryPack().maturity().wireValue()).isIn("seeded", "cold");
          assertThat(profile.scoreCaps()).anyMatch(value -> value.contains("5 requires"));
        });
  }

  @Test
  void task47SemiconductorV2PreservesExistingProductionRoleFamilies() {
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);
    var semiconductor = port.findByKey(new IndustryPackKey("semiconductor")).orElseThrow();
    var ontology = port.findActiveOntologyVersion(
        semiconductor.industryPackId(),
        Instant.parse("2026-05-09T00:00:00Z")).orElseThrow();

    assertThat(ontology.versionKey()).isEqualTo("ontology-semiconductor-v2");
    assertThat(List.of("dv_verification", "physical_design", "dft", "analog_mixed_signal", "firmware_embedded"))
        .allSatisfy(roleFamily -> assertThat(port.findRoleFamilyTemplate(
            semiconductor.industryPackId(),
            ontology.ontologyVersionId(),
            roleFamily))
            .as(roleFamily)
            .isPresent());
  }

  @Test
  void calibrationReviewQueueDoesNotMarkSeededPacksAsProduction() {
    JdbcIndustryPackReadPort port = new JdbcIndustryPackReadPort(dataSource);
    IndustryPackService service = new IndustryPackService(port);

    var queue = service.buildCalibrationReviewQueue(Instant.parse("2026-05-09T00:00:00Z"));

    assertThat(queue)
        .extracting(item -> item.packKey().value())
        .contains("finance", "healthcare", "internet_ai", "sales", "executive_search", "manufacturing");
    assertThat(queue)
        .noneSatisfy(item -> assertThat(item.packKey().value()).isEqualTo("semiconductor"));
    assertThat(queue)
        .allSatisfy(item -> assertThat(item.reason()).containsAnyOf("calibration", "drift"));
  }

  @Test
  void task47AddsCalibrationColumnsWithoutRewritingPriorMigration() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("""
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'recruiting'
              AND table_name = 'industry_pack'
              AND column_name IN (
                'calibration_review_by',
                'gold_cases',
                'negative_cases',
                'pack_anti_patterns',
                'score_caps',
                'drift_signals'
              )
            """)) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(6);
    }
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
