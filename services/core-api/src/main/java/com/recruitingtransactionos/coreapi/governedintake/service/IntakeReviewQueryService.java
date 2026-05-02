package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.port.LatestReviewEventLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class IntakeReviewQueryService {

  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort;
  private final LatestReviewEventLookupPort latestReviewEventLookupPort;

  public IntakeReviewQueryService(
      IntakeExtractionRunPort intakeExtractionRunPort,
      ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort,
      LatestReviewEventLookupPort latestReviewEventLookupPort) {
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort, "intakeExtractionRunPort must not be null");
    this.claimLedgerSourceReferenceLookupPort = Objects.requireNonNull(
        claimLedgerSourceReferenceLookupPort, "claimLedgerSourceReferenceLookupPort must not be null");
    this.latestReviewEventLookupPort = Objects.requireNonNull(
        latestReviewEventLookupPort, "latestReviewEventLookupPort must not be null");
  }

  public IntakeReviewView reviewView(UUID organizationId, InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    IntakeExtractionRun run = latestRun(organizationId, informationPacketId)
        .orElseThrow(() -> new IllegalArgumentException("governed_ai_intake_run_not_found"));
    IntakeExtractionOutputEnvelope envelope = run.outputEnvelope()
        .orElseThrow(() -> new IllegalArgumentException("governed_ai_intake_output_missing"));
    List<ReviewedCleanFact> facts = envelope.cleanFactCandidates().stream()
        .map(candidate -> toReviewedCleanFact(envelope, candidate))
        .toList();
    return new IntakeReviewView(run, facts);
  }

  private ReviewedCleanFact toReviewedCleanFact(
      IntakeExtractionOutputEnvelope envelope,
      IntakeCleanFactCandidate candidate) {
    ClaimId claimId = claimLedgerSourceReferenceLookupPort
        .findBySourceSpanReference(
            envelope.organizationId(),
            sourceSpanReference(envelope, candidate))
        .map(existing -> existing.claimId())
        .orElse(null);
    ReviewEventForCanonicalWrite latestReview = claimId == null
        ? null
        : latestReviewEventLookupPort.findLatestByClaimIdAndOrganizationId(
            envelope.organizationId(), claimId).orElse(null);
    return new ReviewedCleanFact(candidate, claimId, latestReview);
  }

  private Optional<IntakeExtractionRun> latestRun(UUID organizationId, InformationPacketId informationPacketId) {
    return intakeExtractionRunPort.listByInformationPacket(organizationId, informationPacketId).stream()
        .filter(run -> run.mode() == IntakeExtractionMode.GOVERNED_AI_V1)
        .filter(run -> run.outputEnvelope().isPresent())
        .max(Comparator.comparing(IntakeExtractionRun::createdAt));
  }

  private static com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef sourceSpanReference(
      IntakeExtractionOutputEnvelope envelope,
      IntakeCleanFactCandidate candidate) {
    return new com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef(
        "intake.extraction_run:" + envelope.extractionRunId().value()
            + "|intake.information_packet:" + envelope.informationPacketId().value()
            + "|packet_type:" + envelope.packetType().wireValue()
            + "|intended_entity_type:" + envelope.intendedEntityType().wireValue()
            + "|intake.source_item:" + candidate.sourceItemId().value()
            + "|field:" + candidate.claimFieldName()
            + "|discriminator:" + sourceSpanDiscriminator(candidate));
  }

  private static String sourceSpanDiscriminator(IntakeCleanFactCandidate candidate) {
    if (candidate.sourceSpanDiscriminator() != null) {
      return candidate.sourceSpanDiscriminator();
    }
    return "target_field:" + candidate.targetFieldPath()
        + "|chunk:" + candidate.parsedDocumentChunkId()
        + "|offsets:" + candidate.startOffset() + "-" + candidate.endOffset();
  }

  public record IntakeReviewView(IntakeExtractionRun run, List<ReviewedCleanFact> facts) {}

  public record ReviewedCleanFact(
      IntakeCleanFactCandidate candidate,
      ClaimId claimId,
      ReviewEventForCanonicalWrite latestReview) {}
}
