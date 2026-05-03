package com.recruitingtransactionos.coreapi.privacyredaction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Deterministic UUID derivations for Task 30 re-identification assessment
 * workflow events. Mirrors the
 * {@code ConsentDisclosureWorkflowEntityIds} pattern.
 *
 * <p>The deterministic UUID is used as the {@code workflow.workflow_event}
 * entity id when emitting audit events, so that idempotent re-evaluation of
 * the same assessment ref always lands on the same workflow event row.
 */
public final class RedactionAuditWorkflowEntityIds {

  private RedactionAuditWorkflowEntityIds() {
  }

  public static UUID assessmentEntityId(
      UUID organizationId,
      String reidentificationRiskAssessmentRef) {
    String key = "reidentification_risk_assessment|"
        + organizationId
        + "|"
        + reidentificationRiskAssessmentRef;
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }
}
