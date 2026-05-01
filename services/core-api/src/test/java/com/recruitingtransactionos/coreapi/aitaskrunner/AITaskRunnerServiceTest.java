package com.recruitingtransactionos.coreapi.aitaskrunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunUpdateCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AITaskRunnerServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000210001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000210002");
  private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000210003");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void executePersistsAuditedSucceededRun() throws IOException {
    InMemoryAITaskRunPort port = new InMemoryAITaskRunPort();
    AITaskRunnerService service = runner(port, new StubProvider(
        "stub",
        OBJECT_MAPPER.readTree("{\"headline\":\"Platform engineer\",\"summary\":\"Builds core systems\",\"primarySkills\":[\"Java\",\"PostgreSQL\"],\"projects\":[\"Workflow audit\"],\"timelineHighlights\":[\"Led hiring automation\"]}"),
        "trace-123"));

    AITaskExecutionResult result = service.execute(new AITaskExecutionRequest(
        ORGANIZATION_ID,
        "candidate-profile-parser",
        "candidate-profile-parser.v1",
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", TARGET_ID),
        List.of(UUID.fromString("00000000-0000-0000-0000-000000210004")),
        OBJECT_MAPPER.readTree("{\"resumeText\":\"Built workflow systems for recruiting\"}"),
        null,
        null,
        null));

    assertThat(result.runRecord().status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
    assertThat(result.runRecord().traceRef()).isEqualTo("trace-123");
    assertThat(result.runRecord().outputPayloadJson()).contains("Platform engineer");
    assertThat(result.runRecord().metadataJson()).contains("candidate-profile-parser");
    assertThat(port.records).hasSize(1);
    assertThat(port.records.values().iterator().next().status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
  }

  @Test
  void replayCreatesNewRunWithLineage() throws IOException {
    InMemoryAITaskRunPort port = new InMemoryAITaskRunPort();
    AITaskRunnerService runner = runner(port, new StubProvider(
        "stub",
        OBJECT_MAPPER.readTree("{\"authenticityRisk\":\"medium\",\"specificityScore\":62,\"independentEvidenceGap\":true,\"flags\":[\"marketing_language\"]}"),
        "trace-456"));
    AITaskReplayService replayService = new AITaskReplayService(new AITaskRunService(port), runner, OBJECT_MAPPER);

    AITaskExecutionResult original = runner.execute(new AITaskExecutionRequest(
        ORGANIZATION_ID,
        "authenticity-risk-assessor",
        "authenticity-risk-assessor.v1",
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", TARGET_ID),
        List.of(),
        OBJECT_MAPPER.readTree("{\"resumeText\":\"Growth hacker with 10x outcomes everywhere\"}"),
        null,
        null,
        null));

    AITaskExecutionResult replayed = replayService.replay(new AITaskReplayRequest(
        ORGANIZATION_ID,
        original.runRecord().aiTaskRunId().value(),
        new ActorRef(ACTOR_ID, ActorRole.ADMIN)));

    assertThat(replayed.runRecord().aiTaskRunId()).isNotEqualTo(original.runRecord().aiTaskRunId());
    assertThat(replayed.runRecord().replayedFromAiTaskRunId()).isEqualTo(original.runRecord().aiTaskRunId());
    assertThat(replayed.runRecord().metadataJson()).contains(original.runRecord().aiTaskRunId().value().toString());
    assertThat(port.records).hasSize(2);
  }

  @Test
  void executePersistsFailedRunWithRawOutputWhenOutputSchemaValidationFails() throws IOException {
    InMemoryAITaskRunPort port = new InMemoryAITaskRunPort();
    AITaskRunnerService service = runner(port, new StubProvider(
        "stub",
        OBJECT_MAPPER.readTree("{\"fullName\":\"Alex Chen\",\"skills\":[\"Java\"]}"),
        "trace-invalid"));

    assertThatThrownBy(() -> service.execute(new AITaskExecutionRequest(
        ORGANIZATION_ID,
        "candidate-profile-parser",
        "candidate-profile-parser.v1",
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", TARGET_ID),
        List.of(),
        OBJECT_MAPPER.readTree("{\"resumeText\":\"Backend engineer\"}"),
        null,
        null,
        null)))
        .isInstanceOf(AITaskSchemaValidationException.class)
        .hasMessageContaining("output_schema_validation_failed");

    assertThat(port.records).hasSize(1);
    AITaskRunRecord failed = port.records.values().iterator().next();
    assertThat(failed.status()).isEqualTo(AITaskRunStatus.FAILED);
    assertThat(failed.outputPayloadJson()).contains("fullName");
    assertThat(failed.traceRef()).isEqualTo("trace-invalid");
    assertThat(failed.errorCode()).isEqualTo("output_schema_validation_failed");
    assertThat(failed.metadataJson()).contains("schema_validation_error");
    assertThat(failed.metadataJson()).doesNotContain("Alex Chen");
    assertThat(failed.metadataJson()).doesNotContain("\"skills\":[\"Java\"]");
  }

  private static AITaskRunnerService runner(InMemoryAITaskRunPort port, AITaskProvider provider) {
    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route candidateRoute = new AITaskRunnerProperties.Route();
    candidateRoute.setProvider(provider.providerKey());
    candidateRoute.setModel("stub-model");
    properties.getRoutes().put("candidate-profile-parser", candidateRoute);
    AITaskRunnerProperties.Route authenticityRoute = new AITaskRunnerProperties.Route();
    authenticityRoute.setProvider(provider.providerKey());
    authenticityRoute.setModel("stub-model");
    properties.getRoutes().put("authenticity-risk-assessor", authenticityRoute);

    return new AITaskRunnerService(
        new AITaskRunService(port),
        (organizationId, definition, modelRoute) -> {
        },
        new AITaskRunnerConfiguration().aiTaskDefinitionRegistry(),
        new AITaskPromptRegistry(),
        new AITaskSchemaValidator(OBJECT_MAPPER),
        new AITaskModelRouter(properties),
        List.of(provider),
        OBJECT_MAPPER);
  }

  private record StubProvider(String providerKey, JsonNode outputPayload, String traceRef)
      implements AITaskProvider {

    @Override
    public AITaskProviderResponse execute(AITaskProviderRequest request) {
      return new AITaskProviderResponse(outputPayload, "[]", BigDecimal.valueOf(42), traceRef, Duration.ofMillis(12));
    }
  }

  private static final class InMemoryAITaskRunPort implements AITaskRunPort {
    private final Map<AITaskRunId, AITaskRunRecord> records = new LinkedHashMap<>();

    @Override
    public AITaskRunAppendResult append(AITaskRunAppendCommand command) {
      AITaskRunId id = new AITaskRunId(UUID.randomUUID());
      records.put(id, new AITaskRunRecord(
          id,
          command.organizationId(),
          command.taskName(),
          command.taskVersion(),
          command.inputSchemaVersion(),
          command.outputSchemaVersion(),
          command.promptVersion(),
          command.model(),
          command.status(),
          command.humanReviewStatus(),
          command.writeBackTarget(),
          command.inputPayloadJson(),
          command.outputPayloadJson(),
          command.toolCallsJson(),
          command.costUnits(),
          command.traceRef(),
          command.errorCode(),
          command.metadataJson(),
          command.replayedFromAiTaskRunId(),
          command.requestedBy(),
          command.correlationId(),
          command.causationId(),
          command.targetEntity(),
          command.sourceReferenceIds(),
          command.startedAt(),
          command.completedAt(),
          command.failureReason(),
          command.startedAt()));
      return new AITaskRunAppendResult(id);
    }

    @Override
    public AITaskRunRecord update(AITaskRunUpdateCommand command) {
      AITaskRunRecord original = records.get(command.aiTaskRunId());
      AITaskRunRecord updated = new AITaskRunRecord(
          original.aiTaskRunId(),
          original.organizationId(),
          original.taskName(),
          original.taskVersion(),
          original.inputSchemaVersion(),
          original.outputSchemaVersion(),
          original.promptVersion(),
          new ModelRef(original.model().provider(), original.model().name(), original.model().version()),
          command.status(),
          original.humanReviewStatus(),
          original.writeBackTarget(),
          original.inputPayloadJson(),
          command.outputPayloadJson() != null ? command.outputPayloadJson() : original.outputPayloadJson(),
          command.toolCallsJson() != null ? command.toolCallsJson() : original.toolCallsJson(),
          command.costUnits() != null ? command.costUnits() : original.costUnits(),
          command.traceRef() != null ? command.traceRef() : original.traceRef(),
          command.errorCode() != null ? command.errorCode() : original.errorCode(),
          command.metadataJson() != null ? command.metadataJson() : original.metadataJson(),
          original.replayedFromAiTaskRunId(),
          original.requestedBy(),
          original.correlationId(),
          original.causationId(),
          original.targetEntity(),
          original.sourceReferenceIds(),
          original.startedAt(),
          command.completedAt(),
          command.failureReason(),
          original.createdAt());
      records.put(command.aiTaskRunId(), updated);
      return updated;
    }

    @Override
    public Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId) {
      return Optional.ofNullable(records.get(aiTaskRunId));
    }
  }
}
