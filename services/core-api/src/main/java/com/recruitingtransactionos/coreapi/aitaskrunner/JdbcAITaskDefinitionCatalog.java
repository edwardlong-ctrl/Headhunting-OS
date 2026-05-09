package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcAITaskDefinitionCatalog implements AITaskDefinitionCatalog {

  private static final String UPSERT_DEFINITION_SQL = """
      INSERT INTO governance.ai_task_definition (
        ai_task_definition_id,
        organization_id,
        task_key,
        task_version,
        status,
        input_schema_version,
        output_schema_version,
        human_review_policy,
        description,
        model_routing_policy,
        write_back_target,
        eval_suite_ref,
        metadata
      )
      VALUES (?, ?, ?, ?, 'active', ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?, ?::jsonb)
      ON CONFLICT (organization_id, task_key, task_version) DO UPDATE
      SET
        status = EXCLUDED.status,
        input_schema_version = EXCLUDED.input_schema_version,
        output_schema_version = EXCLUDED.output_schema_version,
        human_review_policy = EXCLUDED.human_review_policy,
        description = EXCLUDED.description,
        model_routing_policy = EXCLUDED.model_routing_policy,
        write_back_target = EXCLUDED.write_back_target,
        eval_suite_ref = EXCLUDED.eval_suite_ref,
        metadata = EXCLUDED.metadata,
        updated_at = now(),
        version = governance.ai_task_definition.version + 1
      """;

  private final DataSource dataSource;
  private final ObjectMapper objectMapper;

  public JdbcAITaskDefinitionCatalog(
      DataSource dataSource,
      ObjectMapper objectMapper) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void ensureRegistered(
      UUID organizationId,
      AITaskDefinition definition,
      AITaskModelRoute modelRoute) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(definition, "definition must not be null");
    Objects.requireNonNull(modelRoute, "modelRoute must not be null");

    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_DEFINITION_SQL)) {
      statement.setObject(1, deterministicDefinitionId(organizationId, definition));
      statement.setObject(2, organizationId);
      statement.setString(3, definition.taskKey());
      statement.setString(4, definition.taskVersion());
      statement.setString(5, definition.inputSchemaResourcePath());
      statement.setString(6, definition.outputSchemaResourcePath());
      statement.setString(7, humanReviewPolicy(definition));
      statement.setString(8, "Registry-backed AI task definition");
      statement.setString(9, modelRoutingPolicy(modelRoute));
      statement.setString(10, definition.writeBackTarget().wireValue());
      statement.setString(11, definition.evalSuiteResourcePath());
      statement.setString(12, metadata(definition));
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to register AI task definition", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String humanReviewPolicy(AITaskDefinition definition) {
    ObjectNode policy = objectMapper.createObjectNode();
    policy.put("status", definition.humanReviewStatus().wireValue());
    policy.put("prompt_version", definition.promptVersion());
    return policy.toString();
  }

  private String modelRoutingPolicy(AITaskModelRoute modelRoute) {
    ObjectNode policy = objectMapper.createObjectNode();
    policy.put("provider", modelRoute.providerKey());
    policy.put("model", modelRoute.modelName());
    return policy.toString();
  }

  private String metadata(AITaskDefinition definition) {
    ObjectNode metadata = objectMapper.createObjectNode();
    metadata.put("registry_task_id", definition.registryTaskId());
    metadata.put("display_name", definition.displayName());
    metadata.put("registry_group", definition.registryGroup());
    metadata.put("prompt_resource_path", definition.promptResourcePath());
    metadata.put("input_schema_resource_path", definition.inputSchemaResourcePath());
    metadata.put("output_schema_resource_path", definition.outputSchemaResourcePath());
    metadata.put("eval_suite_resource_path", definition.evalSuiteResourcePath());
    return metadata.toString();
  }

  private static UUID deterministicDefinitionId(
      UUID organizationId,
      AITaskDefinition definition) {
    return UUID.nameUUIDFromBytes(
        (organizationId + ":" + definition.taskKey() + ":" + definition.taskVersion())
            .getBytes(StandardCharsets.UTF_8));
  }
}
