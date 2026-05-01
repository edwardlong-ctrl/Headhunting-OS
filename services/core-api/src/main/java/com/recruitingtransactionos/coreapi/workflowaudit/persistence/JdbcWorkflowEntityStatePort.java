package com.recruitingtransactionos.coreapi.workflowaudit.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    } catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException("Failed to read entity state", e);
    }
    return Optional.empty();
  }

  @Override
  public void updateStateJson(UUID organizationId, String entityNamespace, String entityType, UUID entityId, String newStateJson) {
    String update = updateFor(entityNamespace, entityType);
    if (update == null) {
      return;
    }
    try {
      StateWrapper wrapper = objectMapper.readValue(newStateJson, StateWrapper.class);
      if (wrapper.status == null) {
        return;
      }
      try (Connection connection = dataSource.getConnection();
           PreparedStatement statement = connection.prepareStatement(update)) {
        statement.setString(1, wrapper.status);
        statement.setObject(2, organizationId);
        statement.setObject(3, entityId);
        statement.executeUpdate();
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException("Failed to update entity state", e);
    }
  }

  private String queryFor(String namespace, String type) {
    return switch (namespace + ":" + type) {
      case "job:job" -> "SELECT status FROM job.job WHERE organization_id = ? AND job_id = ?";
      case "shortlist:shortlist" -> "SELECT status FROM shortlist.shortlist WHERE organization_id = ? AND shortlist_id = ?";
      case "consent:consent" -> "SELECT status FROM consent_disclosure.consent WHERE organization_id = ? AND consent_id = ?";
      case "disclosure:disclosure" -> "SELECT status FROM consent_disclosure.disclosure WHERE organization_id = ? AND disclosure_id = ?";
      default -> null;
    };
  }

  private String updateFor(String namespace, String type) {
    return switch (namespace + ":" + type) {
      case "job:job" -> "UPDATE job.job SET status = ? WHERE organization_id = ? AND job_id = ?";
      case "shortlist:shortlist" -> "UPDATE shortlist.shortlist SET status = ? WHERE organization_id = ? AND shortlist_id = ?";
      case "consent:consent" -> "UPDATE consent_disclosure.consent SET status = ? WHERE organization_id = ? AND consent_id = ?";
      case "disclosure:disclosure" -> "UPDATE consent_disclosure.disclosure SET status = ? WHERE organization_id = ? AND disclosure_id = ?";
      default -> null;
    };
  }

  private static class StateWrapper {
    public String status;
    public StateWrapper() {}
    public StateWrapper(String status) { this.status = status; }
  }
}
