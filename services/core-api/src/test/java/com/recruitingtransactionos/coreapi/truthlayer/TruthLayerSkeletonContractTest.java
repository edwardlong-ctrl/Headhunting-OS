package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TruthLayerSkeletonContractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Path MIGRATION =
      Path.of("src/main/resources/db/migration/V2__create_truth_layer_core_tables.sql");
  private static final Path CONTRACT_SCHEMA_DIR = Path.of("../../packages/contracts/schemas");

  private static final Map<String, String> REQUIRED_TABLES = Map.ofEntries(
      Map.entry("Organization", "identity.organization"),
      Map.entry("UserAccount", "identity.user_account"),
      Map.entry("RoleAssignment", "identity.role_assignment"),
      Map.entry("Candidate", "recruiting.candidate"),
      Map.entry("CandidateProfile", "recruiting.candidate_profile"),
      Map.entry("SourceItem", "recruiting.source_item"),
      Map.entry("InformationPacket", "recruiting.information_packet"),
      Map.entry("ClaimLedgerItem", "governance.claim_ledger_item"),
      Map.entry("ReviewEvent", "governance.review_event"),
      Map.entry("WorkflowEvent", "workflow.workflow_event"),
      Map.entry("AITaskDefinition", "governance.ai_task_definition"),
      Map.entry("AITaskRun", "governance.ai_task_run"),
      Map.entry("AuditLog", "audit.audit_log"));

  private static final List<String> REQUIRED_SCHEMA_FILES = List.of(
      "truth-layer.enums.schema.json",
      "truth-layer.rules.schema.json",
      "organization.schema.json",
      "user-account.schema.json",
      "role-assignment.schema.json",
      "candidate.schema.json",
      "candidate-profile.schema.json",
      "source-item.schema.json",
      "information-packet.schema.json",
      "claim-ledger-item.schema.json",
      "review-event.schema.json",
      "workflow-event.schema.json",
      "ai-task-definition.schema.json",
      "ai-task-run.schema.json",
      "audit-log.schema.json");

  @Test
  void migrationCreatesOnlyTheFirstTruthLayerTablesInTheApprovedNamespaces() throws IOException {
    String sql = readMigrationSql();

    REQUIRED_TABLES.forEach((objectName, tableName) ->
        assertThat(sql)
            .as("Task 2B migration should create %s as %s", objectName, tableName)
            .contains("create table " + tableName));

    assertThat(sql)
        .as("Task 2B must not implement consent/disclosure tables yet")
        .doesNotContain("create table privacy.consent_record")
        .doesNotContain("create table privacy.disclosure_record");
  }

  @Test
  void migrationDocumentsTruthLayerRulesWithoutImplementingBehavior() throws IOException {
    String sql = readMigrationSql();

    assertThat(sql).contains("field_status_map jsonb not null");
    assertThat(sql).contains("canonical_write_allowed boolean not null default false");
    assertThat(sql).contains("ai output must enter claimledgeritem");
    assertThat(sql).contains("raw candidate must not be exposed to client-facing contracts");
    assertThat(sql).contains("client-safe dto generation is server-owned and implemented later");
    assertThat(sql).contains("workflowevent is required for key state transitions");
    assertThat(sql).contains("aitaskrun records ai-assisted actions");
    assertThat(sql).contains("consent/disclosure behavior is intentionally not implemented");
  }

  @Test
  void sharedContractSchemasExistAndAreParseableJson() throws IOException {
    for (String fileName : REQUIRED_SCHEMA_FILES) {
      Path schemaPath = CONTRACT_SCHEMA_DIR.resolve(fileName);

      assertThat(schemaPath).as("expected schema file").exists().isRegularFile();
      JsonNode schema = OBJECT_MAPPER.readTree(schemaPath.toFile());

      assertThat(schema.path("$schema").asText()).isNotBlank();
      assertThat(schema.path("title").asText()).isNotBlank();
      assertThat(schema.path("type").asText()).isIn("object", "string");
    }
  }

  @Test
  void sharedContractSchemasEncodeTheFirstTruthLayerGuardrails() throws IOException {
    JsonNode candidateProfile = readSchema("candidate-profile.schema.json");
    JsonNode claimLedgerItem = readSchema("claim-ledger-item.schema.json");
    JsonNode workflowEvent = readSchema("workflow-event.schema.json");
    JsonNode aiTaskRun = readSchema("ai-task-run.schema.json");
    JsonNode rules = readSchema("truth-layer.rules.schema.json");

    assertRequiredFields(candidateProfile, "organization_id", "candidate_id", "field_status_map");
    assertThat(candidateProfile.toString())
        .contains("Candidate canonical fields must carry verification/status metadata");

    assertRequiredFields(claimLedgerItem, "organization_id", "entity_type", "entity_id",
        "verification_status", "canonical_write_allowed");
    assertThat(claimLedgerItem.toString())
        .contains("AI output must enter ClaimLedgerItem, not canonical Candidate fields directly");

    assertRequiredFields(workflowEvent, "organization_id", "entity_namespace", "entity_type",
        "entity_id", "action", "before_state", "after_state", "actor_user_id", "actor_role");
    assertThat(workflowEvent.toString()).contains("WorkflowEvent is required for key state transitions");

    assertRequiredFields(aiTaskRun, "organization_id", "ai_task_definition_id", "task_version",
        "input_schema_version", "output_schema_version", "prompt_version", "model_provider",
        "model_name", "human_review_status", "status");
    assertThat(aiTaskRun.toString()).contains("AITaskRun records AI-assisted actions");

    assertThat(rules.toString()).contains("Raw Candidate must not be exposed to Client-facing contracts");
    assertThat(rules.toString()).contains("Client-safe DTOs are server-owned and will be implemented later");
    assertThat(rules.toString()).contains("Consent/disclosure behavior is not implemented in Task 2B");
  }

  private static String readMigrationSql() throws IOException {
    assertThat(MIGRATION).exists().isRegularFile();
    return Files.readString(MIGRATION).toLowerCase(Locale.ROOT);
  }

  private static JsonNode readSchema(String fileName) throws IOException {
    return OBJECT_MAPPER.readTree(CONTRACT_SCHEMA_DIR.resolve(fileName).toFile());
  }

  private static void assertRequiredFields(JsonNode schema, String... fields) {
    Set<String> required = Set.copyOf(OBJECT_MAPPER.convertValue(
        schema.path("required"),
        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)));

    assertThat(required).contains(fields);
  }
}
