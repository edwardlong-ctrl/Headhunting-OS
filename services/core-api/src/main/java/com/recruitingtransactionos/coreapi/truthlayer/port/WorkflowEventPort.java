package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowEventPort {

  Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey);

  WorkflowEventAppendResult append(WorkflowEventAppendCommand command);
}
