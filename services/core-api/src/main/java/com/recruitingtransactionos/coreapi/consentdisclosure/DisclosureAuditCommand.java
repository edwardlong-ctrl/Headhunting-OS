package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.Objects;
import java.util.Optional;

public record DisclosureAuditCommand(
    WorkflowActionCode actionCode,
    RiskTier riskTier,
    Optional<WorkflowEventId> workflowEventId) {

  public DisclosureAuditCommand {
    Objects.requireNonNull(actionCode, "actionCode must not be null");
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    workflowEventId = Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
  }
}
