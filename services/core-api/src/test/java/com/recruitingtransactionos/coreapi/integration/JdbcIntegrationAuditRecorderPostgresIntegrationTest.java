package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class JdbcIntegrationAuditRecorderPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000490501");
  private static final UUID ACTOR_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000490502");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrateAndSeed() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUser(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    dataSource = source;

    try (Connection connection = DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      insertOrganizationAndUser(connection);
    }
  }

  @Test
  void recordsOutboundIntegrationAuditEvidenceInTenantScopedAuditLog() throws SQLException {
    JdbcIntegrationAuditRecorder recorder = new JdbcIntegrationAuditRecorder(dataSource);
    OutboundIntegrationCommand command = new OutboundIntegrationCommand(
        ORGANIZATION_ID,
        ACTOR_USER_ID,
        ORGANIZATION_ID,
        "consultant_reviewed_shortlist_summary",
        IntegrationChannel.EMAIL,
        new OutboundIntegrationTarget("client@example.test", ORGANIZATION_ID),
        IntegrationPayloadKind.SAFE_SUMMARY_EXPORT,
        "Shortlist summary",
        "Anonymous shortlist summary for client review.",
        null,
        RedactionDecision.SAFE_SUMMARY_ONLY,
        DisclosureState.NOT_DISCLOSED,
        "task49-jdbc-audit");

    UUID auditId = recorder.recordOutbound(
        command,
        IntegrationProviderResult.accepted("test_email", "queued-1"));

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT action, target_entity_type, result, reason, sensitivity_level, metadata::text
            FROM audit.audit_log
            WHERE organization_id = ?
              AND audit_log_id = ?
            """)) {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setObject(2, auditId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("action")).isEqualTo("integration.outbound.email");
        assertThat(resultSet.getString("target_entity_type")).isEqualTo("integration");
        assertThat(resultSet.getString("result")).isEqualTo("allowed");
        assertThat(resultSet.getString("reason")).isEqualTo("consultant_reviewed_shortlist_summary");
        assertThat(resultSet.getString("sensitivity_level")).isEqualTo("safe_summary");
        assertThat(resultSet.getString("metadata"))
            .contains("\"payloadKind\": \"SAFE_SUMMARY_EXPORT\"")
            .contains("\"providerStatus\": \"ACCEPTED\"")
            .contains("\"rawSensitivePayloadIncluded\": false");
      }
    }
  }

  private static void insertOrganizationAndUser(Connection connection) throws SQLException {
    try (PreparedStatement organization = connection.prepareStatement("""
        INSERT INTO identity.organization (
          organization_id, legal_name, display_name, status, default_timezone
        )
        VALUES (?, ?, ?, 'active', 'UTC')
        ON CONFLICT DO NOTHING
        """);
        PreparedStatement user = connection.prepareStatement("""
        INSERT INTO identity.user_account (
          user_account_id, organization_id, email, display_name, status
        )
        VALUES (?, ?, ?, 'Integration Audit User', 'active')
        ON CONFLICT DO NOTHING
        """)) {
      organization.setObject(1, ORGANIZATION_ID);
      organization.setString(2, "Integration Audit Org");
      organization.setString(3, "Integration Audit Org");
      organization.executeUpdate();
      user.setObject(1, ACTOR_USER_ID);
      user.setObject(2, ORGANIZATION_ID);
      user.setString(3, "integration-audit@example.test");
      user.executeUpdate();
    }
  }
}
