package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionDecision;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowTransitionAuditServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0002");
  private static final UUID CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0003");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0004");
  private static final UUID SOURCE_REF_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0005");
  private static final UUID AI_TASK_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0006");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0007");
  private static final UUID CORRELATION_UUID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0008");
  private static final UUID CAUSATION_UUID =
      UUID.fromString("00000000-0000-0000-0000-0000000d0009");
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T06:00:00Z");

  @Test
  void requestRequiresOrganizationId() {
    assertThatThrownBy(() -> requestBuilder().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void requestRequiresEntityType() {
    assertThatThrownBy(() -> requestBuilder().entityType(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("entityType must not be blank");
  }

  @Test
  void requestRequiresEntityId() {
    assertThatThrownBy(() -> requestBuilder().entityId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("entityId must not be null");
  }

  @Test
  void requestRequiresActionCode() {
    assertThatThrownBy(() -> requestBuilder().actionCode(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("actionCode must not be blank");
  }

  @Test
  void requestRequiresActorType() {
    assertThatThrownBy(() -> requestBuilder().actorType(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("actorType must not be null");
  }

  @Test
  void requestRequiresBeforeState() {
    assertThatThrownBy(() -> requestBuilder()
        .beforeState((WorkflowStateSnapshot) null)
        .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("beforeState must not be null");
  }

  @Test
  void requestRequiresAfterState() {
    assertThatThrownBy(() -> requestBuilder()
        .afterState((WorkflowStateSnapshot) null)
        .build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("afterState must not be null");
  }

  @Test
  void requestRejectsEqualBeforeAndAfterState() {
    assertThatThrownBy(() -> requestBuilder()
        .beforeState("{\"status\":\"client_review\"}")
        .afterState("{\"status\":\"client_review\"}")
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("beforeState and afterState must not be equal");
  }

  @Test
  void serviceRejectsUnknownActionCodeBeforeAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    assertThatThrownBy(() -> service(port).record(requestBuilder()
        .actionCode("CANDIDATE_ARBITRARY_AUDIT_RECORD")
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown workflow action code");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceRejectsNonTransitionAuditActions() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    assertThatThrownBy(() -> service(port).record(requestBuilder()
        .entityType("CLAIM_LEDGER_ITEM")
        .entityId(CLAIM_ID)
        .actionCode("CLAIM_LEDGER_ITEM_APPENDED")
        .actorType(ActorRole.SYSTEM)
        .aiInvolvement(WorkflowAiInvolvement.AI_ASSISTED)
        .beforeState("{\"status\":\"not_appended\"}")
        .afterState("{\"status\":\"appended\"}")
        .reason(null)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not configured as a state-transition audit action");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceEnforcesT3AndT4ReasonAndHumanActorPolicy() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowTransitionAuditService service = service(port);

    assertThatThrownBy(() -> service.record(requestBuilder()
        .reason(" ")
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reason is required");

    assertThatThrownBy(() -> service.record(requestBuilder()
        .actionCode("CANDIDATE_IDENTITY_DISCLOSED")
        .actorType(ActorRole.AI)
        .aiInvolvement(WorkflowAiInvolvement.AI_AUTOMATED_LOW_RISK)
        .beforeState("{\"status\":\"client_review\"}")
        .afterState("{\"status\":\"identity_disclosed\"}")
        .reason("identity disclosure requires human approval")
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a human final actor");

    assertThat(port.commands).isEmpty();
  }

  @Test
  void servicePreservesIdempotencyCorrelationAndCausationIds() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    service(port).record(requestBuilder().build());

    assertThat(port.commands).hasSize(1);
    WorkflowEventAppendCommand command = port.commands.getFirst();
    assertThat(command.idempotencyKey())
        .isEqualTo(new WorkflowIdempotencyKey("transition-audit-service-test"));
    assertThat(command.correlationId()).isEqualTo(new WorkflowCorrelationId(CORRELATION_UUID));
    assertThat(command.causationId()).isEqualTo(new WorkflowCausationId(CAUSATION_UUID));
  }

  @Test
  void duplicateSameIdempotencyKeyReturnsExistingEventThroughTransitionAuditService() {
    WorkflowTransitionAuditRequest request = requestBuilder()
        .idempotencyKey("transition-audit-duplicate")
        .build();
    WorkflowEventId existingEventId =
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-0000000d0101"));
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowTransitionAuditService service = service(port);

    service.record(request);
    WorkflowEventAppendCommand firstAppend = port.commands.getFirst();
    port.commands.clear();
    port.existing = new WorkflowEventIdempotencyRecord(existingEventId, firstAppend);

    WorkflowEventAppendResult result = service.record(request);

    assertThat(result.workflowEventId()).isEqualTo(existingEventId);
    assertThat(port.commands).isEmpty();
    assertThat(port.idempotencyLookups)
        .containsExactly(
            new IdempotencyLookup(
                ORGANIZATION_ID,
                new WorkflowIdempotencyKey("transition-audit-duplicate")),
            new IdempotencyLookup(
                ORGANIZATION_ID,
                new WorkflowIdempotencyKey("transition-audit-duplicate")));
  }

  @Test
  void serviceAppendsWorkflowEventWithBeforeAndAfterState() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    WorkflowEventAppendResult result = service(port).record(requestBuilder().build());

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).hasSize(1);
    WorkflowEventAppendCommand command = port.commands.getFirst();
    assertThat(command.action()).isEqualTo("CANDIDATE_SHORTLISTED");
    assertThat(command.beforeState().json()).isEqualTo("{\"status\":\"consultant_review\"}");
    assertThat(command.afterState().json()).isEqualTo("{\"status\":\"client_review\"}");
    assertThat(command.actor().role()).isEqualTo(ActorRole.CONSULTANT);
    assertThat(command.actor().userId()).isEqualTo(ACTOR_ID);
    assertThat(command.sourceType()).isEqualTo("domain_service");
    assertThat(command.sourceRefId()).isEqualTo(SOURCE_REF_ID);
  }

  @Test
  void serviceRejectsIllegalTransitionStatusSequence() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowTransitionAuditRequest oddButAuditableRequest = requestBuilder()
        .actionCode("CANDIDATE_CONSULTANT_REVIEW_STARTED")
        .beforeState("{\"status\":\"placed\"}")
        .afterState("{\"status\":\"unreviewed_surprise_state\"}")
        .reason(null)
        .idempotencyKey("transition-audit-legality-not-checked")
        .build();

    assertThatThrownBy(() -> service(port).record(oddButAuditableRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void placementRecordedTransitionIsAuditableFromAbsentToOfferPending() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    WorkflowEventAppendResult result = service(port).record(requestBuilder()
        .entityType("placement")
        .entityId(UUID.fromString("00000000-0000-0000-0000-0000000d0201"))
        .actionCode("PLACEMENT_RECORDED")
        .beforeState("{\"status\":\"absent\"}")
        .afterState("{\"status\":\"offer_pending\"}")
        .reason("consultant recorded placement offer")
        .build());

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).hasSize(1);
    assertThat(port.commands.getFirst().action()).isEqualTo("PLACEMENT_RECORDED");
  }

  @Test
  void commissionPendingTransitionAllowsInitialAbsentToPendingCreation() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();

    WorkflowEventAppendResult result = service(port).record(requestBuilder()
        .entityType("commission")
        .entityId(UUID.fromString("00000000-0000-0000-0000-0000000d0202"))
        .actionCode("COMMISSION_PENDING")
        .beforeState("{\"status\":\"absent\"}")
        .afterState("{\"status\":\"pending\"}")
        .reason("expected fee was calculated for invoice-ready placement")
        .build());

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).hasSize(1);
    assertThat(port.commands.getFirst().action()).isEqualTo("COMMISSION_PENDING");
  }

  @Test
  void serviceDoesNotExposeTargetEntityStateMutationOrLookupDependencies() {
    assertThat(publicDeclaredMethodNames(WorkflowTransitionAuditService.class))
        .containsExactly("preview", "record");
    assertThat(Stream.of(WorkflowTransitionAuditService.class.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .map(field -> field.getType().getSimpleName())
        .toList())
        .containsExactlyInAnyOrder(
            "WorkflowActionRegistry",
            "WorkflowTransitionLegalityPolicy",
            "WorkflowEventService",
            "WorkflowEntityStatePort");
  }

  @Test
  void previewReturnsBlockerWhenActualStateNoLongerMatchesBeforeState() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowTransitionAuditService service = new WorkflowTransitionAuditService(
        new WorkflowEventService(port),
        new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
          @Override
          public Optional<String> getCurrentStateJson(UUID orgId, String ns, String type, UUID id) {
            return Optional.of("{\"status\":\"available\"}");
          }
        });

    WorkflowTransitionDecision decision = service.preview(requestBuilder().build());

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.hasBlocker("before_state_mismatch")).isTrue();
    assertThat(port.commands).isEmpty();
  }

  private static WorkflowTransitionAuditService service(WorkflowEventPort workflowEventPort) {
    return new WorkflowTransitionAuditService(new WorkflowEventService(workflowEventPort), new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
      @Override
      public Optional<String> getCurrentStateJson(UUID orgId, String ns, String type, UUID id) { return Optional.empty(); }
    });
  }

  private static WorkflowTransitionAuditRequest.Builder requestBuilder() {
    return WorkflowTransitionAuditRequest.builder()
        .organizationId(ORGANIZATION_ID)
        .entityNamespace("recruiting")
        .entityType("CANDIDATE")
        .entityId(CANDIDATE_ID)
        .entityVersion(7)
        .actionCode("CANDIDATE_SHORTLISTED")
        .actorType(ActorRole.CONSULTANT)
        .actorId(ACTOR_ID)
        .aiInvolvement(WorkflowAiInvolvement.AI_ASSISTED)
        .beforeState("{\"status\":\"consultant_review\"}")
        .afterState("{\"status\":\"client_review\"}")
        .reason("consultant published anonymous shortlist after review")
        .idempotencyKey("transition-audit-service-test")
        .correlationId(CORRELATION_UUID)
        .causationId(CAUSATION_UUID)
        .sourceType("domain_service")
        .sourceRefId(SOURCE_REF_ID)
        .aiTaskRunId(AI_TASK_RUN_ID)
        .reviewEventId(REVIEW_EVENT_ID)
        .occurredAt(OCCURRED_AT);
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(method -> method.getName())
        .sorted()
        .toList();
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final List<IdempotencyLookup> idempotencyLookups = new ArrayList<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-0000000d0100")));
    private WorkflowEventIdempotencyRecord existing;

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      idempotencyLookups.add(new IdempotencyLookup(organizationId, idempotencyKey));
      return Optional.ofNullable(existing);
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return result;
    }
  }

  private record IdempotencyLookup(
      UUID organizationId,
      WorkflowIdempotencyKey idempotencyKey) {
  }
}
