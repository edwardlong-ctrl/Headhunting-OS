package com.recruitingtransactionos.coreapi.truthlayer;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.util.Objects;

public record WorkflowAuditPolicyRequest(
    WorkflowActionCode actionCode,
    WorkflowEntityType entityType,
    ActorRole actorRole,
    WorkflowAiInvolvement aiInvolvement,
    WorkflowStateSnapshot beforeState,
    WorkflowStateSnapshot afterState,
    String reason) {

  public WorkflowAuditPolicyRequest {
    Objects.requireNonNull(actionCode, "actionCode must not be null");
    Objects.requireNonNull(entityType, "entityType must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(aiInvolvement, "aiInvolvement must not be null");
  }
}
