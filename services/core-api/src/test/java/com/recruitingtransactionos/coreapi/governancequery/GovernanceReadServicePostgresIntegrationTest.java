package com.recruitingtransactionos.coreapi.governancequery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerConfiguration;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.governanceconfig.JdbcGovernanceConfigPort;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class GovernanceReadServicePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000370101");
  private static final UUID ACTOR_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000370102");
  private static final UUID ALLOW_ATTEMPT_ID = UUID.fromString("00000000-0000-0000-0000-000000370103");
  private static final UUID BLOCK_ATTEMPT_ID = UUID.fromString("00000000-0000-0000-0000-000000370104");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static GovernanceReadService readService;
  private static GovernanceConfigService configService;
  private static DataSource dataSource;

  @BeforeAll
  static void migrateAndSeed() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    dataSource = new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
    ObjectMapper objectMapper = new ObjectMapper();
    configService = new GovernanceConfigService(new JdbcGovernanceConfigPort(dataSource), objectMapper);
    readService = new GovernanceReadService(
        dataSource,
        configService,
        objectMapper,
        new AITaskRunnerConfiguration().aiTaskDefinitionRegistry(),
        new AITaskModelRouter(defaultRouteProperties()));

    try (Connection connection = dataSource.getConnection();
        PreparedStatement organizationStatement = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, 'Task37 Governance Org', 'Task37 Governance Org', 'active', 'Asia/Shanghai')
            """);
        PreparedStatement userStatement = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, 'task37-admin@example.test', 'Task37 Admin', 'active')
            """);
        PreparedStatement allowAttemptStatement = connection.prepareStatement("""
            INSERT INTO governance.canonical_write_attempt (
              canonical_write_attempt_id,
              organization_id,
              entity_type,
              entity_id,
              target_field_path,
              proposed_value_ref,
              decision,
              actor_user_id,
              actor_role,
              occurred_at,
              idempotency_key
            )
            VALUES (?, ?, 'candidate_profile', ?, 'profile.summary', 'allow-ref', 'allow', ?, 'admin', now(), 'task37-allow')
            """);
        PreparedStatement blockAttemptStatement = connection.prepareStatement("""
            INSERT INTO governance.canonical_write_attempt (
              canonical_write_attempt_id,
              organization_id,
              entity_type,
              entity_id,
              target_field_path,
              proposed_value_ref,
              decision,
              actor_user_id,
              actor_role,
              occurred_at,
              idempotency_key
            )
            VALUES (?, ?, 'candidate_profile', ?, 'profile.summary', 'block-ref', 'block', ?, 'admin', now(), 'task37-block')
            """)) {
      organizationStatement.setObject(1, ORGANIZATION_ID);
      organizationStatement.executeUpdate();
      userStatement.setObject(1, ACTOR_USER_ID);
      userStatement.setObject(2, ORGANIZATION_ID);
      userStatement.executeUpdate();
      allowAttemptStatement.setObject(1, ALLOW_ATTEMPT_ID);
      allowAttemptStatement.setObject(2, ORGANIZATION_ID);
      allowAttemptStatement.setObject(3, UUID.fromString("00000000-0000-0000-0000-000000370105"));
      allowAttemptStatement.setObject(4, ACTOR_USER_ID);
      allowAttemptStatement.executeUpdate();
      blockAttemptStatement.setObject(1, BLOCK_ATTEMPT_ID);
      blockAttemptStatement.setObject(2, ORGANIZATION_ID);
      blockAttemptStatement.setObject(3, UUID.fromString("00000000-0000-0000-0000-000000370106"));
      blockAttemptStatement.setObject(4, ACTOR_USER_ID);
      blockAttemptStatement.executeUpdate();
    }
  }

  private static AITaskRunnerProperties defaultRouteProperties() {
    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route defaultRoute = new AITaskRunnerProperties.Route();
    defaultRoute.setProvider("deepseek");
    defaultRoute.setModel("deepseek-v4-pro");
    properties.getRoutes().put("default", defaultRoute);
    return properties;
  }

  @Test
  void industryPackGovernanceSectionsLoadAgainstRealSchema() {
    assertThatCode(() -> readService.loadOwnerSection(ORGANIZATION_ID, "data-quality"))
        .doesNotThrowAnyException();
    assertThatCode(() -> readService.loadAdminSection(ORGANIZATION_ID, "ontology-governance"))
        .doesNotThrowAnyException();
    assertThatCode(() -> readService.loadAdminSection(ORGANIZATION_ID, "industry-packs"))
        .doesNotThrowAnyException();
  }

  @Test
  void claimLedgerGovernanceSectionLoadsAgainstRealVerificationStatusEnum() {
    assertThatCode(() -> readService.loadAdminSection(ORGANIZATION_ID, "claim-ledger"))
        .doesNotThrowAnyException();
  }

  @Test
  void governanceConfigSaveWritesAuditLogRow() throws SQLException {
    int before = countRows("audit.audit_log");

    GovernanceConfigRecord record = configService.save(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "deepseek",
                "model": "deepseek-v4-pro"
              }
            }
            """,
        true,
        ACTOR_USER_ID);

    assertThat(record.configType()).isEqualTo("model-routing");
    assertThat(countRows("audit.audit_log") - before).isEqualTo(1);
  }

  @Test
  void governanceConfigSavePreservesSystemActorRoleInAuditLog() throws SQLException {
    GovernanceConfigRecord record = configService.save(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "deepseek",
                "model": "deepseek-v4-pro"
              }
            }
            """,
        true,
        ACTOR_USER_ID,
        PortalRole.SYSTEM);

    assertThat(actorRoleForConfigAudit(record.governanceConfigId()))
        .isEqualTo("system");
  }

  @Test
  void canonicalWriteDeniedMetricExcludesAllowedAttempts() {
    GovernanceSectionResponse ownerRisk = readService.loadOwnerSection(ORGANIZATION_ID, "risk");
    GovernanceSectionResponse adminSecurity = readService.loadAdminSection(ORGANIZATION_ID, "security");

    assertThat(metricValue(ownerRisk, "canonicalRejects")).isEqualTo("1");
    assertThat(metricValue(adminSecurity, "deniedWrites")).isEqualTo("1");
  }

  @Test
  void onlyRuntimeWiredAdminSectionsAreEditable() {
    assertThat(readService.loadAdminSection(ORGANIZATION_ID, "model-routing").editable())
        .isTrue();
    for (String sectionKey : List.of(
        "ontology-governance",
        "privacy-redaction",
        "eval-feedback",
        "ai-policy",
        "industry-packs",
        "workflow-rules",
        "permissions",
        "integrations")) {
      assertThat(readService.loadAdminSection(ORGANIZATION_ID, sectionKey).editable())
          .as(sectionKey)
          .isFalse();
    }
  }

  @Test
  void aiTaskRegistryListsAllProductionDefinitionsAgainstRealSchema() {
    GovernanceSectionResponse section = readService.loadAdminSection(ORGANIZATION_ID, "ai-task-registry");

    assertThat(metricValue(section, "taskDefinitions")).isEqualTo("28");
    assertThat(section.items()).hasSize(28);
    assertThat(section.items().getFirst().detail())
        .contains(
            "schema:/ai/schemas/source-classifier-input.schema.json",
            "evalResult:registered",
            "latencyMs:n/a",
            "replayHistory:0");
  }

  private static String metricValue(GovernanceSectionResponse section, String metricKey) {
    return section.metrics().stream()
        .filter(metric -> metric.key().equals(metricKey))
        .findFirst()
        .orElseThrow()
        .value();
  }

  private static int countRows(String tableName) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
        ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static String actorRoleForConfigAudit(UUID governanceConfigId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT actor_role::text
            FROM audit.audit_log
            WHERE target_entity_id = ?
            ORDER BY occurred_at DESC
            LIMIT 1
            """)) {
      statement.setObject(1, governanceConfigId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    }
  }
}
