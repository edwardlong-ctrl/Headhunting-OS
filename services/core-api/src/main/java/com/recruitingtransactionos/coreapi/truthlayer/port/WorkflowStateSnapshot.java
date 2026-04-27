package com.recruitingtransactionos.coreapi.truthlayer.port;

public record WorkflowStateSnapshot(String json) {

  public WorkflowStateSnapshot {
    json = PortContractGuards.requireNonBlank(json, "workflowStateSnapshot");
  }
}
