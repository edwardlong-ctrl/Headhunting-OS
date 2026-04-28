package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
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
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T03:00:00Z");

  @Test
  void blockedGateDecisionDoesNotAppendWorkflowEvent() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            VerificationStatus.AI_EXTRACTED,
            ClientShareability.CLIENT_SAFE,
            false))
        .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.workflowEventAppended()).isFalse();
    assertThat(result.workflowEventId()).isNull();
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

  private static CanonicalWriteService service(WorkflowEventPort workflowPort) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        CanonicalWriteTransactionBoundary.immediate());
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
}
