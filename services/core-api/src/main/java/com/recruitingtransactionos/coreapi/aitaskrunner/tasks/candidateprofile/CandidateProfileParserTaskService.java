package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionRequest;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CandidateProfileParserTaskService {

  private final AITaskRunnerService aiTaskRunnerService;
  private final ObjectMapper objectMapper;
  private final ClaimLedgerService claimLedgerService;

  public CandidateProfileParserTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper,
      ClaimLedgerService claimLedgerService) {
    this.aiTaskRunnerService = Objects.requireNonNull(aiTaskRunnerService, "aiTaskRunnerService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.claimLedgerService = claimLedgerService; // can be null if not wired
  }

  public CandidateProfileParserResult execute(
      UUID organizationId,
      ActorRef requestedBy,
      EntityRef targetEntity,
      List<UUID> sourceReferenceIds,
      CandidateProfileParserInput input,
      WorkflowCorrelationId correlationId,
      WorkflowCausationId causationId) {
    Objects.requireNonNull(input, "input must not be null");
    AITaskExecutionResult execution = aiTaskRunnerService.execute(new AITaskExecutionRequest(
        organizationId,
        "candidate-profile-parser",
        "candidate-profile-parser.v1",
        requestedBy,
        targetEntity,
        sourceReferenceIds,
        objectMapper.valueToTree(input),
        correlationId,
        causationId,
        null));
        
    CandidateProfileParserOutput parsed = objectMapper.convertValue(execution.outputPayload(), CandidateProfileParserOutput.class);

    if (claimLedgerService != null && targetEntity != null && "candidate".equals(targetEntity.entityType())) {
      UUID sourceItemId = sourceReferenceIds != null && !sourceReferenceIds.isEmpty() ? sourceReferenceIds.getFirst() : null;
      SourceSpanRef span = new SourceSpanRef("ai_task:" + execution.runRecord().aiTaskRunId().value());
      
      claimLedgerService.append(new ClaimLedgerAppendCommand(
          organizationId,
          targetEntity,
          "headline",
          parsed.headline(),
          ClaimType.INFERENCE,
          AssertionStrength.WEAK_SIGNAL,
          span,
          ActorRole.AI,
          VerificationStatus.AI_EXTRACTED,
          ClientShareability.INTERNAL_ONLY,
          sourceItemId,
          execution.runRecord().aiTaskRunId()));
          
      claimLedgerService.append(new ClaimLedgerAppendCommand(
          organizationId,
          targetEntity,
          "summary",
          parsed.summary(),
          ClaimType.INFERENCE,
          AssertionStrength.WEAK_SIGNAL,
          span,
          ActorRole.AI,
          VerificationStatus.AI_EXTRACTED,
          ClientShareability.INTERNAL_ONLY,
          sourceItemId,
          execution.runRecord().aiTaskRunId()));
    }

    return new CandidateProfileParserResult(execution, parsed);
  }
}
