package com.recruitingtransactionos.coreapi.datalifecycle;

import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DataLifecycleModels {

  private DataLifecycleModels() {
  }

  public enum DataLifecycleEntityType {
    CANDIDATE(WorkflowEntityType.CANDIDATE),
    COMPANY(WorkflowEntityType.COMPANY),
    JOB(WorkflowEntityType.JOB);

    private final WorkflowEntityType workflowEntityType;

    DataLifecycleEntityType(WorkflowEntityType workflowEntityType) {
      this.workflowEntityType = workflowEntityType;
    }

    public WorkflowEntityType workflowEntityType() {
      return workflowEntityType;
    }
  }

  public enum DataLifecycleFieldStatus {
    AI_EXTRACTED,
    HUMAN_ACKNOWLEDGED,
    CONSULTANT_ATTESTED,
    CANDIDATE_CONFIRMED,
    EXTERNAL_VERIFIED,
    CONFLICTING,
    NEEDS_CONFIRMATION,
    STALE,
    UNVERIFIED,
    LIKELY_CURRENT;

    public boolean isConfirmedFact() {
      return this == CANDIDATE_CONFIRMED || this == EXTERNAL_VERIFIED;
    }
  }

  public record DataLifecycleEntitySnapshot(
      UUID organizationId,
      DataLifecycleEntityType entityType,
      UUID entityId,
      int entityVersion,
      Map<String, String> attributes,
      List<DataLifecycleFieldSnapshot> fields,
      Instant updatedAt) {

    public DataLifecycleEntitySnapshot {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(entityType, "entityType must not be null");
      Objects.requireNonNull(entityId, "entityId must not be null");
      if (entityVersion <= 0) {
        throw new IllegalArgumentException("entityVersion must be positive");
      }
      attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
      fields = List.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
      Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public String attribute(String key) {
      return attributes.get(key);
    }
  }

  public record DataLifecycleFieldSnapshot(
      String fieldPath,
      String value,
      DataLifecycleFieldStatus status,
      Instant lastUpdatedAt) {

    public DataLifecycleFieldSnapshot(String fieldPath, String value, DataLifecycleFieldStatus status) {
      this(fieldPath, value, status, null);
    }

    public DataLifecycleFieldSnapshot {
      fieldPath = requireNonBlank(fieldPath, "fieldPath");
      value = requireNonBlank(value, "value");
      Objects.requireNonNull(status, "status must not be null");
    }
  }

  public record DuplicateDetectionCommand(
      UUID organizationId,
      DataLifecycleEntitySnapshot incomingSnapshot,
      List<DataLifecycleEntitySnapshot> existingSnapshots,
      ActorRef actor,
      Instant occurredAt,
      String reason) {

    public DuplicateDetectionCommand {
      existingSnapshots = List.copyOf(
          Objects.requireNonNull(existingSnapshots, "existingSnapshots must not be null"));
    }

    public void validate() {
      validateCommandBase(organizationId, incomingSnapshot, actor, occurredAt, reason);
    }
  }

  public record DataLifecycleDuplicateMatch(
      UUID matchedEntityId,
      DuplicateConfidence confidence,
      String justification) {

    public DataLifecycleDuplicateMatch {
      Objects.requireNonNull(matchedEntityId, "matchedEntityId must not be null");
      Objects.requireNonNull(confidence, "confidence must not be null");
      justification = requireNonBlank(justification, "justification");
    }
  }

  public enum DuplicateConfidence {
    LOW,
    HIGH
  }

  public enum DuplicateDecision {
    NO_DUPLICATE,
    LOW_CONFIDENCE_WARNING,
    HIGH_CONFIDENCE_BLOCK
  }

  public record DuplicateDetectionResult(
      DuplicateDecision decision,
      Optional<DataLifecycleDuplicateMatch> match,
      WorkflowEventId workflowEventId) {

    public DuplicateDetectionResult {
      Objects.requireNonNull(decision, "decision must not be null");
      match = Objects.requireNonNull(match, "match must not be null");
    }
  }

  public record MergeProposalCommand(
      UUID organizationId,
      DataLifecycleEntitySnapshot sourceSnapshot,
      DataLifecycleEntitySnapshot targetSnapshot,
      ActorRef actor,
      Instant occurredAt,
      String reason) {

    public void validate() {
      validateCommandBase(organizationId, targetSnapshot, actor, occurredAt, reason);
      Objects.requireNonNull(sourceSnapshot, "sourceSnapshot must not be null");
      if (!sourceSnapshot.organizationId().equals(organizationId)) {
        throw new IllegalArgumentException("sourceSnapshot organizationId must match command");
      }
      if (sourceSnapshot.entityType() != targetSnapshot.entityType()) {
        throw new IllegalArgumentException("merge source and target entity types must match");
      }
    }
  }

  public enum MergeProposalStatus {
    PROPOSED,
    BLOCKED_CONFIRMED_FACT_CONFLICT
  }

  public record MergeFieldConflict(
      String fieldPath,
      String sourceValue,
      String confirmedTargetValue,
      DataLifecycleFieldStatus confirmedTargetStatus) {

    public MergeFieldConflict {
      fieldPath = requireNonBlank(fieldPath, "fieldPath");
      sourceValue = requireNonBlank(sourceValue, "sourceValue");
      confirmedTargetValue = requireNonBlank(confirmedTargetValue, "confirmedTargetValue");
      Objects.requireNonNull(confirmedTargetStatus, "confirmedTargetStatus must not be null");
    }
  }

  public record MergeProposalResult(
      MergeProposalStatus status,
      List<MergeFieldConflict> conflicts,
      WorkflowEventId workflowEventId) {

    public MergeProposalResult {
      Objects.requireNonNull(status, "status must not be null");
      conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts must not be null"));
      Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    }
  }

  public record StaleDetectionCommand(
      UUID organizationId,
      DataLifecycleEntitySnapshot snapshot,
      Instant staleBefore,
      Instant reviewBy,
      ActorRef actor,
      Instant occurredAt,
      String reason) {

    public void validate() {
      validateCommandBase(organizationId, snapshot, actor, occurredAt, reason);
      Objects.requireNonNull(staleBefore, "staleBefore must not be null");
      Objects.requireNonNull(reviewBy, "reviewBy must not be null");
      if (!reviewBy.isAfter(occurredAt)) {
        throw new IllegalArgumentException("reviewBy must be after occurredAt");
      }
    }
  }

  public record StaleFieldDecision(
      String fieldPath,
      String staleReason,
      Instant reviewBy) {

    public StaleFieldDecision {
      fieldPath = requireNonBlank(fieldPath, "fieldPath");
      staleReason = requireNonBlank(staleReason, "staleReason");
      Objects.requireNonNull(reviewBy, "reviewBy must not be null");
    }
  }

  public record StaleDetectionResult(
      List<StaleFieldDecision> staleFields,
      boolean refreshWorkflowRequired,
      WorkflowEventId workflowEventId) {

    public StaleDetectionResult {
      staleFields = List.copyOf(Objects.requireNonNull(staleFields, "staleFields must not be null"));
    }
  }

  public record ConflictResolutionCommand(
      UUID organizationId,
      DataLifecycleEntitySnapshot snapshot,
      String fieldPath,
      String resolutionSummary,
      ActorRef actor,
      Instant occurredAt,
      String reason) {

    public ConflictResolutionCommand {
      fieldPath = requireNonBlank(fieldPath, "fieldPath");
      resolutionSummary = requireNonBlank(resolutionSummary, "resolutionSummary");
    }

    public void validate() {
      validateCommandBase(organizationId, snapshot, actor, occurredAt, reason);
    }
  }

  public enum ConflictResolutionStatus {
    RECORDED_FOR_REVIEW
  }

  public record ConflictResolutionResult(
      ConflictResolutionStatus status,
      boolean canonicalMutationPerformed,
      WorkflowEventId workflowEventId) {

    public ConflictResolutionResult {
      Objects.requireNonNull(status, "status must not be null");
      Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    }
  }

  public record RetentionDeletionCommand(
      UUID organizationId,
      DataLifecycleEntitySnapshot snapshot,
      boolean preserveConfirmedFactTombstone,
      ActorRef actor,
      Instant occurredAt,
      String reason) {

    public void validate() {
      validateCommandBase(organizationId, snapshot, actor, occurredAt, reason);
    }
  }

  public enum RetentionDeletionStatus {
    BLOCKED_CONFIRMED_FACTS,
    SOFT_DELETE_EXECUTED
  }

  public record RetentionDeletionResult(
      RetentionDeletionStatus status,
      WorkflowEventId workflowEventId) {

    public RetentionDeletionResult {
      Objects.requireNonNull(status, "status must not be null");
      Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    }
  }

  private static void validateCommandBase(
      UUID organizationId,
      DataLifecycleEntitySnapshot snapshot,
      ActorRef actor,
      Instant occurredAt,
      String reason) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    if (!snapshot.organizationId().equals(organizationId)) {
      throw new IllegalArgumentException("snapshot organizationId must match command");
    }
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    requireNonBlank(reason, "reason");
  }

  private static String requireNonBlank(String value, String name) {
    if (value == null) {
      throw new NullPointerException(name + " must not be null");
    }
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return stripped;
  }
}
