package com.recruitingtransactionos.coreapi.candidatedocument.persistence;

import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentStatus;
import com.recruitingtransactionos.coreapi.candidatedocument.port.CandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCandidateDocumentPersistencePort implements CandidateDocumentPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.candidate_document (
        candidate_document_id, organization_id, candidate_id, document_type,
        title, storage_ref, content_hash, source_item_id, language,
        status, superseded_by_document_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT candidate_document_id, organization_id, candidate_id, document_type,
        title, storage_ref, content_hash, source_item_id, language,
        status, superseded_by_document_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.candidate_document
      WHERE organization_id = ? AND candidate_document_id = ?
      """;

  private static final String FIND_BY_CANDIDATE_SQL = """
      SELECT candidate_document_id, organization_id, candidate_id, document_type,
        title, storage_ref, content_hash, source_item_id, language,
        status, superseded_by_document_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.candidate_document
      WHERE organization_id = ? AND candidate_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcCandidateDocumentPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public CandidateDocument create(CandidateDocument document) {
    Objects.requireNonNull(document, "document must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, document.candidateDocumentId().value());
      statement.setObject(2, document.organizationId());
      statement.setObject(3, document.candidateId().value());
      statement.setString(4, document.documentType());
      statement.setString(5, document.title());
      statement.setString(6, document.storageRef());
      statement.setString(7, document.contentHash());
      statement.setObject(8, document.sourceItemId());
      statement.setString(9, document.language());
      statement.setString(10, document.status().wireValue());
      statement.setObject(11, document.supersededByDocumentId());
      statement.setString(12, document.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(document.organizationId(), document.candidateDocumentId())
          .orElseThrow(() -> new IllegalStateException(
              "candidate_document not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create candidate_document", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<CandidateDocument> findByIdAndOrganizationId(
      UUID organizationId, CandidateDocumentId documentId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, documentId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toCandidateDocument(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidate_document by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CandidateDocument> findByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CANDIDATE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CandidateDocument> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCandidateDocument(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidate_documents by candidate", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static CandidateDocument toCandidateDocument(ResultSet rs) throws SQLException {
    return CandidateDocument.builder()
        .candidateDocumentId(new CandidateDocumentId(
            rs.getObject("candidate_document_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .candidateId(new CandidateId(rs.getObject("candidate_id", UUID.class)))
        .documentType(rs.getString("document_type"))
        .title(rs.getString("title"))
        .storageRef(rs.getString("storage_ref"))
        .contentHash(rs.getString("content_hash"))
        .sourceItemId(rs.getObject("source_item_id", UUID.class))
        .language(rs.getString("language"))
        .status(CandidateDocumentStatus.fromWireValue(rs.getString("status")))
        .supersededByDocumentId(rs.getObject("superseded_by_document_id", UUID.class))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
