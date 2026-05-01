package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptId;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.service.CandidateProfileCanonicalWriteTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteResult;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteReviewEvidence;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CanonicalWriteServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070002");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070003");
  private static final UUID CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070004");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070005");
  private static final UUID CORRELATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070006");
  private static final UUID CAUSATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000070007");
  private static final CandidateProfileId PROFILE_ID = new CandidateProfileId(
      UUID.fromString("00000000-0000-0000-0000-000000070008"));
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T03:00:00Z");

  @Test
  void blockedGateDecisionDoesNotAppendWorkflowEventOrCandidateProfileField() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCandidateProfilePort profilePort = new RecordingCandidateProfilePort();
    CanonicalWriteResult result = service(workflowPort, profilePort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            VerificationStatus.AI_EXTRACTED,
            ClientShareability.CLIENT_SAFE,
            false))
        .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .candidateProfileWriteTarget(candidateProfileWriteTarget())
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.workflowEventAppended()).isFalse();
    assertThat(result.workflowEventId()).isNull();
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(workflowPort.commands).isEmpty();
    assertThat(profilePort.upsertedFields).isEmpty();
  }

  @Test
  void blockedGateDecisionDoesNotStartTransactionBoundary() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingTransactionBoundary transactionBoundary = new RecordingTransactionBoundary();
    CanonicalWriteService service = new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        transactionBoundary);

    CanonicalWriteResult result = service.attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(transactionBoundary.invocations).isZero();
    assertThat(workflowPort.commands).isEmpty();
  }

  @Test
  void requireReviewDecisionDoesNotAppendSuccessWorkflowEvent() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
        .targetRiskTier(RiskTier.T3_HIGH_RISK)
        .targetVerificationStatus(VerificationStatus.CANDIDATE_CONFIRMED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.NEEDS_CONFIRMATION,
            false,
            false,
            "candidate confirmation is still pending"))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(result.workflowEventAppended()).isFalse();
    assertThat(workflowPort.commands).isEmpty();
  }

  @Test
  void allowedDecisionAppendsWorkflowAuditBoundary() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder().build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isEqualTo(workflowPort.result.workflowEventId());
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(result.canonicalPersistenceStatus())
        .isEqualTo("not_implemented_no_safe_canonical_write_target_in_task_3d");
    assertThat(workflowPort.commands).hasSize(1);
    WorkflowEventAppendCommand audit = workflowPort.commands.getFirst();
    assertThat(audit.action()).isEqualTo("CANONICAL_WRITE_ALLOWED");
    assertThat(audit.reviewEventId()).isEqualTo(new ReviewEventId(REVIEW_EVENT_ID));
    assertThat(audit.sourceRefId()).isEqualTo(CLAIM_ID);
    assertThat(audit.reason()).isEqualTo("reviewed source span before canonical boundary");
    assertThat(audit.idempotencyKey().value()).isEqualTo("canonical-write-boundary-test");
    assertThat(audit.correlationId().value()).isEqualTo(CORRELATION_ID);
    assertThat(audit.causationId().value()).isEqualTo(CAUSATION_ID);
  }

  @Test
  void allowedCandidateProfileTargetWritesFieldAfterAuditAndReportsPersistence() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCandidateProfilePort profilePort = new RecordingCandidateProfilePort();

    CanonicalWriteResult result = service(workflowPort, profilePort).attempt(commandBuilder()
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .candidateProfileWriteTarget(candidateProfileWriteTarget())
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isEqualTo(workflowPort.result.workflowEventId());
    assertThat(result.canonicalPersistencePerformed()).isTrue();
    assertThat(result.canonicalPersistenceStatus()).isEqualTo("candidate_profile_field_persisted");
    assertThat(workflowPort.commands).hasSize(1);
    assertThat(profilePort.upsertedFields).hasSize(1);
    CandidateProfileField field = profilePort.upsertedFields.getFirst();
    assertThat(field.fieldPath()).isEqualTo(CandidateProfileFieldPath.IDENTITY_FULL_NAME);
    assertThat(field.value()).isEqualTo(CandidateProfileFieldValue.ofString("Jane Candidate"));
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
    assertThat(field.sourceClaimId()).isEqualTo(new ClaimId(CLAIM_ID));
    assertThat(field.sourceReviewEventId()).isEqualTo(new ReviewEventId(REVIEW_EVENT_ID));
    assertThat(field.sourceWorkflowEventId()).isEqualTo(workflowPort.result.workflowEventId());
    assertThat(field.confirmedByActorId()).isEqualTo(ACTOR_ID);
  }

  @Test
  void allowedCandidateProfileTargetDoesNotReportPersistenceWhenFieldWriteFails() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCandidateProfilePort profilePort = new RecordingCandidateProfilePort();
    profilePort.failOnUpsert = true;

    assertThatThrownBy(() -> service(workflowPort, profilePort).attempt(commandBuilder()
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .candidateProfileWriteTarget(candidateProfileWriteTarget())
        .build()))
        .isInstanceOf(DeliberateProfileWriteFailure.class)
        .hasMessage("candidate profile write failed after audit");

    assertThat(workflowPort.commands).hasSize(1);
    assertThat(profilePort.upsertedFields).isEmpty();
  }

  @Test
  void bulkReviewCannotBecomeVerifiedCanonicalWrite() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
        .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.APPROVED,
            true,
            false,
            "bulk review accepted low-risk cleanup only"))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.decision().reasons())
        .contains("bulk_approve_cannot_create_external_verified");
    assertThat(workflowPort.commands).isEmpty();
  }

  @Test
  void systemInferenceCannotPassCanonicalWriteService() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.decision().reasons())
        .contains("system_inference_cannot_be_canonical_fact");
    assertThat(workflowPort.commands).isEmpty();
  }

  @Test
  void t4TransactionLegalWithoutStrongApprovalCannotPass() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
        .targetRiskTier(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING)
        .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.APPROVED,
            false,
            false,
            "generic review is not transaction/legal approval"))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(result.decision().reasons())
        .contains("high_risk_write_requires_explicit_review_approval");
    assertThat(workflowPort.commands).isEmpty();
  }

  @Test
  void serviceDoesNotExposeCandidateProfilePersistenceSurface() {
    assertThat(publicDeclaredMethodNames(CanonicalWriteService.class)).containsExactly("attempt");
    assertThat(declaredMethodNames(CanonicalWriteService.class))
        .noneMatch(this::looksLikeCandidatePersistenceSurface);
  }

  @Test
  void serviceRequiresReasonForUnsafeDecision() {
    CanonicalWriteResult blocked = service(new RecordingWorkflowEventPort()).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());
    CanonicalWriteResult requireReview = service(new RecordingWorkflowEventPort()).attempt(
        commandBuilder()
            .targetRiskTier(RiskTier.T3_HIGH_RISK)
            .targetVerificationStatus(VerificationStatus.CANDIDATE_CONFIRMED)
            .reviewEvidence(new CanonicalWriteReviewEvidence(
                new ReviewEventId(REVIEW_EVENT_ID),
                ReviewDecision.NEEDS_CONFIRMATION,
                false,
                false,
                "candidate confirmation is still pending"))
            .build());

    assertThat(blocked.decision().reasons()).isNotEmpty().allSatisfy(reason ->
        assertThat(reason).isNotBlank());
    assertThat(requireReview.decision().reasons()).isNotEmpty().allSatisfy(reason ->
        assertThat(reason).isNotBlank());
  }

  @Test
  void allowedAttemptRunsInsideExplicitTransactionBoundary() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingTransactionBoundary transactionBoundary = new RecordingTransactionBoundary();
    CanonicalWriteService service = new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        transactionBoundary);

    CanonicalWriteResult result = service.attempt(commandBuilder().build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(transactionBoundary.invocations).isEqualTo(1);
    assertThat(workflowPort.commands).hasSize(1);
  }

  @Test
  void blockedDecisionPersistsAttemptRecordViaPort() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCanonicalWriteAttemptPort attemptPort = new RecordingCanonicalWriteAttemptPort();
    CanonicalWriteResult result = service(workflowPort, attemptPort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.canonicalWriteAttemptId()).isNotNull();
    assertThat(result.canonicalWriteAttemptId()).isEqualTo(attemptPort.lastAppendedId);
    assertThat(attemptPort.commands).hasSize(1);
    CanonicalWriteAttemptAppendCommand cmd = attemptPort.commands.getFirst();
    assertThat(cmd.decision()).isEqualTo("block");
    assertThat(cmd.reasonCodes()).contains("system_inference_cannot_be_canonical_fact");
    assertThat(cmd.organizationId()).isEqualTo(ORGANIZATION_ID);
  }

  @Test
  void requireReviewDecisionPersistsAttemptRecordViaPort() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCanonicalWriteAttemptPort attemptPort = new RecordingCanonicalWriteAttemptPort();
    CanonicalWriteResult result = service(workflowPort, attemptPort).attempt(commandBuilder()
        .targetRiskTier(RiskTier.T3_HIGH_RISK)
        .targetVerificationStatus(VerificationStatus.CANDIDATE_CONFIRMED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.NEEDS_CONFIRMATION,
            false,
            false,
            "candidate confirmation is still pending"))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(result.canonicalWriteAttemptId()).isNotNull();
    assertThat(result.canonicalWriteAttemptId()).isEqualTo(attemptPort.lastAppendedId);
    assertThat(attemptPort.commands).hasSize(1);
    CanonicalWriteAttemptAppendCommand cmd = attemptPort.commands.getFirst();
    assertThat(cmd.decision()).isEqualTo("require_review");
  }

  @Test
  void allowedDecisionPersistsAttemptRecordInsideTransactionBoundary() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCanonicalWriteAttemptPort attemptPort = new RecordingCanonicalWriteAttemptPort();
    CanonicalWriteResult result = service(workflowPort, attemptPort).attempt(
        commandBuilder().build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.canonicalWriteAttemptId()).isNotNull();
    assertThat(result.canonicalWriteAttemptId()).isEqualTo(attemptPort.lastAppendedId);
    assertThat(attemptPort.commands).hasSize(1);
    CanonicalWriteAttemptAppendCommand cmd = attemptPort.commands.getFirst();
    assertThat(cmd.decision()).isEqualTo("allow");
    assertThat(cmd.reasonCodes()).contains("low_risk_human_acknowledged_write_allowed");
    assertThat(cmd.workflowEventId()).isNotNull();
  }

  @Test
  void blockedAttemptIsIdempotentViaAttemptPort() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCanonicalWriteAttemptPort attemptPort = new RecordingCanonicalWriteAttemptPort();
    CanonicalWriteService svc = service(workflowPort, attemptPort);
    CanonicalWriteCommand command = commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build();

    CanonicalWriteResult first = svc.attempt(command);
    attemptPort.idempotencyRecord = new CanonicalWriteAttemptIdempotencyRecord(
        attemptPort.lastAppendedId);
    CanonicalWriteResult second = svc.attempt(command);

    assertThat(first.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(second.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(second.canonicalWriteAttemptId()).isEqualTo(first.canonicalWriteAttemptId());
    assertThat(attemptPort.commands).hasSize(1);
    assertThat(attemptPort.findCalls).isEqualTo(2);
  }

  @Test
  void allowedAttemptIsIdempotentViaAttemptPort() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    RecordingCanonicalWriteAttemptPort attemptPort = new RecordingCanonicalWriteAttemptPort();
    CanonicalWriteService svc = service(workflowPort, attemptPort);
    CanonicalWriteCommand command = commandBuilder().build();

    CanonicalWriteResult first = svc.attempt(command);
    attemptPort.idempotencyRecord = new CanonicalWriteAttemptIdempotencyRecord(
        attemptPort.lastAppendedId);
    CanonicalWriteResult second = svc.attempt(command);

    assertThat(first.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(second.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(second.canonicalWriteAttemptId()).isEqualTo(first.canonicalWriteAttemptId());
  }

  private static CanonicalWriteService service(WorkflowEventPort workflowPort) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        CanonicalWriteTransactionBoundary.immediate());
  }

  private static CanonicalWriteService service(
      WorkflowEventPort workflowPort,
      RecordingCandidateProfilePort profilePort) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        CanonicalWriteTransactionBoundary.immediate(),
        new CandidateProfileService(profilePort));
  }

  private static CanonicalWriteService service(
      WorkflowEventPort workflowPort,
      CanonicalWriteAttemptPort attemptPort) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        CanonicalWriteTransactionBoundary.immediate(),
        null,
        attemptPort);
  }

  private static CanonicalWriteService service(
      WorkflowEventPort workflowPort,
      CanonicalWriteAttemptPort attemptPort,
      CanonicalWriteTransactionBoundary transactionBoundary) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        transactionBoundary,
        null,
        attemptPort);
  }

  private static CanonicalWriteCommand.Builder commandBuilder() {
    return CanonicalWriteCommand.builder()
        .organizationId(ORGANIZATION_ID)
        .targetEntity(new EntityRef("CANDIDATE", CANDIDATE_ID))
        .targetFieldPath("headline")
        .proposedValueRef("claim-value:headline:v1")
        .claimId(new ClaimId(CLAIM_ID))
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE,
            false))
        .canonicalWriteAllowed(true)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.APPROVED,
            false,
            false,
            "reviewed source span before canonical boundary"))
        .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .targetRiskTier(RiskTier.T1_LOW_RISK)
        .clientVisible(false)
        .conflictsWithCanonical(false)
        .actor(new ActorRef(ACTOR_ID, ActorRole.CONSULTANT))
        .reason("reviewed source span before canonical boundary")
        .correlationId(CORRELATION_ID)
        .causationId(CAUSATION_ID)
        .idempotencyKey("canonical-write-boundary-test")
        .occurredAt(OCCURRED_AT);
  }

  private static CandidateProfileCanonicalWriteTarget candidateProfileWriteTarget() {
    return new CandidateProfileCanonicalWriteTarget(
        PROFILE_ID,
        CandidateProfileFieldPath.IDENTITY_FULL_NAME,
        CandidateProfileFieldValue.ofString("Jane Candidate"),
        CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
  }

  private boolean looksLikeCandidatePersistenceSurface(String methodName) {
    String normalized = methodName.toLowerCase(Locale.ROOT).replace("_", "");
    return normalized.contains("savecandidateprofile")
        || normalized.contains("updatecandidateprofile")
        || normalized.contains("savecanonicalcandidate")
        || normalized.contains("writerawcandidate")
        || normalized.contains("unlock")
        || normalized.contains("disclose");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> declaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000070101")));

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      return Optional.empty();
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return result;
    }
  }

  private static final class RecordingTransactionBoundary
      implements CanonicalWriteTransactionBoundary {
    private int invocations;

    @Override
    public <T> T run(Work<T> work) {
      invocations++;
      try {
        return work.execute();
      } catch (RuntimeException | Error exception) {
        throw exception;
      } catch (Exception exception) {
        throw new IllegalStateException("unexpected checked test failure", exception);
      }
    }
  }

  private static final class RecordingCandidateProfilePort
      implements CandidateProfilePersistencePort {
    private final List<CandidateProfileField> upsertedFields = new ArrayList<>();
    private boolean failOnUpsert;

    @Override
    public CandidateProfile createCandidateProfile(CandidateProfile candidateProfile) {
      throw new UnsupportedOperationException("not used by canonical write service test");
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      return Optional.empty();
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
        UUID organizationId,
        CandidateId candidateId) {
      return Optional.empty();
    }

    @Override
    public CandidateProfileField upsertCandidateProfileField(
        UUID organizationId,
        CandidateProfileId candidateProfileId,
        CandidateProfileField field) {
      if (failOnUpsert) {
        throw new DeliberateProfileWriteFailure("candidate profile write failed after audit");
      }
      upsertedFields.add(field);
      return field;
    }

    @Override
    public List<CandidateProfileField> listCandidateProfileFields(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      return List.copyOf(upsertedFields);
    }
  }

  private static final class DeliberateProfileWriteFailure extends RuntimeException {
    private DeliberateProfileWriteFailure(String message) {
      super(message);
    }
  }

  private static final class RecordingCanonicalWriteAttemptPort
      implements CanonicalWriteAttemptPort {
    private final List<CanonicalWriteAttemptAppendCommand> commands = new ArrayList<>();
    private CanonicalWriteAttemptId lastAppendedId;
    private CanonicalWriteAttemptIdempotencyRecord idempotencyRecord;
    private int findCalls;

    @Override
    public Optional<CanonicalWriteAttemptIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      findCalls++;
      return Optional.ofNullable(idempotencyRecord);
    }

    @Override
    public CanonicalWriteAttemptAppendResult append(
        CanonicalWriteAttemptAppendCommand command) {
      commands.add(command);
      lastAppendedId = new CanonicalWriteAttemptId(
          UUID.nameUUIDFromBytes(
              ("test-cwa:" + commands.size()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      return new CanonicalWriteAttemptAppendResult(lastAppendedId);
    }
  }
}
