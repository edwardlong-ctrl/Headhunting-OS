package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Optional;

public final class AITaskGovernancePolicy {

  public AITaskGovernanceDecision decide(AITaskGovernanceRequest request) {
    if (request == null) {
      return deny(
          "ai_task_governance_request_required",
          "AI task governance metadata is required.",
          false,
          false,
          false,
          false,
          false);
    }

    Optional<AITaskHumanReviewStatus> reviewStatus =
        AITaskHumanReviewStatus.fromWireValue(request.humanReviewStatus());
    if (reviewStatus.isEmpty()) {
      return deny(
          "unknown_human_review_status_denied",
          "Unknown human review status is denied by default.",
          false,
          false,
          false,
          false,
          false);
    }

    if (request.writeBackTarget() == null || request.writeBackTarget().isBlank()) {
      return deny(
          "write_back_target_required",
          "AI task write-back target metadata is required.",
          false,
          false,
          false,
          false,
          false);
    }

    Optional<AITaskWriteBackTarget> target =
        AITaskWriteBackTarget.fromWireValue(request.writeBackTarget());
    if (target.isEmpty()) {
      return deny(
          "unknown_write_back_target_denied",
          "Unknown AI write-back target is denied by default.",
          false,
          false,
          false,
          false,
          false);
    }

    if (reviewStatus.get() == AITaskHumanReviewStatus.APPROVED
        && isAiOrSystem(request.reviewActor())) {
      return deny(
          "ai_or_system_cannot_self_approve_ai_output",
          "AI and system actors cannot approve AI output by role alone.",
          true,
          isCanonicalTarget(target.get()),
          isConsentDisclosureUnlockTarget(target.get()),
          isWorkflowActionTarget(target.get()),
          isCommercialOrPlacementTarget(target.get()));
    }

    return switch (target.get()) {
      case NONE, NO_WRITE_BACK -> allow(
          "no_write_back_metadata_only",
          "AI task output is recorded as metadata only.",
          false,
          false,
          false,
          false,
          false);
      case CLAIM_LEDGER_PROPOSAL -> allow(
          "claim_ledger_proposal_metadata_only",
          "AI task output remains a claim-ledger proposal and does not write canonical data.",
          false,
          false,
          false,
          false,
          false);
      case REVIEW_QUEUE, HUMAN_REVIEW_REQUIRED -> decideHumanReviewQueue(reviewStatus.get());
      case CANONICAL_CANDIDATE_PROFILE, JOB_PROFILE, COMPANY_PROFILE ->
          decideCanonicalTarget(request, reviewStatus.get());
      case CLIENT_SAFE_PROJECTION -> decideClientSafeProjection(request);
      case CONSENT_DISCLOSURE -> deny(
          "consent_disclosure_gate_required",
          "Consent and disclosure behavior stays blocked until a dedicated gate exists.",
          true,
          false,
          true,
          false,
          false);
      case WORKFLOW_ACTION -> deny(
          "workflow_action_gate_required",
          "Workflow action execution stays blocked until a workflow gate exists.",
          true,
          false,
          false,
          true,
          false);
      case COMMERCIAL_OR_PLACEMENT -> deny(
          "commercial_placement_gate_required",
          "Commercial and placement behavior stays blocked until a transaction gate exists.",
          true,
          false,
          false,
          false,
          true);
    };
  }

  private static AITaskGovernanceDecision decideHumanReviewQueue(
      AITaskHumanReviewStatus reviewStatus) {
    if (reviewStatus == AITaskHumanReviewStatus.NOT_REQUIRED) {
      return deny(
          "human_review_target_requires_review_status",
          "Human review targets require required or pending review metadata.",
          true,
          false,
          false,
          false,
          false);
    }
    return allow(
        "human_review_queue_metadata_only",
        "AI task output is routed as human-review metadata only.",
        true,
        false,
        false,
        false,
        false);
  }

