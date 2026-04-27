package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
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
class ClaimLedgerPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

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
  void appendClaimLedgerItemPersistsGovernanceVocabulary() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000020001");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000020002");
    insertOrganization(organizationId);

    ClaimLedgerAppendCommand command = command(
        organizationId,
        candidateId,
        ClaimType.INTENT,
        AssertionStrength.WEAK_SIGNAL,
        "source-item:12-18",
        ActorRole.CANDIDATE,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.CONSENT_REQUIRED,
        "motivation");

    ClaimLedgerAppendResult result = service().append(command);

    PersistedClaim persisted = findClaim(result.claimId());
    assertThat(persisted.organizationId()).isEqualTo(organizationId);
    assertThat(persisted.entityType()).isEqualTo("candidate");
    assertThat(persisted.entityId()).isEqualTo(candidateId);
    assertThat(persisted.claimType()).isEqualTo(ClaimType.INTENT.wireValue());
    assertThat(persisted.assertionStrength()).isEqualTo(AssertionStrength.WEAK_SIGNAL.wireValue());
    assertThat(persisted.sourceSpanRef()).isEqualTo("source-item:12-18");
    assertThat(persisted.speaker()).isEqualTo(ActorRole.CANDIDATE.wireValue());
    assertThat(persisted.verificationStatus()).isEqualTo(VerificationStatus.AI_EXTRACTED.wireValue());
    assertThat(persisted.clientShareability()).isEqualTo(ClientShareability.CONSENT_REQUIRED.wireValue());
    assertThat(persisted.targetFieldPath()).isEqualTo("motivation");
    assertThat(persisted.canonicalWriteAllowed()).isFalse();
    assertThat(persisted.createdAt()).isNotNull();
    assertThat(persisted.updatedAt()).isNotNull();
  }

  @Test
  void appendDoesNotWriteCanonicalFact() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000020101");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000020102");
    insertOrganization(organizationId);

    ClaimLedgerAppendResult result = service().append(command(
        organizationId,
        candidateId,
        ClaimType.FACT,
        AssertionStrength.EXPLICIT,
        "source-item:30-36",
        ActorRole.AI,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.INTERNAL_ONLY,
        "headline"));

    assertThat(findClaim(result.claimId()).canonicalWriteAllowed()).isFalse();
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("governance.ai_task_run", organizationId)).isZero();
  }

  @Test
  void appendUsesExplicitWireValues() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000020201");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000020202");
    insertOrganization(organizationId);

    ClaimLedgerAppendCommand command = command(
        organizationId,
        candidateId,
        ClaimType.PREFERENCE,
        AssertionStrength.CONTRADICTION,
        "source-item:41-43",
        ActorRole.AI,
        VerificationStatus.SYSTEM_INFERENCE,
        ClientShareability.FORBIDDEN,
        "availability.notice_period");

    PersistedClaim persisted = findClaim(service().append(command).claimId());

    assertThat(persisted.claimType()).isEqualTo(ClaimType.PREFERENCE.wireValue());
    assertThat(persisted.claimType()).isNotEqualTo(ClaimType.PREFERENCE.name());
    assertThat(persisted.assertionStrength()).isEqualTo(AssertionStrength.CONTRADICTION.wireValue());
    assertThat(persisted.assertionStrength()).isNotEqualTo(AssertionStrength.CONTRADICTION.name());
    assertThat(persisted.speaker()).isEqualTo(ActorRole.AI.wireValue());
    assertThat(persisted.speaker()).isNotEqualTo(ActorRole.AI.name());
    assertThat(persisted.verificationStatus()).isEqualTo(VerificationStatus.SYSTEM_INFERENCE.wireValue());
    assertThat(persisted.verificationStatus()).isNotEqualTo(VerificationStatus.SYSTEM_INFERENCE.name());
    assertThat(persisted.clientShareability()).isEqualTo(ClientShareability.FORBIDDEN.wireValue());
    assertThat(persisted.clientShareability()).isNotEqualTo(ClientShareability.FORBIDDEN.name());
  }

  @Test
  void appendIsInsertOnlyByContract() {
    assertThat(publicDeclaredMethodNames(ClaimLedgerPort.class)).containsExactly("append");
    assertThat(publicDeclaredMethodNames(JdbcClaimLedgerPort.class)).containsExactly("append");
    assertThat(declaredMethodNames(ClaimLedgerPort.class)).noneMatch(this::looksLikeMutableWriteShortcut);
    assertThat(declaredMethodNames(JdbcClaimLedgerPort.class)).noneMatch(this::looksLikeMutableWriteShortcut);
  }

  @Test
  void nullOrInvalidRequiredClaimDataIsRejected() {
    RecordingClaimLedgerPort port = new RecordingClaimLedgerPort();
    ClaimLedgerService claimLedgerService = new ClaimLedgerService(port);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> claimLedgerService.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands()).isEmpty();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new ClaimLedgerAppendCommand(
        uuid("00000000-0000-0000-0000-000000020301"),
        new EntityRef("candidate", uuid("00000000-0000-0000-0000-000000020302")),
        "",
        ClaimType.INTENT,
        AssertionStrength.WEAK_SIGNAL,
        new SourceSpanRef("source-item:1-2"),
        ActorRole.CANDIDATE,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.INTERNAL_ONLY,
        null,
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("targetFieldPath must not be blank");
    assertThat(port.commands()).isEmpty();
  }

  @Test
  void fullFlywayMigrationStillAppliesBeforePersistenceTest() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(2);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2");
  }

  @Test
  void knownConsentDisclosureGapRemainsOutOfScope() throws SQLException {
    assertThat(tableExists("privacy", "consent_record")).isFalse();
    assertThat(tableExists("privacy", "disclosure_record")).isFalse();
  }

  private static ClaimLedgerService service() {
    return new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource));
  }

  private static ClaimLedgerAppendCommand command(
      UUID organizationId,
      UUID candidateId,
      ClaimType claimType,
      AssertionStrength assertionStrength,
      String sourceSpanRef,
      ActorRole speaker,
      VerificationStatus verificationStatus,
      ClientShareability clientShareability,
      String targetFieldPath) {
    return new ClaimLedgerAppendCommand(
        organizationId,
        new EntityRef("candidate", candidateId),
        targetFieldPath,
        claimType,
        assertionStrength,
        new SourceSpanRef(sourceSpanRef),
        speaker,
        verificationStatus,
        clientShareability,
        null,
        null);
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, "Task 3A Org " + organizationId);
      statement.setString(3, "Task 3A Org");
      statement.executeUpdate();
    }
  }

  private static PersistedClaim findClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              claim_ledger_item_id,
              organization_id,
              entity_type,
              entity_id,
              claim_type::text AS claim_type,
              assertion_strength::text AS assertion_strength,
              source_span_ref,
              speaker::text AS speaker,
              verification_status::text AS verification_status,
              canonical_write_allowed,
              client_shareability::text AS client_shareability,
              target_field_path,
              created_at,
              updated_at
            FROM governance.claim_ledger_item
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedClaim(
            resultSet.getObject("claim_ledger_item_id", UUID.class),
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class),
            resultSet.getString("claim_type"),
            resultSet.getString("assertion_strength"),
            resultSet.getString("source_span_ref"),
            resultSet.getString("speaker"),
            resultSet.getString("verification_status"),
            resultSet.getBoolean("canonical_write_allowed"),
            resultSet.getString("client_shareability"),
            resultSet.getString("target_field_path"),
            resultSet.getObject("created_at", OffsetDateTime.class),
            resultSet.getObject("updated_at", OffsetDateTime.class));
      }
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private boolean looksLikeMutableWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("update")
        || normalized.contains("delete")
        || normalized.contains("upsert")
        || normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> declaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return connection();
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
      }

      @Override
      public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
      }

      @Override
      public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("DriverManager parent logger is not supported");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
      }
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private record PersistedClaim(
      UUID claimLedgerItemId,
      UUID organizationId,
      String entityType,
      UUID entityId,
      String claimType,
      String assertionStrength,
      String sourceSpanRef,
      String speaker,
      String verificationStatus,
      boolean canonicalWriteAllowed,
      String clientShareability,
      String targetFieldPath,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
  }

  private record RecordingClaimLedgerPort(List<ClaimLedgerAppendCommand> commands)
      implements ClaimLedgerPort {

    private RecordingClaimLedgerPort() {
      this(new java.util.ArrayList<>());
    }

    @Override
    public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
      commands.add(command);
      return new ClaimLedgerAppendResult(
          new ClaimId(uuid("00000000-0000-0000-0000-000000020999")));
    }
  }
}
