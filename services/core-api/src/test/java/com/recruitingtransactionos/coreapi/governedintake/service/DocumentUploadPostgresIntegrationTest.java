package com.recruitingtransactionos.coreapi.governedintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreException;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.documentstorage.NoOpVirusScanPort;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class DocumentUploadPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000201001");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000201002");
  private static final byte[] FILE_CONTENT = "same-cv-content".getBytes();

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
  void duplicateUploadAllowsIndependentSourceItemsWhileReusingBlob() throws Exception {
    insertOrganization(ORGANIZATION_ID);
    DocumentUploadService uploadService = uploadService(
        new JdbcInformationPacketPersistencePort(dataSource),
        new RecordingDocumentStore());

    DocumentUploadResult first = uploadService.upload(
        uploadCommand(),
        new ByteArrayInputStream(FILE_CONTENT));
    DocumentUploadResult second = uploadService.upload(
        uploadCommand(),
        new ByteArrayInputStream(FILE_CONTENT));

    assertThat(migrateResult.migrationsExecuted).isEqualTo(32);
    assertThat(first.sourceItemId()).isNotEqualTo(second.sourceItemId());
    assertThat(first.storageRef()).isEqualTo(second.storageRef());
    assertThat(first.scanStatus()).isEqualTo("clean");
    assertThat(second.scanStatus()).isEqualTo("clean");
    assertThat(countRows("intake.source_item", ORGANIZATION_ID)).isEqualTo(2);
    assertThat(countRows("intake.information_packet", ORGANIZATION_ID)).isEqualTo(2);
    assertThat(countRows("intake.information_packet_source_item", ORGANIZATION_ID)).isEqualTo(2);
    assertThat(countRows("workflow.workflow_event", ORGANIZATION_ID)).isEqualTo(4);
    assertThat(countWorkflowEventsByAction(ORGANIZATION_ID, WorkflowActionCode.SOURCE_ITEM_REGISTERED))
        .isEqualTo(2);
    assertThat(countWorkflowEventsByAction(
        ORGANIZATION_ID, WorkflowActionCode.INFORMATION_PACKET_CREATED)).isEqualTo(2);
  }

  @Test
  void failedAttachRollsBackDatabaseWritesAndDeletesNewBlob() throws Exception {
    insertOrganization(ORGANIZATION_ID);
    RecordingDocumentStore documentStore = new RecordingDocumentStore();
    JdbcInformationPacketPersistencePort delegate =
        new JdbcInformationPacketPersistencePort(dataSource);
    DocumentUploadService uploadService = uploadService(
        new FailingAttachInformationPacketPort(delegate),
        documentStore);

    assertThatThrownBy(() -> uploadService.upload(
        uploadCommand(),
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("attach failed");

    assertThat(countRows("intake.source_item", ORGANIZATION_ID)).isZero();
    assertThat(countRows("intake.information_packet", ORGANIZATION_ID)).isZero();
    assertThat(countRows("intake.information_packet_source_item", ORGANIZATION_ID)).isZero();
    assertThat(countRows("workflow.workflow_event", ORGANIZATION_ID)).isZero();
    assertThat(documentStore.storedObjectCount()).isZero();
  }

  private static DocumentUploadService uploadService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      DocumentStore documentStore) {
    JdbcSourceItemPersistencePort sourceItemPort = new JdbcSourceItemPersistencePort(dataSource);
    GovernedIntakeService governedIntakeService = new GovernedIntakeService(
        sourceItemPort,
        informationPacketPersistencePort);
    return new DocumentUploadService(
        sourceItemPort,
        governedIntakeService,
        documentStore,
        new NoOpVirusScanPort(),
        new SpringCanonicalWriteTransactionBoundary(new DataSourceTransactionManager(dataSource)),
        new WorkflowTransitionAuditService(
            new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
            new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
              @Override
              public java.util.Optional<String> getCurrentStateJson(java.util.UUID orgId, String ns, String type, java.util.UUID id) { return java.util.Optional.empty(); }
            }));
  }

  private static DocumentUploadCommand uploadCommand() {
    return new DocumentUploadCommand.Builder(
        ORGANIZATION_ID,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        ActorRole.CONSULTANT)
        .title("Candidate CV")
        .uploadedByActorId(ACTOR_ID)
        .originalFilename("cv.pdf")
        .mimeType("application/pdf")
        .contentLength(FILE_CONTENT.length)
        .build();
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement deleteWorkflowEvent = connection.prepareStatement(
             "DELETE FROM workflow.workflow_event WHERE organization_id = ?");
         PreparedStatement deletePacketSource = connection.prepareStatement(
             "DELETE FROM intake.information_packet_source_item WHERE organization_id = ?");
         PreparedStatement deletePacket = connection.prepareStatement(
             "DELETE FROM intake.information_packet WHERE organization_id = ?");
         PreparedStatement deleteSource = connection.prepareStatement(
             "DELETE FROM intake.source_item WHERE organization_id = ?");
         PreparedStatement deleteUser = connection.prepareStatement(
             "DELETE FROM identity.user_account WHERE organization_id = ?");
         PreparedStatement deleteOrg = connection.prepareStatement(
             "DELETE FROM identity.organization WHERE organization_id = ?");
         PreparedStatement insertOrg = connection.prepareStatement("""
             INSERT INTO identity.organization (
               organization_id, legal_name, display_name, status, default_timezone
             ) VALUES (?, ?, ?, 'active', 'UTC')
             """);
         PreparedStatement insertUser = connection.prepareStatement("""
             INSERT INTO identity.user_account (
               user_account_id,
               organization_id,
               email,
               display_name,
               status,
               password_hash
             ) VALUES (?, ?, ?, ?, 'active', NULL)
             """)) {
      deleteWorkflowEvent.setObject(1, organizationId);
      deleteWorkflowEvent.executeUpdate();
      deletePacketSource.setObject(1, organizationId);
      deletePacketSource.executeUpdate();
      deletePacket.setObject(1, organizationId);
      deletePacket.executeUpdate();
      deleteSource.setObject(1, organizationId);
      deleteSource.executeUpdate();
      deleteUser.setObject(1, organizationId);
      deleteUser.executeUpdate();
      deleteOrg.setObject(1, organizationId);
      deleteOrg.executeUpdate();

      insertOrg.setObject(1, organizationId);
      insertOrg.setString(2, "org-" + organizationId);
      insertOrg.setString(3, "Org " + organizationId.toString().substring(30));
      insertOrg.executeUpdate();

      insertUser.setObject(1, ACTOR_ID);
      insertUser.setObject(2, organizationId);
      insertUser.setString(3, "consultant+" + organizationId + "@example.com");
      insertUser.setString(4, "Consultant User");
      insertUser.executeUpdate();
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
             "SELECT COUNT(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static int countWorkflowEventsByAction(
      UUID organizationId,
      WorkflowActionCode actionCode) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT COUNT(*)
             FROM workflow.workflow_event
             WHERE organization_id = ?
               AND action = ?
             """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, actionCode.wireValue());
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
        // no-op for test datasource
      }

      @Override
      public void setLoginTimeout(int seconds) {
        // no-op for test datasource
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

  private static final class FailingAttachInformationPacketPort
      implements InformationPacketPersistencePort {

    private final JdbcInformationPacketPersistencePort delegate;

    private FailingAttachInformationPacketPort(JdbcInformationPacketPersistencePort delegate) {
      this.delegate = delegate;
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      return delegate.create(command);
    }

    @Override
    public java.util.Optional<InformationPacket> findById(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      return delegate.findById(organizationId, informationPacketId);
    }

    @Override
    public boolean hasSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      return delegate.hasSourceItem(organizationId, informationPacketId, sourceItemId);
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      throw new IllegalStateException("attach failed");
    }

    @Override
    public List<SourceItem> listSourceItems(UUID organizationId, InformationPacketId informationPacketId) {
      return delegate.listSourceItems(organizationId, informationPacketId);
    }
  }

  private static final class RecordingDocumentStore implements DocumentStore {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public void store(DocumentStoreKey key, InputStream content, long contentLength) {
      try {
        storage.put(key.storagePath(), content.readAllBytes());
      } catch (Exception exception) {
        throw new DocumentStoreException("Failed to store document", exception);
      }
    }

    @Override
    public InputStream retrieve(DocumentStoreKey key) {
      byte[] bytes = storage.get(key.storagePath());
      if (bytes == null) {
        throw new DocumentStoreException("Document not found");
      }
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean exists(DocumentStoreKey key) {
      return storage.containsKey(key.storagePath());
    }

    @Override
    public void delete(DocumentStoreKey key) {
      storage.remove(key.storagePath());
    }

    private int storedObjectCount() {
      return storage.size();
    }
  }
}
