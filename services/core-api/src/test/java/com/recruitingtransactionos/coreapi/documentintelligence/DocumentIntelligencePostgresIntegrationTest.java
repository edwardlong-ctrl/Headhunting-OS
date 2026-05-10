package com.recruitingtransactionos.coreapi.documentintelligence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.documentintelligence.persistence.JdbcDocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
class DocumentIntelligencePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000222001");
  private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000222002");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000222003");
  private static final SourceItemId SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000222004"));

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
    seedOrganization(OTHER_ORGANIZATION_ID);
    seedSourceItem(ORGANIZATION_ID, SOURCE_ITEM_ID.value(), ACTOR_ID);
    seedSourceItem(OTHER_ORGANIZATION_ID,
        UUID.fromString("00000000-0000-0000-0000-000000222005"), ACTOR_ID);
  }

  @Test
  void savesAndLoadsLatestParsedDocumentWithinOrganization() {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(34);
    JdbcDocumentIntelligencePersistencePort port = new JdbcDocumentIntelligencePersistencePort(dataSource);
    Instant now = Instant.parse("2026-05-01T00:00:00Z");
    ParsedDocument parsedDocument = port.saveParsedDocument(new ParsedDocument(
        UUID.fromString("00000000-0000-0000-0000-000000222006"),
        ORGANIZATION_ID,
        SOURCE_ITEM_ID,
        DocumentProcessingStatus.SUCCEEDED,
        "txt-parser",
        "v1",
        "text/plain",
        "sha256:test",
        "en",
        false,
        now,
        Optional.of(now),
        Optional.empty()));
    ParsedDocumentChunk chunk = new ParsedDocumentChunk(
        UUID.fromString("00000000-0000-0000-0000-000000222007"),
        ORGANIZATION_ID,
        parsedDocument.parsedDocumentId(),
        0,
        null,
        0,
        32,
        "Nebula evidence chunk for retrieval",
        now);
    ParsedDocumentSpan span = new ParsedDocumentSpan(
        UUID.fromString("00000000-0000-0000-0000-000000222008"),
        ORGANIZATION_ID,
        parsedDocument.parsedDocumentId(),
        chunk.parsedDocumentChunkId(),
        0,
        null,
        0,
        32,
        now);
    port.replaceChunksAndSpans(ORGANIZATION_ID, parsedDocument.parsedDocumentId(), List.of(chunk), List.of(span));

    ParsedDocument loaded = port.findLatestParsedDocumentBySourceItem(ORGANIZATION_ID, SOURCE_ITEM_ID).orElseThrow();
    assertThat(loaded.processingStatus()).isEqualTo(DocumentProcessingStatus.SUCCEEDED);
    assertThat(port.listChunksByParsedDocument(ORGANIZATION_ID, parsedDocument.parsedDocumentId()))
        .hasSize(1)
        .first()
        .extracting(ParsedDocumentChunk::chunkText)
        .isEqualTo("Nebula evidence chunk for retrieval");
    assertThat(port.listSpansByParsedDocument(ORGANIZATION_ID, parsedDocument.parsedDocumentId()))
        .hasSize(1);
    assertThat(port.findLatestParsedDocumentBySourceItem(OTHER_ORGANIZATION_ID, SOURCE_ITEM_ID)).isEmpty();
  }

  private static void seedOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
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
      insertOrg.setString(3, "Org " + organizationId.toString().substring(30));
      insertOrg.executeUpdate();

      insertUser.setObject(1, ACTOR_ID);
      insertUser.setObject(2, organizationId);
      insertUser.setString(3, "consultant+" + organizationId + "@example.com");
      insertUser.setString(4, "Consultant User");
      insertUser.executeUpdate();
    }
  }

  private static void seedSourceItem(UUID organizationId, UUID sourceItemId, UUID actorId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO intake.source_item (
               source_item_id,
               organization_id,
               source_type,
               origin,
               title,
               content_hash,
               storage_ref,
               language,
               uploaded_by_actor_type,
               uploaded_by_actor_id,
               received_at,
               created_at,
               metadata_json,
               status,
               mime_type,
               file_size_bytes,
               original_filename,
               scan_status
             ) VALUES (?, ?, 'CV', 'CONSULTANT_UPLOAD', ?, 'sha256:test', ?, 'en', 'consultant', ?, ?, ?, '{}'::jsonb, 'RECEIVED', 'text/plain', 10, 'candidate.txt', 'clean')
             ON CONFLICT (source_item_id) DO NOTHING
             """)) {
      statement.setObject(1, sourceItemId);
      statement.setObject(2, organizationId);
      statement.setString(3, "Candidate CV");
      statement.setString(4, organizationId + "/" + sourceItemId + "/sha256_test/candidate.txt");
      statement.setObject(5, actorId);
      statement.setObject(6, java.time.OffsetDateTime.parse("2026-05-01T00:00:00Z"));
      statement.setObject(7, java.time.OffsetDateTime.parse("2026-05-01T00:00:00Z"));
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
      }

      @Override
      public void setLoginTimeout(int seconds) {
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
}
