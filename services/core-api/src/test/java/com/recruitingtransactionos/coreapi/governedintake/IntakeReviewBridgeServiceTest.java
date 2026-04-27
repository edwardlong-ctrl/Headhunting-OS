package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForReview;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IntakeReviewBridgeServiceTest {

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000190001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000190002");
  private static final UUID REVIEWER_ID = uuid("00000000-0000-0000-0000-000000190003");
  private static final ClaimId CLAIM_ID =
      new ClaimId(uuid("00000000-0000-0000-0000-000000190004"));
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(uuid("00000000-0000-0000-0000-000000190005"));
  private static final IntakeExtractionRunId RUN_ID =
      new IntakeExtractionRunId(uuid("00000000-0000-0000-0000-000000190006"));
  private static final SourceItemId SOURCE_ID =
      new SourceItemId(uuid("00000000-0000-0000-0000-000000190007"));

  @Test
  void reviewBridgeRequestRequiresOrganizationId() {
    assertThatThrownBy(() -> new IntakeReviewBridgeRequest(
        null,
        CLAIM_ID,
        ActorRole.CONSULTANT,
        REVIEWER_ID,
        ReviewDecision.APPROVED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        "reviewed governed-intake claim source lineage",
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void reviewBridgeRequestRequiresClaimLedgerItemId() {
    assertThatThrownBy(() -> new IntakeReviewBridgeRequest(
        ORG_A,
        null,
        ActorRole.CONSULTANT,
        REVIEWER_ID,
        ReviewDecision.APPROVED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        "reviewed governed-intake claim source lineage",
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("claimLedgerItemId must not be null");
  }

  @Test
  void reviewBridgeRequestRequiresReviewerActorType() {
    assertThatThrownBy(() -> new IntakeReviewBridgeRequest(
        ORG_A,
        CLAIM_ID,
        null,
        REVIEWER_ID,
        ReviewDecision.APPROVED,
        RiskTier.T2_MEDIUM_RISK,
        false,
        "reviewed governed-intake claim source lineage",
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("reviewerActorType must not be null");
  }

  @Test
  void reviewBridgeRequestRequiresReviewDecision() {
    assertThatThrownBy(() -> new IntakeReviewBridgeRequest(
        ORG_A,
        CLAIM_ID,
        ActorRole.CONSULTANT,
        REVIEWER_ID,
        null,
        RiskTier.T2_MEDIUM_RISK,
        false,
        "reviewed governed-intake claim source lineage",
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("reviewDecision must not be null");
  }

  @Test
  void reviewBridgeRequestRequiresRiskTier() {
    assertThatThrownBy(() -> new IntakeReviewBridgeRequest(
        ORG_A,
        CLAIM_ID,
        ActorRole.CONSULTANT,
        REVIEWER_ID,
        ReviewDecision.APPROVED,
        null,
        false,
        "reviewed governed-intake claim source lineage",
        null,
        null,
        IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("riskTier must not be null");
  }

  @Test
  void reviewBridgeRejectsMissingClaim() {
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult result =
        service(new ClaimStore(), reviewEvents).bridge(request());

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(result.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(reviewEvents.commands).isEmpty();
  }

  @Test
  void reviewBridgeRejectsWrongOrganizationClaim() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult result =
        service(claims, reviewEvents).bridge(new IntakeReviewBridgeRequest(
            ORG_B,
            CLAIM_ID,
            ActorRole.CONSULTANT,
            REVIEWER_ID,
            ReviewDecision.APPROVED,
            RiskTier.T2_MEDIUM_RISK,
            false,
            "reviewed governed-intake claim source lineage",
            null,
            null,
            IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY));

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(result.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(reviewEvents.commands).isEmpty();
  }

  @Test
  void reviewBridgeAppendsReviewEventThroughReviewEventService() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult result = service(claims, reviewEvents).bridge(request());

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(result.reviewEventId()).isNotNull();
    assertThat(result.existingReviewEventId()).isNull();
    assertThat(reviewEvents.commands).hasSize(1);

    ReviewEventAppendCommand command = reviewEvents.commands.getFirst();
    assertThat(command.organizationId()).isEqualTo(ORG_A);
    assertThat(command.reviewerId()).isEqualTo(REVIEWER_ID);
    assertThat(command.targetEntity()).isEqualTo(new EntityRef(
        "information_packet",
        PACKET_ID.value()));
    assertThat(command.targetFieldPath()).isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(command.riskTier()).isEqualTo(RiskTier.T2_MEDIUM_RISK);
    assertThat(command.decision()).isEqualTo(ReviewDecision.APPROVED);
    assertThat(command.bulkApproval()).isFalse();
    assertThat(command.reason()).isEqualTo("reviewed governed-intake claim source lineage");
    assertThat(command.claimId()).isEqualTo(CLAIM_ID);
    assertThat(command.sourceSpanReference().value())
        .contains("intake.review_bridge")
        .contains("claim_ledger_item:" + CLAIM_ID.value())
        .contains("reviewer:consultant:" + REVIEWER_ID)
        .contains("risk_tier:T2_MEDIUM_RISK")
        .contains("decision:approved");
  }

  @Test
  void reviewBridgeDoesNotMutateClaimLedgerVerificationStatus() {
    ClaimLedgerItemForReview claim = governedClaim(ORG_A);
    ClaimStore claims = new ClaimStore(claim);

    service(claims, new ReviewEventStore()).bridge(request());

    assertThat(claims.readBack(CLAIM_ID).verificationStatus())
        .isEqualTo(VerificationStatus.AI_EXTRACTED);
  }

  @Test
  void t3AndT4ReviewRequiresHumanReviewerAndReason() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult aiT3 = service(claims, reviewEvents).bridge(requestBuilder()
        .reviewerActorType(ActorRole.AI)
        .riskTier(RiskTier.T3_HIGH_RISK)
        .reason("high-risk review cannot be finalized by AI")
        .build());
    IntakeReviewBridgeResult systemT4 = service(claims, reviewEvents).bridge(requestBuilder()
        .reviewerActorType(ActorRole.SYSTEM)
        .riskTier(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING)
        .reason("legal review cannot be finalized by system actor")
        .build());

    assertThat(aiT3.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(aiT3.blockedReason()).isEqualTo("high_risk_review_requires_human_reviewer");
    assertThat(systemT4.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(systemT4.blockedReason()).isEqualTo("high_risk_review_requires_human_reviewer");
    assertThat(reviewEvents.commands).isEmpty();

    assertThatThrownBy(() -> requestBuilder()
        .riskTier(RiskTier.T3_HIGH_RISK)
        .reason(" ")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reason must not be blank");
  }

  @Test
  void bulkReviewDoesNotProduceCandidateConfirmedOrExternalVerifiedSemantics() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult result = service(claims, reviewEvents).bridge(requestBuilder()
        .bulkFlag(true)
        .reviewDecision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .reason("bulk acknowledged low-risk governed-intake review evidence")
        .build());

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(reviewEvents.commands).hasSize(1);
    assertThat(reviewEvents.commands.getFirst().bulkApproval()).isTrue();
    assertThat(reviewEvents.commands.getFirst().decision()).isEqualTo(ReviewDecision.APPROVED);
    assertThat(recordComponentNames(ReviewEventAppendCommand.class))
        .doesNotContain("verificationStatus", "candidateConfirmed", "externalVerified");
  }

  @Test
  void repeatedIdenticalBridgeCallReturnsExistingReviewEvent() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();
    IntakeReviewBridgeService service = service(claims, reviewEvents);

    IntakeReviewBridgeResult first = service.bridge(request());
    IntakeReviewBridgeResult second = service.bridge(request());

    assertThat(first.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_ALREADY_EXISTS);
    assertThat(second.existingReviewEventId()).isEqualTo(first.reviewEventId());
    assertThat(reviewEvents.commands).hasSize(1);
  }

  @Test
  void materiallyDifferentRepeatedBridgeCallAppendsNewReviewEvent() {
    ClaimStore claims = new ClaimStore(governedClaim(ORG_A));
    ReviewEventStore reviewEvents = new ReviewEventStore();
    IntakeReviewBridgeService service = service(claims, reviewEvents);

    IntakeReviewBridgeResult first = service.bridge(request());
    IntakeReviewBridgeResult second = service.bridge(requestBuilder()
        .reason("second reviewer note is materially different")
        .build());

    assertThat(first.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.reviewEventId()).isNotEqualTo(first.reviewEventId());
    assertThat(reviewEvents.commands).hasSize(2);
    assertThat(reviewEvents.commands.get(0).sourceSpanReference())
        .isNotEqualTo(reviewEvents.commands.get(1).sourceSpanReference());
  }

  @Test
  void reviewBridgeRejectsClaimWithoutGovernedIntakeLineage() {
    ClaimLedgerItemForReview oldSkeletonClaim = new ClaimLedgerItemForReview(
        CLAIM_ID,
        ORG_A,
        new EntityRef("candidate", uuid("00000000-0000-0000-0000-000000190099")),
        "headline",
        new SourceSpanRef("recruiting.source_item:legacy"),
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.INTERNAL_ONLY);
    ReviewEventStore reviewEvents = new ReviewEventStore();

    IntakeReviewBridgeResult result =
        service(new ClaimStore(oldSkeletonClaim), reviewEvents).bridge(request());

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(result.blockedReason()).isEqualTo("claim_not_from_governed_intake_lineage");
    assertThat(reviewEvents.commands).isEmpty();
  }

  @Test
  void reviewBridgeServiceDoesNotExposeCanonicalWorkflowProfileOrBusinessEntityQueries() {
    assertThat(publicDeclaredMethodNames(IntakeReviewBridgeService.class))
        .containsExactly("bridge");
    assertThat(nonStaticFieldTypeNames(IntakeReviewBridgeService.class))
        .containsExactlyInAnyOrder(
            "ClaimLedgerItemReviewLookupPort",
            "ReviewEventService",
            "ReviewEventSourceReferenceLookupPort");
    assertThat(allDeclaredNames(IntakeReviewBridgeService.class))
        .noneMatch(this::looksLikeForbiddenBoundaryOrBusinessEntityQuery);
  }

  private static IntakeReviewBridgeService service(
      ClaimStore claims,
      ReviewEventStore reviewEvents) {
    return new IntakeReviewBridgeService(
        claims,
        new ReviewEventService(reviewEvents),
        reviewEvents);
  }

  private static IntakeReviewBridgeRequest request() {
    return requestBuilder().build();
  }

  private static IntakeReviewBridgeRequest.Builder requestBuilder() {
    return IntakeReviewBridgeRequest.builder()
        .organizationId(ORG_A)
        .claimLedgerItemId(CLAIM_ID)
        .reviewerActorType(ActorRole.CONSULTANT)
        .reviewerActorId(REVIEWER_ID)
        .reviewDecision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T2_MEDIUM_RISK)
        .bulkFlag(false)
        .reason("reviewed governed-intake claim source lineage")
        .reviewPolicy(IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY);
  }

  private static ClaimLedgerItemForReview governedClaim(UUID organizationId) {
    return new ClaimLedgerItemForReview(
        CLAIM_ID,
        organizationId,
        new EntityRef("information_packet", PACKET_ID.value()),
        "intake.bridge_eligible.quality_note",
        new SourceSpanRef(
            "intake.extraction_run:" + RUN_ID.value()
                + "|intake.information_packet:" + PACKET_ID.value()
                + "|packet_type:CANDIDATE"
                + "|intended_entity_type:CANDIDATE"
                + "|intake.source_item:" + SOURCE_ID.value()
                + "|field:intake.bridge_eligible.quality_note"),
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.INTERNAL_ONLY);
  }

  private boolean looksLikeForbiddenBoundaryOrBusinessEntityQuery(String name) {
    String normalized = normalized(name);
    return normalized.contains("canonicalwrite")
        || normalized.contains("workflowevent")
        || normalized.contains("candidateprofile")
        || normalized.contains("rawcandidate")
        || normalized.contains("candidatequery")
        || normalized.contains("companyquery")
        || normalized.contains("jobquery")
        || normalized.contains("transitionlegality");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> nonStaticFieldTypeNames(Class<?> type) {
    return Stream.of(type.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .map(field -> field.getType().getSimpleName())
        .sorted()
        .toList();
  }

  private static List<String> allDeclaredNames(Class<?> type) {
    List<String> names = new ArrayList<>();
    Stream.of(type.getDeclaredFields()).forEach(field -> names.add(field.getName()));
    Stream.of(type.getDeclaredMethods()).map(Method::getName).forEach(names::add);
    return names;
  }

  private static List<String> recordComponentNames(Class<?> type) {
    return Stream.of(type.getRecordComponents())
        .map(component -> component.getName())
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class ClaimStore implements ClaimLedgerItemReviewLookupPort {
    private final Map<ClaimId, ClaimLedgerItemForReview> claims = new LinkedHashMap<>();

    private ClaimStore(ClaimLedgerItemForReview... claims) {
      for (ClaimLedgerItemForReview claim : claims) {
        this.claims.put(claim.claimLedgerItemId(), claim);
      }
    }

    @Override
    public Optional<ClaimLedgerItemForReview> findByIdAndOrganizationId(
        UUID organizationId,
        ClaimId claimLedgerItemId) {
      return Optional.ofNullable(claims.get(claimLedgerItemId))
          .filter(claim -> claim.organizationId().equals(organizationId));
    }

    private ClaimLedgerItemForReview readBack(ClaimId claimId) {
      return claims.get(claimId);
    }
  }

  private static final class ReviewEventStore
      implements ReviewEventPort, ReviewEventSourceReferenceLookupPort {
    private final List<ReviewEventAppendCommand> commands = new ArrayList<>();
    private final Map<SourceSpanRef, ReviewEventSourceReference> sourceReferences =
        new LinkedHashMap<>();
    private int sequence;

    @Override
    public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
      commands.add(command);
      ReviewEventId reviewEventId = new ReviewEventId(uuid("00000000-0000-0000-0000-0000001901"
          + String.format("%02d", ++sequence)));
      sourceReferences.put(command.sourceSpanReference(), new ReviewEventSourceReference(
          reviewEventId,
          command.organizationId(),
          command.targetEntity(),
          command.targetFieldPath(),
          command.claimId(),
          command.sourceSpanReference(),
          command.decision(),
          command.riskTier()));
      return new ReviewEventAppendResult(reviewEventId);
    }

    @Override
    public Optional<ReviewEventSourceReference> findBySourceSpanReference(
        UUID organizationId,
        SourceSpanRef sourceSpanReference) {
      return Optional.ofNullable(sourceReferences.get(sourceSpanReference))
          .filter(reference -> reference.organizationId().equals(organizationId));
    }
  }
}
