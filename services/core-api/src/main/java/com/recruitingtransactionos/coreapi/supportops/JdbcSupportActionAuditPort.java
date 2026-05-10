package com.recruitingtransactionos.coreapi.supportops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcSupportActionAuditPort implements SupportActionAuditPort {

  private static final String INSERT_SQL = """
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
      VALUES (?, ?, ?, ?::governance.actor_role, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private final DataSource dataSource;
  private final ObjectMapper objectMapper;

  public JdbcSupportActionAuditPort(DataSource dataSource) {
    this(dataSource, new ObjectMapper());
  }

  JdbcSupportActionAuditPort(DataSource dataSource, ObjectMapper objectMapper) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public UUID record(SupportActionAuditCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    UUID auditId = UUID.randomUUID();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, auditId);
      statement.setObject(2, command.organizationId());
      statement.setObject(3, command.supportActorId());
      statement.setString(4, auditActorRole(command.supportActorRole()));
      statement.setString(5, "support." + command.actionType().wireValue());
      statement.setString(6, command.targetType().wireValue());
      statement.setObject(7, command.targetId());
      statement.setString(8, command.result());
      statement.setObject(9, OffsetDateTime.now(ZoneOffset.UTC));
      statement.setString(10, command.reason().isBlank() ? command.ticketRef() : command.reason());
      statement.setString(11, "system_governance");
      statement.setString(12, metadataJson(command));
      statement.executeUpdate();
      return auditId;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to record support action audit", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String metadataJson(SupportActionAuditCommand command) {
    try {
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("ticketRef", command.ticketRef());
      metadata.put("supportActionType", command.actionType().wireValue());
      metadata.put("supportTargetType", command.targetType().wireValue());
      metadata.put("supportMetadata", objectMapper.readTree(command.metadataJson()));
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize support audit metadata", exception);
    }
  }

  private static String auditActorRole(PortalRole role) {
    return switch (role) {
      case OWNER -> "owner";
      case CONSULTANT -> "consultant";
      case CLIENT -> "client";
      case CANDIDATE -> "candidate";
      case ADMIN -> "admin";
      case SYSTEM -> "system";
      case AI_ASSISTANT -> "ai";
      case UNKNOWN -> "system";
    };
  }
}
