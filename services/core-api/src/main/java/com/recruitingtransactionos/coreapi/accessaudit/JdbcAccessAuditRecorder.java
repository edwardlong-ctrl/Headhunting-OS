package com.recruitingtransactionos.coreapi.accessaudit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAuditEvent;
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

public final class JdbcAccessAuditRecorder implements com.recruitingtransactionos.coreapi.identityaccess.AccessAuditRecorder {

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

  public JdbcAccessAuditRecorder(DataSource dataSource) {
    this(dataSource, new ObjectMapper());
  }

  public JdbcAccessAuditRecorder(DataSource dataSource, ObjectMapper objectMapper) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void record(AccessAuditEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, event.context().organizationId());
      statement.setObject(3, event.context().actorUserId());
      statement.setString(4, auditActorRole(event.request().actorRole()));
      statement.setString(5, "access." + event.request().action().wireValue());
      statement.setString(6, event.request().resourceType().wireValue());
      statement.setObject(7, event.context().targetEntityId());
      statement.setString(8, event.decision().allowed() ? "allowed" : "denied");
      statement.setObject(9, OffsetDateTime.now(ZoneOffset.UTC));
      statement.setString(10, event.decision().reasonCode());
      statement.setString(11, event.context().sensitivityLevel());
      statement.setString(12, metadataJson(event));
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to record access audit event", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String metadataJson(AccessAuditEvent event) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("safeExplanation", event.decision().safeExplanation());
    metadata.put("fieldClassification", event.request().fieldClassification().wireValue());
    metadata.put("relationshipScopes", event.request().relationshipScopes().stream()
        .map(scope -> scope.wireValue())
        .sorted()
        .toList());
    metadata.put("identityDisclosureRequested", event.request().identityDisclosureRequested());
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize access audit metadata", exception);
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
