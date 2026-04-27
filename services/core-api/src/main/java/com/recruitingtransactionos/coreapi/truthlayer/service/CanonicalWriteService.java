package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecision;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteGate;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteRequest;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimInput;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.util.List;
import java.util.Objects;

public final class CanonicalWriteService {

  public static final String CANONICAL_PERSISTENCE_DEFERRED =
      "not_implemented_no_safe_canonical_write_target_in_task_3d";

  private final CanonicalWriteGate gate;
  private final WorkflowEventService workflowEventService;
  private final CanonicalWriteTransactionBoundary transactionBoundary;

  public CanonicalWriteService(
      CanonicalWriteGate gate,
      WorkflowEventService workflowEventService,
      CanonicalWriteTransactionBoundary transactionBoundary) {
    this.gate = Objects.requireNonNull(gate, "gate must not be null");
    this.workflowEventService = Objects.requireNonNull(workflowEventService,
        "workflowEventService must not be null");
    this.transactionBoundary = Objects.requireNonNull(transactionBoundary,
        "transactionBoundary must not be null");
  }

  public CanonicalWriteResult attempt(CanonicalWriteCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return transactionBoundary.run(() -> attemptWithinBoundary(command));
  }

  private CanonicalWriteResult attemptWithinBoundary(CanonicalWriteCommand command) {
    CanonicalWriteDecision decision = gate.decide(new CanonicalWriteRequest(
        claimWithReviewBulkFlag(command),
        command.targetVerificationStatus(),
        command.targetRiskTier(),
        command.clientVisible(),
        command.conflictsWithCanonical(),
        command.reviewEvidence().isExplicitApprovalFor(command.targetRiskTier())));

    if (decision.type() != CanonicalWriteDecisionType.ALLOW) {
      return stopped(decision);
    }

    if (!command.reviewEvidence().isApproved()) {
      return stopped(new CanonicalWriteDecision(
          CanonicalWriteDecisionType.REQUIRE_REVIEW,
          List.of("review_event_must_approve_canonical_boundary")));
    }

    WorkflowEventAppendResult auditResult = workflowEventService.append(auditCommand(command));
    return new CanonicalWriteResult(
        decision,
        true,
        auditResult.workflowEventId(),
        false,
        CANONICAL_PERSISTENCE_DEFERRED);
  }

  private static CanonicalWriteResult stopped(CanonicalWriteDecision decision) {
    return new CanonicalWriteResult(
        decision,
        false,
        null,
        false,
        "not_attempted_gate_did_not_allow");
  }

  private static ClaimInput claimWithReviewBulkFlag(CanonicalWriteCommand command) {
    ClaimInput claim = command.claim();
    if (!command.reviewEvidence().bulkApproval()) {
      return claim;
    }
    return new ClaimInput(
        claim.type(),
        claim.assertionStrength(),
        claim.verificationStatus(),
        claim.clientShareability(),
        true);
  }

  private static WorkflowEventAppendCommand auditCommand(CanonicalWriteCommand command) {
    return new WorkflowEventAppendCommand(
        command.organizationId(),
        "recruiting",
        command.targetEntity(),
        command.targetEntityVersion(),
        WorkflowActionCode.CANONICAL_WRITE_ALLOWED.wireValue(),
        new WorkflowStateSnapshot(beforeState(command)),
        new WorkflowStateSnapshot(afterState(command)),
        command.actor(),
        "canonical_write_service",
        command.claimId().value(),
        command.aiTaskRunId(),
        command.reviewEvidence().reviewEventId(),
        command.reason(),
        command.idempotencyKey(),
        command.correlationId(),
        null,
        command.occurredAt());
  }

  private static String beforeState(CanonicalWriteCommand command) {
    return "{"
        + "\"boundary\":\"canonical_write\","
        + "\"status\":\"requested\","
        + "\"targetFieldPath\":\"" + json(command.targetFieldPath()) + "\","
        + "\"proposedValueRef\":\"" + json(command.proposedValueRef()) + "\","
        + "\"claimId\":\"" + command.claimId().value() + "\","
        + "\"reviewEventId\":\"" + command.reviewEvidence().reviewEventId().value() + "\""
        + "}";
  }

  private static String afterState(CanonicalWriteCommand command) {
    return "{"
        + "\"boundary\":\"canonical_write\","
        + "\"status\":\"allowed_audit_appended\","
        + "\"targetFieldPath\":\"" + json(command.targetFieldPath()) + "\","
        + "\"canonicalPersistence\":\"" + CANONICAL_PERSISTENCE_DEFERRED + "\""
        + "}";
  }

  private static String json(String value) {
    StringBuilder escaped = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> escaped.append(character);
      }
    }
    return escaped.toString();
  }
}
