package com.recruitingtransactionos.coreapi.datalifecycle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.ConflictResolutionStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleDuplicateMatch;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntitySnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleFieldSnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleFieldStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateConfidence;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDecision;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeFieldConflict;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.MergeProposalStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.RetentionDeletionStatus;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleDetectionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleDetectionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.StaleFieldDecision;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DataLifecycleService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WorkflowEventService workflowEventService;

  public DataLifecycleService(WorkflowEventService workflowEventService) {
    this.workflowEventService = Objects.requireNonNull(
        workflowEventService,
        "workflowEventService must not be null");
  }

  public DuplicateDetectionResult evaluateDuplicates(DuplicateDetectionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    command.validate();

    Optional<DataLifecycleDuplicateMatch> match = command.existingSnapshots().stream()
        .filter(existing -> existing.entityType() == command.incomingSnapshot().entityType())
        .filter(existing -> existing.organizationId().equals(command.organizationId()))
        .map(existing -> match(command.incomingSnapshot(), existing))
        .flatMap(Optional::stream)
        .max(Comparator.comparing(DataLifecycleDuplicateMatch::confidence));

    DuplicateDecision decision = match.map(DataLifecycleService::duplicateDecision)
        .orElse(DuplicateDecision.NO_DUPLICATE);
    WorkflowEventId workflowEventId = null;
    if (decision != DuplicateDecision.NO_DUPLICATE) {
      WorkflowActionCode action = decision == DuplicateDecision.HIGH_CONFIDENCE_BLOCK
          ? WorkflowActionCode.DATA_DUPLICATE_BLOCKED
          : WorkflowActionCode.DATA_DUPLICATE_WARNING_RECORDED;
      workflowEventId = appendAudit(
          command.organizationId(),
          command.incomingSnapshot(),
          action,
          command.actor(),
          command.occurredAt(),
          command.reason(),
          jsonState(Map.of("duplicateDecision", "pending")),
          jsonState(Map.of(
              "duplicateDecision", decision.name().toLowerCase(Locale.ROOT),
              "matchedEntityId", match.orElseThrow().matchedEntityId().toString())));
    }
    return new DuplicateDetectionResult(decision, match, workflowEventId);
  }

  public MergeProposalResult proposeMerge(MergeProposalCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    command.validate();

    List<MergeFieldConflict> conflicts = confirmedFactConflicts(
        command.sourceSnapshot(),
        command.targetSnapshot());
    MergeProposalStatus status = conflicts.isEmpty()
        ? MergeProposalStatus.PROPOSED
        : MergeProposalStatus.BLOCKED_CONFIRMED_FACT_CONFLICT;
    WorkflowActionCode action = conflicts.isEmpty()
        ? WorkflowActionCode.DATA_MERGE_PROPOSED
        : WorkflowActionCode.DATA_MERGE_BLOCKED_CONFIRMED_FACT_CONFLICT;

    WorkflowEventId workflowEventId = appendAudit(
        command.organizationId(),
        command.targetSnapshot(),
        action,
        command.actor(),
        command.occurredAt(),
        command.reason(),
        jsonState(Map.of("mergeStatus", "not_reviewed")),
        jsonState(Map.of(
            "mergeStatus", status.name().toLowerCase(Locale.ROOT),
            "sourceEntityId", command.sourceSnapshot().entityId().toString(),
            "conflictCount", conflicts.size())));
    return new MergeProposalResult(status, conflicts, workflowEventId);
  }

  public StaleDetectionResult detectStaleFields(StaleDetectionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    command.validate();

    List<StaleFieldDecision> staleFields = command.snapshot().fields().stream()
        .filter(field -> field.lastUpdatedAt() != null)
        .filter(field -> field.lastUpdatedAt().isBefore(command.staleBefore()))
        .filter(field -> !field.status().isConfirmedFact())
        .map(field -> new StaleFieldDecision(
            field.fieldPath(),
            "field has not been refreshed since " + field.lastUpdatedAt(),
            command.reviewBy()))
        .toList();

    WorkflowEventId workflowEventId = null;
    if (!staleFields.isEmpty()) {
      workflowEventId = appendAudit(
          command.organizationId(),
          command.snapshot(),
          WorkflowActionCode.DATA_REFRESH_REQUESTED,
          command.actor(),
          command.occurredAt(),
          command.reason(),
          jsonState(Map.of("refreshWorkflow", "not_requested")),
          jsonState(Map.of(
              "refreshWorkflow", "requested",
              "staleFieldCount", staleFields.size(),
              "reviewBy", command.reviewBy().toString())));
    }
    return new StaleDetectionResult(staleFields, !staleFields.isEmpty(), workflowEventId);
  }

  public ConflictResolutionResult recordConflictResolution(ConflictResolutionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    command.validate();
    boolean fieldExists = command.snapshot().fields().stream()
        .anyMatch(field -> field.fieldPath().equals(command.fieldPath())
            && field.status() == DataLifecycleFieldStatus.CONFLICTING);
    if (!fieldExists) {
      throw new IllegalArgumentException(
          "conflict resolution requires a matching CONFLICTING field");
    }
    WorkflowEventId workflowEventId = appendAudit(
        command.organizationId(),
        command.snapshot(),
        WorkflowActionCode.DATA_CONFLICT_RESOLUTION_RECORDED,
        command.actor(),
        command.occurredAt(),
        command.reason(),
        jsonState(Map.of("conflictResolution", "not_recorded")),
        jsonState(Map.of(
            "conflictResolution", "recorded_for_review",
            "fieldPath", command.fieldPath(),
            "resolution", command.resolutionSummary(),
            "canonicalMutationPerformed", false)));
    return new ConflictResolutionResult(
        ConflictResolutionStatus.RECORDED_FOR_REVIEW,
        false,
        workflowEventId);
  }

  public RetentionDeletionResult executeRetentionDeletion(RetentionDeletionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    command.validate();

    long confirmedFactCount = command.snapshot().fields().stream()
        .filter(field -> field.status().isConfirmedFact())
        .count();
    boolean blocked = confirmedFactCount > 0 && !command.preserveConfirmedFactTombstone();
    RetentionDeletionStatus status = blocked
        ? RetentionDeletionStatus.BLOCKED_CONFIRMED_FACTS
        : RetentionDeletionStatus.SOFT_DELETE_EXECUTED;
    WorkflowActionCode action = blocked
        ? WorkflowActionCode.DATA_RETENTION_DELETION_BLOCKED
        : WorkflowActionCode.DATA_RETENTION_DELETION_EXECUTED;

    WorkflowEventId workflowEventId = appendAudit(
        command.organizationId(),
        command.snapshot(),
        action,
        command.actor(),
        command.occurredAt(),
        command.reason(),
        jsonState(Map.of("retentionDeletion", "not_executed")),
        jsonState(Map.of(
            "retentionDeletion", status.name().toLowerCase(Locale.ROOT),
            "confirmedFactCount", confirmedFactCount,
            "tombstonePreserved", command.preserveConfirmedFactTombstone())));
    return new RetentionDeletionResult(status, workflowEventId);
  }

  private WorkflowEventId appendAudit(
      UUID organizationId,
      DataLifecycleEntitySnapshot snapshot,
      WorkflowActionCode action,
      ActorRef actor,
      Instant occurredAt,
      String reason,
      String beforeState,
      String afterState) {
    return workflowEventService.append(new WorkflowEventAppendCommand(
        organizationId,
        "recruiting",
        new EntityRef(snapshot.entityType().workflowEntityType().wireValue(), snapshot.entityId()),
        snapshot.entityVersion(),
        action.wireValue(),
        new WorkflowStateSnapshot(beforeState),
        new WorkflowStateSnapshot(afterState),
        actor,
        "data_lifecycle_service",
        null,
        null,
        null,
        reason,
        new WorkflowIdempotencyKey(action.wireValue() + ":" + snapshot.entityId() + ":"
            + occurredAt.toString()),
        null,
        null,
        occurredAt)).workflowEventId();
  }

  private static Optional<DataLifecycleDuplicateMatch> match(
      DataLifecycleEntitySnapshot incoming,
      DataLifecycleEntitySnapshot existing) {
    return switch (incoming.entityType()) {
      case CANDIDATE -> candidateMatch(incoming, existing);
      case COMPANY -> companyMatch(incoming, existing);
      case JOB -> jobMatch(incoming, existing);
    };
  }

  private static Optional<DataLifecycleDuplicateMatch> candidateMatch(
      DataLifecycleEntitySnapshot incoming,
      DataLifecycleEntitySnapshot existing) {
    String incomingFingerprint = normalized(incoming.attribute("identityFingerprintHash"));
    String existingFingerprint = normalized(existing.attribute("identityFingerprintHash"));
    if (!incomingFingerprint.isBlank() && incomingFingerprint.equals(existingFingerprint)) {
      return Optional.of(new DataLifecycleDuplicateMatch(
          existing.entityId(),
          DuplicateConfidence.HIGH,
          "candidate identity fingerprint matched an existing candidate"));
    }
    return Optional.empty();
  }

  private static Optional<DataLifecycleDuplicateMatch> companyMatch(
      DataLifecycleEntitySnapshot incoming,
      DataLifecycleEntitySnapshot existing) {
    String incomingWebsite = normalizedWebsite(incoming.attribute("website"));
    String existingWebsite = normalizedWebsite(existing.attribute("website"));
    if (!incomingWebsite.isBlank() && incomingWebsite.equals(existingWebsite)) {
      return Optional.of(new DataLifecycleDuplicateMatch(
          existing.entityId(),
          DuplicateConfidence.HIGH,
          "normalized company website matched an existing company"));
    }
    String incomingName = normalized(incoming.attribute("name"));
    String existingName = normalized(existing.attribute("name"));
    if (!incomingName.isBlank() && incomingName.equals(existingName)) {
      return Optional.of(new DataLifecycleDuplicateMatch(
          existing.entityId(),
          DuplicateConfidence.LOW,
          "normalized company name matched but stronger identifiers differ or are missing"));
    }
    return Optional.empty();
  }

  private static Optional<DataLifecycleDuplicateMatch> jobMatch(
      DataLifecycleEntitySnapshot incoming,
      DataLifecycleEntitySnapshot existing) {
    String incomingCompanyId = normalized(incoming.attribute("companyId"));
    String existingCompanyId = normalized(existing.attribute("companyId"));
    boolean sameCompany = !incomingCompanyId.isBlank()
        && incomingCompanyId.equals(existingCompanyId);
    String incomingTitle = normalized(incoming.attribute("title"));
    String existingTitle = normalized(existing.attribute("title"));
    boolean sameTitle = !incomingTitle.isBlank() && incomingTitle.equals(existingTitle);
    if (sameCompany && sameTitle) {
      return Optional.of(new DataLifecycleDuplicateMatch(
          existing.entityId(),
          DuplicateConfidence.LOW,
          "same company and normalized job title matched; review active req details before merge"));
    }
    return Optional.empty();
  }

  private static DuplicateDecision duplicateDecision(DataLifecycleDuplicateMatch match) {
    return match.confidence() == DuplicateConfidence.HIGH
        ? DuplicateDecision.HIGH_CONFIDENCE_BLOCK
        : DuplicateDecision.LOW_CONFIDENCE_WARNING;
  }

  private static List<MergeFieldConflict> confirmedFactConflicts(
      DataLifecycleEntitySnapshot source,
      DataLifecycleEntitySnapshot target) {
    Map<String, DataLifecycleFieldSnapshot> targetFields = new HashMap<>();
    target.fields().forEach(field -> targetFields.put(field.fieldPath(), field));

    List<MergeFieldConflict> conflicts = new ArrayList<>();
    for (DataLifecycleFieldSnapshot sourceField : source.fields()) {
      DataLifecycleFieldSnapshot targetField = targetFields.get(sourceField.fieldPath());
      if (targetField == null || !targetField.status().isConfirmedFact()) {
        continue;
      }
      if (!Objects.equals(sourceField.value(), targetField.value())) {
        conflicts.add(new MergeFieldConflict(
            sourceField.fieldPath(),
            sourceField.value(),
            targetField.value(),
            targetField.status()));
      }
    }
    return List.copyOf(conflicts);
  }

  private static String normalized(String value) {
    return value == null
        ? ""
        : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static String normalizedWebsite(String value) {
    String normalized = normalized(value);
    return normalized
        .replaceFirst("^https?://", "")
        .replaceFirst("^www\\.", "")
        .replaceFirst("/+$", "");
  }

  private static String jsonState(Map<String, ?> state) {
    try {
      return OBJECT_MAPPER.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("failed_to_serialize_data_lifecycle_state", exception);
    }
  }
}
