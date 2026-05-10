package com.recruitingtransactionos.coreapi.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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

public final class JdbcIntegrationAuditRecorder implements IntegrationAuditRecorder {

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
      VALUES (?, ?, ?, 'system'::governance.actor_role, ?, 'integration', ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private final DataSource dataSource;
  private final ObjectMapper objectMapper;

  public JdbcIntegrationAuditRecorder(DataSource dataSource) {
    this(dataSource, new ObjectMapper());
  }

  public JdbcIntegrationAuditRecorder(DataSource dataSource, ObjectMapper objectMapper) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public UUID recordOutbound(
      OutboundIntegrationCommand command,
      IntegrationProviderResult providerResult) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(providerResult, "providerResult must not be null");
    UUID auditId = UUID.randomUUID();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, auditId);
      statement.setObject(2, command.organizationId());
      statement.setObject(3, command.actorId());
      statement.setString(4, "integration.outbound." + command.channel().name().toLowerCase());
      statement.setObject(5, targetEntityId(command));
      statement.setString(6, auditResult(providerResult.status()));
      statement.setObject(7, OffsetDateTime.now(ZoneOffset.UTC));
      statement.setString(8, command.reason());
      statement.setString(9, command.hasRawSensitivePayload() ? "sensitive_blocked" : "safe_summary");
      statement.setString(10, metadataJson(command, providerResult));
      statement.executeUpdate();
      return auditId;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to record integration audit event", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String metadataJson(
      OutboundIntegrationCommand command,
      IntegrationProviderResult providerResult) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("channel", command.channel().name());
    metadata.put("payloadKind", command.payloadKind().name());
    metadata.put("targetAddress", command.target().address());
    metadata.put("targetOrganizationId", command.target().organizationId().toString());
    metadata.put("redactionDecision", command.redactionDecision().name());
    metadata.put("disclosureState", command.disclosureState().name());
    metadata.put("providerKey", providerResult.providerKey());
    metadata.put("providerStatus", providerResult.status().name());
    metadata.put("safeStatusCode", providerResult.safeStatusCode());
    metadata.put("rawSensitivePayloadIncluded", command.hasRawSensitivePayload());
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize integration audit metadata", exception);
    }
  }

  private static UUID targetEntityId(OutboundIntegrationCommand command) {
    String stableKey = command.idempotencyKey() != null
        ? command.idempotencyKey()
        : command.channel() + "|" + command.target().organizationId() + "|" + command.target().address();
    return UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8));
  }

  private static String auditResult(IntegrationProviderStatus status) {
    return switch (status) {
      case ACCEPTED, DELIVERED -> "allowed";
      case UNCONFIGURED, PRODUCTION_PLACEHOLDER -> "skipped";
      case FAILED_CLOSED, REJECTED -> "denied";
    };
  }
}
