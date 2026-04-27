package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForReview;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

public final class IntakeReviewBridgeService {

  private static final String GOVERNED_INTAKE_ENTITY_TYPE = "information_packet";
  private static final String GOVERNED_INTAKE_FIELD_PREFIX = "intake.bridge_eligible.";

  private final ClaimLedgerItemReviewLookupPort claimLedgerItemReviewLookupPort;
  private final ReviewEventService reviewEventService;
  private final ReviewEventSourceReferenceLookupPort reviewEventSourceReferenceLookupPort;

  public IntakeReviewBridgeService(
      ClaimLedgerItemReviewLookupPort claimLedgerItemReviewLookupPort,
      ReviewEventService reviewEventService,
      ReviewEventSourceReferenceLookupPort reviewEventSourceReferenceLookupPort) {
    this.claimLedgerItemReviewLookupPort = Objects.requireNonNull(
        claimLedgerItemReviewLookupPort,
        "claimLedgerItemReviewLookupPort must not be null");
    this.reviewEventService = Objects.requireNonNull(
        reviewEventService,
        "reviewEventService must not be null");
    this.reviewEventSourceReferenceLookupPort = Objects.requireNonNull(
        reviewEventSourceReferenceLookupPort,
        "reviewEventSourceReferenceLookupPort must not be null");
  }

  public IntakeReviewBridgeResult bridge(IntakeReviewBridgeRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    if (request.reviewPolicy() != IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY) {
      return blocked(request, "unsupported_review_policy");
    }

    ClaimLedgerItemForReview claim = claimLedgerItemReviewLookupPort
        .findByIdAndOrganizationId(request.organizationId(), request.claimLedgerItemId())
        .orElse(null);
    if (claim == null) {
      return blocked(request, "claim_ledger_item_not_found_in_organization");
    }
    if (!isGovernedIntakeClaim(claim)) {
      return blocked(request, "claim_not_from_governed_intake_lineage");
    }
    if (request.riskTier().requiresHumanFinalActor()
        && !isHumanReviewer(request.reviewerActorType())) {
      return blocked(request, "high_risk_review_requires_human_reviewer");
    }

    ReviewEventAppendCommand command = reviewCommand(request, claim);
    return reviewEventSourceReferenceLookupPort
        .findBySourceSpanReference(request.organizationId(), command.sourceSpanReference())
        .map(existing -> existingResult(request, command, existing))
        .orElseGet(() -> appendReviewEvent(request, command));
  }

  private static boolean isGovernedIntakeClaim(ClaimLedgerItemForReview claim) {
    String sourceSpan = claim.sourceSpanReference().value();
    return GOVERNED_INTAKE_ENTITY_TYPE.equals(claim.targetEntity().entityType())
        && claim.targetFieldPath() != null
        && claim.targetFieldPath().startsWith(GOVERNED_INTAKE_FIELD_PREFIX)
        && sourceSpan.contains("intake.extraction_run:")
        && sourceSpan.contains("intake.information_packet:")
        && sourceSpan.contains("intake.source_item:");
  }

  private static boolean isHumanReviewer(ActorRole reviewerActorType) {
    return reviewerActorType != ActorRole.AI && reviewerActorType != ActorRole.SYSTEM;
  }

  private static ReviewEventAppendCommand reviewCommand(
      IntakeReviewBridgeRequest request,
      ClaimLedgerItemForReview claim) {
    return new ReviewEventAppendCommand(
        request.organizationId(),
        request.reviewerActorId(),
        claim.targetEntity(),
        claim.targetFieldPath(),
        request.riskTier(),
        request.reviewDecision(),
        request.bulkFlag(),
        request.reason(),
        Duration.ZERO,
        request.claimLedgerItemId(),
        reviewSourceSpanReference(request));
  }

  private static IntakeReviewBridgeResult existingResult(
      IntakeReviewBridgeRequest request,
      ReviewEventAppendCommand command,
      ReviewEventSourceReference existing) {
    if (!existing.targetEntity().equals(command.targetEntity())
        || !existing.targetFieldPath().equals(command.targetFieldPath())
        || !existing.claimId().equals(command.claimId())
        || existing.decision() != command.decision()
        || existing.riskTier() != command.riskTier()) {
      throw new IllegalStateException("review event source reference conflict");
    }
    return new IntakeReviewBridgeResult(
        request.organizationId(),
        request.claimLedgerItemId(),
        null,
        existing.reviewEventId(),
        IntakeReviewBridgeStatus.REVIEW_EVENT_ALREADY_EXISTS,
        "duplicate_review_source_reference_already_appended",
        null,
        "identical governed-intake review event already exists");
  }

  private IntakeReviewBridgeResult appendReviewEvent(
      IntakeReviewBridgeRequest request,
      ReviewEventAppendCommand command) {
    ReviewEventAppendResult result = reviewEventService.append(command);
    return new IntakeReviewBridgeResult(
        request.organizationId(),
        request.claimLedgerItemId(),
        result.reviewEventId(),
        null,
        IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED,
        null,
        null,
        "governed-intake claim review event appended");
  }

  private static IntakeReviewBridgeResult blocked(
      IntakeReviewBridgeRequest request,
      String blockedReason) {
    return new IntakeReviewBridgeResult(
        request.organizationId(),
        request.claimLedgerItemId(),
        null,
        null,
        IntakeReviewBridgeStatus.BLOCKED,
        null,
        blockedReason,
        "governed-intake review bridge blocked: " + blockedReason);
  }

  private static SourceSpanRef reviewSourceSpanReference(IntakeReviewBridgeRequest request) {
    return new SourceSpanRef(
        "intake.review_bridge"
            + "|claim_ledger_item:" + request.claimLedgerItemId().value()
            + "|reviewer:" + request.reviewerActorType().wireValue()
            + ":" + request.reviewerActorId()
            + "|risk_tier:" + request.riskTier().wireValue()
            + "|decision:" + request.reviewDecision().wireValue()
            + "|bulk:" + request.bulkFlag()
            + "|reason_sha256:" + sha256(request.reason()));
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
