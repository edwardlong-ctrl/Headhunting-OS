package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
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
class GovernedIntakePostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000060001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000060002");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T09:00:00Z");

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
  void flywayMigrationCreatesGovernedIntakeTables() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(6);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6");
    assertThat(schemaExists("intake")).isTrue();
    assertThat(tableExists("intake", "source_item")).isTrue();
    assertThat(tableExists("intake", "information_packet")).isTrue();
    assertThat(tableExists("intake", "information_packet_source_item")).isTrue();
    assertThat(indexExists("intake", "source_item", "intake_source_item_org_status_idx"))
        .isTrue();
    assertThat(indexExists("intake", "information_packet",
        "intake_information_packet_org_type_status_idx")).isTrue();
    assertThat(indexExists("intake", "information_packet_source_item",
        "intake_packet_source_item_source_idx")).isTrue();
  }

  @Test
  void sourceItemPersistsAndReadsBack() {
    SourceItem sourceItem = service().registerSourceItem(sourceCommand(ORG_A, "readback").build());

    Optional<SourceItem> readBack = service().findSourceItem(ORG_A, sourceItem.sourceItemId());

    assertThat(readBack).contains(sourceItem);
    assertThat(sourceItem.organizationId()).isEqualTo(ORG_A);
    assertThat(sourceItem.sourceType()).isEqualTo(SourceItemType.CV);
    assertThat(sourceItem.origin()).isEqualTo(SourceItemOrigin.CONSULTANT_UPLOAD);
    assertThat(sourceItem.status()).isEqualTo(SourceItemStatus.REGISTERED);
    assertThat(sourceItem.contentHash()).isEqualTo("sha256:060001-readback");
    assertThat(sourceItem.storageRef()).isEqualTo("s3://internal-intake/060001-readback");
    assertThat(sourceItem.rawRef()).isEqualTo("vault://source-060001-readback/raw");
    assertThat(sourceItem.metadataJson()).isEqualTo("{\"case\":\"060001-readback\"}");
  }

  @Test
  void informationPacketPersistsAndReadsBack() {
    InformationPacket packet = service().createInformationPacket(packetCommand(ORG_A).build());

    Optional<InformationPacket> readBack =
        service().findInformationPacket(ORG_A, packet.informationPacketId());

    assertThat(readBack).contains(packet);
    assertThat(packet.packetType()).isEqualTo(InformationPacketType.CANDIDATE);
    assertThat(packet.intendedEntityType()).isEqualTo(IntendedEntityType.CANDIDATE);
    assertThat(packet.processingStatus()).isEqualTo(InformationPacketStatus.CREATED);
    assertThat(packet.notes()).isEqualTo("candidate packet 060001");
  }

  @Test
  void sourceItemToInformationPacketLinkPersistsAndReadsBack() {
    GovernedIntakeService service = service();
    SourceItem sourceItem = service.registerSourceItem(sourceCommand(ORG_A, "link").build());
    InformationPacket packet = service.createInformationPacket(packetCommand(ORG_A).build());

    service.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    assertThat(service.listSourceItemsForPacket(ORG_A, packet.informationPacketId()))
        .containsExactly(sourceItem);
  }

  @Test
  void organizationIsolationWorksForReadAndAttachment() throws SQLException {
    GovernedIntakeService service = service();
    SourceItem orgASource = service.registerSourceItem(sourceCommand(ORG_A, "isolation-a").build());
    SourceItem orgBSource = service.registerSourceItem(sourceCommand(ORG_B, "isolation-b").build());
    InformationPacket orgAPacket = service.createInformationPacket(packetCommand(ORG_A).build());

    service.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        orgAPacket.informationPacketId(),
        orgASource.sourceItemId()));

    assertThat(service.findSourceItem(ORG_B, orgASource.sourceItemId())).isEmpty();
    assertThat(service.findInformationPacket(ORG_B, orgAPacket.informationPacketId())).isEmpty();
    assertThat(service.listSourceItemsForPacket(ORG_B, orgAPacket.informationPacketId())).isEmpty();

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.attachSourceItemToPacket(
        new AttachSourceItemToPacketCommand(ORG_A, orgAPacket.informationPacketId(),
            orgBSource.sourceItemId())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("source item not found in organization");

    assertThat(countRows("governance.claim_ledger_item", ORG_A)).isZero();
    assertThat(countRows("governance.review_event", ORG_A)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isZero();
  }

  private static GovernedIntakeService service() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(UUID organizationId) {
    return sourceCommand(organizationId, "default");
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(
      UUID organizationId,
      String suffix) {
    String orgPrefix = organizationId.equals(ORG_A) ? "060001" : "060002";
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
        .processingStatus(InformationPacketStatus.CREATED)
        .notes(organizationId.equals(ORG_A) ? "candidate packet 060001" : "candidate packet 060002")
        .metadataJson(organizationId.equals(ORG_A)
            ? "{\"packet\":\"060001\"}"
            : "{\"packet\":\"060002\"}");
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
      statement.setString(2, "Task 5A Org " + organizationId);
      statement.setString(3, "Task 5A Org");
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

  private static boolean schemaExists(String schema) throws SQLException {
    return exists(
        "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
        schema);
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
