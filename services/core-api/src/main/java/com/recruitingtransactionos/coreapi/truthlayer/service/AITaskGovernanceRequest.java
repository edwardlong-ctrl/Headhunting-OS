package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import java.util.Objects;

public record AITaskGovernanceRequest(
    String writeBackTarget,
    String humanReviewStatus,
    ActorRef reviewActor,
    boolean clientSafeBoundaryApplied,
    boolean bulkApproval) {

  public static AITaskGovernanceRequest from(AITaskRunAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    String target = command.writeBackTarget() == null
        ? AITaskWriteBackTarget.NO_WRITE_BACK.wireValue()
        : command.writeBackTarget().value();
    String reviewStatus = command.humanReviewStatus() == null
        ? AITaskHumanReviewStatus.NOT_REQUIRED.wireValue()
        : command.humanReviewStatus();
    return new AITaskGovernanceRequest(
        target,
        reviewStatus,
        command.requestedBy(),
        false,
        false);
  }
}
