package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeReviewBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IntakeReviewDecisionService {

  private final IntakeReviewBridgeService intakeReviewBridgeService;
  private final IntakeCanonicalWriteBridgeService intakeCanonicalWriteBridgeService;
  private final IntakeReviewQueryService intakeReviewQueryService;
  private final CandidateProfileService candidateProfileService;

  public IntakeReviewDecisionService(
      IntakeReviewBridgeService intakeReviewBridgeService,
      IntakeCanonicalWriteBridgeService intakeCanonicalWriteBridgeService,
      IntakeReviewQueryService intakeReviewQueryService,
      CandidateProfileService candidateProfileService) {
    this.intakeReviewBridgeService = Objects.requireNonNull(
        intakeReviewBridgeService, "intakeReviewBridgeService must not be null");
    this.intakeCanonicalWriteBridgeService = Objects.requireNonNull(
        intakeCanonicalWriteBridgeService, "intakeCanonicalWriteBridgeService must not be null");
    this.intakeReviewQueryService = Objects.requireNonNull(
        intakeReviewQueryService, "intakeReviewQueryService must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService, "candidateProfileService must not be null");
  }

  public IntakeReviewBridgeResult decide(
      UUID organizationId,
      ClaimId claimId,
      UUID reviewerActorId,
      ActorRole reviewerActorRole,
      ReviewDecision decision,
      RiskTier riskTier,
      boolean bulkFlag,
      String reason) {
    return intakeReviewBridgeService.bridge(new IntakeReviewBridgeRequest(
        organizationId,
        claimId,
        reviewerActorRole,
        reviewerActorId,
        decision,
        riskTier,
        bulkFlag,
        reason,
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY));
  }

  public PublishResult publish(
      UUID organizationId,
      InformationPacketId informationPacketId,
      UUID actorId,
      PublishRequest request) {
    IntakeReviewQueryService.IntakeReviewView view =
        intakeReviewQueryService.reviewView(organizationId, informationPacketId);
    List<IntakeCanonicalWriteBridgeResult> canonicalWrites = new ArrayList<>();
    List<String> directWrites = new ArrayList<>();
    IntendedEntityType intendedEntityType =
        view.run().outputEnvelope().orElseThrow().intendedEntityType();
    if (intendedEntityType == IntendedEntityType.COMPANY) {
      throw new IllegalArgumentException("company_publish_requires_future_governed_canonical_write_path");
    }
    if (intendedEntityType == IntendedEntityType.JOB) {
      throw new IllegalArgumentException("job_publish_requires_future_governed_canonical_write_path");
    }
    CandidateProfileId candidateProfileId = null;
    if (intendedEntityType == IntendedEntityType.CANDIDATE) {
      candidateProfileId = ensureCandidateProfile(organizationId, request.candidateId());
    }

    for (IntakeReviewQueryService.ReviewedCleanFact fact : view.facts()) {
      if (!"candidate_profile".equals(fact.candidate().targetEntityType())) {
        continue;
      }
      if (fact.claimId() == null || fact.latestReview() == null) {
        continue;
      }
      ReviewEventForCanonicalWrite latestReview = fact.latestReview();
      if (latestReview.decision() != ReviewDecision.APPROVED) {
        continue;
      }
      if (fact.candidate().conflictsWithCanonical()) {
        continue;
      }
      if (!candidatePublishEligible(fact, request.candidateId())) {
        continue;
      }
      canonicalWrites.add(intakeCanonicalWriteBridgeService.bridge(
          IntakeCanonicalWriteBridgeRequest.builder()
              .organizationId(organizationId)
              .claimLedgerItemId(fact.claimId())
              .reviewEventId(latestReview.reviewEventId())
              .requestedByActorType(ActorRole.CONSULTANT)
              .requestedByActorId(actorId)
              .candidateProfileId(candidateProfileId)
              .targetEntityType("candidate_profile")
              .targetEntityId(candidateProfileId.value())
              .targetFieldPath(fact.candidate().targetFieldPath())
              .requestedCanonicalValue(fact.candidate().proposedValue())
              .targetVerificationStatus(fact.candidate().suggestedVerificationStatus())
              .riskTier(fact.candidate().suggestedRiskTier())
              .clientVisible(false)
              .conflictsWithCanonical(fact.candidate().conflictsWithCanonical())
              .transactionLegalApproval(true)
              .reason(request.reason())
              .occurredAt(Instant.now())
              .bridgePolicy(IntakeCanonicalWriteBridgePolicy.GOVERNED_INTAKE_CLAIM_AND_REVIEW_ONLY)
              .build()));
    }
    return new PublishResult(canonicalWrites, directWrites);
  }

  private static boolean candidatePublishEligible(
      IntakeReviewQueryService.ReviewedCleanFact fact,
      UUID requestedCandidateId) {
    String resolvedEntityId = fact.candidate().resolvedEntityId();
    if (resolvedEntityId != null && !resolvedEntityId.equals(requestedCandidateId.toString())) {
      return false;
    }
    return switch (fact.candidate().entityResolutionStatus()) {
      case "EXISTING_MATCH", "NEW_ENTITY_REVIEW_REQUIRED" -> true;
      default -> false;
    };
  }

  private CandidateProfileId ensureCandidateProfile(UUID organizationId, UUID existingCandidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    if (existingCandidateId == null) {
      throw new IllegalArgumentException("candidate_publish_requires_existing_candidate_id");
    }
    CandidateId candidateId = new CandidateId(existingCandidateId);
    return candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(organizationId, candidateId)
        .map(profile -> profile.candidateProfileId())
        .orElseThrow(() -> new IllegalArgumentException("candidate_publish_requires_existing_profile"));
  }

  public record PublishRequest(
      UUID candidateId,
      UUID companyId,
      UUID jobId,
      UUID jobCompanyId,
      String reason) {}

  public record PublishResult(
      List<IntakeCanonicalWriteBridgeResult> canonicalWrites,
      List<String> directWrites) {}
}
