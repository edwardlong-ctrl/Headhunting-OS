package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinition;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinitionCatalog;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinitionRegistry;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProvider;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProviderRequest;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProviderResponse;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerService;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskPromptRegistry;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskSchemaValidator;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunUpdateCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CandidateProfileParserTaskServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void upperCaseCandidateEntityTypeStillAppendsClaimLedgerProposals() throws Exception {
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000220001");
    UUID sourceRefId = UUID.fromString("00000000-0000-0000-0000-000000220002");
    UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000220003");
    RecordingClaimLedgerPort claimLedgerPort = new RecordingClaimLedgerPort();
    CandidateProfileParserTaskService service = new CandidateProfileParserTaskService(
        runner(new InMemoryAITaskRunPort(), new StubProvider(
            OBJECT_MAPPER.readTree("""
                {"headline":"AI headline","summary":"AI summary","primarySkills":["verification"],"projects":["project-a"],"timelineHighlights":["timeline-a"]}
                """))),
        OBJECT_MAPPER,
        new ClaimLedgerService(claimLedgerPort));

    service.execute(
        organizationId,
        new ActorRef(UUID.fromString("00000000-0000-0000-0000-000000220005"), ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", candidateId),
        List.of(sourceRefId),
        new CandidateProfileParserInput("candidate summary", "", "", "", ""),
        new WorkflowCorrelationId(UUID.fromString("00000000-0000-0000-0000-000000220006")),
        new WorkflowCausationId(UUID.fromString("00000000-0000-0000-0000-000000220007")));

    assertThat(claimLedgerPort.commands).hasSize(2);
    assertThat(claimLedgerPort.commands)
        .extracting(ClaimLedgerAppendCommand::targetFieldPath)
        .containsExactly("headline", "summary");
    assertThat(claimLedgerPort.commands)
        .extracting(ClaimLedgerAppendCommand::aiTaskRunId)
        .allSatisfy(runId -> assertThat(runId).isNotNull());
  }

  private static AITaskRunnerService runner(InMemoryAITaskRunPort port, AITaskProvider provider) {
    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route candidateRoute = new AITaskRunnerProperties.Route();
    candidateRoute.setProvider(provider.providerKey());
    candidateRoute.setModel("stub-model");
    properties.getRoutes().put("candidate-profile-parser", candidateRoute);

    return new AITaskRunnerService(
        new AITaskRunService(port),
        (AITaskDefinitionCatalog) (organizationId, definition, modelRoute) -> { },
        new AITaskDefinitionRegistry(List.of(new AITaskDefinition(
            "candidate-profile-parser",
            "candidate-profile-parser.v1",
            "prompt.candidate-profile-parser.v1",
            "/ai/prompts/candidate-profile-parser-v1.txt",
            "/ai/schemas/candidate-profile-parser-input.schema.json",
            "/ai/schemas/candidate-profile-parser-output.schema.json",
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskHumanReviewStatus.REQUIRED))),
        new AITaskPromptRegistry(),
        new AITaskSchemaValidator(OBJECT_MAPPER),
        new AITaskModelRouter(properties),
        List.of(provider),
        OBJECT_MAPPER);
  }

  private record StubProvider(JsonNode outputPayload) implements AITaskProvider {
    @Override
    public String providerKey() {
      return "stub";
    }

    @Override
    public AITaskProviderResponse execute(AITaskProviderRequest request) {
      return new AITaskProviderResponse(
          outputPayload,
          "[]",
          BigDecimal.ONE,
          "trace-claim-ledger",
          Duration.ofMillis(5));
    }
  }

  private static final class RecordingClaimLedgerPort implements ClaimLedgerPort {
    private final List<ClaimLedgerAppendCommand> commands = new ArrayList<>();

    @Override
    public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
      commands.add(command);
      return new ClaimLedgerAppendResult(new ClaimId(UUID.randomUUID()));
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
          new ModelRef(
              original.model().provider(),
              original.model().name(),
              original.model().version()),
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
