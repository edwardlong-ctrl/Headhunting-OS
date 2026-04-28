package com.recruitingtransactionos.coreapi.truthlayer.port;

public enum AITaskRunStatus {
  CREATED("created"),
  RUNNING("running"),
  SUCCEEDED("succeeded"),
  FAILED("failed"),
  CANCELLED("cancelled");

  private final String wireValue;

  AITaskRunStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static AITaskRunStatus fromWireValue(String wireValue) {
    for (AITaskRunStatus status : values()) {
      if (status.wireValue.equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("unknown AI task run status: " + wireValue);
  }
}
