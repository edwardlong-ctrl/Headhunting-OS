package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcInformationPacketPersistencePort implements InformationPacketPersistencePort {

  private static final String INSERT_PACKET_SQL = """
      INSERT INTO intake.information_packet (
        information_packet_id,
        organization_id,
        packet_type,
        intended_entity_type,
        intended_entity_id,
        created_by_actor_type,
        created_by_actor_id,
        processing_status,
        notes,
        metadata_json
      )
      VALUES (?, ?, ?, ?, ?, ?::governance.actor_role, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_PACKET_BY_ID_SQL = """
      SELECT
        information_packet_id,
        organization_id,
        packet_type,
        intended_entity_type,
        intended_entity_id,
        created_by_actor_type::text AS created_by_actor_type,
        created_by_actor_id,
        processing_status,
        created_at,
        updated_at,
        notes,
        metadata_json::text AS metadata_json
      FROM intake.information_packet
      WHERE organization_id = ?
        AND information_packet_id = ?
      """;

  private static final String HAS_SOURCE_ITEM_SQL = """
      SELECT EXISTS (
        SELECT 1
        FROM intake.information_packet_source_item
        WHERE organization_id = ?
          AND information_packet_id = ?
          AND source_item_id = ?
      )
      """;

  private static final String ATTACH_SOURCE_SQL = """
      INSERT INTO intake.information_packet_source_item (
        organization_id,
        information_packet_id,
        source_item_id
      )
      VALUES (?, ?, ?)
      """;

  private static final String LIST_SOURCE_ITEMS_SQL = """
      SELECT
        source_item.source_item_id,
        source_item.organization_id,
        source_item.source_type,
        source_item.origin,
        source_item.title,
        source_item.content_hash,
        source_item.external_ref,
        source_item.storage_ref,
        source_item.raw_ref,
        source_item.language,
        source_item.uploaded_by_actor_type::text AS uploaded_by_actor_type,
        source_item.uploaded_by_actor_id,
        source_item.received_at,
        source_item.created_at,
        source_item.metadata_json::text AS metadata_json,
        source_item.status,
        source_item.mime_type,
        source_item.file_size_bytes,
        source_item.original_filename,
        source_item.scan_status
      FROM intake.information_packet_source_item packet_source
      JOIN intake.source_item source_item
        ON source_item.organization_id = packet_source.organization_id
       AND source_item.source_item_id = packet_source.source_item_id
      WHERE packet_source.organization_id = ?
        AND packet_source.information_packet_id = ?
      ORDER BY packet_source.attached_at ASC, packet_source.source_item_id ASC
      """;

  private final DataSource dataSource;

  public JdbcInformationPacketPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public InformationPacket create(InformationPacketCreateCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    InformationPacketId informationPacketId = new InformationPacketId(UUID.randomUUID());
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_PACKET_SQL)) {
      bindCreate(statement, informationPacketId, command);
      statement.executeUpdate();
      return findById(command.organizationId(), informationPacketId)
          .orElseThrow(() -> new IllegalStateException(
              "information packet was not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create information packet", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<InformationPacket> findById(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_PACKET_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, informationPacketId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toInformationPacket(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find information packet by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public boolean hasSourceItem(
      UUID organizationId,
      InformationPacketId informationPacketId,
      SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(HAS_SOURCE_ITEM_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, informationPacketId.value());
      statement.setObject(3, sourceItemId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return false;
        }
        return resultSet.getBoolean(1);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to check packet source link", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public void attachSourceItem(
      UUID organizationId,
      InformationPacketId informationPacketId,
      SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(ATTACH_SOURCE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, informationPacketId.value());
      statement.setObject(3, sourceItemId.value());
      statement.executeUpdate();
    } catch (SQLException exception) {
      if ("23505".equals(exception.getSQLState())) {
        throw new IllegalArgumentException(
            "source item already attached to information packet", exception);
      }
      throw new IllegalStateException("Failed to attach source item to information packet",
          exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<SourceItem> listSourceItems(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(LIST_SOURCE_ITEMS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, informationPacketId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<SourceItem> sourceItems = new ArrayList<>();
        while (resultSet.next()) {
          sourceItems.add(JdbcSourceItemPersistencePort.toSourceItem(resultSet));
        }
        return List.copyOf(sourceItems);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list source items for information packet",
          exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static InformationPacket toInformationPacket(ResultSet resultSet) throws SQLException {
    return new InformationPacket(
        new InformationPacketId(resultSet.getObject("information_packet_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        InformationPacketType.fromWireValue(resultSet.getString("packet_type")),
        IntendedEntityType.fromWireValue(resultSet.getString("intended_entity_type")),
        resultSet.getObject("intended_entity_id", UUID.class),
        actorRole(resultSet.getString("created_by_actor_type")),
        resultSet.getObject("created_by_actor_id", UUID.class),
        InformationPacketStatus.fromWireValue(resultSet.getString("processing_status")),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant(),
        resultSet.getString("notes"),
        resultSet.getString("metadata_json"));
  }

  private static void bindCreate(
      PreparedStatement statement,
      InformationPacketId informationPacketId,
      InformationPacketCreateCommand command) throws SQLException {
    statement.setObject(1, informationPacketId.value());
    statement.setObject(2, command.organizationId());
    statement.setString(3, command.packetType().wireValue());
    statement.setString(4, command.intendedEntityType().wireValue());
    setNullableUuid(statement, 5, command.intendedEntityId());
    statement.setString(6, command.createdByActorType().wireValue());
    setNullableUuid(statement, 7, command.createdByActorId());
    statement.setString(8, command.processingStatus().wireValue());
    setNullableString(statement, 9, command.notes());
    statement.setString(10, command.metadataJson());
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