  private static AITaskGovernanceDecision decideCanonicalTarget(
      AITaskGovernanceRequest request,
      AITaskHumanReviewStatus reviewStatus) {
    if (reviewStatus != AITaskHumanReviewStatus.APPROVED) {
      return deny(
          "canonical_target_requires_approved_human_review",
          "Canonical targets require approved human review before any future gate.",
          true,
          true,
          false,
          false,
          false);
    }
    if (request.bulkApproval()) {
      return deny(
          "bulk_approval_cannot_approve_canonical_write_back",
          "Bulk approval cannot approve canonical AI write-back metadata.",
          true,
          true,
          false,
          false,
          false);
    }
    if (request.reviewActor() == null) {
      return deny(
          "human_review_approval_actor_required",
          "Approved canonical targets require an explicit human reviewer.",
          true,
          true,
          false,
          false,
          false);
    }
    if (request.requestedBy() != null && request.requestedBy().equals(request.reviewActor())) {
      return deny(
          "requester_cannot_self_approve_canonical_write_back",
          "Canonical targets require an independent human reviewer.",
          true,
          true,
          false,
          false,
          false);
    }
    return allow(
        "canonical_target_metadata_requires_canonical_write_gate",
        "Canonical target metadata is accepted only with a required canonical write gate.",
        true,
        true,
        false,
        false,
        false);
  }

  private static AITaskGovernanceDecision decideClientSafeProjection(
      AITaskGovernanceRequest request) {
    if (!request.clientSafeBoundaryApplied()) {
      return deny(
          "client_visible_output_requires_client_safe_boundary",
          "Client-visible output requires the client-safe projection boundary.",
          true,
          false,
          false,
          false,
          false);
    }
    return allow(
        "client_safe_projection_boundary_metadata_only",
        "Client-safe projection target metadata is accepted only after the safe boundary.",
        false,
        false,
        false,
        false,
        false);
  }

  private static boolean isAiOrSystem(ActorRef actor) {
    return actor != null && (actor.role() == ActorRole.AI || actor.role() == ActorRole.SYSTEM);
  }

  private static boolean isCanonicalTarget(AITaskWriteBackTarget target) {
    return target == AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE
        || target == AITaskWriteBackTarget.JOB_PROFILE
        || target == AITaskWriteBackTarget.COMPANY_PROFILE;
  }

  private static boolean isConsentDisclosureUnlockTarget(AITaskWriteBackTarget target) {
    return target == AITaskWriteBackTarget.CONSENT_DISCLOSURE;
  }

  private static boolean isWorkflowActionTarget(AITaskWriteBackTarget target) {
    return target == AITaskWriteBackTarget.WORKFLOW_ACTION;
  }

  private static boolean isCommercialOrPlacementTarget(AITaskWriteBackTarget target) {
    return target == AITaskWriteBackTarget.COMMERCIAL_OR_PLACEMENT;
  }

  private static AITaskGovernanceDecision allow(
      String reasonCode,
      String safeExplanation,
      boolean humanReviewRequired,
      boolean canonicalGateRequired,
      boolean consentDisclosureUnlockGateRequired,
      boolean workflowActionGateRequired,
      boolean commercialPlacementGateRequired) {
    return AITaskGovernanceDecision.allow(
        reasonCode,
        safeExplanation,
        humanReviewRequired,
        canonicalGateRequired,
        consentDisclosureUnlockGateRequired,
        workflowActionGateRequired,
        commercialPlacementGateRequired);
  }

  private static AITaskGovernanceDecision deny(
      String reasonCode,
      String safeExplanation,
      boolean humanReviewRequired,
      boolean canonicalGateRequired,
      boolean consentDisclosureUnlockGateRequired,
      boolean workflowActionGateRequired,
      boolean commercialPlacementGateRequired) {
    return AITaskGovernanceDecision.deny(
        reasonCode,
        safeExplanation,
        humanReviewRequired,
        canonicalGateRequired,
        consentDisclosureUnlockGateRequired,
        workflowActionGateRequired,
        commercialPlacementGateRequired);
  }
}
