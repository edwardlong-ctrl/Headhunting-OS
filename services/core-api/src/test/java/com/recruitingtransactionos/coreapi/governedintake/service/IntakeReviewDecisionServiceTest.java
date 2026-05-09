package com.recruitingtransactionos.coreapi.governedintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntakeReviewDecisionServiceTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000230201");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000230202");
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000230203"));
  private static final IntakeExtractionRunId RUN_ID =
      new IntakeExtractionRunId(UUID.fromString("00000000-0000-0000-0000-000000230204"));
  private static final SourceItemId SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000230205"));
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000230206");
  private static final CandidateProfileId CANDIDATE_PROFILE_ID =
      new CandidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000230207"));
  private static final ClaimId CLAIM_ID =
      new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000230208"));

  @Test
  void publishRejectsCompanyPacketsUntilGovernedCanonicalPathExists() {
    IntakeReviewDecisionService service = serviceFor(intendedEntityView(IntendedEntityType.COMPANY));

    assertThatThrownBy(() -> service.publish(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        new IntakeReviewDecisionService.PublishRequest(null, null, null, null, "publish")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("company_publish_requires_future_governed_canonical_write_path");
  }

  @Test
  void publishRejectsJobPacketsUntilGovernedCanonicalPathExists() {
    IntakeReviewDecisionService service = serviceFor(intendedEntityView(IntendedEntityType.JOB));

    assertThatThrownBy(() -> service.publish(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        new IntakeReviewDecisionService.PublishRequest(null, null, null, null, "publish")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("job_publish_requires_future_governed_canonical_write_path");
  }

  @Test
  void publishAllowsCandidatePacketsWithExistingTargetEvenWhenResolutionNeedsReview() {
    CandidateProfileService candidateProfileService = mock(CandidateProfileService.class);
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORG_ID,
        new CandidateId(CANDIDATE_ID))).thenReturn(Optional.of(CandidateProfile.builder()
            .candidateProfileId(CANDIDATE_PROFILE_ID)
            .organizationId(ORG_ID)
            .candidateId(new CandidateId(CANDIDATE_ID))
            .profileVersion(new CandidateProfileVersion(1))
            .fields(List.of())
            .createdAt(Instant.parse("2026-05-02T03:00:00Z"))
            .updatedAt(Instant.parse("2026-05-02T03:00:00Z"))
            .build()));
    IntakeCanonicalWriteBridgeService canonicalWriteBridgeService =
        mock(IntakeCanonicalWriteBridgeService.class);
    when(canonicalWriteBridgeService.bridge(any())).thenReturn(new IntakeCanonicalWriteBridgeResult(
        ORG_ID,
        CLAIM_ID,
        new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000230209")),
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000230210")),
        CanonicalWriteDecisionType.ALLOW,
        true,
        "persisted",
        IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED,
        null,
        "candidate profile field updated"));
    IntakeReviewDecisionService service = serviceFor(
        candidateView("NEW_ENTITY_REVIEW_REQUIRED", null),
        canonicalWriteBridgeService,
        candidateProfileService);

    IntakeReviewDecisionService.PublishResult result = service.publish(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        new IntakeReviewDecisionService.PublishRequest(CANDIDATE_ID, null, null, null, "publish"));

    assertThat(result.canonicalWrites()).hasSize(1);
    assertThat(result.directWrites()).isEmpty();
  }

  @Test
  void publishCreatesCandidateBeforeProfileWhenCandidatePacketHasNoResolvedTarget() {
    CandidateProfileService candidateProfileService = mock(CandidateProfileService.class);
    when(candidateProfileService.createCandidateProfile(any())).thenReturn(CandidateProfile.builder()
        .candidateProfileId(CANDIDATE_PROFILE_ID)
        .organizationId(ORG_ID)
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000230214")))
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of())
        .createdAt(Instant.parse("2026-05-02T03:00:00Z"))
        .updatedAt(Instant.parse("2026-05-02T03:00:00Z"))
        .build());
    CandidateService candidateService = mock(CandidateService.class);
    when(candidateService.createCandidate(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(candidateService.linkCurrentProfile(any(), any(), any())).thenAnswer(invocation -> Candidate.builder()
        .candidateId(invocation.getArgument(1))
        .organizationId(invocation.getArgument(0))
        .currentProfileId(invocation.getArgument(2))
        .status(com.recruitingtransactionos.coreapi.candidate.CandidateStatus.PROFILE_PARSED)
        .privacyStatus("internal_only")
        .createdAt(Instant.parse("2026-05-02T03:00:00Z"))
        .updatedAt(Instant.parse("2026-05-02T03:00:00Z"))
        .version(1)
        .build());
    IntakeCanonicalWriteBridgeService canonicalWriteBridgeService =
        mock(IntakeCanonicalWriteBridgeService.class);
    when(canonicalWriteBridgeService.bridge(any())).thenReturn(new IntakeCanonicalWriteBridgeResult(
        ORG_ID,
        CLAIM_ID,
        new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000230209")),
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000230210")),
        CanonicalWriteDecisionType.ALLOW,
        true,
        "persisted",
        IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED,
        null,
        "candidate profile field updated"));
    IntakeReviewDecisionService service = serviceFor(
        candidateView("NEW_ENTITY_REVIEW_REQUIRED", null),
        canonicalWriteBridgeService,
        candidateProfileService,
        candidateService);

    IntakeReviewDecisionService.PublishResult result = service.publish(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        new IntakeReviewDecisionService.PublishRequest(null, null, null, null, "publish"));

    ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
    verify(candidateService).createCandidate(candidateCaptor.capture());
    Candidate createdCandidate = candidateCaptor.getValue();
    assertThat(createdCandidate.organizationId()).isEqualTo(ORG_ID);
    assertThat(createdCandidate.privacyStatus()).isEqualTo("internal_only");
    verify(candidateProfileService).createCandidateProfile(any());
    verify(candidateService).linkCurrentProfile(
        ORG_ID,
        createdCandidate.candidateId(),
        CANDIDATE_PROFILE_ID);
    assertThat(result.canonicalWrites()).hasSize(1);
  }

  private static IntakeReviewDecisionService serviceFor(
      IntakeReviewQueryService.IntakeReviewView view) {
    return serviceFor(
        view,
        mock(IntakeCanonicalWriteBridgeService.class),
        mock(CandidateProfileService.class),
        mock(CandidateService.class));
  }

  private static IntakeReviewDecisionService serviceFor(
      IntakeReviewQueryService.IntakeReviewView view,
      IntakeCanonicalWriteBridgeService canonicalWriteBridgeService,
      CandidateProfileService candidateProfileService) {
    return serviceFor(
        view,
        canonicalWriteBridgeService,
        candidateProfileService,
        mock(CandidateService.class));
  }

  private static IntakeReviewDecisionService serviceFor(
      IntakeReviewQueryService.IntakeReviewView view,
      IntakeCanonicalWriteBridgeService canonicalWriteBridgeService,
      CandidateProfileService candidateProfileService,
      CandidateService candidateService) {
    IntakeReviewQueryService reviewQueryService = mock(IntakeReviewQueryService.class);
    when(reviewQueryService.reviewView(ORG_ID, PACKET_ID)).thenReturn(view);
    return new IntakeReviewDecisionService(
        mock(IntakeReviewBridgeService.class),
        canonicalWriteBridgeService,
        reviewQueryService,
        candidateProfileService,
        candidateService,
        null,
        null);
  }

  private static IntakeReviewQueryService.IntakeReviewView intendedEntityView(
      IntendedEntityType intendedEntityType) {
    Instant now = Instant.parse("2026-05-02T03:00:00Z");
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        RUN_ID,
        ORG_ID,
        PACKET_ID,
        packetType(intendedEntityType),
        intendedEntityType,
        "intake-output.v3",
        List.of(SOURCE_ITEM_ID),
        List.of(),
        List.of(),
        List.of(new IntakeExtractedField(
            "intake.bridge_eligible.fixture",
            "fixture",
            SOURCE_ITEM_ID,
            1.0d,
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            "test fixture",
            "fixture:publish")),
        List.of(),
        List.of(),
        List.of(),
        now);
    IntakeExtractionRun run = new IntakeExtractionRun(
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
    return new IntakeReviewQueryService.IntakeReviewView(run, List.of());
  }

  private static IntakeReviewQueryService.IntakeReviewView candidateView(
      String entityResolutionStatus,
      String resolvedEntityId) {
    Instant now = Instant.parse("2026-05-02T03:00:00Z");
    IntakeExtractionRun run = intendedEntityView(IntendedEntityType.CANDIDATE).run();
    IntakeCleanFactCandidate candidate = new IntakeCleanFactCandidate(
        "intake.bridge_eligible.candidate_profile.profile.headline",
        "candidate_profile",
        "profile.headline",
        "Senior AI engineer",
        SOURCE_ITEM_ID,
        UUID.fromString("00000000-0000-0000-0000-000000230211"),
        UUID.fromString("00000000-0000-0000-0000-000000230212"),
        1,
        5,
        32,
        0.91d,
        VerificationStatus.AI_EXTRACTED,
        RiskTier.T1_LOW_RISK,
        entityResolutionStatus,
        resolvedEntityId,
        "target_field:profile.headline|chunk:00000000-0000-0000-0000-000000230212|offsets:5-32|claim_ordinal:1",
        false,
        "Clear title signal from uploaded CV",
        "Senior AI engineer");
    ReviewEventForCanonicalWrite latestReview = ReviewEventForCanonicalWrite.builder()
        .reviewEventId(new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000230213")))
        .organizationId(ORG_ID)
        .targetEntity(new EntityRef("candidate_profile", CANDIDATE_PROFILE_ID.value()))
        .targetFieldPath("profile.headline")
        .claimLedgerItemId(CLAIM_ID)
        .sourceSpanReference("intake.review_bridge|" + CLAIM_ID.value())
        .decision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .bulkApproval(false)
        .reviewerId(ACTOR_ID)
        .reason("approved after evidence review")
        .build();
    return new IntakeReviewQueryService.IntakeReviewView(
        run,
        List.of(new IntakeReviewQueryService.ReviewedCleanFact(candidate, CLAIM_ID, latestReview)));
  }

  private static InformationPacketType packetType(IntendedEntityType intendedEntityType) {
    return switch (intendedEntityType) {
      case COMPANY -> InformationPacketType.COMPANY;
      case JOB -> InformationPacketType.JOB;
      default -> InformationPacketType.CANDIDATE;
    };
  }
}
