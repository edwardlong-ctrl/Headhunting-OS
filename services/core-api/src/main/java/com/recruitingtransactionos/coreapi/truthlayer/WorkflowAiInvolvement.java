package com.recruitingtransactionos.coreapi.truthlayer;

public enum WorkflowAiInvolvement {
  NONE,
  AI_RECOMMENDED,
  AI_ASSISTED,
  AI_AUTOMATED_LOW_RISK,
  AI_BLOCKED_BY_POLICY;

  public String wireValue() {
    return name();
  }
}
