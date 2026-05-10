package com.recruitingtransactionos.coreapi.supportops;

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

  private final SupportOperationsPermissionPolicy permissionPolicy;
  private final SupportUserLookupPort userLookupPort;
  private final FailedNotificationRetryPort failedNotificationRetryPort;
  private final AITaskSupportReplayPort aiTaskSupportReplayPort;
  private final ReviewEventService reviewEventService;
  private final WorkflowEventService workflowEventService;
  private final SupportActionAuditPort supportActionAuditPort;

  public SupportOperationsService(
      SupportOperationsPermissionPolicy permissionPolicy,
      SupportUserLookupPort userLookupPort,
      FailedNotificationRetryPort failedNotificationRetryPort,
      AITaskSupportReplayPort aiTaskSupportReplayPort,
      ReviewEventService reviewEventService,
      WorkflowEventService workflowEventService,
      SupportActionAuditPort supportActionAuditPort) {
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
  }

  public SupportUserLookupResult lookupUser(SupportUserLookupCommand command) {
    Objects.requireNonNull(command, "command must not be null");
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
    SupportAuthorizationDecision decision = permissionPolicy.authorize(command);
    if (!decision.allowed()) {
      UUID auditId = audit(command, "denied", permissionMetadata(decision));
      return deniedAction(auditId);
    }
    AITaskSupportReplayOutcome outcome = aiTaskSupportReplayPort.replay(new AITaskSupportReplayRequest(
        command.targetOrganizationId(),
        command.aiTaskRunId(),
        new ActorRef(command.actor().userId(), ActorRole.ADMIN),
        command.ticketRef(),
        command.reason()));
    if (outcome.canonicalFactsWritten()) {
      UUID auditId = audit(command, "ai_replay_canonical_write_blocked", "{\"canonicalFactsWritten\":true}");
      return new SupportActionResult(false, "ai_replay_canonical_write_blocked", auditId, Optional.empty());
    }
    UUID auditId = audit(command, outcome.resultCode(), "{\"canonicalFactsWritten\":false}");
    return new SupportActionResult(true, outcome.resultCode(), auditId, Optional.of(outcome.replayedRunId().value()));
  }

  public DataCorrectionRequestResult requestDataCorrection(DataCorrectionSupportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
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
        "{\"canonicalFactsMutated\":false,\"reviewEventId\":\""
            + reviewResult.reviewEventId().value()
            + "\",\"workflowEventId\":\""
            + workflowResult.workflowEventId().value()
            + "\"}");
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
    return new WorkflowStateSnapshot("""
        {"status":"%s","targetEntityType":"%s","targetEntityId":"%s","targetFieldPath":"%s","ticketRef":"%s"}
        """.formatted(
            status,
            command.targetEntity().entityType(),
            command.targetEntity().entityId(),
            command.targetFieldPath(),
            command.ticketRef()).strip());
  }
}
