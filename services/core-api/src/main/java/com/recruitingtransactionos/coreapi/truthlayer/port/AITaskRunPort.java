package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Optional;
import java.util.UUID;

public interface AITaskRunPort {

  AITaskRunAppendResult append(AITaskRunAppendCommand command);

  Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId);
}
