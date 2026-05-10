package com.recruitingtransactionos.coreapi.supportops;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportOperationsServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000560001");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000560002");
  private static final UUID SUPPORT_ADMIN = UUID.fromString("00000000-0000-0000-0000-000000560101");
  private static final UUID TARGET_USER = UUID.fromString("00000000-0000-0000-0000-000000560201");
  private static final UUID TARGET_NOTIFICATION = UUID.fromString("00000000-0000-0000-0000-000000560301");
  private static final UUID TARGET_AI_RUN = UUID.fromString("00000000-0000-0000-0000-000000560401");
  private static final UUID TARGET_CANDIDATE = UUID.fromString("00000000-0000-0000-0000-000000560501");

  @Test
  void supportLookupCannotInferOrAccessAnotherOrganizationsUser() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingUserLookupPort userLookupPort = new RecordingUserLookupPort(Optional.of(
        new SupportUserSummary(
            TARGET_USER,
            ORG_B,
            "victim@example.test",
            "Cross Org User",
            "active",
            List.of(PortalRole.CANDIDATE))));
    SupportOperationsService service = service(auditPort, userLookupPort);

    SupportUserLookupResult result = service.lookupUser(new SupportUserLookupCommand(
        admin(ORG_A),
        ORG_B,
        TARGET_USER,
        "SUP-56",
        "Help user find failed notification state."));

    assertThat(result.allowed()).isFalse();
    assertThat(result.resultCode()).isEqualTo("support_action_denied");
    assertThat(result.user()).isEmpty();
    assertThat(userLookupPort.commands).isEmpty();
    assertThat(auditPort.commands).hasSize(1);
    assertThat(auditPort.commands.getFirst().result()).isEqualTo("denied");
    assertThat(auditPort.commands.getFirst().targetType()).isEqualTo(SupportTargetType.USER_ACCOUNT);
  }

  @Test
  void supportRetryRequiresTicketAndReasonAndWritesAuditEvidence() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingNotificationRetryPort retryPort = new RecordingNotificationRetryPort(
        new FailedNotificationRetryOutcome(true, TARGET_NOTIFICATION, "notification_retry_created"));
    SupportOperationsService service = service(auditPort, retryPort);

    SupportActionResult denied = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_A,
        TARGET_NOTIFICATION,
        "",
        " "));

    assertThat(denied.allowed()).isFalse();
    assertThat(denied.resultCode()).isEqualTo("support_action_denied");
    assertThat(retryPort.commands).isEmpty();
    assertThat(auditPort.commands).hasSize(1);
    assertThat(auditPort.commands.getFirst().actionType()).isEqualTo(SupportActionType.RETRY_FAILED_NOTIFICATION);

    SupportActionResult allowed = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_A,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Customer missed the reminder email; retry provider-safe delivery."));

    assertThat(allowed.allowed()).isTrue();
    assertThat(allowed.resultCode()).isEqualTo("notification_retry_created");
    assertThat(allowed.auditId()).isNotNull();
    assertThat(retryPort.commands).hasSize(1);
    assertThat(auditPort.commands).hasSize(2);
    assertThat(auditPort.commands.get(1).result()).isEqualTo("notification_retry_created");
    assertThat(auditPort.commands.get(1).ticketRef()).isEqualTo("SUP-56");
  }

  @Test
  void failedNotificationRetryIsDuplicateSafeAndOrganizationScoped() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingNotificationRetryPort retryPort = new RecordingNotificationRetryPort(
        new FailedNotificationRetryOutcome(false, TARGET_NOTIFICATION, "notification_retry_duplicate_skipped"));
    SupportOperationsService service = service(auditPort, retryPort);

    SupportActionResult crossOrg = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_B,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Retry failed delivery."));
    assertThat(crossOrg.allowed()).isFalse();
    assertThat(retryPort.commands).isEmpty();

    SupportActionResult duplicate = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_A,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Retry failed delivery."));
    SupportActionResult duplicateAgain = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_A,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Retry failed delivery."));

    assertThat(duplicate.allowed()).isTrue();
    assertThat(duplicateAgain.allowed()).isTrue();
    assertThat(duplicate.resultCode()).isEqualTo("notification_retry_duplicate_skipped");
    assertThat(duplicateAgain.resultCode()).isEqualTo("notification_retry_duplicate_skipped");
    assertThat(retryPort.commands).extracting(FailedNotificationRetryRequest::organizationId)
        .containsOnly(ORG_A);
  }

  @Test
  void aiReplayRequiresAuditEvidenceAndCannotDirectlyWriteCanonicalFacts() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingAITaskReplayPort replayPort = new RecordingAITaskReplayPort(
        new AITaskSupportReplayOutcome(
            new AITaskRunId(UUID.fromString("00000000-0000-0000-0000-000000560402")),
            true,
            "ai_replay_attempted_canonical_write"));
    SupportOperationsService service = service(auditPort, replayPort);

    SupportActionResult denied = service.replayAiTask(new AITaskSupportReplayCommand(
        admin(ORG_A),
        ORG_A,
        TARGET_AI_RUN,
        "SUP-56",
        "Replay failed parser run."));

    assertThat(denied.allowed()).isFalse();
    assertThat(denied.resultCode()).isEqualTo("ai_replay_canonical_write_blocked");
    assertThat(replayPort.commands).hasSize(1);
    assertThat(auditPort.commands.getFirst().actionType()).isEqualTo(SupportActionType.REPLAY_AI_TASK);
    assertThat(auditPort.commands.getFirst().result()).isEqualTo("ai_replay_canonical_write_blocked");
  }

  @Test
  void dataCorrectionRequestCreatesReviewAndWorkflowItemsAndPreservesExistingFacts() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingReviewEventPort reviewEventPort = new RecordingReviewEventPort();
    RecordingWorkflowEventPort workflowEventPort = new RecordingWorkflowEventPort();
    SupportOperationsService service = service(
        auditPort,
        new ReviewEventService(reviewEventPort),
        new WorkflowEventService(workflowEventPort));

    DataCorrectionRequestResult result = service.requestDataCorrection(new DataCorrectionSupportCommand(
        admin(ORG_A),
        ORG_A,
        new EntityRef(WorkflowEntityType.CANDIDATE.wireValue(), TARGET_CANDIDATE),
        "profile.summary",
        "Candidate says the current summary mixes two employers.",
        "SUP-56",
        "Open a governed correction request instead of editing facts."));

    assertThat(result.allowed()).isTrue();
    assertThat(result.reviewEventId()).isNotNull();
    assertThat(result.workflowEventId()).isNotNull();
    assertThat(result.canonicalFactsMutated()).isFalse();
    assertThat(reviewEventPort.commands).hasSize(1);
    assertThat(reviewEventPort.commands.getFirst().decision()).isEqualTo(ReviewDecision.NEEDS_CONFIRMATION);
    assertThat(reviewEventPort.commands.getFirst().riskTier()).isEqualTo(RiskTier.T3_HIGH_RISK);
    assertThat(workflowEventPort.commands).hasSize(1);
    assertThat(workflowEventPort.commands.getFirst().action())
        .isEqualTo(WorkflowActionCode.REVIEW_EVENT_APPENDED.wireValue());
    assertThat(auditPort.commands.getFirst().actionType()).isEqualTo(SupportActionType.REQUEST_DATA_CORRECTION);
  }

  @Test
  void permissionDeniedResponsesDoNotLeakRecordExistence() {
    RecordingSupportAuditPort auditPort = new RecordingSupportAuditPort();
    RecordingNotificationRetryPort retryPort = new RecordingNotificationRetryPort(
        new FailedNotificationRetryOutcome(true, TARGET_NOTIFICATION, "notification_retry_created"));
    SupportOperationsService service = service(auditPort, retryPort);

    SupportActionResult nonAdmin = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        new SupportActor(ORG_A, SUPPORT_ADMIN, PortalRole.CONSULTANT),
        ORG_A,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Retry failed delivery."));
    SupportActionResult crossOrg = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(ORG_A),
        ORG_B,
        TARGET_NOTIFICATION,
        "SUP-56",
        "Retry failed delivery."));

    assertThat(nonAdmin.resultCode()).isEqualTo("support_action_denied");
    assertThat(crossOrg.resultCode()).isEqualTo("support_action_denied");
    assertThat(nonAdmin.targetId()).isEmpty();
    assertThat(crossOrg.targetId()).isEmpty();
    assertThat(retryPort.commands).isEmpty();
  }

  private static SupportOperationsService service(RecordingSupportAuditPort auditPort) {
    return service(auditPort, new RecordingUserLookupPort(Optional.empty()));
  }

  private static SupportOperationsService service(
      RecordingSupportAuditPort auditPort,
      SupportUserLookupPort userLookupPort) {
    return new SupportOperationsService(
        new SupportOperationsPermissionPolicy(),
        userLookupPort,
        command -> new FailedNotificationRetryOutcome(true, command.notificationId(), "notification_retry_created"),
        command -> new AITaskSupportReplayOutcome(new AITaskRunId(UUID.randomUUID()), false, "ai_replay_created"),
        reviewService(),
        workflowService(),
        auditPort);
  }

  private static SupportOperationsService service(
      RecordingSupportAuditPort auditPort,
      FailedNotificationRetryPort notificationRetryPort) {
    return new SupportOperationsService(
        new SupportOperationsPermissionPolicy(),
        new RecordingUserLookupPort(Optional.empty()),
        notificationRetryPort,
        command -> new AITaskSupportReplayOutcome(new AITaskRunId(UUID.randomUUID()), false, "ai_replay_created"),
        reviewService(),
        workflowService(),
        auditPort);
  }

  private static SupportOperationsService service(
      RecordingSupportAuditPort auditPort,
      AITaskSupportReplayPort aiTaskReplayPort) {
    return new SupportOperationsService(
        new SupportOperationsPermissionPolicy(),
        new RecordingUserLookupPort(Optional.empty()),
        command -> new FailedNotificationRetryOutcome(true, command.notificationId(), "notification_retry_created"),
        aiTaskReplayPort,
        reviewService(),
        workflowService(),
        auditPort);
  }

  private static SupportOperationsService service(
      RecordingSupportAuditPort auditPort,
      ReviewEventService reviewEventService,
      WorkflowEventService workflowEventService) {
    return new SupportOperationsService(
        new SupportOperationsPermissionPolicy(),
        new RecordingUserLookupPort(Optional.empty()),
        command -> new FailedNotificationRetryOutcome(true, command.notificationId(), "notification_retry_created"),
        command -> new AITaskSupportReplayOutcome(new AITaskRunId(UUID.randomUUID()), false, "ai_replay_created"),
        reviewEventService,
        workflowEventService,
        auditPort);
  }

  private static ReviewEventService reviewService() {
    return new ReviewEventService(new RecordingReviewEventPort());
  }

  private static WorkflowEventService workflowService() {
    return new WorkflowEventService(new RecordingWorkflowEventPort());
  }

  private static SupportActor admin(UUID organizationId) {
    return new SupportActor(organizationId, SUPPORT_ADMIN, PortalRole.ADMIN);
  }

  private static final class RecordingSupportAuditPort implements SupportActionAuditPort {
    private final List<SupportActionAuditCommand> commands = new ArrayList<>();

    @Override
    public UUID record(SupportActionAuditCommand command) {
      commands.add(command);
      return UUID.nameUUIDFromBytes(("audit-" + commands.size()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  private static final class RecordingUserLookupPort implements SupportUserLookupPort {
    private final Optional<SupportUserSummary> result;
    private final List<SupportUserLookupPortCommand> commands = new ArrayList<>();

    private RecordingUserLookupPort(Optional<SupportUserSummary> result) {
      this.result = result;
    }

    @Override
    public Optional<SupportUserSummary> findUser(SupportUserLookupPortCommand command) {
      commands.add(command);
      return result;
    }
  }

  private static final class RecordingNotificationRetryPort implements FailedNotificationRetryPort {
    private final FailedNotificationRetryOutcome outcome;
    private final List<FailedNotificationRetryRequest> commands = new ArrayList<>();

    private RecordingNotificationRetryPort(FailedNotificationRetryOutcome outcome) {
      this.outcome = outcome;
    }

    @Override
    public FailedNotificationRetryOutcome retry(FailedNotificationRetryRequest command) {
      commands.add(command);
      return outcome;
    }
  }

  private static final class RecordingAITaskReplayPort implements AITaskSupportReplayPort {
    private final AITaskSupportReplayOutcome outcome;
    private final List<AITaskSupportReplayRequest> commands = new ArrayList<>();

    private RecordingAITaskReplayPort(AITaskSupportReplayOutcome outcome) {
      this.outcome = outcome;
    }

    @Override
    public AITaskSupportReplayOutcome replay(AITaskSupportReplayRequest command) {
      commands.add(command);
      return outcome;
    }
  }

  private static final class RecordingReviewEventPort implements com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort {
    private final List<ReviewEventAppendCommand> commands = new ArrayList<>();

    @Override
    public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
      commands.add(command);
      return new ReviewEventAppendResult(new ReviewEventId(
          UUID.nameUUIDFromBytes(("review-" + commands.size()).getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
  }

  private static final class RecordingWorkflowEventPort implements com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return new WorkflowEventAppendResult(new WorkflowEventId(
          UUID.nameUUIDFromBytes(("workflow-" + commands.size()).getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    @Override
    public Optional<com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey idempotencyKey) {
      return Optional.empty();
    }
  }
}
