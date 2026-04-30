package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Optional;
import java.util.UUID;

public interface CanonicalWriteAttemptPort {

  Optional<CanonicalWriteAttemptIdempotencyRecord> findByIdempotencyKey(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey);

  CanonicalWriteAttemptAppendResult append(CanonicalWriteAttemptAppendCommand command);
}
