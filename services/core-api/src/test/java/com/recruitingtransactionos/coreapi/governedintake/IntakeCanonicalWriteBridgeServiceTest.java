package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeCanonicalWriteBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteGate;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IntakeCanonicalWriteBridgeServiceTest {

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000210001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000210002");
  private static final UUID ACTOR_ID = uuid("00000000-0000-0000-0000-000000210003");
  private static final UUID TARGET_ENTITY_ID = uuid("00000000-0000-0000-0000-000000210004");
  private static final ClaimId CLAIM_ID =
      new ClaimId(uuid("00000000-0000-0000-0000-000000210005"));
  private static final ClaimId OTHER_CLAIM_ID =
      new ClaimId(uuid("00000000-0000-0000-0000-000000210006"));
  private static final ReviewEventId REVIEW_EVENT_ID =
      new ReviewEventId(uuid("00000000-0000-0000-0000-000000210007"));
  private static final ReviewEventId OTHER_REVIEW_EVENT_ID =
      new ReviewEventId(uuid("00000000-0000-0000-0000-000000210008"));
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(uuid("00000000-0000-0000-0000-000000210009"));
  private static final IntakeExtractionRunId RUN_ID =
      new IntakeExtractionRunId(uuid("00000000-0000-0000-0000-000000210010"));
  private static final SourceItemId SOURCE_ID =
      new SourceItemId(uuid("00000000-0000-0000-0000-000000210011"));
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T15:00:00Z");

  @Test
  void bridgeRequestRequiresOrganizationId() {
    assertThatThrownBy(() -> requestBuilder().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void bridgeRequestRequiresClaimLedgerItemId() {
    assertThatThrownBy(() -> requestBuilder().claimLedgerItemId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("claimLedgerItemId must not be null");
  }

  @Test
  void bridgeRequestRequiresReviewEventId() {
    assertThatThrownBy(() -> requestBuilder().reviewEventId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("reviewEventId must not be null");
  }

  @Test
  void bridgeRequestRequiresRequestedByActorTypeAndActorId() {
    assertThatThrownBy(() -> requestBuilder().requestedByActorType(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("requestedByActorType must not be null");
    assertThatThrownBy(() -> requestBuilder().requestedByActorId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("requestedByActorId must not be null");
  }

  @Test
  void bridgeRequestRequiresTargetEntityAndFieldShape() {
    assertThatThrownBy(() -> requestBuilder().targetEntityType(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("targetEntityType must not be null");
    assertThatThrownBy(() -> requestBuilder().targetEntityType(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("targetEntityType must not be blank");
    assertThatThrownBy(() -> requestBuilder().targetEntityId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("targetEntityId must not be null");
    assertThatThrownBy(() -> requestBuilder().targetFieldPath(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("targetFieldPath must not be blank");
  }

  @Test
  void bridgeRequestRequiresRiskTierTargetVerificationStatusReasonAndPolicy() {
    assertThatThrownBy(() -> requestBuilder().riskTier(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("riskTier must not be null");
    assertThatThrownBy(() -> requestBuilder().targetVerificationStatus(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("targetVerificationStatus must not be null");
    assertThatThrownBy(() -> requestBuilder().reason(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reason must not be blank");
    assertThatThrownBy(() -> requestBuilder().bridgePolicy(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("bridgePolicy must not be null");
  }

  @Test
  void bridgeRejectsMissingOrWrongOrganizationClaimLedgerItem() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();

    IntakeCanonicalWriteBridgeResult missing =
        service(new ClaimStore(), new ReviewStore(reviewEvent(ORG_A)), workflowEvents)
            .bridge(request());
    IntakeCanonicalWriteBridgeResult wrongOrg =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(reviewEvent(ORG_A)),
            workflowEvents)
            .bridge(requestBuilder().organizationId(ORG_B).build());

    assertThat(missing.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(missing.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(wrongOrg.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(wrongOrg.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeRejectsMissingOrWrongOrganizationReviewEvent() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();

    IntakeCanonicalWriteBridgeResult missing =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(), workflowEvents)
            .bridge(request());
    IntakeCanonicalWriteBridgeResult wrongOrg =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(reviewEvent(ORG_A)),
            workflowEvents)
            .bridge(requestBuilder().organizationId(ORG_B).build());

    assertThat(missing.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(missing.blockedReason()).isEqualTo("review_event_not_found_in_organization");
    assertThat(wrongOrg.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(wrongOrg.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeRejectsReviewEventThatDoesNotBelongToClaimLedgerItem() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    ReviewEventForCanonicalWrite wrongClaimReview = reviewEventBuilder()
        .claimLedgerItemId(OTHER_CLAIM_ID)
        .reviewEventId(OTHER_REVIEW_EVENT_ID)
        .build();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(wrongClaimReview),
            workflowEvents)
            .bridge(requestBuilder().reviewEventId(OTHER_REVIEW_EVENT_ID).build());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(result.blockedReason()).isEqualTo("review_event_does_not_belong_to_claim");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeRejectsReviewEventTargetThatDoesNotMatchClaimReviewLineage() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    ReviewEventForCanonicalWrite wrongFieldReview = reviewEventBuilder()
        .targetFieldPath("intake.bridge_eligible.other_note")
        .build();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(wrongFieldReview),
            workflowEvents)
            .bridge(request());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(result.blockedReason()).isEqualTo("review_event_target_does_not_match_claim");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeRejectsNonGovernedIntakeClaim() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    ClaimLedgerItemForCanonicalWrite oldSkeletonClaim = governedClaimBuilder()
        .targetEntity(new EntityRef("candidate", uuid("00000000-0000-0000-0000-000000210099")))
        .targetFieldPath("headline")
        .sourceSpanReference(new SourceSpanRef("recruiting.source_item:legacy"))
        .build();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(oldSkeletonClaim), new ReviewStore(reviewEvent(ORG_A)),
            workflowEvents)
            .bridge(request());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(result.blockedReason()).isEqualTo("claim_not_from_governed_intake_lineage");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeRejectsReviewEventNotCreatedThroughGovernedIntakeReviewBridge() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    ReviewEventForCanonicalWrite genericReview = reviewEventBuilder()
        .sourceSpanReference("manual-review-without-intake-review-bridge")
        .build();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(genericReview),
            workflowEvents)
            .bridge(request());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(result.blockedReason()).isEqualTo("review_event_not_from_governed_intake_bridge");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeCallsCanonicalWriteServiceAndSurfacesGateBlockedDecision() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(governedClaim(ORG_A)), new ReviewStore(reviewEvent(ORG_A)),
            workflowEvents)
            .bridge(request());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED);
    assertThat(result.gateDecision()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.blockedReason())
        .contains("system_inference_cannot_be_canonical_fact")
        .contains("ai_extracted_claim_cannot_be_canonical_fact");
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void allowedGateResultIsAuditedButStillDoesNotPersistCanonicalProfile() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();

    IntakeCanonicalWriteBridgeResult result = service(
        new ClaimStore(allowedGovernedClaim(ORG_A)),
        new ReviewStore(reviewEvent(ORG_A)),
        workflowEvents)
        .bridge(requestBuilder()
            .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
            .riskTier(RiskTier.T1_LOW_RISK)
            .reason("allowed low-risk boundary audit remains non-persistent")
            .build());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(result.gateDecision()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventId()).isEqualTo(workflowEvents.result.workflowEventId());
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(result.canonicalPersistenceStatus())
        .isEqualTo("not_implemented_no_safe_canonical_write_target_in_task_3d");
    assertThat(workflowEvents.commands).hasSize(1);
    WorkflowEventAppendCommand command = workflowEvents.commands.getFirst();
    assertThat(command.action()).isEqualTo("CANONICAL_WRITE_ALLOWED");
    assertThat(command.sourceType()).isEqualTo("canonical_write_service");
    assertThat(command.sourceRefId()).isEqualTo(CLAIM_ID.value());
    assertThat(command.reviewEventId()).isEqualTo(REVIEW_EVENT_ID);
    assertThat(command.idempotencyKey().value())
        .contains("intake-canonical-write-bridge")
        .contains(CLAIM_ID.value().toString())
        .contains(REVIEW_EVENT_ID.value().toString());
  }

  @Test
  void t3AndT4CanonicalWritePathsRequireHumanActorAndReasonThroughExistingPolicy() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();

    IntakeCanonicalWriteBridgeResult aiT3 = service(
        new ClaimStore(allowedGovernedClaim(ORG_A)),
        new ReviewStore(reviewEvent(ORG_A)),
        workflowEvents)
        .bridge(requestBuilder()
            .requestedByActorType(ActorRole.AI)
            .riskTier(RiskTier.T3_HIGH_RISK)
            .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
            .reason("AI cannot finalize high-risk canonical write boundary")
            .build());
    IntakeCanonicalWriteBridgeResult consultantT3 = service(
        new ClaimStore(allowedGovernedClaim(ORG_A)),
        new ReviewStore(reviewEvent(ORG_A)),
        workflowEvents)
        .bridge(requestBuilder()
            .riskTier(RiskTier.T3_HIGH_RISK)
            .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
            .reason("human approved high-risk canonical write boundary")
            .build());

    assertThat(aiT3.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(aiT3.blockedReason()).isEqualTo("high_risk_write_requires_human_actor");
    assertThat(consultantT3.status())
        .isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(workflowEvents.commands).hasSize(1);
  }

  @Test
  void bulkReviewDoesNotProduceCandidateConfirmedOrExternalVerifiedCanonicalWrite() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    ReviewEventForCanonicalWrite bulkReview = reviewEventBuilder()
        .bulkApproval(true)
        .decision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .build();

    IntakeCanonicalWriteBridgeResult result =
        service(new ClaimStore(allowedGovernedClaim(ORG_A)), new ReviewStore(bulkReview),
            workflowEvents)
            .bridge(requestBuilder()
                .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
                .riskTier(RiskTier.T1_LOW_RISK)
                .reason("bulk review must not create external verified fact")
                .build());

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED);
    assertThat(result.blockedReason()).contains("bulk_approve_cannot_create_external_verified");
    assertThat(workflowEvents.commands).isEmpty();
  }

  @Test
  void bridgeDoesNotMutateClaimLedgerItemOrReviewEvent() {
    ClaimLedgerItemForCanonicalWrite claim = governedClaim(ORG_A);
    ReviewEventForCanonicalWrite review = reviewEvent(ORG_A);
    ClaimStore claims = new ClaimStore(claim);
    ReviewStore reviews = new ReviewStore(review);

    service(claims, reviews, new CanonicalWriteStore()).bridge(request());

    assertThat(claims.readBack(CLAIM_ID)).isEqualTo(claim);
    assertThat(reviews.readBack(REVIEW_EVENT_ID)).isEqualTo(review);
  }

  @Test
  void repeatedIdenticalAllowedBridgeAttemptUsesCanonicalWriteIdempotency() {
    CanonicalWriteStore workflowEvents = new CanonicalWriteStore();
    IntakeCanonicalWriteBridgeService service = service(
        new ClaimStore(allowedGovernedClaim(ORG_A)),
        new ReviewStore(reviewEvent(ORG_A)),
        workflowEvents);
    IntakeCanonicalWriteBridgeRequest request = requestBuilder()
        .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .reason("same bridge attempt should return existing audit event")
        .build();

    IntakeCanonicalWriteBridgeResult first = service.bridge(request);
    IntakeCanonicalWriteBridgeResult second = service.bridge(request);

    assertThat(first.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(second.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(second.workflowEventId()).isEqualTo(first.workflowEventId());
    assertThat(workflowEvents.commands).hasSize(1);
  }

  @Test
  void bridgeServiceDoesNotExposeProfileWorkflowEngineApiOrBusinessEntityQueries() {
    assertThat(publicDeclaredMethodNames(IntakeCanonicalWriteBridgeService.class))
        .containsExactly("bridge");
    assertThat(nonStaticFieldTypeNames(IntakeCanonicalWriteBridgeService.class))
        .containsExactlyInAnyOrder(
            "CanonicalWriteService",
            "ClaimLedgerItemCanonicalWriteLookupPort",
            "ReviewEventCanonicalWriteLookupPort");
    assertThat(allDeclaredNames(IntakeCanonicalWriteBridgeService.class))
        .noneMatch(this::looksLikeForbiddenBoundaryOrBusinessEntityQuery);
  }

  private static IntakeCanonicalWriteBridgeService service(
      ClaimStore claims,
      ReviewStore reviews,
      CanonicalWriteStore workflowEvents) {
    return new IntakeCanonicalWriteBridgeService(
        claims,
        reviews,
        new CanonicalWriteService(
            new CanonicalWriteGate(),
            new WorkflowEventService(workflowEvents),
            CanonicalWriteTransactionBoundary.immediate()));
  }

  private static IntakeCanonicalWriteBridgeRequest request() {
    return requestBuilder().build();
  }

  private static IntakeCanonicalWriteBridgeRequest.Builder requestBuilder() {
    return IntakeCanonicalWriteBridgeRequest.builder()
        .organizationId(ORG_A)
        .claimLedgerItemId(CLAIM_ID)
        .reviewEventId(REVIEW_EVENT_ID)
        .requestedByActorType(ActorRole.CONSULTANT)
        .requestedByActorId(ACTOR_ID)
        .targetEntityType("CANDIDATE")
        .targetEntityId(TARGET_ENTITY_ID)
        .targetFieldPath("headline")
        .requestedCanonicalValue("optional requested value is represented by hash only")
        .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .riskTier(RiskTier.T2_MEDIUM_RISK)
        .clientVisible(false)
        .conflictsWithCanonical(false)
        .transactionLegalApproval(false)
        .reason("reviewed governed-intake claim before canonical boundary")
        .correlationId(uuid("00000000-0000-0000-0000-000000210012"))
        .causationId(uuid("00000000-0000-0000-0000-000000210013"))
        .occurredAt(OCCURRED_AT)
        .bridgePolicy(IntakeCanonicalWriteBridgePolicy.GOVERNED_INTAKE_CLAIM_AND_REVIEW_ONLY);
  }

  private static ClaimLedgerItemForCanonicalWrite governedClaim(UUID organizationId) {
    return governedClaimBuilder().organizationId(organizationId).build();
  }

  private static ClaimLedgerItemForCanonicalWrite allowedGovernedClaim(UUID organizationId) {
    return governedClaimBuilder()
        .organizationId(organizationId)
        .claimType(ClaimType.FACT)
        .assertionStrength(AssertionStrength.EXPLICIT)
        .verificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .clientShareability(ClientShareability.CLIENT_SAFE)
        .canonicalWriteAllowed(true)
        .claimValueText("reviewed operational bridge fixture")
        .build();
  }

  private static ClaimLedgerItemForCanonicalWrite.Builder governedClaimBuilder() {
    return ClaimLedgerItemForCanonicalWrite.builder()
        .claimLedgerItemId(CLAIM_ID)
        .organizationId(ORG_A)
        .targetEntity(new EntityRef("information_packet", PACKET_ID.value()))
        .targetFieldPath("intake.bridge_eligible.quality_note")
        .claimType(ClaimType.INFERENCE)
        .assertionStrength(AssertionStrength.WEAK_SIGNAL)
        .verificationStatus(VerificationStatus.AI_EXTRACTED)
        .clientShareability(ClientShareability.INTERNAL_ONLY)
        .canonicalWriteAllowed(false)
        .claimValueText("explicitly marked operational bridge fixture")
        .sourceSpanReference(new SourceSpanRef(
            "intake.extraction_run:" + RUN_ID.value()
                + "|intake.information_packet:" + PACKET_ID.value()
                + "|packet_type:CANDIDATE"
                + "|intended_entity_type:CANDIDATE"
                + "|intake.source_item:" + SOURCE_ID.value()
                + "|field:intake.bridge_eligible.quality_note"));
  }

  private static ReviewEventForCanonicalWrite reviewEvent(UUID organizationId) {
    return reviewEventBuilder().organizationId(organizationId).build();
  }

  private static ReviewEventForCanonicalWrite.Builder reviewEventBuilder() {
    return ReviewEventForCanonicalWrite.builder()
        .reviewEventId(REVIEW_EVENT_ID)
        .organizationId(ORG_A)
        .targetEntity(new EntityRef("information_packet", PACKET_ID.value()))
        .targetFieldPath("intake.bridge_eligible.quality_note")
        .claimLedgerItemId(CLAIM_ID)
        .sourceSpanReference(
            "intake.review_bridge|claim_ledger_item:" + CLAIM_ID.value())
        .decision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T2_MEDIUM_RISK)
        .bulkApproval(false)
        .reviewerId(ACTOR_ID)
        .reason("reviewed governed-intake claim source lineage");
  }

  private boolean looksLikeForbiddenBoundaryOrBusinessEntityQuery(String name) {
    String normalized = normalized(name);
    return normalized.contains("candidateprofilepersistence")
        || normalized.contains("jdbccandidateprofile")
        || normalized.contains("candidateprofileservice")
        || normalized.contains("rawcandidate")
        || normalized.contains("candidatequery")
        || normalized.contains("companyquery")
        || normalized.contains("jobquery")
        || normalized.contains("transitionlegality")
        || normalized.contains("workflowengine")
        || normalized.contains("controller")
        || normalized.contains("api");
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

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class ClaimStore implements ClaimLedgerItemCanonicalWriteLookupPort {
    private final Map<ClaimId, ClaimLedgerItemForCanonicalWrite> claims = new LinkedHashMap<>();

    private ClaimStore(ClaimLedgerItemForCanonicalWrite... claims) {
      for (ClaimLedgerItemForCanonicalWrite claim : claims) {
        this.claims.put(claim.claimLedgerItemId(), claim);
      }
    }

    @Override
    public Optional<ClaimLedgerItemForCanonicalWrite> findByIdAndOrganizationId(
        UUID organizationId,
        ClaimId claimLedgerItemId) {
      return Optional.ofNullable(claims.get(claimLedgerItemId))
          .filter(claim -> claim.organizationId().equals(organizationId));
    }

    private ClaimLedgerItemForCanonicalWrite readBack(ClaimId claimId) {
      return claims.get(claimId);
    }
  }

  private static final class ReviewStore implements ReviewEventCanonicalWriteLookupPort {
    private final Map<ReviewEventId, ReviewEventForCanonicalWrite> reviews = new LinkedHashMap<>();

    private ReviewStore(ReviewEventForCanonicalWrite... reviews) {
      for (ReviewEventForCanonicalWrite review : reviews) {
        this.reviews.put(review.reviewEventId(), review);
      }
    }

    @Override
    public Optional<ReviewEventForCanonicalWrite> findByIdAndOrganizationId(
        UUID organizationId,
        ReviewEventId reviewEventId) {
      return Optional.ofNullable(reviews.get(reviewEventId))
          .filter(review -> review.organizationId().equals(organizationId));
    }

    private ReviewEventForCanonicalWrite readBack(ReviewEventId reviewEventId) {
      return reviews.get(reviewEventId);
    }
  }

  private static final class CanonicalWriteStore implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final Map<WorkflowIdempotencyKey, WorkflowEventIdempotencyRecord> idempotencyRecords =
        new LinkedHashMap<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(uuid("00000000-0000-0000-0000-000000210101")));

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      return Optional.ofNullable(idempotencyRecords.get(idempotencyKey))
          .filter(record -> record.command().organizationId().equals(organizationId));
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      if (command.idempotencyKey() != null) {
        idempotencyRecords.put(command.idempotencyKey(),
            new WorkflowEventIdempotencyRecord(result.workflowEventId(), command));
      }
      return result;
    }
  }
}
