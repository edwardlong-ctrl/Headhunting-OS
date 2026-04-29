package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsentDisclosureProtectionPolicyTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000120001");
  private static final String CANDIDATE_REF = "candidate_ref_task12a_0001";
  private static final String PROFILE_REF = "profile_ref_task12a_0001";
  private static final String JOB_REF = "job_ref_task12a_0001";
  private static final String CLIENT_REF = "client_ref_task12a_0001";
  private static final String PROFILE_VERSION = "profile-version-12a";
  private static final String CONSENT_TEXT_VERSION = "consent-text-12a";
  private static final Instant NOW = Instant.parse("2026-04-29T10:15:30Z");

  private final ConsentDisclosureProtectionPolicy policy =
      new ConsentDisclosureProtectionPolicy();

  @Test
  void rawCandidateAndProfileAccessRemainDeniedAtProtectionPolicy() {
    UnlockDisclosureDecision rawCandidate = policy.decide(request(
        DisclosureLevel.RAW_CANDIDATE,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary()));
    UnlockDisclosureDecision rawProfile = policy.decide(request(
        DisclosureLevel.RAW_CANDIDATE_PROFILE,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary()));

    assertDenied(rawCandidate, "raw_candidate_client_access_denied");
    assertDenied(rawProfile, "raw_candidate_profile_client_access_denied");
  }

  @Test
  void l4IdentityDisclosureCannotBeGrantedByRoleAlone() {
    UnlockDisclosureDecision consultantOnly = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        null,
        null,
        null,
        null));

    assertDenied(consultantOnly, "missing_confirmed_consent");
    assertThat(consultantOnly.reasonCodes())
        .contains(
            "missing_approved_unlock_decision",
            "missing_approved_disclosure_record",
            "missing_audit_boundary");
    assertThat(consultantOnly.roleAloneGrant()).isFalse();
  }

  @Test
  void unlockDisclosureCannotBypassConsentDisclosureProtection() {
    UnlockDisclosureDecision unlockWithoutDisclosure = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        null,
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary()));
    UnlockDisclosureDecision disclosureWithoutUnlock = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        null,
        auditBoundary()));

    assertDenied(unlockWithoutDisclosure, "missing_approved_disclosure_record");
    assertDenied(disclosureWithoutUnlock, "missing_approved_unlock_decision");
  }

  @Test
  void existingAnonymousClientSafeProjectionLevelsRemainAllowedWithoutIdentityDisclosure() {
    for (DisclosureLevel level : Set.of(
        DisclosureLevel.L0_TEASER,
        DisclosureLevel.L1_GENERALIZED,
        DisclosureLevel.L2_CLIENT_SAFE)) {
      UnlockDisclosureDecision decision = policy.decide(request(
          level,
          null,
          null,
          null,
          null));

      assertThat(decision.status()).isEqualTo(UnlockDisclosureDecisionStatus.ALLOWED);
      assertThat(decision.allowedLevel()).isEqualTo(Optional.of(level));
      assertThat(decision.rawCandidateExposureAllowed()).isFalse();
      assertThat(decision.auditRequiredBeforeRelease()).isFalse();
    }
  }

  @Test
  void l3ConsentedDetailRequiresConfirmedConsentButNotIdentityDisclosure() {
    UnlockDisclosureDecision missingConsent = policy.decide(request(
        DisclosureLevel.L3_CONSENTED_DETAIL,
        null,
        null,
        null,
        null));
    UnlockDisclosureDecision confirmedConsent = policy.decide(request(
        DisclosureLevel.L3_CONSENTED_DETAIL,
        consent(ConsentStatus.CONFIRMED),
        null,
        null,
        null));

    assertDenied(missingConsent, "missing_confirmed_consent");
    assertThat(confirmedConsent.status()).isEqualTo(UnlockDisclosureDecisionStatus.ALLOWED);
    assertThat(confirmedConsent.rawCandidateExposureAllowed()).isFalse();
  }

  @Test
  void protectionFailsClosedForAbsentInvalidExpiredRevokedOrNotHumanApprovedState() {
    assertDenied(policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        null,
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary())), "missing_confirmed_consent");

    for (ConsentStatus status : Set.of(
        ConsentStatus.REQUESTED,
        ConsentStatus.VIEWED_BY_CANDIDATE,
        ConsentStatus.DECLINED,
        ConsentStatus.EXPIRED,
        ConsentStatus.REVOKED,
        ConsentStatus.INVALID)) {
      assertDenied(policy.decide(request(
          DisclosureLevel.L4_IDENTITY_DISCLOSED,
          consent(status),
          disclosure(DisclosureStatus.APPROVED),
          unlockDecision(UnlockDecisionStatus.APPROVED),
          auditBoundary())), "consent_not_confirmed");
    }

    assertDenied(policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        expiredConsent(),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary())), "consent_expired");

    assertDenied(policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.REQUIRES_REVIEW),
        auditBoundary())), "unlock_decision_not_approved");

    assertDenied(policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        systemApprovedUnlockDecision(),
        auditBoundary())), "unlock_human_approval_required");

    assertDenied(policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.REQUESTED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary())), "disclosure_not_approved");
  }

  @Test
  void allowedIdentityDisclosureRequiresAuditBoundaryAndReturnsAuditCommand() {
    UnlockDisclosureDecision missingAudit = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        null));
    UnlockDisclosureDecision allowed = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosure(DisclosureStatus.APPROVED),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary()));

    assertDenied(missingAudit, "missing_audit_boundary");
    assertThat(allowed.status()).isEqualTo(UnlockDisclosureDecisionStatus.ALLOWED);
    assertThat(allowed.allowedLevel()).contains(DisclosureLevel.L4_IDENTITY_DISCLOSED);
    assertThat(allowed.rawCandidateExposureAllowed()).isFalse();
    assertThat(allowed.auditCommand()).isPresent();
    assertThat(allowed.auditCommand().orElseThrow().actionCode())
        .isEqualTo(WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED);
    assertThat(allowed.auditCommand().orElseThrow().riskTier())
        .isEqualTo(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING);
    assertThat(allowed.auditCommand().orElseThrow().workflowEventId())
        .contains(new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000120099")));
  }

  @Test
  void approvedL4DisclosureWithWeakerRedactionLevelFailsClosed() {
    UnlockDisclosureDecision decision = policy.decide(request(
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        consent(ConsentStatus.CONFIRMED),
        disclosureWithRedactionLevel(RedactionLevel.L2_CLIENT_SAFE),
        unlockDecision(UnlockDecisionStatus.APPROVED),
        auditBoundary()));

    assertDenied(decision, "disclosure_redaction_level_mismatch");
  }

  private static UnlockDisclosureRequest request(
      DisclosureLevel requestedLevel,
      ConsentRecord consentRecord,
      DisclosureRecord disclosureRecord,
      UnlockDecision unlockDecision,
      DisclosureAuditBoundary auditBoundary) {
    return new UnlockDisclosureRequest(
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        PortalRole.CONSULTANT,
        requestedLevel,
        Optional.ofNullable(consentRecord),
        Optional.ofNullable(disclosureRecord),
        Optional.ofNullable(unlockDecision),
        Optional.ofNullable(auditBoundary),
        NOW);
  }

  private static ConsentRecord consent(ConsentStatus status) {
    return new ConsentRecord(
        "consent_record_task12a_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        PROFILE_VERSION,
        CONSENT_TEXT_VERSION,
        status,
        Set.of(DisclosureLevel.L3_CONSENTED_DETAIL, DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(3600),
        NOW.plusSeconds(3600),
        false);
  }

  private static ConsentRecord expiredConsent() {
    return new ConsentRecord(
        "consent_record_task12a_expired",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        PROFILE_VERSION,
        CONSENT_TEXT_VERSION,
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(7200),
        NOW.minusSeconds(3600),
        false);
  }

  private static DisclosureRecord disclosure(DisclosureStatus status) {
    return disclosureWithRedactionLevel(status, RedactionLevel.L4_IDENTITY_DISCLOSED);
  }

  private static DisclosureRecord disclosureWithRedactionLevel(RedactionLevel redactionLevel) {
    return disclosureWithRedactionLevel(DisclosureStatus.APPROVED, redactionLevel);
  }

  private static DisclosureRecord disclosureWithRedactionLevel(
      DisclosureStatus status,
      RedactionLevel redactionLevel) {
    return new DisclosureRecord(
        "disclosure_record_task12a_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        status,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        redactionLevel,
        "unlock_decision_task12a_0001",
        "consent_record_task12a_0001",
        Optional.of(new WorkflowEventId(
            UUID.fromString("00000000-0000-0000-0000-000000120098"))),
        NOW);
  }

  private static UnlockDecision unlockDecision(UnlockDecisionStatus status) {
    return new UnlockDecision(
        "unlock_decision_task12a_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        status,
        DisclosureReviewStatus.HUMAN_APPROVED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new ActorRef(
            UUID.fromString("00000000-0000-0000-0000-000000120055"),
            ActorRole.CONSULTANT),
        NOW.minusSeconds(120));
  }

  private static UnlockDecision systemApprovedUnlockDecision() {
    return new UnlockDecision(
        "unlock_decision_task12a_system",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        UnlockDecisionStatus.APPROVED,
        DisclosureReviewStatus.HUMAN_APPROVED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new ActorRef(
            UUID.fromString("00000000-0000-0000-0000-000000120056"),
            ActorRole.SYSTEM),
        NOW.minusSeconds(120));
  }

  private static DisclosureAuditBoundary auditBoundary() {
    return new DisclosureAuditBoundary(
        WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        Optional.of(new WorkflowEventId(
            UUID.fromString("00000000-0000-0000-0000-000000120099"))));
  }

  private static void assertDenied(UnlockDisclosureDecision decision, String reasonCode) {
    assertThat(decision.status()).isEqualTo(UnlockDisclosureDecisionStatus.DENIED);
    assertThat(decision.reasonCodes()).contains(reasonCode);
    assertThat(decision.rawCandidateExposureAllowed()).isFalse();
  }
}
