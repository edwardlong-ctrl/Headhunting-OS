package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.DeterministicIntakeExtractionService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
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
class IntakeClaimLedgerBridgePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000180001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000180002");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T13:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    insertOrganization(ORG_A);
    insertOrganization(ORG_B);
  }

  @Test
  void flywayMigrationAddsBridgeSourceReferenceLookupIndex() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(8);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    assertThat(indexExists(
        "governance",
        "claim_ledger_item",
        "claim_ledger_org_source_span_idx")).isTrue();
  }

  @Test
  void defaultPlaceholderExtractionOutputCreatesNoBusinessClaims() throws SQLException {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "default-placeholder").build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));
    IntakeExtractionRun run =
        extractionService().extract(ORG_A, packet.informationPacketId());

    IntakeClaimLedgerBridgeResult result = bridgeService().bridge(request(ORG_A, run.extractionRunId()));

    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED);
    assertThat(result.appendedClaimIds()).isEmpty();
    assertThat(countRows("intake.source_item", ORG_A)).isGreaterThan(0);
    assertThat(countRows("intake.information_packet", ORG_A)).isGreaterThan(0);
    assertThat(countRows("intake.extraction_run", ORG_A)).isGreaterThan(0);
    assertThat(countClaimsForExtractionRun(run.extractionRunId())).isZero();
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
    assertThat(countRows("governance.review_event", ORG_A)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isZero();
  }

  @Test
  void bridgeEligibleFixtureAppendsClaimWithGovernedIntakeLineage() throws SQLException {
    IntakeExtractionRun run = saveBridgeEligibleRun("eligible-fixture");

    IntakeClaimLedgerBridgeResult result = bridgeService().bridge(request(ORG_A, run.extractionRunId()));

    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.SUCCEEDED);
    assertThat(result.appendedClaimIds()).hasSize(1);
    PersistedClaim persisted = findClaim(result.appendedClaimIds().getFirst().value());
    assertThat(persisted.organizationId()).isEqualTo(ORG_A);
    assertThat(persisted.entityType()).isEqualTo("information_packet");
    assertThat(persisted.entityId()).isEqualTo(run.informationPacketId().value());
    assertThat(persisted.targetFieldPath()).isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(persisted.claimValue()).isEqualTo("explicitly marked operational bridge fixture");
    assertThat(persisted.claimType()).isEqualTo(ClaimType.INFERENCE.wireValue());
    assertThat(persisted.assertionStrength()).isEqualTo(AssertionStrength.WEAK_SIGNAL.wireValue());
    assertThat(persisted.verificationStatus()).isEqualTo(VerificationStatus.AI_EXTRACTED.wireValue());
    assertThat(persisted.clientShareability()).isEqualTo(ClientShareability.INTERNAL_ONLY.wireValue());
    assertThat(persisted.canonicalWriteAllowed()).isFalse();
    assertThat(persisted.sourceItemId()).isNull();
    assertThat(persisted.sourceSpanRef())
        .contains("intake.extraction_run:" + run.extractionRunId().value())
        .contains("intake.information_packet:" + run.informationPacketId().value())
        .contains("intake.source_item:")
        .contains("packet_type:CANDIDATE")
        .contains("intended_entity_type:CANDIDATE");
    assertThat(countRows("governance.review_event", ORG_A)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isZero();
  }

  @Test
  void duplicateBridgeCallDoesNotCreateDuplicateClaims() throws SQLException {
    IntakeExtractionRun run = saveBridgeEligibleRun("duplicate");

    IntakeClaimLedgerBridgeResult first = bridgeService().bridge(request(ORG_A, run.extractionRunId()));
    IntakeClaimLedgerBridgeResult second = bridgeService().bridge(request(ORG_A, run.extractionRunId()));

    assertThat(first.appendedClaimIds()).hasSize(1);
    assertThat(second.appendedClaimIds()).isEmpty();
    assertThat(second.existingClaimIds()).containsExactly(first.appendedClaimIds().getFirst());
    assertThat(countClaimsForExtractionRun(run.extractionRunId())).isEqualTo(1);
  }

  @Test
  void bridgePreservesOrganizationIsolation() throws SQLException {
    IntakeExtractionRun run = saveBridgeEligibleRun("org-scope");

    assertThatThrownBy(() -> bridgeService().bridge(request(ORG_B, run.extractionRunId())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraction run not found in organization");

    bridgeService().bridge(request(ORG_A, run.extractionRunId()));
    assertThat(countRows("governance.claim_ledger_item", ORG_B)).isZero();
  }

  private static IntakeExtractionRun saveBridgeEligibleRun(String suffix) {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(sourceCommand(ORG_A, suffix).build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));
    IntakeExtractionRunId runId = new IntakeExtractionRunId(uuid(
        "00000000-0000-0000-0000-" + deterministicTail(suffix)));
    Instant now = RECEIVED_AT.plusSeconds(Math.abs(suffix.hashCode() % 1000));
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        runId,
        ORG_A,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        "intake-extraction-envelope.v1",
        List.of(sourceItem.sourceItemId()),
        List.of(new IntakeExtractionSourceSnapshot(
            sourceItem.sourceItemId(),
            sourceItem.sourceType(),
            sourceItem.title(),
            sourceItem.contentHash(),
            sourceItem.externalRef())),
        List.of(new IntakeExtractedField(
            "intake.bridge_eligible.quality_note",
            "explicitly marked operational bridge fixture",
            sourceItem.sourceItemId(),
            0.5d,
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            "Operational fixture only; not a canonical candidate/company/job fact.")),
        List.of(),
        List.of(),
        now);
    return extractionPort().save(new IntakeExtractionRun(
        runId,
        ORG_A,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.SUCCEEDED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:" + suffix,
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope)));
  }

  private static GovernedIntakeService intakeService() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static DeterministicIntakeExtractionService extractionService() {
    return new DeterministicIntakeExtractionService(
        new JdbcInformationPacketPersistencePort(dataSource),
        extractionPort());
  }

  private static IntakeClaimLedgerBridgeService bridgeService() {
    return new IntakeClaimLedgerBridgeService(
        extractionPort(),
        new JdbcInformationPacketPersistencePort(dataSource),
        new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource)),
        new JdbcClaimLedgerSourceReferenceLookupPort(dataSource));
  }

  private static JdbcIntakeExtractionRunPort extractionPort() {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  private static IntakeClaimLedgerBridgeRequest request(
      UUID organizationId,
      IntakeExtractionRunId extractionRunId) {
    return new IntakeClaimLedgerBridgeRequest(
        organizationId,
        extractionRunId,
        ActorRole.CONSULTANT,
        null,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null);
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(
      UUID organizationId,
      String suffix) {
    String orgPrefix = organizationId.equals(ORG_A) ? "180001" : "180002";
    String value = orgPrefix + "-" + suffix;
    return SourceItemRegistrationCommand.builder()
        .organizationId(organizationId)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Senior DV engineer CV " + suffix)
        .contentHash("sha256:" + value)
        .externalRef("ats:" + value)
        .storageRef("s3://internal-intake/" + value)
        .rawRef("vault://source-" + value + "/raw")
        .language("en")
        .uploadedByActorType(ActorRole.CONSULTANT)
        .receivedAt(RECEIVED_AT)
        .metadataJson("{\"case\":\"" + value + "\"}")
        .status(SourceItemStatus.REGISTERED);
  }

  private static InformationPacketCreateCommand.Builder packetCommand(UUID organizationId) {
    return InformationPacketCreateCommand.builder()
        .organizationId(organizationId)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("candidate packet 180001")
        .metadataJson("{\"packet\":\"180001\"}");
  }

  private static PersistedClaim findClaim(UUID claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              organization_id,
              entity_type,
              entity_id,
              claim_type::text AS claim_type,
              assertion_strength::text AS assertion_strength,
              source_span_ref,
              verification_status::text AS verification_status,
              canonical_write_allowed,
              client_shareability::text AS client_shareability,
              target_field_path,
              claim_value_text,
              source_item_id
            FROM governance.claim_ledger_item
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedClaim(
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class),
            resultSet.getString("claim_type"),
            resultSet.getString("assertion_strength"),
            resultSet.getString("source_span_ref"),
            resultSet.getString("verification_status"),
            resultSet.getBoolean("canonical_write_allowed"),
            resultSet.getString("client_shareability"),
            resultSet.getString("target_field_path"),
            resultSet.getString("claim_value_text"),
            resultSet.getObject("source_item_id", UUID.class));
      }
    }
  }

  private static int countClaimsForExtractionRun(IntakeExtractionRunId extractionRunId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM governance.claim_ledger_item
            WHERE organization_id = ?
              AND source_span_ref LIKE ?
            """)) {
      statement.setObject(1, ORG_A);
      statement.setString(2, "%intake.extraction_run:" + extractionRunId.value() + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
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

  private static boolean indexExists(String schema, String table, String indexName)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM pg_indexes "
                + "WHERE schemaname = ? AND tablename = ? AND indexname = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, indexName);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
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
      statement.setString(2, "Task 5C Org " + organizationId);
      statement.setString(3, "Task 5C Org");
      statement.executeUpdate();
    }
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

  private static String deterministicTail(String suffix) {
    return switch (suffix) {
      case "eligible-fixture" -> "000000180101";
      case "duplicate" -> "000000180102";
      case "org-scope" -> "000000180103";
      default -> "000000180199";
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private record PersistedClaim(
      UUID organizationId,
      String entityType,
      UUID entityId,
      String claimType,
      String assertionStrength,
      String sourceSpanRef,
      String verificationStatus,
      boolean canonicalWriteAllowed,
      String clientShareability,
      String targetFieldPath,
      String claimValue,
      UUID sourceItemId) {
  }
}
