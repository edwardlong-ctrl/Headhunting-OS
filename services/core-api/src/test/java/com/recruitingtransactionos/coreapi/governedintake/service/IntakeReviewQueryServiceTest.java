package com.recruitingtransactionos.coreapi.governedintake.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.port.LatestReviewEventLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntakeReviewQueryServiceTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000230101");
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000230102"));
  private static final IntakeExtractionRunId RUN_ID =
      new IntakeExtractionRunId(UUID.fromString("00000000-0000-0000-0000-000000230103"));
  private static final SourceItemId SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000230104"));

  @Test
  void reviewViewUsesStableSourceSpanPerCandidateWhenFieldNamesRepeat() {
    IntakeCleanFactCandidate first = candidate(
        UUID.fromString("00000000-0000-0000-0000-000000230105"),
        5,
        18,
        "Platform Engineer",
        "target_field:experience.current_title|chunk:00000000-0000-0000-0000-000000230105|offsets:5-18|claim_ordinal:1");
    IntakeCleanFactCandidate second = candidate(
        UUID.fromString("00000000-0000-0000-0000-000000230106"),
        5,
        18,
        "Staff Engineer",
        "target_field:experience.current_title|chunk:00000000-0000-0000-0000-000000230105|offsets:5-18|claim_ordinal:2");
    IntakeExtractionRun run = run(List.of(first, second));

    ClaimId firstClaimId =
        new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000230107"));
    ClaimId secondClaimId =
        new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000230108"));

    SourceSpanRef firstSpan = expectedSourceSpan(run.outputEnvelope().orElseThrow(), first);
    SourceSpanRef secondSpan = expectedSourceSpan(run.outputEnvelope().orElseThrow(), second);

    IntakeReviewQueryService service = new IntakeReviewQueryService(
        new SingleRunPort(run),
        new MapClaimLedgerLookupPort(Map.of(
            firstSpan.value(), firstClaimId,
            secondSpan.value(), secondClaimId)),
        new MapLatestReviewLookupPort(Map.of(
            firstClaimId, approvedReview(firstClaimId, "approved-first"),
            secondClaimId, approvedReview(secondClaimId, "approved-second"))));

    IntakeReviewQueryService.IntakeReviewView view = service.reviewView(ORG_ID, PACKET_ID);

    assertThat(view.facts()).hasSize(2);
    assertThat(view.facts().get(0).claimId()).isEqualTo(firstClaimId);
    assertThat(view.facts().get(0).latestReview().reason()).isEqualTo("approved-first");
    assertThat(view.facts().get(1).claimId()).isEqualTo(secondClaimId);
    assertThat(view.facts().get(1).latestReview().reason()).isEqualTo("approved-second");
  }

  private static IntakeExtractionRun run(List<IntakeCleanFactCandidate> candidates) {
    Instant now = Instant.parse("2026-05-02T02:00:00Z");
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        RUN_ID,
        ORG_ID,
        PACKET_ID,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        "intake-output.v3",
        List.of(SOURCE_ITEM_ID),
        List.of(),
        List.of(),
        candidates.stream().map(IntakeReviewQueryServiceTest::extractedField).toList(),
        candidates,
        List.of(),
        List.of(),
        now);
    return new IntakeExtractionRun(
        RUN_ID,
        ORG_ID,
        PACKET_ID,
        IntakeExtractionMode.GOVERNED_AI_V1,
        IntakeExtractionStatus.SUCCEEDED,
        "input.v1",
        "output.v3",
        "governed-ai-v1",
        "snapshot-hash",
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope));
  }

  private static IntakeCleanFactCandidate candidate(
      UUID chunkId,
      int startOffset,
      int endOffset,
      String value,
      String sourceSpanDiscriminator) {
    return new IntakeCleanFactCandidate(
        "intake.bridge_eligible.candidate_profile.experience.current_title",
        "candidate_profile",
        "experience.current_title",
        value,
        SOURCE_ITEM_ID,
        UUID.fromString("00000000-0000-0000-0000-000000230109"),
        chunkId,
        1,
        startOffset,
        endOffset,
        0.8d,
        VerificationStatus.AI_EXTRACTED,
        RiskTier.T1_LOW_RISK,
        "EXISTING_MATCH",
        UUID.fromString("00000000-0000-0000-0000-000000230110").toString(),
        sourceSpanDiscriminator,
        false,
        "evidence-backed title extraction",
        value);
  }

  private static IntakeExtractedField extractedField(IntakeCleanFactCandidate candidate) {
    return new IntakeExtractedField(
        candidate.claimFieldName(),
        candidate.proposedValue(),
        candidate.sourceItemId(),
        candidate.confidence(),
        IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
        "test fixture",
        candidate.sourceSpanDiscriminator());
  }

  private static SourceSpanRef expectedSourceSpan(
      IntakeExtractionOutputEnvelope envelope,
      IntakeCleanFactCandidate candidate) {
    return new SourceSpanRef(
        "intake.extraction_run:" + envelope.extractionRunId().value()
            + "|intake.information_packet:" + envelope.informationPacketId().value()
            + "|packet_type:" + envelope.packetType().wireValue()
            + "|intended_entity_type:" + envelope.intendedEntityType().wireValue()
            + "|intake.source_item:" + candidate.sourceItemId().value()
            + "|field:" + candidate.claimFieldName()
            + "|discriminator:" + candidate.sourceSpanDiscriminator());
  }

  private static ReviewEventForCanonicalWrite approvedReview(ClaimId claimId, String reason) {
    return ReviewEventForCanonicalWrite.builder()
        .reviewEventId(new ReviewEventId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .targetEntity(new EntityRef("candidate_profile", UUID.randomUUID()))
        .targetFieldPath("experience.current_title")
        .claimLedgerItemId(claimId)
        .sourceSpanReference("intake.review_bridge|" + claimId.value())
        .decision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .bulkApproval(false)
        .reviewerId(UUID.fromString("00000000-0000-0000-0000-000000230111"))
        .reason(reason)
        .build();
  }

  private record SingleRunPort(IntakeExtractionRun run) implements IntakeExtractionRunPort {
    @Override
    public IntakeExtractionRun save(IntakeExtractionRun run) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntakeExtractionRun> findById(UUID organizationId, IntakeExtractionRunId extractionRunId) {
      return Optional.empty();
    }

    @Override
    public List<IntakeExtractionRun> listByInformationPacket(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      return List.of(run);
    }
  }

  private record MapClaimLedgerLookupPort(Map<String, ClaimId> claimsBySourceSpan)
      implements ClaimLedgerSourceReferenceLookupPort {
    @Override
    public Optional<ClaimLedgerSourceReference> findBySourceSpanReference(
        UUID organizationId,
        SourceSpanRef sourceSpanReference) {
      ClaimId claimId = claimsBySourceSpan.get(sourceSpanReference.value());
      if (claimId == null) {
        return Optional.empty();
      }
      return Optional.of(new ClaimLedgerSourceReference(
          claimId,
          organizationId,
          new EntityRef("information_packet", PACKET_ID.value()),
          "intake.bridge_eligible.candidate_profile.experience.current_title",
          sourceSpanReference));
    }
  }

  private record MapLatestReviewLookupPort(Map<ClaimId, ReviewEventForCanonicalWrite> reviews)
      implements LatestReviewEventLookupPort {
    @Override
    public Optional<ReviewEventForCanonicalWrite> findLatestByClaimIdAndOrganizationId(
        UUID organizationId,
        ClaimId claimId) {
      return Optional.ofNullable(reviews.get(claimId));
    }
  }
}
