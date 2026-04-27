package com.recruitingtransactionos.coreapi.truthlayer.port;

public enum AITaskRunStatus {
  QUEUED("queued"),
  RUNNING("running"),
  SUCCEEDED("succeeded"),
  FAILED("failed"),
  BLOCKED_BY_GATE("blocked_by_gate"),
  REQUIRES_REVIEW("requires_review"),
  WRITE_BACK_COMPLETED("write_back_completed"),
  CANCELLED("cancelled");

  private final String wireValue;

  AITaskRunStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
