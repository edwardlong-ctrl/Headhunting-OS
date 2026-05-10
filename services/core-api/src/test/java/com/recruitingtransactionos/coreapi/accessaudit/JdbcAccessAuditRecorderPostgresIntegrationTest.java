package com.recruitingtransactionos.coreapi.accessaudit;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAuditContext;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAuditEvent;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
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
class JdbcAccessAuditRecorderPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000410101");
  private static final UUID ACTOR_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000410102");
  private static final UUID TARGET_ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000410103");

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
  void recordsDeniedAccessDecisionInImmutableAuditLog() throws SQLException {
    JdbcAccessAuditRecorder recorder = new JdbcAccessAuditRecorder(dataSource);

    recorder.record(new AccessAuditEvent(
        new AccessRequest(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.PII,
            Set.of(RelationshipScope.SAME_ORGANIZATION),
            false),
        new AccessDecision(false, "client_raw_candidate_profile_denied", "Client cannot read raw candidate profile."),
        new AccessAuditContext(ORGANIZATION_ID, ACTOR_USER_ID, TARGET_ENTITY_ID, "pii")));

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT actor_role::text, action, target_entity_type, target_entity_id,
                   result, reason, sensitivity_level, metadata::text
            FROM audit.audit_log
            WHERE organization_id = ?
            ORDER BY occurred_at DESC
            LIMIT 1
            """)) {
      statement.setObject(1, ORGANIZATION_ID);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("actor_role")).isEqualTo("client");
        assertThat(resultSet.getString("action")).isEqualTo("access.read");
        assertThat(resultSet.getString("target_entity_type")).isEqualTo("candidate_profile");
        assertThat(resultSet.getObject("target_entity_id", UUID.class)).isEqualTo(TARGET_ENTITY_ID);
        assertThat(resultSet.getString("result")).isEqualTo("denied");
        assertThat(resultSet.getString("reason")).isEqualTo("client_raw_candidate_profile_denied");
        assertThat(resultSet.getString("sensitivity_level")).isEqualTo("pii");
        assertThat(resultSet.getString("metadata"))
            .contains("\"fieldClassification\": \"pii\"")
            .contains("\"identityDisclosureRequested\": false");
      }
    }
  }

  @Test
  void accessAuditSearchIsTenantScopedAndDoesNotRevealOtherOrganizationRows() throws SQLException {
    UUID otherOrganizationId = UUID.fromString("00000000-0000-0000-0000-000000410201");
    UUID otherActorUserId = UUID.fromString("00000000-0000-0000-0000-000000410202");
    UUID otherTargetEntityId = UUID.fromString("00000000-0000-0000-0000-000000410203");
    try (Connection connection = dataSource.getConnection()) {
      insertOrganizationAndUser(connection, otherOrganizationId, otherActorUserId, "other-access-audit@example.test");
    }
    JdbcAccessAuditRecorder recorder = new JdbcAccessAuditRecorder(dataSource);
    recorder.record(new AccessAuditEvent(
        new AccessRequest(
            PortalRole.ADMIN,
            ResourceType.DISCLOSURE_RECORD,
            AccessAction.EXPORT,
            FieldClassification.SYSTEM_GOVERNANCE,
            Set.of(RelationshipScope.SAME_ORGANIZATION),
            false),
        new AccessDecision(true, "admin_disclosure_audit_export_allowed", "Admin export allowed."),
        new AccessAuditContext(ORGANIZATION_ID, ACTOR_USER_ID, TARGET_ENTITY_ID, "system_governance")));
    recorder.record(new AccessAuditEvent(
        new AccessRequest(
            PortalRole.ADMIN,
            ResourceType.DISCLOSURE_RECORD,
            AccessAction.EXPORT,
            FieldClassification.SYSTEM_GOVERNANCE,
            Set.of(RelationshipScope.SAME_ORGANIZATION),
            false),
        new AccessDecision(true, "admin_disclosure_audit_export_allowed", "Admin export allowed."),
        new AccessAuditContext(otherOrganizationId, otherActorUserId, otherTargetEntityId, "system_governance")));

    JdbcAccessAuditSearchReader reader = new JdbcAccessAuditSearchReader(dataSource);
    var records = reader.search(new AccessAuditSearchQuery(
        ORGANIZATION_ID,
        null,
        null,
        null,
        null,
        null,
        50,
        0));

    assertThat(records).extracting(AccessAuditRecord::organizationId)
        .containsOnly(ORGANIZATION_ID);
    assertThat(records).extracting(AccessAuditRecord::targetEntityId)
        .contains(TARGET_ENTITY_ID)
        .doesNotContain(otherTargetEntityId);
  }

  private static void insertOrganizationAndUser(Connection connection) throws SQLException {
    insertOrganizationAndUser(connection, ORGANIZATION_ID, ACTOR_USER_ID, "access-audit@example.test");
  }

  private static void insertOrganizationAndUser(
      Connection connection,
      UUID organizationId,
      UUID userAccountId,
      String email) throws SQLException {
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
        VALUES (?, ?, ?, 'Access Audit User', 'active')
        ON CONFLICT DO NOTHING
        """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Access Audit Org " + organizationId);
      organization.setString(3, "Access Audit Org " + organizationId);
      organization.executeUpdate();
      user.setObject(1, userAccountId);
      user.setObject(2, organizationId);
      user.setString(3, email);
      user.executeUpdate();
    }
  }
}
