package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
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
  private final WorkflowEventService workflowEventService;
  private final CanonicalWriteTransactionBoundary transactionBoundary;

  public ConsentDisclosureService(
      ConsentRecordPort consentRecordPort,
      UnlockDecisionPort unlockDecisionPort,
      DisclosureRecordPort disclosureRecordPort,
      ConsentDisclosureProtectionPolicy protectionPolicy,
      WorkflowEventService workflowEventService,
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
    this.workflowEventService = Objects.requireNonNull(
        workflowEventService,
        "workflowEventService must not be null");
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

    List<String> chainReasons = approvalChainDenialReasons(request, disclosureRecord);
    if (!chainReasons.isEmpty()) {
      return ConsentDisclosureServiceResult.denied(chainReasons);
    }
    if (request.requestedLevel() == DisclosureLevel.L3_CONSENTED_DETAIL) {
      return ConsentDisclosureServiceResult.allowed(request.requestedLevel());
    }

    List<String> reviewReasons = deferredReviewReasons(request.prerequisites(), request.requestedLevel());
    if (!reviewReasons.isEmpty()) {
      return ConsentDisclosureServiceResult.requiresReview(reviewReasons);
    }

    return transactionBoundary.run(() -> appendAuditAndDisclosureBoundary(request, decision));
  }

  static UUID disclosureEntityId(UUID organizationId, String disclosureRecordRef) {
    return uuidFor("disclosure|" + organizationId + "|" + disclosureRecordRef);
  }

  private ConsentDisclosureServiceResult appendAuditAndDisclosureBoundary(
      ConsentDisclosureServiceRequest request,
      UnlockDisclosureDecision decision) {
    String resultingDisclosureRecordRef = "disclosure_boundary_release_"
        + sha256(request.organizationId() + "|" + request.approvedDisclosureRecordRef()
            + "|" + request.requestedAt()).substring(0, 16);
    WorkflowEventAppendResult workflowEvent = workflowEventService.append(new WorkflowEventAppendCommand(
        request.organizationId(),
        "workflow",
        new EntityRef("DISCLOSURE", disclosureEntityId(request.organizationId(), resultingDisclosureRecordRef)),
        1,
        WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED.wireValue(),
        new WorkflowStateSnapshot(beforeState(request)),
        new WorkflowStateSnapshot(afterState(request, resultingDisclosureRecordRef)),
        request.actor(),
        "consent_disclosure_service",
        null,
        null,
        null,
        request.reason(),
        new WorkflowIdempotencyKey(idempotencyKey(request, resultingDisclosureRecordRef)),
        null,
        null,
        request.requestedAt()));

    DisclosureRecord appendedBoundary = disclosureRecordPort.appendIfAbsent(new DisclosureRecord(
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
        Optional.of(workflowEvent.workflowEventId()),
        request.requestedAt()));

    return ConsentDisclosureServiceResult.allowed(
        decision.allowedLevel().orElseThrow(),
        workflowEvent.workflowEventId(),
        appendedBoundary.disclosureRecordRef());
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

  private static List<String> deferredReviewReasons(
      ConsentDisclosurePrerequisites prerequisites,
      DisclosureLevel requestedLevel) {
    if (!requestedLevel.requiresUnlockAndDisclosure()) {
      return List.of();
    }
    List<String> reasons = new ArrayList<>();
    if (!prerequisites.jobActivated()) {
      reasons.add("future_job_activation_gate_required");
    }
    if (!prerequisites.feeAgreementActive()) {
      reasons.add("future_fee_agreement_gate_required");
    }
    if (!prerequisites.priorContactCleared()) {
      reasons.add("future_prior_contact_review_required");
    }
    if (!prerequisites.priorApplicationCleared()) {
      reasons.add("future_prior_application_review_required");
    }
    return reasons;
  }

  private static String beforeState(ConsentDisclosureServiceRequest request) {
    return "{"
        + "\"status\":\"approved\","
        + "\"disclosureRecordRef\":\"" + request.approvedDisclosureRecordRef() + "\","
        + "\"requestedLevel\":\"" + request.requestedLevel().wireValue() + "\""
        + "}";
  }

  private static String afterState(
      ConsentDisclosureServiceRequest request,
      String resultingDisclosureRecordRef) {
    return "{"
        + "\"status\":\"identity_disclosed\","
        + "\"disclosureRecordRef\":\"" + resultingDisclosureRecordRef + "\","
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
