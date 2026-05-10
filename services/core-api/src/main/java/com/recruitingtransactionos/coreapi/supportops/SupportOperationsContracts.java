package com.recruitingtransactionos.coreapi.supportops;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

enum SupportActionType {
  LOOKUP_USER("lookup_user"),
  RETRY_FAILED_NOTIFICATION("retry_failed_notification"),
  REPLAY_AI_TASK("replay_ai_task"),
  REQUEST_DATA_CORRECTION("request_data_correction");

  private final String wireValue;

  SupportActionType(String wireValue) {
    this.wireValue = wireValue;
  }

  String wireValue() {
    return wireValue;
  }
}

enum SupportTargetType {
  USER_ACCOUNT("user_account"),
  NOTIFICATION("notification"),
  AI_TASK_RUN("ai_task_run"),
  DATA_CORRECTION_REQUEST("data_correction_request");

  private final String wireValue;

  SupportTargetType(String wireValue) {
    this.wireValue = wireValue;
  }

  String wireValue() {
    return wireValue;
  }
}

record SupportActor(
    UUID organizationId,
    UUID userId,
    PortalRole role) {

  SupportActor {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(role, "role must not be null");
  }
}

record SupportUserLookupCommand(
    SupportActor actor,
    UUID targetOrganizationId,
    UUID userAccountId,
    String ticketRef,
    String reason) implements SupportCommand {

  SupportUserLookupCommand {
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(targetOrganizationId, "targetOrganizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    ticketRef = SupportOpsGuards.normalize(ticketRef);
    reason = SupportOpsGuards.normalize(reason);
  }

  @Override
  public UUID targetId() {
    return userAccountId;
  }

  @Override
  public SupportActionType actionType() {
    return SupportActionType.LOOKUP_USER;
  }

  @Override
  public SupportTargetType targetType() {
    return SupportTargetType.USER_ACCOUNT;
  }
}

record FailedNotificationSupportRetryCommand(
    SupportActor actor,
    UUID targetOrganizationId,
    UUID notificationId,
    String ticketRef,
    String reason) implements SupportCommand {

  FailedNotificationSupportRetryCommand {
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(targetOrganizationId, "targetOrganizationId must not be null");
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    ticketRef = SupportOpsGuards.normalize(ticketRef);
    reason = SupportOpsGuards.normalize(reason);
  }

  @Override
  public UUID targetId() {
    return notificationId;
  }

  @Override
  public SupportActionType actionType() {
    return SupportActionType.RETRY_FAILED_NOTIFICATION;
  }

  @Override
  public SupportTargetType targetType() {
    return SupportTargetType.NOTIFICATION;
  }
}

record AITaskSupportReplayCommand(
    SupportActor actor,
    UUID targetOrganizationId,
    UUID aiTaskRunId,
    String ticketRef,
    String reason) implements SupportCommand {

  AITaskSupportReplayCommand {
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(targetOrganizationId, "targetOrganizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    ticketRef = SupportOpsGuards.normalize(ticketRef);
    reason = SupportOpsGuards.normalize(reason);
  }

  @Override
  public UUID targetId() {
    return aiTaskRunId;
  }

  @Override
  public SupportActionType actionType() {
    return SupportActionType.REPLAY_AI_TASK;
  }

  @Override
  public SupportTargetType targetType() {
    return SupportTargetType.AI_TASK_RUN;
  }
}

record DataCorrectionSupportCommand(
    SupportActor actor,
    UUID targetOrganizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    String correctionSummary,
    String ticketRef,
    String reason) implements SupportCommand {

  DataCorrectionSupportCommand {
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(targetOrganizationId, "targetOrganizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = SupportOpsGuards.requireNonBlank(targetFieldPath, "targetFieldPath");
    correctionSummary = SupportOpsGuards.requireNonBlank(correctionSummary, "correctionSummary");
    ticketRef = SupportOpsGuards.normalize(ticketRef);
    reason = SupportOpsGuards.normalize(reason);
  }

  @Override
  public UUID targetId() {
    return targetEntity.entityId();
  }

  @Override
  public SupportActionType actionType() {
    return SupportActionType.REQUEST_DATA_CORRECTION;
  }

  @Override
  public SupportTargetType targetType() {
    return SupportTargetType.DATA_CORRECTION_REQUEST;
  }
}

sealed interface SupportCommand permits
    SupportUserLookupCommand,
    FailedNotificationSupportRetryCommand,
    AITaskSupportReplayCommand,
    DataCorrectionSupportCommand {

  SupportActor actor();

  UUID targetOrganizationId();

  UUID targetId();

  String ticketRef();

  String reason();

  SupportActionType actionType();

  SupportTargetType targetType();
}

record SupportActionResult(
    boolean allowed,
    String resultCode,
    UUID auditId,
    Optional<UUID> targetId) {

  SupportActionResult {
    resultCode = SupportOpsGuards.requireNonBlank(resultCode, "resultCode");
    Objects.requireNonNull(targetId, "targetId must not be null");
  }
}

record SupportUserLookupResult(
    boolean allowed,
    String resultCode,
    UUID auditId,
    Optional<SupportUserSummary> user) {

  SupportUserLookupResult {
    resultCode = SupportOpsGuards.requireNonBlank(resultCode, "resultCode");
    Objects.requireNonNull(user, "user must not be null");
  }
}

record DataCorrectionRequestResult(
    boolean allowed,
    String resultCode,
    UUID auditId,
    ReviewEventId reviewEventId,
    WorkflowEventId workflowEventId,
    boolean canonicalFactsMutated) {

  DataCorrectionRequestResult {
    resultCode = SupportOpsGuards.requireNonBlank(resultCode, "resultCode");
  }
}

record SupportUserSummary(
    UUID userAccountId,
    UUID organizationId,
    String email,
    String displayName,
    String status,
    List<PortalRole> roles) {

  SupportUserSummary {
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    email = SupportOpsGuards.requireNonBlank(email, "email");
    displayName = SupportOpsGuards.requireNonBlank(displayName, "displayName");
    status = SupportOpsGuards.requireNonBlank(status, "status");
    roles = List.copyOf(roles == null ? List.of() : roles);
  }
}

record SupportUserLookupPortCommand(
    UUID organizationId,
    UUID userAccountId) {

  SupportUserLookupPortCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
  }
}

interface SupportUserLookupPort {
  Optional<SupportUserSummary> findUser(SupportUserLookupPortCommand command);
}

record FailedNotificationRetryRequest(
    UUID organizationId,
    UUID notificationId,
    UUID supportActorId,
    String ticketRef,
    String reason) {

  FailedNotificationRetryRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    Objects.requireNonNull(supportActorId, "supportActorId must not be null");
    ticketRef = SupportOpsGuards.requireNonBlank(ticketRef, "ticketRef");
    reason = SupportOpsGuards.requireNonBlank(reason, "reason");
  }
}

record FailedNotificationRetryOutcome(
    boolean retryCreated,
    UUID notificationId,
    String resultCode) {

  FailedNotificationRetryOutcome {
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    resultCode = SupportOpsGuards.requireNonBlank(resultCode, "resultCode");
  }
}

interface FailedNotificationRetryPort {
  FailedNotificationRetryOutcome retry(FailedNotificationRetryRequest command);
}

record AITaskSupportReplayRequest(
    UUID organizationId,
    UUID aiTaskRunId,
    ActorRef supportActor,
    String ticketRef,
    String reason) {

  AITaskSupportReplayRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(aiTaskRunId, "aiTaskRunId must not be null");
    Objects.requireNonNull(supportActor, "supportActor must not be null");
    ticketRef = SupportOpsGuards.requireNonBlank(ticketRef, "ticketRef");
    reason = SupportOpsGuards.requireNonBlank(reason, "reason");
  }
}

