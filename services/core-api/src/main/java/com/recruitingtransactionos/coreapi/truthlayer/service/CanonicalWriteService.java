package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
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
  public static final String CANDIDATE_PROFILE_FIELD_PERSISTED =
      "candidate_profile_field_persisted";

  private final CanonicalWriteGate gate;
  private final WorkflowEventService workflowEventService;
  private final CanonicalWriteTransactionBoundary transactionBoundary;
  private final CandidateProfileService candidateProfileService;

  public CanonicalWriteService(
      CanonicalWriteGate gate,
      WorkflowEventService workflowEventService,
      CanonicalWriteTransactionBoundary transactionBoundary) {
    this(gate, workflowEventService, transactionBoundary, null);
  }

  public CanonicalWriteService(
      CanonicalWriteGate gate,
      WorkflowEventService workflowEventService,
      CanonicalWriteTransactionBoundary transactionBoundary,
      CandidateProfileService candidateProfileService) {
    this.gate = Objects.requireNonNull(gate, "gate must not be null");
    this.workflowEventService = Objects.requireNonNull(workflowEventService,
        "workflowEventService must not be null");
    this.transactionBoundary = Objects.requireNonNull(transactionBoundary,
        "transactionBoundary must not be null");
    this.candidateProfileService = candidateProfileService;
  }

  public CanonicalWriteResult attempt(CanonicalWriteCommand command) {
    Objects.requireNonNull(command, "command must not be null");
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

    return transactionBoundary.run(() -> allowedAttemptWithinBoundary(command, decision));
  }

  private CanonicalWriteResult allowedAttemptWithinBoundary(
      CanonicalWriteCommand command,
      CanonicalWriteDecision decision) {
    WorkflowEventAppendResult auditResult = workflowEventService.append(auditCommand(command));
    if (command.candidateProfileWriteTarget() != null) {
      if (candidateProfileService == null) {
        throw new IllegalStateException(
            "candidateProfileService is required for candidate profile canonical writes");
      }
      candidateProfileService.upsertCandidateProfileField(profileFieldRequest(
          command,
          auditResult));
      return new CanonicalWriteResult(
          decision,
          true,
          auditResult.workflowEventId(),
          true,
          CANDIDATE_PROFILE_FIELD_PERSISTED);
    }
    return new CanonicalWriteResult(
        decision,
        true,
        auditResult.workflowEventId(),
        false,
        CANONICAL_PERSISTENCE_DEFERRED);
  }

  private static UpsertCandidateProfileFieldRequest profileFieldRequest(
      CanonicalWriteCommand command,
      WorkflowEventAppendResult auditResult) {
    CandidateProfileCanonicalWriteTarget target = command.candidateProfileWriteTarget();
    return UpsertCandidateProfileFieldRequest.builder()
        .organizationId(command.organizationId())
        .candidateProfileId(target.candidateProfileId())
        .fieldPath(target.fieldPath())
        .value(target.value())
        .fieldStatus(target.fieldStatus())
        .lineage(lineage(command, auditResult))
        .lastReviewedAt(command.occurredAt())
        .confirmedByActorId(command.actor().userId())
        .confirmedAgainstProfileVersion(confirmedProfileVersion(command, target.fieldStatus()))
        .sourceClaimId(command.claimId())
        .sourceReviewEventId(command.reviewEvidence().reviewEventId())
        .sourceWorkflowEventId(auditResult.workflowEventId())
        .notes("minimal canonical CandidateProfile field write through CanonicalWriteService")
        .bulkApproval(command.reviewEvidence().bulkApproval())
        .build();
  }

  private static CandidateProfileFieldLineage lineage(
      CanonicalWriteCommand command,
      WorkflowEventAppendResult auditResult) {
    return new CandidateProfileFieldLineage(
        List.of(
            CandidateProfileFieldSourceReference.claimLedgerItem(
                command.claimId(),
                command.occurredAt()),
            CandidateProfileFieldSourceReference.reviewEvent(
                command.reviewEvidence().reviewEventId(),
                command.occurredAt()),
            CandidateProfileFieldSourceReference.workflowEvent(
                auditResult.workflowEventId(),
                command.occurredAt()),
            CandidateProfileFieldSourceReference.sourceSpan(
                command.proposedValueRef(),
                "canonical_write_requested_value_ref",
                command.occurredAt())),
        "canonical-write-service",
        command.occurredAt());
  }

  private static CandidateProfileVersion confirmedProfileVersion(
      CanonicalWriteCommand command,
      CandidateProfileFieldStatus fieldStatus) {
    if (fieldStatus != CandidateProfileFieldStatus.CANDIDATE_CONFIRMED) {
      return null;
    }
    if (command.targetEntityVersion() == null) {
      return null;
    }
    return new CandidateProfileVersion(command.targetEntityVersion());
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
        command.causationId(),
        command.occurredAt());
  }

  private static String beforeState(CanonicalWriteCommand command) {
    return "{"
        + "\"boundary\":\"canonical_write\","
        + "\"status\":\"requested\","
        + "\"targetFieldPath\":\"" + json(command.targetFieldPath()) + "\","
        + "\"proposedValueRef\":\"" + json(command.proposedValueRef()) + "\","
        + "\"claimId\":\"" + command.claimId().value() + "\","
        + "\"reviewEventId\":\"" + command.reviewEvidence().reviewEventId().value() + "\","
        + "\"candidateProfileId\":\"" + candidateProfileId(command) + "\""
        + "}";
  }

  private static String afterState(CanonicalWriteCommand command) {
    boolean willWriteProfile = command.candidateProfileWriteTarget() != null;
    return "{"
        + "\"boundary\":\"canonical_write\","
        + "\"status\":\""
        + (willWriteProfile ? "allowed_candidate_profile_field_persisted"
            : "allowed_audit_appended")
        + "\","
        + "\"targetFieldPath\":\"" + json(command.targetFieldPath()) + "\","
        + "\"canonicalPersistence\":\""
        + (willWriteProfile ? CANDIDATE_PROFILE_FIELD_PERSISTED : CANONICAL_PERSISTENCE_DEFERRED)
        + "\""
        + "}";
  }

  private static String candidateProfileId(CanonicalWriteCommand command) {
    if (command.candidateProfileWriteTarget() == null) {
      return "";
    }
    return command.candidateProfileWriteTarget().candidateProfileId().value().toString();
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
