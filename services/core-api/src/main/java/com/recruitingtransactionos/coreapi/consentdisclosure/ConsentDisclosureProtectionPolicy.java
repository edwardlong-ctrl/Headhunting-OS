package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ConsentDisclosureProtectionPolicy {

  public UnlockDisclosureDecision decide(UnlockDisclosureRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    if (request.requestedLevel() == DisclosureLevel.RAW_CANDIDATE) {
      return UnlockDisclosureDecision.deny(List.of("raw_candidate_client_access_denied"));
    }
    if (request.requestedLevel() == DisclosureLevel.RAW_CANDIDATE_PROFILE) {
      return UnlockDisclosureDecision.deny(List.of("raw_candidate_profile_client_access_denied"));
    }
    if (request.requestedLevel().isAnonymousClientSafeLevel()
        && !request.requestedLevel().requiresConfirmedConsent()) {
      return UnlockDisclosureDecision.allow(request.requestedLevel(), false, Optional.empty());
    }
    if (request.requestedLevel() == DisclosureLevel.L3_CONSENTED_DETAIL) {
      List<String> consentReasons = consentDenialReasons(request);
      if (!consentReasons.isEmpty()) {
        return UnlockDisclosureDecision.deny(consentReasons);
      }
      return UnlockDisclosureDecision.allow(DisclosureLevel.L3_CONSENTED_DETAIL, false,
          Optional.empty());
    }

    List<String> denialReasons = new ArrayList<>();
    denialReasons.addAll(consentDenialReasons(request));
    denialReasons.addAll(unlockDenialReasons(request));
    denialReasons.addAll(disclosureDenialReasons(request));
    denialReasons.addAll(auditBoundaryDenialReasons(request));
    if (!denialReasons.isEmpty()) {
      return UnlockDisclosureDecision.deny(denialReasons);
    }

    DisclosureAuditBoundary boundary = request.auditBoundary().orElseThrow();
    return UnlockDisclosureDecision.allow(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        true,
        Optional.of(new DisclosureAuditCommand(
            boundary.actionCode(),
            boundary.riskTier(),
            boundary.workflowEventId())));
  }

  private static List<String> consentDenialReasons(UnlockDisclosureRequest request) {
    if (request.consentRecord().isEmpty()) {
      return List.of("missing_confirmed_consent");
    }
    ConsentRecord consent = request.consentRecord().orElseThrow();
    List<String> reasons = new ArrayList<>();
    if (!sameCoreScope(request, consent.organizationId(), consent.candidateRef(),
        consent.candidateProfileRef(), consent.jobRef())) {
      reasons.add("consent_scope_mismatch");
    }
    if (consent.status() != ConsentStatus.CONFIRMED) {
      reasons.add("consent_not_confirmed");
    }
    if (consent.revoked() || consent.status() == ConsentStatus.REVOKED) {
      reasons.add("consent_revoked");
    }
    if (consent.isExpiredAt(request.requestedAt())) {
      reasons.add("consent_expired");
    }
    if (!consent.permittedDisclosureLevels().contains(request.requestedLevel())) {
      reasons.add("consent_scope_excludes_requested_level");
    }
    return reasons;
  }

  private static List<String> unlockDenialReasons(UnlockDisclosureRequest request) {
    if (request.unlockDecision().isEmpty()) {
      return List.of("missing_approved_unlock_decision");
    }
    UnlockDecision unlockDecision = request.unlockDecision().orElseThrow();
    List<String> reasons = new ArrayList<>();
    if (!sameFullScope(request, unlockDecision.organizationId(), unlockDecision.candidateRef(),
        unlockDecision.candidateProfileRef(), unlockDecision.jobRef(), unlockDecision.clientRef())) {
      reasons.add("unlock_scope_mismatch");
    }
    if (unlockDecision.status() != UnlockDecisionStatus.APPROVED) {
      reasons.add("unlock_decision_not_approved");
    }
    if (unlockDecision.reviewStatus() != DisclosureReviewStatus.HUMAN_APPROVED) {
      reasons.add("unlock_human_review_not_approved");
    }
    if (!isHumanActor(unlockDecision.approvedBy().role())) {
      reasons.add("unlock_human_approval_required");
    }
    if (unlockDecision.requestedDisclosureLevel() != request.requestedLevel()) {
      reasons.add("unlock_level_mismatch");
    }
    if (unlockDecision.riskTier() != RiskTier.T4_TRANSACTION_LEGAL_BLOCKING) {
      reasons.add("unlock_risk_tier_not_t4");
    }
    return reasons;
  }

  private static List<String> disclosureDenialReasons(UnlockDisclosureRequest request) {
    if (request.disclosureRecord().isEmpty()) {
      return List.of("missing_approved_disclosure_record");
    }
    DisclosureRecord disclosure = request.disclosureRecord().orElseThrow();
    List<String> reasons = new ArrayList<>();
    if (!sameFullScope(request, disclosure.organizationId(), disclosure.candidateRef(),
        disclosure.candidateProfileRef(), disclosure.jobRef(), disclosure.clientRef())) {
      reasons.add("disclosure_scope_mismatch");
    }
    if (disclosure.status() != DisclosureStatus.CONSULTANT_APPROVED) {
      reasons.add("disclosure_not_approved");
    }
    if (disclosure.disclosureLevel() != request.requestedLevel()) {
      reasons.add("disclosure_level_mismatch");
    }
    request.requestedLevel().redactionLevel().ifPresent(expectedRedactionLevel -> {
      if (disclosure.redactionLevel() != expectedRedactionLevel) {
        reasons.add("disclosure_redaction_level_mismatch");
      }
    });
    return reasons;
  }

  private static List<String> auditBoundaryDenialReasons(UnlockDisclosureRequest request) {
    if (request.auditBoundary().isEmpty()) {
      return List.of("missing_audit_boundary");
    }
    DisclosureAuditBoundary boundary = request.auditBoundary().orElseThrow();
    List<String> reasons = new ArrayList<>();
    if (boundary.actionCode() != WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED) {
      reasons.add("audit_action_not_identity_disclosure");
    }
    if (boundary.riskTier() != RiskTier.T4_TRANSACTION_LEGAL_BLOCKING) {
      reasons.add("audit_risk_tier_not_t4");
    }
    return reasons;
  }

  private static boolean sameCoreScope(
      UnlockDisclosureRequest request,
      java.util.UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef) {
    return request.organizationId().equals(organizationId)
        && request.candidateRef().equals(candidateRef)
        && request.candidateProfileRef().equals(candidateProfileRef)
        && request.jobRef().equals(jobRef);
  }

  private static boolean sameFullScope(
      UnlockDisclosureRequest request,
      java.util.UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef,
      String clientRef) {
    return sameCoreScope(request, organizationId, candidateRef, candidateProfileRef, jobRef)
        && request.clientRef().equals(clientRef);
  }

  private static boolean isHumanActor(ActorRole role) {
    return role == ActorRole.OWNER
        || role == ActorRole.CONSULTANT
        || role == ActorRole.CLIENT
        || role == ActorRole.CANDIDATE
        || role == ActorRole.ADMIN;
  }
}
