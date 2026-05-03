package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.DeterministicIntakeExtractionService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
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
class IntakeExtractionPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000160001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000160002");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T11:00:00Z");

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
  void flywayMigrationCreatesExtractionRunTableAndIndexes() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(25);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "24", "25");
    assertThat(tableExists("intake", "extraction_run")).isTrue();
    assertThat(indexExists("intake", "extraction_run",
        "intake_extraction_run_org_packet_created_idx")).isTrue();
    assertThat(indexExists("intake", "extraction_run",
        "intake_extraction_run_org_status_idx")).isTrue();
  }

  @Test
  void extractionRunAndOutputEnvelopePersistAndReadBack() {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "readback").build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    IntakeExtractionRun run =
        extractionService().extract(ORG_A, packet.informationPacketId());

    Optional<IntakeExtractionRun> readBack =
        extractionPort().findById(ORG_A, run.extractionRunId());

    assertThat(readBack).contains(run);
    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.SUCCEEDED);
    assertThat(run.mode()).isEqualTo(IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER);
    assertThat(run.outputEnvelope()).isPresent();
    assertThat(run.outputEnvelope().orElseThrow().sourceItemIds())
        .containsExactly(sourceItem.sourceItemId());
    assertThat(run.outputEnvelope().orElseThrow().packetType())
        .isEqualTo(InformationPacketType.CANDIDATE);
    assertThat(fieldValue(run.outputEnvelope().orElseThrow(), "real_ai_extraction_performed"))
        .isEqualTo("false");
    assertThat(fieldValue(run.outputEnvelope().orElseThrow(), "claim_ledger_append_allowed"))
        .isEqualTo("false");
    assertThat(fieldValue(run.outputEnvelope().orElseThrow(), "canonical_write_allowed"))
        .isEqualTo("false");
  }

  @Test
  void extractionRunIsOrganizationScopedAndLinkedToIntakeInformationPacket() {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "scope").build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    IntakeExtractionRun run =
        extractionService().extract(ORG_A, packet.informationPacketId());

    assertThat(extractionPort().findById(ORG_A, run.extractionRunId())).isPresent();
    assertThat(extractionPort().findById(ORG_B, run.extractionRunId())).isEmpty();
    assertThat(extractionPort().listByInformationPacket(ORG_A, packet.informationPacketId()))
        .containsExactly(run);
    assertThat(extractionPort().listByInformationPacket(ORG_B, packet.informationPacketId()))
        .isEmpty();
  }

  @Test
  void extractionUsesIntakeTablesAndDoesNotWriteCanonicalOrGovernanceTables()
      throws SQLException {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "table-boundary").build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    extractionService().extract(ORG_A, packet.informationPacketId());

    assertThat(countRows("intake.source_item", ORG_A)).isGreaterThan(0);
    assertThat(countRows("intake.information_packet", ORG_A)).isGreaterThan(0);
    assertThat(countRows("intake.extraction_run", ORG_A)).isGreaterThan(0);
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
    assertThat(countRows("governance.claim_ledger_item", ORG_A)).isZero();
    assertThat(countRows("governance.review_event", ORG_A)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isZero();
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

  private static JdbcIntakeExtractionRunPort extractionPort() {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(
      UUID organizationId,
      String suffix) {
    String orgPrefix = organizationId.equals(ORG_A) ? "160001" : "160002";
    String value = orgPrefix + "-" + suffix;
    return SourceItemRegistrationCommand.builder()
        .organizationId(organizationId)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Senior DV engineer CV")
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
        .notes(organizationId.equals(ORG_A) ? "candidate packet 160001" : "candidate packet 160002")
        .metadataJson(organizationId.equals(ORG_A)
            ? "{\"packet\":\"160001\"}"
            : "{\"packet\":\"160002\"}");
  }

  private static String fieldValue(
      IntakeExtractionOutputEnvelope envelope,
      String fieldName) {
    return envelope.extractedFields().stream()
        .filter(field -> field.fieldName().equals(fieldName))
        .findFirst()
        .orElseThrow()
        .fieldValue();
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
      statement.setString(2, "Task 5B Org " + organizationId);
      statement.setString(3, "Task 5B Org");
      statement.executeUpdate();
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
    return exists(
        "SELECT EXISTS ("
            + "SELECT 1 FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')",
        schema,
        table);
  }

  private static boolean indexExists(String schema, String table, String indexName)
      throws SQLException {
    return exists(
        "SELECT EXISTS ("
            + "SELECT 1 FROM pg_indexes "
            + "WHERE schemaname = ? AND tablename = ? AND indexname = ?)",
        schema,
        table,
        indexName);
  }

  private static boolean exists(String sql, String... parameters) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
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
}
