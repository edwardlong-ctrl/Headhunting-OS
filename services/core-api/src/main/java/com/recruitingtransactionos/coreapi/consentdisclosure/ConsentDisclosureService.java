package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.CandidateWorkflowStatePort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ConsentDisclosureService {

  private final ConsentRecordPort consentRecordPort;
  private final UnlockDecisionPort unlockDecisionPort;
  private final DisclosureRecordPort disclosureRecordPort;
  private final ConsentDisclosureProtectionPolicy protectionPolicy;
  private final ConsentDisclosurePrerequisiteEvaluator prerequisiteEvaluator;
  private final CandidateWorkflowStatePort candidateWorkflowStatePort;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;
  private final CanonicalWriteTransactionBoundary transactionBoundary;

  public ConsentDisclosureService(
      ConsentRecordPort consentRecordPort,
      UnlockDecisionPort unlockDecisionPort,
      DisclosureRecordPort disclosureRecordPort,
      ConsentDisclosureProtectionPolicy protectionPolicy,
      ConsentDisclosurePrerequisiteEvaluator prerequisiteEvaluator,
      CandidateWorkflowStatePort candidateWorkflowStatePort,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      CanonicalWriteTransactionBoundary transactionBoundary) {
    this.consentRecordPort = Objects.requireNonNull(
        consentRecordPort,
        "consentRecordPort must not be null");
    this.unlockDecisionPort = Objects.requireNonNull(
        unlockDecisionPort,
        "unlockDecisionPort must not be null");
    this.disclosureRecordPort = Objects.requireNonNull(
        disclosureRecordPort,
        "disclosureRecordPort must not be null");
    this.protectionPolicy = Objects.requireNonNull(
        protectionPolicy,
        "protectionPolicy must not be null");
    this.prerequisiteEvaluator = Objects.requireNonNull(
        prerequisiteEvaluator,
        "prerequisiteEvaluator must not be null");
    this.candidateWorkflowStatePort = Objects.requireNonNull(
        candidateWorkflowStatePort,
        "candidateWorkflowStatePort must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService,
        "workflowTransitionAuditService must not be null");
    this.transactionBoundary = Objects.requireNonNull(
        transactionBoundary,
        "transactionBoundary must not be null");
  }

  public ConsentDisclosureServiceResult evaluateDisclosureAttempt(
      ConsentDisclosureServiceRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    Optional<ConsentRecord> consent = consentRecordPort.findByRefAndOrganizationId(
        request.organizationId(),
        request.consentRecordRef());
    Optional<UnlockDecision> unlockDecision = unlockDecisionPort.findByRefAndOrganizationId(
        request.organizationId(),
        request.unlockDecisionRef());
    Optional<DisclosureRecord> disclosureRecord = disclosureRecordPort.findByRefAndOrganizationId(
        request.organizationId(),
        request.approvedDisclosureRecordRef());
    Optional<ConsentDisclosureServiceResult> idempotentResult = existingAllowedResult(request);
    if (idempotentResult.isPresent()) {
      return idempotentResult.orElseThrow();
    }

    UnlockDisclosureDecision decision = protectionPolicy.decide(new UnlockDisclosureRequest(
        request.organizationId(),
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef(),
        request.clientRef(),
        request.requestedByRole(),
        request.requestedLevel(),
        consent,
        disclosureRecord,
        unlockDecision,
        Optional.of(auditBoundary()),
        request.requestedAt()));

    if (decision.status() == UnlockDisclosureDecisionStatus.DENIED) {
      return ConsentDisclosureServiceResult.denied(decision.reasonCodes());
    }

    if (request.requestedLevel() == DisclosureLevel.L3_CONSENTED_DETAIL) {
      return ConsentDisclosureServiceResult.allowed(request.requestedLevel());
    }

    if (!unlockDecisionPort.approvedByBelongsToOrganization(
        request.organizationId(),
        request.unlockDecisionRef())) {
      return ConsentDisclosureServiceResult.denied(List.of(
          "unlock_approver_organization_mismatch"));
    }

    List<String> chainReasons = approvalChainDenialReasons(request, disclosureRecord);
    if (!chainReasons.isEmpty()) {
      return ConsentDisclosureServiceResult.denied(chainReasons);
    }

    ConsentDisclosurePrerequisites prerequisites = prerequisiteEvaluator.evaluate(
        request,
        unlockDecision,
        disclosureRecord);
    List<String> gateReasons = blockingGateReasons(prerequisites, request.requestedLevel());
    if (!gateReasons.isEmpty()) {
      return ConsentDisclosureServiceResult.denied(gateReasons);
    }

    return transactionBoundary.run(() -> appendAuditAndDisclosureBoundary(request, decision));
  }

  static UUID disclosureEntityId(UUID organizationId, String disclosureRecordRef) {
    return ConsentDisclosureWorkflowEntityIds.disclosureEntityId(organizationId, disclosureRecordRef);
  }

  static UUID candidateEntityId(UUID organizationId, String candidateRef) {
    return ConsentDisclosureWorkflowEntityIds.candidateEntityId(organizationId, candidateRef);
  }

  private ConsentDisclosureServiceResult appendAuditAndDisclosureBoundary(
      ConsentDisclosureServiceRequest request,
      UnlockDisclosureDecision decision) {
    String resultingDisclosureRecordRef = resultingDisclosureRecordRef(request);
    WorkflowEventAppendResult disclosureWorkflowEvent =
        workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
            .organizationId(request.organizationId())
            .entityNamespace("workflow")
            .entityType("DISCLOSURE")
            .entityId(disclosureEntityId(request.organizationId(), request.approvedDisclosureRecordRef()))
            .entityVersion(1)
            .actionCode(WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED.wireValue())
            .actorType(request.actor().role())
            .actorId(request.actor().userId())
            .aiInvolvement(WorkflowAiInvolvement.NONE)
            .beforeState(beforeState(request))
            .afterState(afterState(request))
            .reason(request.reason())
            .idempotencyKey(idempotencyKey(request, resultingDisclosureRecordRef))
            .sourceType("consent_disclosure_service")
            .occurredAt(request.requestedAt())
            .build());
    disclosureRecordPort.transitionToIdentityDisclosed(
        request.organizationId(),
        request.approvedDisclosureRecordRef(),
        disclosureWorkflowEvent.workflowEventId(),
        request.requestedAt());

    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(request.organizationId())
        .entityNamespace("workflow")
        .entityType("CANDIDATE")
        .entityId(candidateEntityId(request.organizationId(), request.candidateRef()))
        .entityVersion(1)
        .actionCode(WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED.wireValue())
        .actorType(request.actor().role())
        .actorId(request.actor().userId())
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(candidateBeforeState())
        .afterState(candidateAfterState())
        .reason(request.reason())
        .idempotencyKey(candidateDisclosureIdempotencyKey(request, resultingDisclosureRecordRef))
        .sourceType("consent_disclosure_service")
        .occurredAt(request.requestedAt())
        .build());
    candidateWorkflowStatePort.transitionToIdentityDisclosed(
        request.organizationId(),
        request.candidateRef(),
        request.requestedAt());

    DisclosureRecord boundary = new DisclosureRecord(
        resultingDisclosureRecordRef,
        request.organizationId(),
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef(),
        request.clientRef(),
        DisclosureStatus.IDENTITY_DISCLOSED,
        decision.allowedLevel().orElseThrow(),
        decision.allowedLevel().orElseThrow().redactionLevel().orElseThrow(),
        request.unlockDecisionRef(),
        request.consentRecordRef(),
        Optional.of(disclosureWorkflowEvent.workflowEventId()),
        request.requestedAt());
    DisclosureRecord appendedBoundary = appendFinalDisclosureRetrySafely(boundary);

    return ConsentDisclosureServiceResult.allowed(
        decision.allowedLevel().orElseThrow(),
        disclosureWorkflowEvent.workflowEventId(),
        appendedBoundary.disclosureRecordRef());
  }

  private Optional<ConsentDisclosureServiceResult> existingAllowedResult(
      ConsentDisclosureServiceRequest request) {
    return disclosureRecordPort.findByRefAndOrganizationId(
            request.organizationId(),
            resultingDisclosureRecordRef(request))
        .filter(existing -> existing.status() == DisclosureStatus.IDENTITY_DISCLOSED)
        .filter(existing -> existing.disclosureLevel() == request.requestedLevel())
        .filter(existing -> existing.redactionLevel()
            == request.requestedLevel().redactionLevel().orElse(null))
        .flatMap(existing -> existing.workflowEventId()
            .map(workflowEventId -> ConsentDisclosureServiceResult.allowed(
                existing.disclosureLevel(),
                workflowEventId,
                existing.disclosureRecordRef())));
  }

  private DisclosureRecord appendFinalDisclosureRetrySafely(DisclosureRecord boundary) {
    try {
      return disclosureRecordPort.appendIfAbsent(boundary);
    } catch (IllegalStateException exception) {
      return disclosureRecordPort.findByRefAndOrganizationId(
          boundary.organizationId(),
          boundary.disclosureRecordRef())
          .filter(existing -> sameFinalDisclosureBoundary(existing, boundary))
          .orElseThrow(() -> exception);
    }
  }

  private static boolean sameFinalDisclosureBoundary(
      DisclosureRecord existing,
      DisclosureRecord requested) {
    return existing.organizationId().equals(requested.organizationId())
        && existing.disclosureRecordRef().equals(requested.disclosureRecordRef())
        && existing.candidateRef().equals(requested.candidateRef())
        && existing.candidateProfileRef().equals(requested.candidateProfileRef())
        && existing.jobRef().equals(requested.jobRef())
        && existing.clientRef().equals(requested.clientRef())
        && existing.status() == requested.status()
        && existing.disclosureLevel() == requested.disclosureLevel()
        && existing.redactionLevel() == requested.redactionLevel()
        && existing.unlockDecisionRef().equals(requested.unlockDecisionRef())
        && existing.consentRecordRef().equals(requested.consentRecordRef())
        && existing.workflowEventId().equals(requested.workflowEventId())
        && existing.decidedAt().equals(requested.decidedAt());
  }

  private static List<String> approvalChainDenialReasons(
      ConsentDisclosureServiceRequest request,
      Optional<DisclosureRecord> disclosureRecord) {
    if (disclosureRecord.isEmpty()) {
      return List.of();
    }
    DisclosureRecord disclosure = disclosureRecord.orElseThrow();
    List<String> reasons = new ArrayList<>();
    if (!disclosure.consentRecordRef().equals(request.consentRecordRef())) {
      reasons.add("disclosure_consent_record_ref_mismatch");
    }
    if (!disclosure.unlockDecisionRef().equals(request.unlockDecisionRef())) {
      reasons.add("disclosure_unlock_decision_ref_mismatch");
    }
    return reasons;
  }

  private static DisclosureAuditBoundary auditBoundary() {
    return new DisclosureAuditBoundary(
        WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
        com.recruitingtransactionos.coreapi.truthlayer.RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        Optional.empty());
  }

  private static List<String> blockingGateReasons(
      ConsentDisclosurePrerequisites prerequisites,
      DisclosureLevel requestedLevel) {
    if (!requestedLevel.requiresUnlockAndDisclosure()) {
      return List.of();
    }
    List<String> reasons = new ArrayList<>();
    if (!prerequisites.jobActivated()) {
      reasons.add("job_activation_gate_required");
    }
    if (!prerequisites.feeAgreementActive()) {
      reasons.add("fee_agreement_gate_required");
    }
    if (!prerequisites.priorContactCleared()) {
      reasons.add("prior_contact_review_required");
    }
    if (!prerequisites.priorApplicationCleared()) {
      reasons.add("prior_application_review_required");
    }
    if (!prerequisites.privacyRiskCleared()) {
      reasons.add("privacy_risk_gate_required");
    }
    return reasons;
  }

  private static String beforeState(ConsentDisclosureServiceRequest request) {
    return "{"
        + "\"status\":\"consultant_approved\","
        + "\"disclosureRecordRef\":\"" + request.approvedDisclosureRecordRef() + "\","
        + "\"requestedLevel\":\"" + request.requestedLevel().wireValue() + "\""
        + "}";
  }

  private static String afterState(ConsentDisclosureServiceRequest request) {
    return "{"
        + "\"status\":\"identity_disclosed\","
        + "\"disclosureRecordRef\":\"" + request.approvedDisclosureRecordRef() + "\","
        + "\"requestedLevel\":\"" + request.requestedLevel().wireValue() + "\""
        + "}";
  }

  private static String idempotencyKey(
      ConsentDisclosureServiceRequest request,
      String resultingDisclosureRecordRef) {
    return "consent-disclosure-release|"
        + sha256(request.organizationId()
            + "|" + request.approvedDisclosureRecordRef()
            + "|" + request.unlockDecisionRef()
            + "|" + resultingDisclosureRecordRef);
  }

  private static String candidateDisclosureIdempotencyKey(
      ConsentDisclosureServiceRequest request,
      String resultingDisclosureRecordRef) {
    return "candidate-identity-disclosed|"
        + sha256(request.organizationId()
            + "|" + request.candidateRef()
            + "|" + request.unlockDecisionRef()
            + "|" + resultingDisclosureRecordRef);
  }

  private static String candidateBeforeState() {
    return "{\"status\":\"consent_confirmed\"}";
  }

  private static String candidateAfterState() {
    return "{\"status\":\"identity_disclosed\"}";
  }

  private static String resultingDisclosureRecordRef(ConsentDisclosureServiceRequest request) {
    return "disclosure_boundary_release_"
        + sha256(request.organizationId() + "|" + request.approvedDisclosureRecordRef()
            + "|" + request.requestedAt()).substring(0, 16);
  }

  private static UUID uuidFor(String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(Object value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(
          digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
    }
  }
}
