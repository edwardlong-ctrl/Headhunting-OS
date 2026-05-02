package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionRequest;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CompanyIntakeTaskService {

  private final AITaskRunnerService aiTaskRunnerService;
  private final ObjectMapper objectMapper;

  public CompanyIntakeTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    this.aiTaskRunnerService = Objects.requireNonNull(aiTaskRunnerService, "aiTaskRunnerService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  public CompanyIntakeResult execute(
      UUID organizationId,
      ActorRef requestedBy,
      EntityRef targetEntity,
      List<UUID> sourceReferenceIds,
      CompanyIntakeInput input,
      WorkflowCorrelationId correlationId,
      WorkflowCausationId causationId) {
    Objects.requireNonNull(input, "input must not be null");
    AITaskExecutionResult execution = aiTaskRunnerService.execute(new AITaskExecutionRequest(
        organizationId,
        "company-intake",
        "company-intake.v1",
        requestedBy,
        targetEntity,
        sourceReferenceIds,
        objectMapper.valueToTree(input),
        correlationId,
        causationId,
        null));
    return new CompanyIntakeResult(
        execution,
        objectMapper.convertValue(execution.outputPayload(), CompanyIntakeOutput.class));
  }
}
