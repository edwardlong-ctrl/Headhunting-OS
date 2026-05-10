package com.recruitingtransactionos.coreapi.supportops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SupportOperationsService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SupportOperationsPermissionPolicy permissionPolicy;
  private final SupportUserLookupPort userLookupPort;
  private final FailedNotificationRetryPort failedNotificationRetryPort;
  private final AITaskSupportReplayPort aiTaskSupportReplayPort;
  private final ReviewEventService reviewEventService;
  private final WorkflowEventService workflowEventService;
  private final SupportActionAuditPort supportActionAuditPort;
  private final SupportOperationsTransactionBoundary transactionBoundary;

  public SupportOperationsService(
      SupportOperationsPermissionPolicy permissionPolicy,
      SupportUserLookupPort userLookupPort,
      FailedNotificationRetryPort failedNotificationRetryPort,
      AITaskSupportReplayPort aiTaskSupportReplayPort,
      ReviewEventService reviewEventService,
      WorkflowEventService workflowEventService,
      SupportActionAuditPort supportActionAuditPort) {
    this(
        permissionPolicy,
        userLookupPort,
        failedNotificationRetryPort,
        aiTaskSupportReplayPort,
        reviewEventService,
        workflowEventService,
        supportActionAuditPort,
        new NoOpSupportOperationsTransactionBoundary());
  }

  public SupportOperationsService(
      SupportOperationsPermissionPolicy permissionPolicy,
      SupportUserLookupPort userLookupPort,
      FailedNotificationRetryPort failedNotificationRetryPort,
      AITaskSupportReplayPort aiTaskSupportReplayPort,
      ReviewEventService reviewEventService,
      WorkflowEventService workflowEventService,
      SupportActionAuditPort supportActionAuditPort,
      SupportOperationsTransactionBoundary transactionBoundary) {
    this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy must not be null");
    this.userLookupPort = Objects.requireNonNull(userLookupPort, "userLookupPort must not be null");
    this.failedNotificationRetryPort = Objects.requireNonNull(
        failedNotificationRetryPort, "failedNotificationRetryPort must not be null");
    this.aiTaskSupportReplayPort = Objects.requireNonNull(
        aiTaskSupportReplayPort, "aiTaskSupportReplayPort must not be null");
    this.reviewEventService = Objects.requireNonNull(reviewEventService, "reviewEventService must not be null");
    this.workflowEventService = Objects.requireNonNull(workflowEventService, "workflowEventService must not be null");
    this.supportActionAuditPort = Objects.requireNonNull(
        supportActionAuditPort, "supportActionAuditPort must not be null");
    this.transactionBoundary = Objects.requireNonNull(
        transactionBoundary, "transactionBoundary must not be null");
  }

  public SupportUserLookupResult lookupUser(SupportUserLookupCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return transactionBoundary.run(() -> lookupUserInTransaction(command));
  }

  private SupportUserLookupResult lookupUserInTransaction(SupportUserLookupCommand command) {
    SupportAuthorizationDecision decision = permissionPolicy.authorize(command);
    if (!decision.allowed()) {
      UUID auditId = audit(command, "denied", permissionMetadata(decision));
      return new SupportUserLookupResult(false, "support_action_denied", auditId, Optional.empty());
    }
    Optional<SupportUserSummary> user = userLookupPort.findUser(new SupportUserLookupPortCommand(
        command.actor().organizationId(),
        command.userAccountId()));
    String result = user.isPresent() ? "support_user_lookup_found" : "support_user_lookup_not_found";
    UUID auditId = audit(command, result, "{}");
    return new SupportUserLookupResult(true, result, auditId, user);
  }

  public SupportActionResult retryFailedNotification(FailedNotificationSupportRetryCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return transactionBoundary.run(() -> retryFailedNotificationInTransaction(command));
  }

  private SupportActionResult retryFailedNotificationInTransaction(
      FailedNotificationSupportRetryCommand command) {
    SupportAuthorizationDecision decision = permissionPolicy.authorize(command);
    if (!decision.allowed()) {
      UUID auditId = audit(command, "denied", permissionMetadata(decision));
      return deniedAction(auditId);
    }
    FailedNotificationRetryOutcome outcome = failedNotificationRetryPort.retry(new FailedNotificationRetryRequest(
        command.actor().organizationId(),
        command.notificationId(),
        command.actor().userId(),
        command.ticketRef(),
        command.reason()));
    UUID auditId = audit(command, outcome.resultCode(), "{\"duplicateSafe\":true}");
    return new SupportActionResult(true, outcome.resultCode(), auditId, Optional.of(outcome.notificationId()));
  }

  public SupportActionResult replayAiTask(AITaskSupportReplayCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return transactionBoundary.run(() -> replayAiTaskInTransaction(command));
  }

  private SupportActionResult replayAiTaskInTransaction(AITaskSupportReplayCommand command) {
    SupportAuthorizationDecision decision = permissionPolicy.authorize(command);
    if (!decision.allowed()) {
      UUID auditId = audit(command, "denied", permissionMetadata(decision));
      return deniedAction(auditId);
    }
    AITaskSupportReplayOutcome outcome;
    try {
      outcome = aiTaskSupportReplayPort.replay(new AITaskSupportReplayRequest(
          command.targetOrganizationId(),
          command.aiTaskRunId(),
          new ActorRef(command.actor().userId(), ActorRole.ADMIN),
          command.ticketRef(),
          command.reason()));
    } catch (IllegalArgumentException exception) {
      String resultCode = "ai_task_run_not_found".equals(exception.getMessage())
          ? "ai_replay_not_found"
          : "ai_replay_failed";
      UUID auditId = audit(command, resultCode, "{\"replayCompleted\":false}");
      return new SupportActionResult(false, resultCode, auditId, Optional.empty());
    } catch (RuntimeException exception) {
      UUID auditId = audit(command, "ai_replay_failed", "{\"replayCompleted\":false}");
      return new SupportActionResult(false, "ai_replay_failed", auditId, Optional.empty());
    }
    if (outcome.canonicalFactsWritten()) {
      UUID auditId = audit(command, "ai_replay_canonical_write_blocked", "{\"canonicalFactsWritten\":true}");
      return new SupportActionResult(false, "ai_replay_canonical_write_blocked", auditId, Optional.empty());
    }
    UUID auditId = audit(command, outcome.resultCode(), "{\"canonicalFactsWritten\":false}");
    return new SupportActionResult(true, outcome.resultCode(), auditId, Optional.of(outcome.replayedRunId().value()));
  }

  public DataCorrectionRequestResult requestDataCorrection(DataCorrectionSupportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return transactionBoundary.run(() -> requestDataCorrectionInTransaction(command));
  }

  private DataCorrectionRequestResult requestDataCorrectionInTransaction(
      DataCorrectionSupportCommand command) {
    SupportAuthorizationDecision decision = permissionPolicy.authorize(command);
    if (!decision.allowed()) {
      UUID auditId = audit(command, "denied", permissionMetadata(decision));
      return new DataCorrectionRequestResult(false, "support_action_denied", auditId, null, null, false);
    }

    ReviewEventAppendResult reviewResult = reviewEventService.append(new ReviewEventAppendCommand(
        command.targetOrganizationId(),
        command.actor().userId(),
        command.targetEntity(),
        command.targetFieldPath(),
        RiskTier.T3_HIGH_RISK,
        ReviewDecision.NEEDS_CONFIRMATION,
        false,
        "Support data correction request " + command.ticketRef() + ": " + command.correctionSummary(),
        Duration.ZERO,
        null,
        null));

    WorkflowEventAppendResult workflowResult = workflowEventService.append(new WorkflowEventAppendCommand(
        command.targetOrganizationId(),
        "governance",
        new EntityRef(WorkflowEntityType.REVIEW_EVENT.wireValue(), reviewResult.reviewEventId().value()),
        1,
        WorkflowActionCode.REVIEW_EVENT_APPENDED.wireValue(),
        correctionState(command, "absent"),
        correctionState(command, "requested"),
        new ActorRef(command.actor().userId(), ActorRole.ADMIN),
        "support_ops",
        command.targetEntity().entityId(),
        null,
        reviewResult.reviewEventId(),
        command.reason(),
        null,
        null,
        null,
        Instant.now()));

    UUID auditId = audit(
        command,
        "data_correction_request_created",
        correctionAuditMetadata(reviewResult, workflowResult));
    return new DataCorrectionRequestResult(
        true,
        "data_correction_request_created",
        auditId,
        reviewResult.reviewEventId(),
        workflowResult.workflowEventId(),
        false);
  }

  private UUID audit(SupportCommand command, String result, String metadataJson) {
    return supportActionAuditPort.record(new SupportActionAuditCommand(
        command.actor().organizationId(),
        command.actor().userId(),
        command.actor().role(),
        command.targetType(),
        command.targetId(),
        command.actionType(),
        command.ticketRef(),
        command.reason(),
        result,
        metadataJson));
  }

  private static SupportActionResult deniedAction(UUID auditId) {
    return new SupportActionResult(false, "support_action_denied", auditId, Optional.empty());
  }

  private static String permissionMetadata(SupportAuthorizationDecision decision) {
    return "{\"policyReason\":\"" + decision.reasonCode() + "\"}";
  }

  private static WorkflowStateSnapshot correctionState(
      DataCorrectionSupportCommand command,
      String status) {
    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    node.put("status", status);
    node.put("targetEntityType", command.targetEntity().entityType());
    node.put("targetEntityId", command.targetEntity().entityId().toString());
    node.put("targetFieldPath", command.targetFieldPath());
    node.put("ticketRef", command.ticketRef());
    return new WorkflowStateSnapshot(node.toString());
  }

  private static String correctionAuditMetadata(
      ReviewEventAppendResult reviewResult,
      WorkflowEventAppendResult workflowResult) {
    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    node.put("canonicalFactsMutated", false);
    node.put("reviewEventId", reviewResult.reviewEventId().value().toString());
    node.put("workflowEventId", workflowResult.workflowEventId().value().toString());
    return node.toString();
  }
}