record AITaskSupportReplayOutcome(
    AITaskRunId replayedRunId,
    boolean canonicalFactsWritten,
    String resultCode) {

  AITaskSupportReplayOutcome {
    Objects.requireNonNull(replayedRunId, "replayedRunId must not be null");
    resultCode = SupportOpsGuards.requireNonBlank(resultCode, "resultCode");
  }
}

interface AITaskSupportReplayPort {
  AITaskSupportReplayOutcome replay(AITaskSupportReplayRequest command);
}

record SupportActionAuditCommand(
    UUID organizationId,
    UUID supportActorId,
    PortalRole supportActorRole,
    SupportTargetType targetType,
    UUID targetId,
    SupportActionType actionType,
    String ticketRef,
    String reason,
    String result,
    String metadataJson) {

  SupportActionAuditCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(supportActorId, "supportActorId must not be null");
    Objects.requireNonNull(supportActorRole, "supportActorRole must not be null");
    Objects.requireNonNull(targetType, "targetType must not be null");
    Objects.requireNonNull(targetId, "targetId must not be null");
    Objects.requireNonNull(actionType, "actionType must not be null");
    ticketRef = SupportOpsGuards.normalize(ticketRef);
    reason = SupportOpsGuards.normalize(reason);
    result = SupportOpsGuards.requireNonBlank(result, "result");
    metadataJson = metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson.strip();
  }
}

interface SupportActionAuditPort {
  UUID record(SupportActionAuditCommand command);
}

record SupportAuthorizationDecision(
    boolean allowed,
    String reasonCode) {

  SupportAuthorizationDecision {
    reasonCode = SupportOpsGuards.requireNonBlank(reasonCode, "reasonCode");
  }

  static SupportAuthorizationDecision allow() {
    return new SupportAuthorizationDecision(true, "support_action_allowed");
  }

  static SupportAuthorizationDecision deny(String reasonCode) {
    return new SupportAuthorizationDecision(false, reasonCode);
  }
}

final class SupportOpsGuards {
  private SupportOpsGuards() {}

  static String normalize(String value) {
    return value == null ? "" : value.strip();
  }

  static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
