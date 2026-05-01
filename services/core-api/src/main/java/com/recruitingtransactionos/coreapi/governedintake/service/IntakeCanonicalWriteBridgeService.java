package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimInput;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.CandidateProfileCanonicalWriteTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteResult;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteReviewEvidence;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class IntakeCanonicalWriteBridgeService {

  private static final String GOVERNED_INTAKE_ENTITY_TYPE = "information_packet";
  private static final String GOVERNED_INTAKE_FIELD_PREFIX = "intake.bridge_eligible.";
  private static final String CANONICAL_PERSISTENCE_NOT_ATTEMPTED =
      "not_attempted_before_canonical_write_service";

  private final ClaimLedgerItemCanonicalWriteLookupPort claimLedgerItemLookupPort;
  private final ReviewEventCanonicalWriteLookupPort reviewEventLookupPort;
  private final CanonicalWriteService canonicalWriteService;

  public IntakeCanonicalWriteBridgeService(
      ClaimLedgerItemCanonicalWriteLookupPort claimLedgerItemLookupPort,
      ReviewEventCanonicalWriteLookupPort reviewEventLookupPort,
      CanonicalWriteService canonicalWriteService) {
    this.claimLedgerItemLookupPort = Objects.requireNonNull(
        claimLedgerItemLookupPort,
        "claimLedgerItemLookupPort must not be null");
    this.reviewEventLookupPort = Objects.requireNonNull(
        reviewEventLookupPort,
        "reviewEventLookupPort must not be null");
    this.canonicalWriteService = Objects.requireNonNull(
        canonicalWriteService,
        "canonicalWriteService must not be null");
  }

  public IntakeCanonicalWriteBridgeResult bridge(IntakeCanonicalWriteBridgeRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    if (request.bridgePolicy()
        != IntakeCanonicalWriteBridgePolicy.GOVERNED_INTAKE_CLAIM_AND_REVIEW_ONLY) {
      return failed(request, "unsupported_canonical_write_bridge_policy");
    }

    ClaimLedgerItemForCanonicalWrite claim = claimLedgerItemLookupPort
        .findByIdAndOrganizationId(request.organizationId(), request.claimLedgerItemId())
        .orElse(null);
    if (claim == null) {
      return failed(request, "claim_ledger_item_not_found_in_organization");
    }

    ReviewEventForCanonicalWrite review = reviewEventLookupPort
        .findByIdAndOrganizationId(request.organizationId(), request.reviewEventId())
        .orElse(null);
    if (review == null) {
      return failed(request, "review_event_not_found_in_organization");
    }

    String validationFailure = validateLineage(request, claim, review);
    if (validationFailure != null) {
      return failed(request, validationFailure);
    }

    try {
      CanonicalWriteResult result =
          canonicalWriteService.attempt(command(request, claim, review));
      return result(request, result);
    } catch (IllegalArgumentException exception) {
      if (exception.getMessage() != null
          && exception.getMessage().contains("requires a human final actor")) {
        return failed(request, "high_risk_write_requires_human_actor");
      }
      throw exception;
    }
  }

  private static String validateLineage(
      IntakeCanonicalWriteBridgeRequest request,
      ClaimLedgerItemForCanonicalWrite claim,
      ReviewEventForCanonicalWrite review) {
    if (!isGovernedIntakeClaim(claim)) {
      return "claim_not_from_governed_intake_lineage";
    }
    if (!review.claimLedgerItemId().equals(request.claimLedgerItemId())) {
      return "review_event_does_not_belong_to_claim";
    }
    if (!review.targetEntity().equals(claim.targetEntity())
        || !review.targetFieldPath().equals(claim.targetFieldPath())) {
      return "review_event_target_does_not_match_claim";
    }
    if (!isGovernedIntakeReviewBridgeEvent(review)) {
      return "review_event_not_from_governed_intake_bridge";
    }
    return null;
  }

  private static boolean isGovernedIntakeClaim(ClaimLedgerItemForCanonicalWrite claim) {
    String sourceSpan = claim.sourceSpanReference().value();
    return GOVERNED_INTAKE_ENTITY_TYPE.equals(claim.targetEntity().entityType())
        && claim.targetFieldPath().startsWith(GOVERNED_INTAKE_FIELD_PREFIX)
        && sourceSpan.contains("intake.extraction_run:")
        && sourceSpan.contains("intake.information_packet:")
        && sourceSpan.contains("intake.source_item:");
  }

  private static boolean isGovernedIntakeReviewBridgeEvent(
      ReviewEventForCanonicalWrite review) {
    return review.sourceSpanReference() != null
        && review.sourceSpanReference().contains("intake.review_bridge")
        && review.sourceSpanReference()
            .contains("claim_ledger_item:" + review.claimLedgerItemId().value());
  }

  private static CanonicalWriteCommand command(
      IntakeCanonicalWriteBridgeRequest request,
      ClaimLedgerItemForCanonicalWrite claim,
      ReviewEventForCanonicalWrite review) {
    return CanonicalWriteCommand.builder()
        .organizationId(request.organizationId())
        .targetEntity(new EntityRef(request.targetEntityType(), request.targetEntityId()))
        .targetFieldPath(request.targetFieldPath())
        .proposedValueRef(proposedValueRef(request))
        .sourceSpanRef(claim.sourceSpanReference().value())
        .candidateProfileWriteTarget(candidateProfileWriteTarget(request))
        .claimId(request.claimLedgerItemId())
        .claim(new ClaimInput(
            claim.claimType(),
            claim.assertionStrength(),
            claim.verificationStatus(),
            claim.clientShareability(),
            review.bulkApproval()))
        .canonicalWriteAllowed(claim.canonicalWriteAllowed())
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            request.reviewEventId(),
            review.decision(),
            review.bulkApproval(),
            request.transactionLegalApproval(),
            review.reason()))
        .targetVerificationStatus(request.targetVerificationStatus())
        .targetRiskTier(request.riskTier())
        .clientVisible(request.clientVisible())
        .conflictsWithCanonical(request.conflictsWithCanonical())
        .actor(new ActorRef(request.requestedByActorId(), request.requestedByActorType()))
        .reason(request.reason())
        .idempotencyKey(idempotencyKey(request))
        .correlationId(request.correlationId())
        .causationId(request.causationId())
        .occurredAt(request.occurredAt())
        .build();
  }

  private static CandidateProfileCanonicalWriteTarget candidateProfileWriteTarget(
      IntakeCanonicalWriteBridgeRequest request) {
    if (request.candidateProfileId() == null) {
      return null;
    }
    return new CandidateProfileCanonicalWriteTarget(
        request.candidateProfileId(),
        CandidateProfileFieldPath.of(request.targetFieldPath()),
        CandidateProfileFieldValue.ofString(request.requestedCanonicalValue()),
        candidateProfileFieldStatus(request.targetVerificationStatus()));
  }

  private static CandidateProfileFieldStatus candidateProfileFieldStatus(
      com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus status) {
    return switch (status) {
      case AI_EXTRACTED -> CandidateProfileFieldStatus.AI_EXTRACTED;
      case HUMAN_ACKNOWLEDGED -> CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED;
      case CONSULTANT_ATTESTED -> CandidateProfileFieldStatus.CONSULTANT_ATTESTED;
      case CANDIDATE_CONFIRMED -> CandidateProfileFieldStatus.CANDIDATE_CONFIRMED;
      case EXTERNAL_VERIFIED -> CandidateProfileFieldStatus.EXTERNAL_VERIFIED;
      case SYSTEM_INFERENCE -> CandidateProfileFieldStatus.SYSTEM_INFERENCE;
      case CONFLICTING -> CandidateProfileFieldStatus.CONFLICTING;
      case NEEDS_CONFIRMATION -> CandidateProfileFieldStatus.NEEDS_CONFIRMATION;
      case REJECTED, RETRACTED -> throw new IllegalArgumentException(
          "unsupported candidate profile field status: " + status.wireValue());
    };
  }

  private static String proposedValueRef(IntakeCanonicalWriteBridgeRequest request) {
    String valueHash = request.requestedCanonicalValue() == null
        ? "absent"
        : "sha256:" + sha256(request.requestedCanonicalValue());
    return "claim-ledger-item:" + request.claimLedgerItemId().value()
        + "|review-event:" + request.reviewEventId().value()
        + "|target-field:" + request.targetFieldPath()
        + "|requested-value:" + valueHash;
  }

  private static String idempotencyKey(IntakeCanonicalWriteBridgeRequest request) {
    String source = request.claimLedgerItemId().value()
        + "|" + request.reviewEventId().value()
        + "|" + request.targetEntityType()
        + "|" + request.targetEntityId()
        + "|" + request.targetFieldPath()
        + "|" + proposedValueRef(request);
    return "intake-canonical-write-bridge"
        + "|claim:" + request.claimLedgerItemId().value()
        + "|review:" + request.reviewEventId().value()
        + "|attempt:" + sha256(source).substring(0, 12);
  }

  private static IntakeCanonicalWriteBridgeResult result(
      IntakeCanonicalWriteBridgeRequest request,
      CanonicalWriteResult result) {
    CanonicalWriteDecisionType decision = result.decision().type();
    IntakeCanonicalWriteBridgeStatus status = switch (decision) {
      case ALLOW -> IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED;
      case BLOCK -> IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED;
      case REQUIRE_REVIEW -> IntakeCanonicalWriteBridgeStatus.REVIEW_REQUIRED;
    };
    String reason = decision == CanonicalWriteDecisionType.ALLOW
        ? null
        : String.join(",", result.decision().reasons());
    return new IntakeCanonicalWriteBridgeResult(
        request.organizationId(),
        request.claimLedgerItemId(),
        request.reviewEventId(),
        result.workflowEventId(),
        decision,
        result.canonicalPersistencePerformed(),
        result.canonicalPersistenceStatus(),
        status,
        reason,
        summary(status));
  }

  private static IntakeCanonicalWriteBridgeResult failed(
      IntakeCanonicalWriteBridgeRequest request,
      String blockedReason) {
    return new IntakeCanonicalWriteBridgeResult(
        request.organizationId(),
        request.claimLedgerItemId(),
        request.reviewEventId(),
        null,
        null,
        false,
        CANONICAL_PERSISTENCE_NOT_ATTEMPTED,
        IntakeCanonicalWriteBridgeStatus.FAILED,
        blockedReason,
        "governed-intake canonical write bridge failed: " + blockedReason);
  }

  private static String summary(IntakeCanonicalWriteBridgeStatus status) {
    return switch (status) {
      case GATE_ALLOWED_AUDITED ->
          "CanonicalWriteService gate allowed the boundary audit; canonical persistence remains deferred";
      case GATE_BLOCKED ->
          "CanonicalWriteGate blocked governed-intake claim promotion";
      case REVIEW_REQUIRED ->
          "CanonicalWriteGate requires additional review before boundary audit";
      case FAILED ->
          "governed-intake canonical write bridge failed before gate result";
    };
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
