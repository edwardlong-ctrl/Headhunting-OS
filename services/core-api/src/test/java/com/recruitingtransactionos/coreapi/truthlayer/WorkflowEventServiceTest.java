package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
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

class WorkflowEventServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050002");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050003");
  private static final UUID SOURCE_REF_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050004");
  private static final UUID AI_TASK_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050005");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050006");
  private static final UUID CORRELATION_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000050007");
  private static final UUID CAUSATION_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000050008");
  private static final WorkflowIdempotencyKey IDEMPOTENCY_KEY =
      new WorkflowIdempotencyKey("workflow-event-service-test");
  private static final WorkflowCorrelationId CORRELATION_ID =
      WorkflowCorrelationId.fromWireValue(CORRELATION_UUID.toString());
  private static final WorkflowCausationId CAUSATION_ID =
      WorkflowCausationId.fromWireValue(CAUSATION_UUID.toString());

  @Test
  void appendDelegatesToPort() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);
    WorkflowEventAppendCommand command = validCommand();

    WorkflowEventAppendResult result = service.append(command);

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).containsExactly(command);
    assertThat(port.idempotencyLookups)
        .containsExactly(new IdempotencyLookup(ORGANIZATION_ID, IDEMPOTENCY_KEY));
  }

  @Test
  void idempotencyKeyRejectsBlankWhitespaceAndOverlongValues() {
    assertThatThrownBy(() -> new WorkflowIdempotencyKey("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("idempotencyKey must not be blank");
    assertThatThrownBy(() -> new WorkflowIdempotencyKey("x".repeat(201)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("idempotencyKey must be 200 characters or fewer");
  }

  @Test
  void correlationIdRejectsBlankWhitespaceAndOverlongValues() {
    assertThatThrownBy(() -> WorkflowCorrelationId.fromWireValue("\t "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("correlationId must not be blank");
    assertThatThrownBy(() -> WorkflowCorrelationId.fromWireValue("x".repeat(65)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("correlationId must be 64 characters or fewer");
  }

  @Test
  void causationIdRejectsBlankWhitespaceAndOverlongValues() {
    assertThatThrownBy(() -> WorkflowCausationId.fromWireValue("\n "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("causationId must not be blank");
    assertThatThrownBy(() -> WorkflowCausationId.fromWireValue("x".repeat(65)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("causationId must be 64 characters or fewer");
  }

  @Test
  void validCorrelationAndCausationIdsArePreservedOnAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);

    service.append(validCommand());

    assertThat(port.commands).hasSize(1);
    assertThat(port.commands.getFirst().correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(port.commands.getFirst().causationId()).isEqualTo(CAUSATION_ID);
  }

  @Test
  void duplicateSameIdempotencyKeyWithEquivalentPayloadReturnsExistingEventWithoutAppend() {
    WorkflowEventAppendCommand command = validCommand();
    WorkflowEventId existingEventId =
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000050202"));
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    port.existing = new WorkflowEventIdempotencyRecord(existingEventId, command);
    WorkflowEventService service = new WorkflowEventService(port);

    WorkflowEventAppendResult result = service.append(command);

    assertThat(result.workflowEventId()).isEqualTo(existingEventId);
    assertThat(port.commands).isEmpty();
    assertThat(port.idempotencyLookups)
        .containsExactly(new IdempotencyLookup(ORGANIZATION_ID, IDEMPOTENCY_KEY));
  }

  @Test
  void duplicateEquivalentIdempotencyRaceReturnsExistingEventAfterAppendFailure() {
    WorkflowEventAppendCommand command = validCommand();
    WorkflowEventId existingEventId =
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000050204"));
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    port.appendFailure = new IllegalStateException("duplicate idempotency key");
    port.existingAfterAppendFailure = new WorkflowEventIdempotencyRecord(existingEventId, command);
    WorkflowEventService service = new WorkflowEventService(port);

    WorkflowEventAppendResult result = service.append(command);

    assertThat(result.workflowEventId()).isEqualTo(existingEventId);
    assertThat(port.commands).isEmpty();
    assertThat(port.idempotencyLookups)
        .containsExactly(
            new IdempotencyLookup(ORGANIZATION_ID, IDEMPOTENCY_KEY),
            new IdempotencyLookup(ORGANIZATION_ID, IDEMPOTENCY_KEY));
  }

  @Test
  void duplicateSameIdempotencyKeyWithDifferentPayloadIsRejectedAsConflict() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    port.existing = new WorkflowEventIdempotencyRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000050203")),
        validCommand());
    WorkflowEventService service = new WorkflowEventService(port);

    assertThatThrownBy(() -> service.append(commandWithReason("different material reason")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("workflow event idempotency conflict");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceRejectsNullCommandBeforePortAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);

    assertThatThrownBy(() -> service.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceRejectsUnknownActionCodeBeforePortAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);

    assertThatThrownBy(() -> service.append(commandWithAction("CANDIDATE_ARBITRARY_AUDIT_RECORD")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown workflow action code");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void t3AndT4HumanAndReasonValidationStillRunsBeforePortAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);

    assertThatThrownBy(() -> new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        7,
        "CANDIDATE_SHORTLISTED",
        new WorkflowStateSnapshot("{\"status\":\"consent_confirmed\"}"),
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        "domain_service",
        SOURCE_REF_ID,
        null,
        null,
        " ",
        IDEMPOTENCY_KEY,
        CORRELATION_ID,
        CAUSATION_ID,
        Instant.parse("2026-04-28T02:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reason must not be blank");

    assertThatThrownBy(() -> service.append(commandWithActorRole(
        "CANDIDATE_IDENTITY_DISCLOSED",
        ActorRole.AI)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a human final actor");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceDoesNotValidateTransitionLegalityOrMutateEntityState() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);
    WorkflowEventAppendCommand oddButAuditableTransition = new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        7,
        "CANDIDATE_CONSULTANT_REVIEW_STARTED",
        new WorkflowStateSnapshot("{\"status\":\"placed\"}"),
        new WorkflowStateSnapshot("{\"status\":\"unreviewed_surprise_state\"}"),
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        "domain_service",
        SOURCE_REF_ID,
        null,
        null,
        "capturing audit state only, not approving a transition",
        new WorkflowIdempotencyKey("workflow-event-transition-legality-test"),
        CORRELATION_ID,
        CAUSATION_ID,
        Instant.parse("2026-04-28T02:00:00Z"));

    service.append(oddButAuditableTransition);

    assertThat(port.commands).containsExactly(oddButAuditableTransition);
  }

  @Test
  void serviceDoesNotExposeCanonicalWriteBehavior() {
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(WorkflowEventService.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void serviceDoesNotExposeTransitionValidationOrMutateStateBehavior() {
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(WorkflowEventService.class))
        .noneMatch(this::looksLikeWorkflowEngineBehavior);
  }

  @Test
  void idempotencyLookupDoesNotBecomeBroadRepositorySearchApi() {
    assertThat(publicDeclaredMethodNames(WorkflowEventPort.class))
        .containsExactly("append", "findByIdempotencyKey");
    assertThat(declaredMethodNames(WorkflowEventPort.class))
        .noneMatch(name -> normalized(name).contains("list")
            || normalized(name).contains("search")
            || normalized(name).contains("findbyentity")
            || normalized(name).contains("findbyactor")
            || normalized(name).contains("findbycorrelation")
            || normalized(name).contains("findbycausation"));
  }

  private static WorkflowEventAppendCommand validCommand() {
    return commandWithAction("CANDIDATE_SHORTLISTED");
  }

  private static WorkflowEventAppendCommand commandWithAction(String action) {
    return commandWithActionAndReason(action, "consultant published anonymous shortlist after review");
  }

  private static WorkflowEventAppendCommand commandWithReason(String reason) {
    return commandWithActionAndReason("CANDIDATE_SHORTLISTED", reason);
  }

  private static WorkflowEventAppendCommand commandWithActorRole(String action, ActorRole role) {
    return new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        7,
        action,
        new WorkflowStateSnapshot("{\"status\":\"consultant_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new ActorRef(ACTOR_ID, role),
        "domain_service",
        SOURCE_REF_ID,
        new AITaskRunId(AI_TASK_RUN_ID),
        new ReviewEventId(REVIEW_EVENT_ID),
        "consultant published anonymous shortlist after review",
        IDEMPOTENCY_KEY,
        CORRELATION_ID,
        CAUSATION_ID,
        Instant.parse("2026-04-28T02:00:00Z"));
  }

  private static WorkflowEventAppendCommand commandWithActionAndReason(String action, String reason) {
    return new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        7,
        action,
        new WorkflowStateSnapshot("{\"status\":\"consultant_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        "domain_service",
        SOURCE_REF_ID,
        new AITaskRunId(AI_TASK_RUN_ID),
        new ReviewEventId(REVIEW_EVENT_ID),
        reason,
        IDEMPOTENCY_KEY,
        CORRELATION_ID,
        CAUSATION_ID,
        Instant.parse("2026-04-28T02:00:00Z"));
  }

  private boolean looksLikeCanonicalWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("updatecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact")
        || normalized.contains("candidateconfirmed")
        || normalized.contains("externalverified");
  }

  private boolean looksLikeWorkflowEngineBehavior(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("transition")
        || normalized.contains("statemachine")
        || normalized.contains("mutatestate")
        || normalized.contains("advancestate")
        || normalized.contains("applystate")
        || normalized.contains("approve")
        || normalized.contains("reject");
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

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final List<IdempotencyLookup> idempotencyLookups = new ArrayList<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000050101")));
    private WorkflowEventIdempotencyRecord existing;
    private WorkflowEventIdempotencyRecord existingAfterAppendFailure;
    private IllegalStateException appendFailure;

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      idempotencyLookups.add(new IdempotencyLookup(organizationId, idempotencyKey));
      return Optional.ofNullable(existing);
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      if (appendFailure != null) {
        existing = existingAfterAppendFailure;
        throw appendFailure;
      }
      commands.add(command);
      return result;
    }
  }

  private record IdempotencyLookup(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey) {
  }
}
