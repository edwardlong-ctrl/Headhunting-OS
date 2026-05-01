package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import java.util.Objects;
import java.util.UUID;

public record AITaskReplayRequest(
    UUID organizationId,
    UUID aiTaskRunId,
    ActorRef replayRequestedBy) {

  public AITaskReplayRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    Objects.requireNonNull(replayRequestedBy, "replayRequestedBy must not be null");
  }
}
