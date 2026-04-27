package com.recruitingtransactionos.coreapi.truthlayer.port;

public record WorkflowIdempotencyKey(String value) {

  public static final int MAX_LENGTH = 200;

  public WorkflowIdempotencyKey {
    value = PortContractGuards.requireNonBlankMaxLength(value, "idempotencyKey", MAX_LENGTH);
  }
}
