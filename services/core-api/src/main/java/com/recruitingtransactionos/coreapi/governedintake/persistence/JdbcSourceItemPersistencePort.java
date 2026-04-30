package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcSourceItemPersistencePort implements SourceItemPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO intake.source_item (
        source_item_id,
        organization_id,
        source_type,
        origin,
        title,
        content_hash,
        external_ref,
        storage_ref,
        raw_ref,
        language,
        uploaded_by_actor_type,
        uploaded_by_actor_id,
        received_at,
        metadata_json,
        status,
        mime_type,
        file_size_bytes,
        original_filename,
        scan_status
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::governance.actor_role, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT
        source_item_id,
        organization_id,
        source_type,
        origin,
        title,
        content_hash,
        external_ref,
        storage_ref,
        raw_ref,
        language,
        uploaded_by_actor_type::text AS uploaded_by_actor_type,
        uploaded_by_actor_id,
        received_at,
        created_at,
        metadata_json::text AS metadata_json,
        status,
        mime_type,
        file_size_bytes,
        original_filename,
        scan_status
      FROM intake.source_item
      WHERE organization_id = ?
        AND source_item_id = ?
      """;

  private static final String FIND_BY_CONTENT_HASH_SQL = """
      SELECT
        source_item_id,
        organization_id,
        source_type,
        origin,
        title,
        content_hash,
        external_ref,
        storage_ref,
        raw_ref,
        language,
        uploaded_by_actor_type::text AS uploaded_by_actor_type,
        uploaded_by_actor_id,
        received_at,
        created_at,
        metadata_json::text AS metadata_json,
        status,
        mime_type,
        file_size_bytes,
        original_filename,
        scan_status
      FROM intake.source_item
      WHERE organization_id = ?
        AND content_hash = ?
      ORDER BY created_at ASC, source_item_id ASC
      LIMIT 1
      """;

  private final DataSource dataSource;

  public JdbcSourceItemPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public SourceItem append(SourceItemRegistrationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    SourceItemId sourceItemId = command.sourceItemId() != null
        ? command.sourceItemId()
        : new SourceItemId(UUID.randomUUID());
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bindAppend(statement, sourceItemId, command);
      statement.executeUpdate();
      return findById(command.organizationId(), sourceItemId)
          .orElseThrow(() -> new IllegalStateException("source item was not readable after append"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append source item", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, sourceItemId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSourceItem(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find source item by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<SourceItem> findByContentHash(UUID organizationId, String contentHash) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(contentHash, "contentHash must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CONTENT_HASH_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, contentHash);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSourceItem(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find source item by content hash", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  static SourceItem toSourceItem(ResultSet resultSet) throws SQLException {
    return new SourceItem(
        new SourceItemId(resultSet.getObject("source_item_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        SourceItemType.fromWireValue(resultSet.getString("source_type")),
        SourceItemOrigin.fromWireValue(resultSet.getString("origin")),
        resultSet.getString("title"),
        resultSet.getString("content_hash"),
        resultSet.getString("external_ref"),
        resultSet.getString("storage_ref"),
        resultSet.getString("raw_ref"),
        resultSet.getString("language"),
        actorRole(resultSet.getString("uploaded_by_actor_type")),
        resultSet.getObject("uploaded_by_actor_id", UUID.class),
        resultSet.getObject("received_at", OffsetDateTime.class).toInstant(),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
        resultSet.getString("metadata_json"),
        SourceItemStatus.fromWireValue(resultSet.getString("status")),
        resultSet.getString("mime_type"),
        nullableLong(resultSet, "file_size_bytes"),
        resultSet.getString("original_filename"),
        resultSet.getString("scan_status"));
  }

  private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
    long value = resultSet.getLong(column);
    return resultSet.wasNull() ? null : value;
  }

  private static void bindAppend(
      PreparedStatement statement,
      SourceItemId sourceItemId,
      SourceItemRegistrationCommand command) throws SQLException {
    statement.setObject(1, sourceItemId.value());
    statement.setObject(2, command.organizationId());
    statement.setString(3, command.sourceType().wireValue());
    statement.setString(4, command.origin().wireValue());
    setNullableString(statement, 5, command.title());
    setNullableString(statement, 6, command.contentHash());
    setNullableString(statement, 7, command.externalRef());
    setNullableString(statement, 8, command.storageRef());
    setNullableString(statement, 9, command.rawRef());
    setNullableString(statement, 10, command.language());
    statement.setString(11, command.uploadedByActorType().wireValue());
    setNullableUuid(statement, 12, command.uploadedByActorId());
    statement.setObject(13, OffsetDateTime.ofInstant(command.receivedAt(), ZoneOffset.UTC));
    statement.setString(14, command.metadataJson());
    statement.setString(15, command.status().wireValue());
    setNullableString(statement, 16, command.mimeType());
    setNullableLong(statement, 17, command.fileSizeBytes());
    setNullableString(statement, 18, command.originalFilename());
    String scanStatus = command.scanStatus();
    statement.setString(19, scanStatus != null ? scanStatus : "not_scanned");
  }

  private static void setNullableLong(
      PreparedStatement statement,
      int index,
      Long value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.BIGINT);
      return;
    }
    statement.setLong(index, value);
  }

  private static ActorRole actorRole(String wireValue) {
    for (ActorRole role : ActorRole.values()) {
      if (role.wireValue().equals(wireValue)) {
        return role;
      }
    }
    throw new IllegalArgumentException("unknown actor role: " + wireValue);
  }

  private static void setNullableString(
      PreparedStatement statement,
      int index,
      String value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
      return;
    }
    statement.setString(index, value);
  }

  private static void setNullableUuid(
      PreparedStatement statement,
      int index,
      UUID value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.OTHER);
      return;
    }
    statement.setObject(index, value);
  }
}
