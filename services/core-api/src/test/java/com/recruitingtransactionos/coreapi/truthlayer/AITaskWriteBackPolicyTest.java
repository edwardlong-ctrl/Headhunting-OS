package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskGovernanceDecision;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskGovernancePolicy;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskGovernanceRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AITaskWriteBackPolicyTest {

  private static final ActorRef HUMAN_REVIEWER = new ActorRef(
      UUID.fromString("00000000-0000-0000-0000-00000010b001"),
      ActorRole.CONSULTANT);
  private static final ActorRef AI_REVIEWER = new ActorRef(
      UUID.fromString("00000000-0000-0000-0000-00000010b002"),
      ActorRole.AI);
  private static final ActorRef SYSTEM_REVIEWER = new ActorRef(
      UUID.fromString("00000000-0000-0000-0000-00000010b003"),
      ActorRole.SYSTEM);

  private final AITaskGovernancePolicy policy = new AITaskGovernancePolicy();

  @Test
  void writeBackTargetVocabularyIsSmallAndExplicit() {
    assertThat(AITaskWriteBackTarget.values())
        .containsExactly(
            AITaskWriteBackTarget.NONE,
            AITaskWriteBackTarget.NO_WRITE_BACK,
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskWriteBackTarget.REVIEW_QUEUE,
            AITaskWriteBackTarget.HUMAN_REVIEW_REQUIRED,
            AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
            AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION,
            AITaskWriteBackTarget.JOB_PROFILE,
            AITaskWriteBackTarget.COMPANY_PROFILE,
            AITaskWriteBackTarget.CONSENT_DISCLOSURE,
            AITaskWriteBackTarget.WORKFLOW_ACTION,
            AITaskWriteBackTarget.COMMERCIAL_OR_PLACEMENT);

    assertThat(AITaskWriteBackTarget.NO_WRITE_BACK.wireValue()).isEqualTo("no_write_back");
    assertThat(AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE.wireValue())
        .isEqualTo("canonical_candidate_profile");
    assertThat(AITaskWriteBackTarget.CONSENT_DISCLOSURE.wireValue())
        .isEqualTo("consent_disclosure");
  }

  @Test
  void humanReviewStatusVocabularyIsSmallAndExplicit() {
    assertThat(AITaskHumanReviewStatus.values())
        .containsExactly(
            AITaskHumanReviewStatus.NOT_REQUIRED,
            AITaskHumanReviewStatus.REQUIRED,
            AITaskHumanReviewStatus.PENDING,
            AITaskHumanReviewStatus.APPROVED,
            AITaskHumanReviewStatus.REJECTED,
            AITaskHumanReviewStatus.NEEDS_REVISION,
            AITaskHumanReviewStatus.EXPIRED);

    assertThat(AITaskHumanReviewStatus.NOT_REQUIRED.wireValue()).isEqualTo("not_required");
    assertThat(AITaskHumanReviewStatus.NEEDS_REVISION.wireValue())
        .isEqualTo("needs_revision");
  }

  @Test
  void noWriteBackIsAllowedAsMetadataOnly() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.NO_WRITE_BACK,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        null,
        false,
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("no_write_back_metadata_only");
    assertThat(decision.humanReviewRequired()).isFalse();
    assertThat(decision.canonicalGateRequired()).isFalse();
    assertThat(decision.futureConsentDisclosureUnlockGateRequired()).isFalse();
  }

  @Test
  void noneWriteBackTargetIsAlsoMetadataOnly() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.NONE,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        null,
        false,
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("no_write_back_metadata_only");
    assertThat(decision.humanReviewRequired()).isFalse();
    assertThat(decision.canonicalGateRequired()).isFalse();
    assertThat(decision.futureConsentDisclosureUnlockGateRequired()).isFalse();
  }

  @Test
  void claimLedgerProposalTargetDoesNotBecomeFactOrCanonicalWrite() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
        AITaskHumanReviewStatus.REQUIRED,
        null,
        false,
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("claim_ledger_proposal_metadata_only");
    assertThat(decision.safeExplanation()).contains("claim");
    assertThat(decision.safeExplanation()).doesNotContain("fact");
    assertThat(decision.canonicalGateRequired()).isFalse();
  }

  @Test
  void canonicalCandidateProfileTargetRequiresApprovedHumanReviewAndCanonicalGate() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
        AITaskHumanReviewStatus.APPROVED,
        HUMAN_REVIEWER,
        false,
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode())
        .isEqualTo("canonical_target_metadata_requires_canonical_write_gate");
    assertThat(decision.humanReviewRequired()).isTrue();
    assertThat(decision.canonicalGateRequired()).isTrue();
    assertThat(decision.futureConsentDisclosureUnlockGateRequired()).isFalse();
  }

  @Test
  void canonicalTargetWithoutApprovedHumanReviewIsDenied() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
        AITaskHumanReviewStatus.PENDING,
        HUMAN_REVIEWER,
        false,
        false));

    assertDenied(decision, "canonical_target_requires_approved_human_review");
    assertThat(decision.humanReviewRequired()).isTrue();
    assertThat(decision.canonicalGateRequired()).isTrue();
  }

  @Test
  void canonicalTargetWithAiOrSystemSelfApprovalIsDenied() {
    AITaskGovernanceDecision aiDecision = policy.decide(request(
        AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
        AITaskHumanReviewStatus.APPROVED,
        AI_REVIEWER,
        false,
        false));
    AITaskGovernanceDecision systemDecision = policy.decide(request(
        AITaskWriteBackTarget.JOB_PROFILE,
        AITaskHumanReviewStatus.APPROVED,
        SYSTEM_REVIEWER,
        false,
        false));

    assertDenied(aiDecision, "ai_or_system_cannot_self_approve_ai_output");
    assertDenied(systemDecision, "ai_or_system_cannot_self_approve_ai_output");
  }

  @Test
  void clientSafeProjectionTargetRequiresClientSafeBoundary() {
    AITaskGovernanceDecision denied = policy.decide(request(
        AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        null,
        false,
        false));
    AITaskGovernanceDecision allowed = policy.decide(request(
        AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION,
        AITaskHumanReviewStatus.NOT_REQUIRED,
        null,
        true,
        false));

    assertDenied(denied, "client_visible_output_requires_client_safe_boundary");
    assertThat(allowed.allowed()).isTrue();
    assertThat(allowed.reasonCode()).isEqualTo("client_safe_projection_boundary_metadata_only");
    assertThat(allowed.canonicalGateRequired()).isFalse();
  }

  @Test
  void consentDisclosureUnlockTargetRequiresFutureGateNotImplementedHere() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.CONSENT_DISCLOSURE,
        AITaskHumanReviewStatus.APPROVED,
        HUMAN_REVIEWER,
        false,
        false));

    assertDenied(decision, "future_consent_disclosure_gate_required");
    assertThat(decision.futureConsentDisclosureUnlockGateRequired()).isTrue();
  }

  @Test
  void workflowActionTargetRequiresFutureGateNotImplementedHere() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.WORKFLOW_ACTION,
        AITaskHumanReviewStatus.APPROVED,
        HUMAN_REVIEWER,
        false,
        false));

    assertDenied(decision, "future_workflow_action_gate_required");
    assertThat(decision.humanReviewRequired()).isTrue();
    assertThat(decision.canonicalGateRequired()).isFalse();
    assertThat(decision.futureConsentDisclosureUnlockGateRequired()).isFalse();
  }

  @Test
  void commercialOrPlacementTargetIsBlockedInThisKernel() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.COMMERCIAL_OR_PLACEMENT,
        AITaskHumanReviewStatus.APPROVED,
        HUMAN_REVIEWER,
        false,
        false));

    assertDenied(decision, "future_commercial_placement_gate_required");
    assertThat(decision.humanReviewRequired()).isTrue();
  }

  @Test
  void unknownTargetDeniesByDefault() {
    AITaskGovernanceDecision decision = policy.decide(new AITaskGovernanceRequest(
        "raw_candidate_profile_write",
        AITaskHumanReviewStatus.APPROVED.wireValue(),
        HUMAN_REVIEWER,
        false,
        false));

    assertDenied(decision, "unknown_write_back_target_denied");
  }

  @Test
  void missingTargetMetadataDeniesByDefault() {
    AITaskGovernanceDecision decision = policy.decide(new AITaskGovernanceRequest(
        null,
        AITaskHumanReviewStatus.NOT_REQUIRED.wireValue(),
        null,
        false,
        false));

    assertDenied(decision, "write_back_target_required");
  }

  @Test
  void unknownReviewStatusDeniesByDefault() {
    AITaskGovernanceDecision decision = policy.decide(new AITaskGovernanceRequest(
        AITaskWriteBackTarget.NO_WRITE_BACK.wireValue(),
        "self_approved_by_model",
        AI_REVIEWER,
        false,
        false));

    assertDenied(decision, "unknown_human_review_status_denied");
  }

  @Test
  void bulkApproveCannotApproveCanonicalWriteBackMetadata() {
    AITaskGovernanceDecision decision = policy.decide(request(
        AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
        AITaskHumanReviewStatus.APPROVED,
        HUMAN_REVIEWER,
        false,
        true));

    assertDenied(decision, "bulk_approval_cannot_approve_canonical_write_back");
  }

  @Test
  void policyDecisionUsesSafeReasonWithoutStackTraceDetails() {
    AITaskGovernanceDecision decision = policy.decide(new AITaskGovernanceRequest(
        "java.lang.IllegalStateException: raw stack trace",
        "unknown\n\tat provider.Secret",
        AI_REVIEWER,
        false,
        false));

    assertDenied(decision, "unknown_human_review_status_denied");
    assertThat(decision.safeExplanation())
        .doesNotContain("java.lang")
        .doesNotContain("provider.Secret")
        .doesNotContain("\n")
        .doesNotContain("\tat ");
  }

  @Test
  void policyIsDeterministicAndFailsClosedForUnknownOrMissingMetadata() {
    AITaskGovernanceRequest unknownTarget = new AITaskGovernanceRequest(
        "execute_canonical_write_now",
        AITaskHumanReviewStatus.APPROVED.wireValue(),
        HUMAN_REVIEWER,
        true,
        false);
    AITaskGovernanceRequest unknownStatus = new AITaskGovernanceRequest(
        AITaskWriteBackTarget.NO_WRITE_BACK.wireValue(),
        "approved_by_model_router",
        HUMAN_REVIEWER,
        false,
        false);
    AITaskGovernanceRequest missingRequest = null;

    AITaskGovernanceDecision targetDecision = policy.decide(unknownTarget);
    AITaskGovernanceDecision targetDecisionAgain = policy.decide(unknownTarget);
    AITaskGovernanceDecision statusDecision = policy.decide(unknownStatus);
    AITaskGovernanceDecision missingDecision = policy.decide(missingRequest);

    assertThat(targetDecision).isEqualTo(targetDecisionAgain);
    assertDenied(targetDecision, "unknown_write_back_target_denied");
    assertDenied(statusDecision, "unknown_human_review_status_denied");
    assertDenied(missingDecision, "ai_task_governance_request_required");
  }

  private static AITaskGovernanceRequest request(
      AITaskWriteBackTarget target,
      AITaskHumanReviewStatus reviewStatus,
      ActorRef reviewActor,
      boolean clientSafeBoundaryApplied,
      boolean bulkApproval) {
    return new AITaskGovernanceRequest(
        target.wireValue(),
        reviewStatus.wireValue(),
        reviewActor,
        clientSafeBoundaryApplied,
        bulkApproval);
  }

  private static void assertDenied(AITaskGovernanceDecision decision, String reasonCode) {
    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo(reasonCode);
    assertThat(decision.safeExplanation()).isNotBlank();
  }
}
