package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AITaskRunService {

  private final AITaskRunPort aiTaskRunPort;
  private final AITaskGovernancePolicy governancePolicy;

  public AITaskRunService(AITaskRunPort aiTaskRunPort) {
    this(aiTaskRunPort, new AITaskGovernancePolicy());
  }

  public AITaskRunService(
      AITaskRunPort aiTaskRunPort,
      AITaskGovernancePolicy governancePolicy) {
    this.aiTaskRunPort = Objects.requireNonNull(aiTaskRunPort,
        "aiTaskRunPort must not be null");
    this.governancePolicy = Objects.requireNonNull(governancePolicy,
        "governancePolicy must not be null");
  }

  public AITaskRunAppendResult append(AITaskRunAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AITaskGovernanceDecision decision = governancePolicy.decide(
        AITaskGovernanceRequest.from(command));
    if (!decision.allowed()) {
      throw new IllegalArgumentException(decision.reasonCode());
    }
    return aiTaskRunPort.append(command);
  }

  public Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    return aiTaskRunPort.findById(organizationId, aiTaskRunId);
  }
}
