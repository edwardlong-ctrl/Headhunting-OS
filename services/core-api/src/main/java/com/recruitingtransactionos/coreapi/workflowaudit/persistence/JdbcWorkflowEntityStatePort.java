package com.recruitingtransactionos.coreapi.workflowaudit.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class JdbcWorkflowEntityStatePort implements WorkflowEntityStatePort {

  private final DataSource dataSource;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JdbcWorkflowEntityStatePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<String> getCurrentStateJson(UUID organizationId, String entityNamespace, String entityType, UUID entityId) {
    String query = queryFor(entityNamespace, entityType);
    if (query == null) {
      return Optional.empty();
    }
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, entityId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          String status = resultSet.getString("status");
          return Optional.of(objectMapper.writeValueAsString(new StateWrapper(status)));
        }
      }
    } catch (SQLException | com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException("Failed to read entity state", e);
    }
    return Optional.empty();
  }

  private String queryFor(String namespace, String type) {
    return switch (normalizedKey(namespace, type)) {
      case "recruiting:job", "job:job" -> """
          SELECT status
          FROM recruiting.job
          WHERE organization_id = ? AND job_id = ?
          """;
      case "recruiting:shortlist", "shortlist:shortlist" -> """
          SELECT status
          FROM recruiting.shortlist
          WHERE organization_id = ? AND shortlist_id = ?
          """;
      case "recruiting:candidate", "workflow:candidate", "candidate:candidate" -> """
          SELECT status
          FROM recruiting.candidate
          WHERE organization_id = ? AND candidate_id = ?
          """;
      case "consent:consent", "workflow:consent" -> """
          SELECT status
          FROM privacy.consent_record
          WHERE organization_id = ? AND workflow_entity_id = ?
          """;
      case "disclosure:disclosure", "workflow:disclosure" -> """
          SELECT status
          FROM privacy.disclosure_record
          WHERE organization_id = ? AND workflow_entity_id = ?
          """;
      default -> null;
    };
  }

  private static String normalizedKey(String namespace, String type) {
    return normalize(namespace) + ":" + normalize(type);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static class StateWrapper {
    public String status;
    public StateWrapper() {}
    public StateWrapper(String status) { this.status = status; }
  }
}
