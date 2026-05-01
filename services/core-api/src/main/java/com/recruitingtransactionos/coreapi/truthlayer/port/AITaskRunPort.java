package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Optional;
import java.util.UUID;

public interface AITaskRunPort {

  AITaskRunAppendResult append(AITaskRunAppendCommand command);

  default AITaskRunRecord update(AITaskRunUpdateCommand command) {
    throw new UnsupportedOperationException("AI task run updates are not supported by this port");
  }

  Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId);
}
