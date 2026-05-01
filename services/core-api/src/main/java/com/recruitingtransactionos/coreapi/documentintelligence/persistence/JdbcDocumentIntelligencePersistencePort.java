package com.recruitingtransactionos.coreapi.documentintelligence.persistence;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentSpan;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcDocumentIntelligencePersistencePort
    implements DocumentIntelligencePersistencePort {

  private static final String INSERT_PARSED_DOCUMENT_SQL = """
      INSERT INTO intake.parsed_document (
        parsed_document_id,
        organization_id,
        source_item_id,
        processing_status,
        parser_name,
        parser_version,
        media_type,
        content_hash,
        language,
        ocr_required,
        failure_reason,
        created_at,
        completed_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_LATEST_BY_SOURCE_ITEM_SQL = """
      SELECT
        parsed_document_id,
        organization_id,
        source_item_id,
        processing_status,
        parser_name,
        parser_version,
        media_type,
        content_hash,
        language,
        ocr_required,
        failure_reason,
        created_at,
        completed_at
      FROM intake.parsed_document
      WHERE organization_id = ?
        AND source_item_id = ?
      ORDER BY created_at DESC, parsed_document_id DESC
      LIMIT 1
      """;

  private static final String DELETE_SPANS_SQL = """
      DELETE FROM intake.parsed_document_span
      WHERE organization_id = ?
        AND parsed_document_id = ?
      """;

  private static final String DELETE_CHUNKS_SQL = """
      DELETE FROM intake.parsed_document_chunk
      WHERE organization_id = ?
        AND parsed_document_id = ?
      """;

  private static final String INSERT_CHUNK_SQL = """
      INSERT INTO intake.parsed_document_chunk (
        parsed_document_chunk_id,
        organization_id,
        parsed_document_id,
        chunk_index,
        page_number,
        start_offset,
        end_offset,
        chunk_text,
        created_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String INSERT_SPAN_SQL = """
      INSERT INTO intake.parsed_document_span (
        parsed_document_span_id,
        organization_id,
        parsed_document_id,
        parsed_document_chunk_id,
        span_index,
        page_number,
        start_offset,
        end_offset,
        created_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String LIST_CHUNKS_SQL = """
      SELECT
        parsed_document_chunk_id,
        organization_id,
        parsed_document_id,
        chunk_index,
        page_number,
        start_offset,
        end_offset,
        chunk_text,
        created_at
      FROM intake.parsed_document_chunk
      WHERE organization_id = ?
        AND parsed_document_id = ?
      ORDER BY chunk_index ASC
      """;

  private static final String LIST_SPANS_SQL = """
      SELECT
        parsed_document_span_id,
        organization_id,
        parsed_document_id,
        parsed_document_chunk_id,
        span_index,
        page_number,
        start_offset,
        end_offset,
        created_at
      FROM intake.parsed_document_span
      WHERE organization_id = ?
        AND parsed_document_id = ?
      ORDER BY parsed_document_chunk_id ASC, span_index ASC
      """;

  private final DataSource dataSource;

  public JdbcDocumentIntelligencePersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ParsedDocument saveParsedDocument(ParsedDocument parsedDocument) {
    Objects.requireNonNull(parsedDocument, "parsedDocument must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_PARSED_DOCUMENT_SQL)) {
      bindParsedDocument(statement, parsedDocument);
      statement.executeUpdate();
      return findLatestParsedDocumentBySourceItem(
          parsedDocument.organizationId(), parsedDocument.sourceItemId()).orElseThrow();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to save parsed document", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public ParsedDocument saveParsedDocumentWithArtifacts(
      ParsedDocument parsedDocument,
      List<ParsedDocumentChunk> chunks,
      List<ParsedDocumentSpan> spans) {
    Objects.requireNonNull(parsedDocument, "parsedDocument must not be null");
    Objects.requireNonNull(chunks, "chunks must not be null");
    Objects.requireNonNull(spans, "spans must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    boolean transactional = DataSourceUtils.isConnectionTransactional(connection, dataSource);
    boolean originalAutoCommit = true;
    try {
      if (!transactional) {
        originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
      }
      try (PreparedStatement insertParsedDocument = connection.prepareStatement(INSERT_PARSED_DOCUMENT_SQL);
          PreparedStatement insertChunk = connection.prepareStatement(INSERT_CHUNK_SQL);
          PreparedStatement insertSpan = connection.prepareStatement(INSERT_SPAN_SQL)) {
        bindParsedDocument(insertParsedDocument, parsedDocument);
        insertParsedDocument.executeUpdate();
        for (ParsedDocumentChunk chunk : chunks) {
          bindChunk(insertChunk, chunk);
          insertChunk.addBatch();
        }
        insertChunk.executeBatch();
        for (ParsedDocumentSpan span : spans) {
          bindSpan(insertSpan, span);
          insertSpan.addBatch();
        }
        insertSpan.executeBatch();
      }
      if (!transactional) {
        connection.commit();
      }
      return findLatestParsedDocumentBySourceItem(
          parsedDocument.organizationId(), parsedDocument.sourceItemId()).orElseThrow();
    } catch (SQLException exception) {
      rollbackIfNecessary(connection, transactional);
      throw new IllegalStateException("Failed to save parsed document with artifacts", exception);
    } finally {
      restoreAutoCommit(connection, transactional, originalAutoCommit);
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public void replaceChunksAndSpans(
      UUID organizationId,
      UUID parsedDocumentId,
      List<ParsedDocumentChunk> chunks,
      List<ParsedDocumentSpan> spans) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    Objects.requireNonNull(chunks, "chunks must not be null");
    Objects.requireNonNull(spans, "spans must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement deleteSpans = connection.prepareStatement(DELETE_SPANS_SQL);
        PreparedStatement deleteChunks = connection.prepareStatement(DELETE_CHUNKS_SQL);
        PreparedStatement insertChunk = connection.prepareStatement(INSERT_CHUNK_SQL);
        PreparedStatement insertSpan = connection.prepareStatement(INSERT_SPAN_SQL)) {
      deleteSpans.setObject(1, organizationId);
      deleteSpans.setObject(2, parsedDocumentId);
      deleteSpans.executeUpdate();
      deleteChunks.setObject(1, organizationId);
      deleteChunks.setObject(2, parsedDocumentId);
      deleteChunks.executeUpdate();
      for (ParsedDocumentChunk chunk : chunks) {
        bindChunk(insertChunk, chunk);
        insertChunk.addBatch();
      }
      insertChunk.executeBatch();
      for (ParsedDocumentSpan span : spans) {
        bindSpan(insertSpan, span);
        insertSpan.addBatch();
      }
      insertSpan.executeBatch();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to replace parsed document chunks and spans", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<ParsedDocument> findLatestParsedDocumentBySourceItem(
      UUID organizationId,
      SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_BY_SOURCE_ITEM_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, sourceItemId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toParsedDocument(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find latest parsed document", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ParsedDocumentChunk> listChunksByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
    return listChunks(organizationId, parsedDocumentId);
  }

  @Override
  public List<ParsedDocumentSpan> listSpansByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(LIST_SPANS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, parsedDocumentId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ParsedDocumentSpan> spans = new ArrayList<>();
        while (resultSet.next()) {
          spans.add(new ParsedDocumentSpan(
              resultSet.getObject("parsed_document_span_id", UUID.class),
              resultSet.getObject("organization_id", UUID.class),
              resultSet.getObject("parsed_document_id", UUID.class),
              resultSet.getObject("parsed_document_chunk_id", UUID.class),
              resultSet.getInt("span_index"),
              (Integer) resultSet.getObject("page_number"),
              resultSet.getInt("start_offset"),
              resultSet.getInt("end_offset"),
              instant(resultSet, "created_at")));
        }
        return List.copyOf(spans);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list parsed document spans", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private List<ParsedDocumentChunk> listChunks(UUID organizationId, UUID parsedDocumentId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(parsedDocumentId, "parsedDocumentId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(LIST_CHUNKS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, parsedDocumentId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<ParsedDocumentChunk> chunks = new ArrayList<>();
        while (resultSet.next()) {
          chunks.add(new ParsedDocumentChunk(
              resultSet.getObject("parsed_document_chunk_id", UUID.class),
              resultSet.getObject("organization_id", UUID.class),
              resultSet.getObject("parsed_document_id", UUID.class),
              resultSet.getInt("chunk_index"),
              (Integer) resultSet.getObject("page_number"),
              resultSet.getInt("start_offset"),
              resultSet.getInt("end_offset"),
              resultSet.getString("chunk_text"),
              instant(resultSet, "created_at")));
        }
        return List.copyOf(chunks);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list parsed document chunks", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static ParsedDocument toParsedDocument(ResultSet resultSet) throws SQLException {
    return new ParsedDocument(
        resultSet.getObject("parsed_document_id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        new SourceItemId(resultSet.getObject("source_item_id", UUID.class)),
        DocumentProcessingStatus.fromWireValue(resultSet.getString("processing_status")),
        resultSet.getString("parser_name"),
        resultSet.getString("parser_version"),
        resultSet.getString("media_type"),
        resultSet.getString("content_hash"),
        resultSet.getString("language"),
        resultSet.getBoolean("ocr_required"),
        instant(resultSet, "created_at"),
        Optional.ofNullable(resultSet.getObject("completed_at", OffsetDateTime.class)).map(OffsetDateTime::toInstant),
        Optional.ofNullable(resultSet.getString("failure_reason")));
  }

  private static void bindParsedDocument(PreparedStatement statement, ParsedDocument parsedDocument)
      throws SQLException {
    statement.setObject(1, parsedDocument.parsedDocumentId());
    statement.setObject(2, parsedDocument.organizationId());
    statement.setObject(3, parsedDocument.sourceItemId().value());
    statement.setString(4, parsedDocument.processingStatus().wireValue());
    statement.setString(5, parsedDocument.parserName());
    statement.setString(6, parsedDocument.parserVersion());
    statement.setString(7, parsedDocument.mediaType());
    setNullableString(statement, 8, parsedDocument.contentHash());
    setNullableString(statement, 9, parsedDocument.language());
    statement.setBoolean(10, parsedDocument.ocrRequired());
    setNullableString(statement, 11, parsedDocument.failureReason().orElse(null));
    statement.setObject(12, OffsetDateTime.ofInstant(parsedDocument.createdAt(), ZoneOffset.UTC));
    if (parsedDocument.completedAt().isPresent()) {
      statement.setObject(13, OffsetDateTime.ofInstant(parsedDocument.completedAt().orElseThrow(), ZoneOffset.UTC));
    } else {
      statement.setNull(13, Types.TIMESTAMP_WITH_TIMEZONE);
    }
  }

  private static void bindChunk(PreparedStatement statement, ParsedDocumentChunk chunk) throws SQLException {
    statement.setObject(1, chunk.parsedDocumentChunkId());
    statement.setObject(2, chunk.organizationId());
    statement.setObject(3, chunk.parsedDocumentId());
    statement.setInt(4, chunk.chunkIndex());
    setNullableInteger(statement, 5, chunk.pageNumber());
    statement.setInt(6, chunk.startOffset());
    statement.setInt(7, chunk.endOffset());
    statement.setString(8, chunk.chunkText());
    statement.setObject(9, OffsetDateTime.ofInstant(chunk.createdAt(), ZoneOffset.UTC));
  }

  private static void bindSpan(PreparedStatement statement, ParsedDocumentSpan span) throws SQLException {
    statement.setObject(1, span.parsedDocumentSpanId());
    statement.setObject(2, span.organizationId());
    statement.setObject(3, span.parsedDocumentId());
    statement.setObject(4, span.parsedDocumentChunkId());
    statement.setInt(5, span.spanIndex());
    setNullableInteger(statement, 6, span.pageNumber());
    statement.setInt(7, span.startOffset());
    statement.setInt(8, span.endOffset());
    statement.setObject(9, OffsetDateTime.ofInstant(span.createdAt(), ZoneOffset.UTC));
  }

  private static Instant instant(ResultSet resultSet, String column) throws SQLException {
    return resultSet.getObject(column, OffsetDateTime.class).toInstant();
  }

  private static void setNullableString(PreparedStatement statement, int index, String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
      return;
    }
    statement.setString(index, value);
  }

  private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.INTEGER);
      return;
    }
    statement.setInt(index, value);
  }

  private static void rollbackIfNecessary(Connection connection, boolean transactional) {
    if (transactional) {
      return;
    }
    try {
      connection.rollback();
    } catch (SQLException rollbackException) {
      throw new IllegalStateException("Failed to rollback parsed document persistence", rollbackException);
    }
  }

  private static void restoreAutoCommit(
      Connection connection,
      boolean transactional,
      boolean originalAutoCommit) {
    if (transactional) {
      return;
    }
    try {
      connection.setAutoCommit(originalAutoCommit);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to restore parsed document connection state", exception);
    }
  }
}
