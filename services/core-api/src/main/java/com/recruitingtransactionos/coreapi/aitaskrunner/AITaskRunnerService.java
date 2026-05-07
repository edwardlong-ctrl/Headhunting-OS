package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunUpdateCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AITaskRunnerService {

  private final AITaskRunService aiTaskRunService;
  private final AITaskDefinitionCatalog definitionCatalog;
  private final AITaskDefinitionRegistry definitionRegistry;
  private final AITaskPromptRegistry promptRegistry;
  private final AITaskSchemaValidator schemaValidator;
  private final AITaskModelRouter modelRouter;
  private final Map<String, AITaskProvider> providers;
  private final ObjectMapper objectMapper;

  public AITaskRunnerService(
      AITaskRunService aiTaskRunService,
      AITaskDefinitionCatalog definitionCatalog,
      AITaskDefinitionRegistry definitionRegistry,
      AITaskPromptRegistry promptRegistry,
      AITaskSchemaValidator schemaValidator,
      AITaskModelRouter modelRouter,
      List<AITaskProvider> providers,
      ObjectMapper objectMapper) {
    this.aiTaskRunService = Objects.requireNonNull(aiTaskRunService, "aiTaskRunService must not be null");
    this.definitionCatalog = Objects.requireNonNull(definitionCatalog, "definitionCatalog must not be null");
    this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry must not be null");
    this.promptRegistry = Objects.requireNonNull(promptRegistry, "promptRegistry must not be null");
    this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator must not be null");
    this.modelRouter = Objects.requireNonNull(modelRouter, "modelRouter must not be null");
    Objects.requireNonNull(providers, "providers must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    Map<String, AITaskProvider> providerMap = new LinkedHashMap<>();
    for (AITaskProvider provider : providers) {
      providerMap.put(provider.providerKey(), provider);
    }
    this.providers = Map.copyOf(providerMap);
  }

  public AITaskExecutionResult execute(AITaskExecutionRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    AITaskDefinition definition = definitionRegistry.findRequired(request.taskKey(), request.taskVersion());
    schemaValidator.validate(definition.inputSchemaResourcePath(), request.inputPayload(), "input");
    AITaskModelRoute modelRoute = modelRouter.routeFor(request.organizationId(), definition.taskKey());
    definitionCatalog.ensureRegistered(request.organizationId(), definition, modelRoute);
    AITaskProvider provider = requireProvider(modelRoute.providerKey());
    String prompt = promptRegistry.loadPrompt(definition);
    ObjectNode metadata = objectMapper.createObjectNode();
    metadata.put("task_key", definition.taskKey());
    metadata.put("task_version", definition.taskVersion());
    metadata.put("prompt_version", definition.promptVersion());
    metadata.put("provider", modelRoute.providerKey());
    metadata.put("model", modelRoute.modelName());
    if (request.replayedFromAiTaskRunId() != null) {
      metadata.put("replay_of_run_id", request.replayedFromAiTaskRunId().value().toString());
    }
    Instant startedAt = Instant.now();
    AITaskRunId runId = aiTaskRunService.append(new AITaskRunAppendCommand(
        request.organizationId(),
        definition.taskKey(),
        definition.taskVersion(),
        definition.inputSchemaResourcePath(),
        definition.outputSchemaResourcePath(),
        definition.promptVersion(),
        new com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef(modelRoute.providerKey(), modelRoute.modelName(), "v1"),
        AITaskRunStatus.CREATED,
        definition.humanReviewStatus().wireValue(),
        WriteBackTarget.of(definition.writeBackTarget()),
        request.inputPayload().toString(),
        null,
        null,
        null,
        null,
        null,
        metadata.toString(),
        request.replayedFromAiTaskRunId(),
        request.requestedBy(),
        request.correlationId(),
        request.causationId(),
        request.targetEntity(),
        request.sourceReferenceIds(),
        startedAt,
        null,
        null)).aiTaskRunId();

    aiTaskRunService.update(new AITaskRunUpdateCommand(
        request.organizationId(),
        runId,
        AITaskRunStatus.RUNNING,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        metadata.toString()));

    Instant providerStartedAt = Instant.now();
    try {
      AITaskProviderResponse providerResponse = provider.execute(new AITaskProviderRequest(
          definition.taskKey(),
          definition.promptVersion(),
          modelRoute.modelName(),
          prompt,
          request.inputPayload()));
      try {
        schemaValidator.validate(
            definition.outputSchemaResourcePath(),
            providerResponse.outputPayload(),
            "output");
      } catch (AITaskSchemaValidationException exception) {
        ObjectNode failureMetadata = metadata.deepCopy();
        failureMetadata.put("latency_ms", providerResponse.latency().toMillis());
        failureMetadata.put("schema_validation_label", exception.label());
        failureMetadata.put("schema_validation_error", exception.safeSummary());
        aiTaskRunService.update(new AITaskRunUpdateCommand(
            request.organizationId(),
            runId,
            AITaskRunStatus.FAILED,
            Instant.now(),
            exception.errorCode(),
            providerResponse.outputPayload().toString(),
            providerResponse.toolCallsJson(),
            providerResponse.costUnits(),
            providerResponse.traceRef(),
            exception.errorCode(),
            failureMetadata.toString()));
        throw exception;
      }
      ObjectNode successMetadata = metadata.deepCopy();
      successMetadata.put("latency_ms", providerResponse.latency().toMillis());
      AITaskRunRecord updated = aiTaskRunService.update(new AITaskRunUpdateCommand(
          request.organizationId(),
          runId,
          AITaskRunStatus.SUCCEEDED,
          Instant.now(),
          null,
          providerResponse.outputPayload().toString(),
          providerResponse.toolCallsJson(),
          providerResponse.costUnits(),
          providerResponse.traceRef(),
          null,
          successMetadata.toString()));
      return new AITaskExecutionResult(updated, providerResponse.outputPayload(), providerResponse.latency());
    } catch (AITaskProviderException exception) {
      ObjectNode failureMetadata = metadata.deepCopy();
      failureMetadata.put("latency_ms", Duration.between(providerStartedAt, Instant.now()).toMillis());
      aiTaskRunService.update(new AITaskRunUpdateCommand(
          request.organizationId(),
          runId,
          AITaskRunStatus.FAILED,
          Instant.now(),
          exception.safeFailureReason(),
          null,
          null,
          null,
          null,
          exception.errorCode(),
          failureMetadata.toString()));
      throw exception;
    } catch (AITaskSchemaValidationException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      ObjectNode failureMetadata = metadata.deepCopy();
      failureMetadata.put("latency_ms", Duration.between(providerStartedAt, Instant.now()).toMillis());
      aiTaskRunService.update(new AITaskRunUpdateCommand(
          request.organizationId(),
          runId,
          AITaskRunStatus.FAILED,
          Instant.now(),
          "ai_task_execution_failed",
          null,
          null,
          null,
          null,
          "ai_task_execution_failed",
          failureMetadata.toString()));
      throw exception;
    }
  }

  private AITaskProvider requireProvider(String providerKey) {
    AITaskProvider provider = providers.get(providerKey);
    if (provider == null) {
      throw new IllegalArgumentException("unknown_ai_task_provider");
    }
    return provider;
  }
}
