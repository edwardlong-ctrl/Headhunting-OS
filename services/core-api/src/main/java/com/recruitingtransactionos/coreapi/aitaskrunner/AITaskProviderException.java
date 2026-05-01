package com.recruitingtransactionos.coreapi.aitaskrunner;

public final class AITaskProviderException extends RuntimeException {

  private final String safeFailureReason;
  private final String errorCode;

  public AITaskProviderException(String safeFailureReason, String errorCode) {
    super(safeFailureReason);
    this.safeFailureReason = safeFailureReason;
    this.errorCode = errorCode;
  }

  public String safeFailureReason() {
    return safeFailureReason;
  }

  public String errorCode() {
    return errorCode;
  }
}
