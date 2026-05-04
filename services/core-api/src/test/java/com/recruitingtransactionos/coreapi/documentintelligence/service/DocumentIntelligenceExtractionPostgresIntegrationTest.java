package com.recruitingtransactionos.coreapi.documentintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.documentintelligence.persistence.JdbcDocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.HexFormat;
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
class DocumentIntelligenceExtractionPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000224001");
  private static final UUID USER_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000224002");
  private static final Instant RECEIVED_AT = Instant.parse("2026-05-01T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;
  private static MigrateResult migrateResult;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    seedOrganization(ORGANIZATION_ID);
  }

  @Test
  void failedDocumentIntelligenceExtractionPersistsWithoutOutputEnvelope() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(28);
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(sourceCommand().build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand().build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORGANIZATION_ID,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    IntakeExtractionRun run = extractionService().extract(ORGANIZATION_ID, packet.informationPacketId());
    String expectedSourceSnapshotHash = expectedSourceSnapshotHash(packet, sourceItem);

    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.FAILED);
    assertThat(run.failureReason()).hasValue("no_documents_ready_for_evidence_retrieval_pending_external_processing");
    assertThat(run.outputEnvelope()).isEmpty();
    assertThat(run.sourceSnapshotHash()).isEqualTo(expectedSourceSnapshotHash);
    assertThat(extractionPort().findById(ORGANIZATION_ID, run.extractionRunId()))
        .hasValueSatisfying(readBack -> {
          assertThat(readBack.status()).isEqualTo(IntakeExtractionStatus.FAILED);
          assertThat(readBack.outputEnvelope()).isEmpty();
          assertThat(readBack.sourceSnapshotHash()).isEqualTo(expectedSourceSnapshotHash);
        });
    assertThat(outputJsonIsNull(run.extractionRunId().value())).isTrue();
    assertThat(parsedDocumentCount(packet.informationPacketId())).isEqualTo(1);
  }

  private static GovernedIntakeService intakeService() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static DocumentIntelligenceExtractionService extractionService() {
    JdbcDocumentIntelligencePersistencePort documentIntelligencePort =
        new JdbcDocumentIntelligencePersistencePort(dataSource);
    return new DocumentIntelligenceExtractionService(
        new JdbcInformationPacketPersistencePort(dataSource),
        extractionPort(),
        new DocumentParsingService(
            intakeService(),
            new NoOpDocumentStore(),
            documentIntelligencePort,
            new NoOpDocumentConversionWorkerPort()),
        documentIntelligencePort);
  }

  private static JdbcIntakeExtractionRunPort extractionPort() {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand() {
    return SourceItemRegistrationCommand.builder()
        .organizationId(ORGANIZATION_ID)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Candidate image CV")
        .contentHash("sha256:224001-image")
        .storageRef("s3://internal-intake/224001-image")
        .language("en")
        .uploadedByActorType(ActorRole.CONSULTANT)
        .uploadedByActorId(USER_ACCOUNT_ID)
        .receivedAt(RECEIVED_AT)
        .metadataJson("{\"case\":\"224001-image\"}")
        .status(SourceItemStatus.RECEIVED)
        .mimeType("image/png")
        .fileSizeBytes(3L)
        .originalFilename("candidate.png")
        .scanStatus("clean");
  }

  private static InformationPacketCreateCommand.Builder packetCommand() {
    return InformationPacketCreateCommand.builder()
        .organizationId(ORGANIZATION_ID)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .createdByActorId(USER_ACCOUNT_ID)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("document intelligence pending packet")
        .metadataJson("{\"packet\":\"224001\"}");
  }

  private static boolean outputJsonIsNull(UUID extractionRunId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT output_json IS NULL
            FROM intake.extraction_run
            WHERE organization_id = ?
              AND extraction_run_id = ?
            """)) {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setObject(2, extractionRunId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static int parsedDocumentCount(InformationPacketId informationPacketId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM intake.parsed_document parsed_document
            JOIN intake.source_item source_item
              ON source_item.organization_id = parsed_document.organization_id
             AND source_item.source_item_id = parsed_document.source_item_id
            JOIN intake.information_packet_source_item link
              ON link.organization_id = source_item.organization_id
             AND link.source_item_id = source_item.source_item_id
            WHERE parsed_document.organization_id = ?
              AND link.information_packet_id = ?
            """)) {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setObject(2, informationPacketId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static String expectedSourceSnapshotHash(InformationPacket packet, SourceItem sourceItem) {
    String payload = packet.organizationId()
        + "|"
        + packet.informationPacketId().value()
        + "|"
        + packet.packetType().wireValue()
        + "|"
        + packet.intendedEntityType().wireValue()
        + "|"
        + sourceItem.sourceItemId().value()
        + ":"
        + sourceItem.sourceType().wireValue()
        + ":"
        + nullToEmpty(sourceItem.title())
        + ":"
        + nullToEmpty(sourceItem.contentHash())
        + ":"
        + nullToEmpty(sourceItem.externalRef());
    return "sha256:" + sha256(payload);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private static void seedOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement insertOrg = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id, legal_name, display_name, status, default_timezone
            ) VALUES (?, ?, ?, 'active', 'UTC')
            ON CONFLICT (organization_id) DO NOTHING
            """);
        PreparedStatement insertUser = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id, organization_id, email, display_name, status, password_hash
            ) VALUES (?, ?, ?, ?, 'active', NULL)
            ON CONFLICT (user_account_id) DO NOTHING
            """)) {
      insertOrg.setObject(1, organizationId);
      insertOrg.setString(2, "org-" + organizationId);
      insertOrg.setString(3, "Org 224001");
      insertOrg.executeUpdate();

      insertUser.setObject(1, USER_ACCOUNT_ID);
      insertUser.setObject(2, organizationId);
      insertUser.setString(3, "consultant+" + organizationId + "@example.com");
      insertUser.setString(4, "Consultant User");
      insertUser.executeUpdate();
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

  private static final class NoOpDocumentStore implements DocumentStore {
    @Override
    public void store(DocumentStoreKey key, java.io.InputStream content, long contentLength) {
      throw new UnsupportedOperationException("store is not used in this test");
    }

    @Override
    public java.io.InputStream retrieve(DocumentStoreKey key) {
      return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public boolean exists(DocumentStoreKey key) {
      return false;
    }

    @Override
    public void delete(DocumentStoreKey key) {
      throw new UnsupportedOperationException("delete is not used in this test");
    }
  }
}
