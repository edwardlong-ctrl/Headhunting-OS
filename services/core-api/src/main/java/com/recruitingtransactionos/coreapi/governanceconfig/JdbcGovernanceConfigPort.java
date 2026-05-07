package com.recruitingtransactionos.coreapi.governanceconfig;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
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

public final class JdbcGovernanceConfigPort implements GovernanceConfigPort {

  private static final String SELECT_COLUMNS = """
      SELECT
        governance_config_id,
        organization_id,
        config_type,
        config_key,
        payload::text AS payload_json,
        enabled,
        created_by_user_id,
        updated_by_user_id,
        created_at,
        updated_at,
        version
      FROM governance.config_entry
      """;

  private static final String FIND_SQL = SELECT_COLUMNS
      + "WHERE organization_id = ? AND config_type = ? AND config_key = ?";

  private static final String LIST_BY_TYPE_SQL = SELECT_COLUMNS
      + "WHERE organization_id = ? AND config_type = ? ORDER BY config_key ASC";

  private static final String UPSERT_SQL = """
      INSERT INTO governance.config_entry (
        governance_config_id,
        organization_id,
        config_type,
        config_key,
        payload,
        enabled,
        created_by_user_id,
        updated_by_user_id
      )
      VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
      ON CONFLICT (organization_id, config_type, config_key)
      DO UPDATE SET
        payload = EXCLUDED.payload,
        enabled = EXCLUDED.enabled,
        updated_by_user_id = EXCLUDED.updated_by_user_id,
        updated_at = now(),
        version = governance.config_entry.version + 1
      RETURNING
        governance_config_id,
        organization_id,
        config_type,
        config_key,
        payload::text AS payload_json,
        enabled,
        created_by_user_id,
        updated_by_user_id,
        created_at,
        updated_at,
        version
      """;

  private static final String INSERT_AUDIT_SQL = """
      INSERT INTO audit.audit_log (
        audit_log_id,
        organization_id,
        actor_user_id,
        actor_role,
        action,
        target_entity_type,
        target_entity_id,
        result,
        occurred_at,
        reason,
        sensitivity_level,
        metadata
      )
      VALUES (
        ?, ?, ?, ?::governance.actor_role, 'governance.config.upsert',
        'governance.config_entry', ?, ?, now(), ?, 'system_governance',
        jsonb_build_object(
          'config_type', ?,
          'config_key', ?,
          'enabled', ?,
          'version', ?
        )
      )
      """;

  private final DataSource dataSource;

  public JdbcGovernanceConfigPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<GovernanceConfigRecord> findByTypeAndKey(
      UUID organizationId,
      String configType,
      String configKey) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(configType, "configType must not be null");
    Objects.requireNonNull(configKey, "configKey must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, configType);
      statement.setString(3, configKey);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toRecord(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load governance config", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<GovernanceConfigRecord> listByType(UUID organizationId, String configType) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(configType, "configType must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(LIST_BY_TYPE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, configType);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<GovernanceConfigRecord> records = new ArrayList<>();
        while (resultSet.next()) {
          records.add(toRecord(resultSet));
        }
        return List.copyOf(records);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list governance configs", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public GovernanceConfigRecord upsert(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson,
      boolean enabled,
      UUID actorUserId,
      PortalRole actorRole) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(configType, "configType must not be null");
    Objects.requireNonNull(configKey, "configKey must not be null");
    String safePayload = payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
    Connection connection = DataSourceUtils.getConnection(dataSource);
    boolean restoreAutoCommit = false;
    try {
      if (connection.getAutoCommit()) {
        connection.setAutoCommit(false);
        restoreAutoCommit = true;
      }
      try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
        statement.setObject(1, UUID.randomUUID());
        statement.setObject(2, organizationId);
        statement.setString(3, configType.strip());
        statement.setString(4, configKey.strip());
        statement.setString(5, safePayload);
        statement.setBoolean(6, enabled);
        setNullableUuid(statement, 7, actorUserId);
        setNullableUuid(statement, 8, actorUserId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            throw new IllegalStateException("Governance config upsert did not return a row");
          }
          GovernanceConfigRecord record = toRecord(resultSet);
          appendAudit(connection, record, actorUserId, actorRole);
          if (restoreAutoCommit) {
            connection.commit();
          }
          return record;
        }
      }
    } catch (SQLException exception) {
      try {
        rollbackIfNeeded(connection, restoreAutoCommit);
      } catch (IllegalStateException rollbackException) {
        exception.addSuppressed(rollbackException);
      }
      throw new IllegalStateException("Failed to upsert governance config", exception);
    } finally {
      restoreAutoCommit(connection, restoreAutoCommit);
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static GovernanceConfigRecord toRecord(ResultSet resultSet) throws SQLException {
    return new GovernanceConfigRecord(
        resultSet.getObject("governance_config_id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("config_type"),
        resultSet.getString("config_key"),
        resultSet.getString("payload_json"),
        resultSet.getBoolean("enabled"),
        resultSet.getObject("created_by_user_id", UUID.class),
        resultSet.getObject("updated_by_user_id", UUID.class),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant(),
        resultSet.getInt("version"));
  }

  private static void setNullableUuid(PreparedStatement statement, int index, UUID value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.OTHER);
      return;
    }
    statement.setObject(index, value);
  }

  private static void appendAudit(
      Connection connection,
      GovernanceConfigRecord record,
      UUID actorUserId,
      PortalRole actorRole) throws SQLException {
    if (actorUserId == null) {
      return;
    }
    try (PreparedStatement statement = connection.prepareStatement(INSERT_AUDIT_SQL)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, record.organizationId());
      statement.setObject(3, actorUserId);
      statement.setString(4, auditActorRole(actorRole));
      statement.setObject(5, record.governanceConfigId());
      statement.setString(6, record.enabled() ? "saved" : "disabled");
      statement.setString(7, record.enabled() ? "governance config saved" : "governance config disabled");
      statement.setString(8, record.configType());
      statement.setString(9, record.configKey());
      statement.setBoolean(10, record.enabled());
      statement.setInt(11, record.version());
      statement.executeUpdate();
    }
  }

  private static String auditActorRole(PortalRole actorRole) {
    if (actorRole == PortalRole.SYSTEM) {
      return "system";
    }
    return "admin";
  }

  private static void rollbackIfNeeded(Connection connection, boolean restoreAutoCommit) {
    if (!restoreAutoCommit) {
      return;
    }
    try {
      connection.rollback();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to rollback governance config transaction", exception);
    }
  }

  private static void restoreAutoCommit(Connection connection, boolean restoreAutoCommit) {
    if (!restoreAutoCommit) {
      return;
    }
    try {
      connection.setAutoCommit(true);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to restore governance config connection auto-commit", exception);
    }
  }
}
