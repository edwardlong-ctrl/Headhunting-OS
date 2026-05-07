package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PilotDataPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    MigrateResult result = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    assertThat(result.migrationsExecuted).isEqualTo(31);
    dataSource = postgresDataSource();
  }

  @Test
  void rebuildImportsAndValidatesPilotDataWithoutDisclosureOrShortlistShortcuts()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    PilotDataReport report = service.rebuild(dataset);

    assertThat(report.command()).isEqualTo("rebuild");
    assertThat(report.valid()).isTrue();
    assertThat(report.counts().get("candidates")).isEqualTo(75);
    assertThat(report.counts().get("activeJobs")).isEqualTo(5);
    assertThat(report.counts().get("underReviewJobs")).isEqualTo(3);
    assertThat(report.counts().get("sourceDocuments")).isGreaterThanOrEqualTo(83);
    assertThat(report.counts().get("seededShortlists")).isZero();
    assertThat(report.counts().get("seededDisclosureRecords")).isZero();
    assertThat(report.counts().get("canonicalWriteAttempts")).isZero();
    assertThat(report.privacyChecks().get("passed")).isEqualTo(true);
    assertThat(report.seededAccountChecks().get("accountsPresent")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("noShortcutShortlists")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("noShortcutDisclosureRecords")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("candidateCurrentProfilesLinked")).isEqualTo(true);
    assertThat(report.failedGateReasons()).isEmpty();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isEqualTo(5);
    assertThatSeededAccountsCanAuthenticate(dataset);
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isEqualTo(75);
    assertThat(countRowsWhere(
        "recruiting.candidate",
        dataset.organization().organizationId(),
        "current_profile_id IS NOT NULL")).isEqualTo(75);
    assertThat(countRows("recruiting.company", dataset.organization().organizationId())).isEqualTo(4);
    assertThat(countRows("intake.source_item", dataset.organization().organizationId()))
        .isGreaterThanOrEqualTo(83);

    PilotDataReport exported = service.export(dataset.organization().organizationId());
    assertThat(exported.valid()).isTrue();
    assertThat(exported.counts().get("candidates")).isEqualTo(75);

    PilotDataReport reset = service.reset(dataset.organization().organizationId(), true);
    assertThat(reset.valid()).isTrue();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void resetFailsClosedWithoutExplicitAllowFlag() {
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        service.reset(UUID.fromString("00000000-0000-0000-0000-000000380001"), false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("pilot_data_reset_requires_RTO_PILOT_DATA_ALLOW_RESET_true");
  }

  @Test
  void importFailsClosedBeforeWritesWhenDatasetViolatesPrivacyRules()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataset.CandidateSeed first = dataset.candidates().getFirst();
    PilotDataset invalidDataset = dataset.withCandidates(List.of(new PilotDataset.CandidateSeed(
        first.candidateId(),
        first.profileId(),
        first.syntheticName(),
        "not-synthetic@example.com",
        first.roleFamily(),
        first.seniorityBand(),
        first.locationRegion(),
        first.status(),
        first.skills(),
        first.summary(),
        first.sourceDocumentRef(),
        first.metadata())));
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.reset(dataset.organization().organizationId(), true);
    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains("unsupported_email_domain");
    assertThat(countRows("identity.organization", dataset.organization().organizationId())).isZero();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void validateFailsClosedWhenStoredCandidateProfileFieldsContainUnsupportedEmailDomains()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.rebuild(dataset);
    executeUpdate(
        """
        UPDATE recruiting.candidate_profile
        SET metadata = replace(metadata::text, '@candidate.example.test', '@gmail.com')::jsonb
        WHERE organization_id = ?
        """,
        dataset.organization().organizationId());

    PilotDataReport report = service.validate(dataset.organization().organizationId());

    assertThat(report.valid()).isFalse();
    assertThat(report.privacyChecks().get("passed")).isEqualTo(false);
    assertThat(report.failedGateReasons()).contains("candidate_profile_unsupported_email_domain");
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    return countRowsWhere(tableName, organizationId, "true");
  }

  private static int countRowsWhere(
      String tableName,
      UUID organizationId,
      String whereClause) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement(
             "SELECT count(*) FROM " + tableName + " WHERE organization_id = ? AND " + whereClause)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static void assertThatSeededAccountsCanAuthenticate(PilotDataset dataset) throws SQLException {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement("""
             SELECT password_hash
             FROM identity.user_account
             WHERE organization_id = ? AND email = ?
             """)) {
      for (PilotDataset.AccountSeed account : dataset.accounts()) {
        statement.setObject(1, dataset.organization().organizationId());
        statement.setString(2, account.email());
        try (ResultSet resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(passwordEncoder.matches(account.password(), resultSet.getString("password_hash")))
              .isTrue();
          assertThat(resultSet.next()).isFalse();
        }
      }
    }
  }

  private static void executeUpdate(String sql, UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      statement.executeUpdate();
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
      public PrintWriter getLogWriter() throws SQLException {
        return null;
      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
      }

      @Override
      public int getLoginTimeout() throws SQLException {
        return 0;
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
      }
    };
  }
}
