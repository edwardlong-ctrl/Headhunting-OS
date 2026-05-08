package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityAITaskRunSearchResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityDisclosureAuditExportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityReviewEventSearchResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityWorkflowEventSearchResponse;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ObservabilityReadService {

  private static final java.util.regex.Pattern SAFE_REASON_CODE =
      java.util.regex.Pattern.compile("[A-Za-z0-9._:-]{1,80}");

  private final ObservabilityWorkflowEventReader workflowEventReader;
  private final ObservabilityReviewEventReader reviewEventReader;
  private final ObservabilityAITaskRunReader aiTaskRunReader;
  private final ObservabilityDisclosureRecordReader disclosureRecordReader;
  private final ObservabilityConsentRecordReader consentRecordReader;
  private final ObservabilityUnlockDecisionReader unlockDecisionReader;

  public ObservabilityReadService(
      ObservabilityWorkflowEventReader workflowEventReader,
      ObservabilityReviewEventReader reviewEventReader,
      ObservabilityAITaskRunReader aiTaskRunReader,
      ObservabilityDisclosureRecordReader disclosureRecordReader) {
    this(
        workflowEventReader,
        reviewEventReader,
        aiTaskRunReader,
        disclosureRecordReader,
        (organizationId, consentRecordRef) -> Optional.empty(),
        (organizationId, unlockDecisionRef) -> Optional.empty());
  }

  public ObservabilityReadService(
      ObservabilityWorkflowEventReader workflowEventReader,
      ObservabilityReviewEventReader reviewEventReader,
      ObservabilityAITaskRunReader aiTaskRunReader,
      ObservabilityDisclosureRecordReader disclosureRecordReader,
      ObservabilityConsentRecordReader consentRecordReader,
      ObservabilityUnlockDecisionReader unlockDecisionReader) {
    this.workflowEventReader = Objects.requireNonNull(workflowEventReader, "workflowEventReader must not be null");
    this.reviewEventReader = Objects.requireNonNull(reviewEventReader, "reviewEventReader must not be null");
    this.aiTaskRunReader = Objects.requireNonNull(aiTaskRunReader, "aiTaskRunReader must not be null");
    this.disclosureRecordReader = Objects.requireNonNull(
        disclosureRecordReader,
        "disclosureRecordReader must not be null");
    this.consentRecordReader = Objects.requireNonNull(consentRecordReader, "consentRecordReader must not be null");
    this.unlockDecisionReader = Objects.requireNonNull(unlockDecisionReader, "unlockDecisionReader must not be null");
  }

  public ObservabilityWorkflowEventSearchResponse searchWorkflowEvents(ObservabilityWorkflowEventQuery query) {
    validateLimit(query.limit(), query.offset());
    validateRange(query.occurredFrom(), query.occurredTo(), "occurredFrom", "occurredTo");
    List<ObservabilityWorkflowEventResponse> items = workflowEventReader.search(query)
        .stream()
        .map(ObservabilityReadService::workflowEventResponse)
        .toList();
    return new ObservabilityWorkflowEventSearchResponse(items, query.limit(), query.offset(), items.size() == query.limit());
  }

  public ObservabilityReviewEventSearchResponse searchReviewEvents(ObservabilityReviewEventQuery query) {
    validateLimit(query.limit(), query.offset());
    validateRange(query.createdFrom(), query.createdTo(), "createdFrom", "createdTo");
    List<ObservabilityReviewEventResponse> items = reviewEventReader.search(query)
        .stream()
        .map(ObservabilityReadService::reviewEventResponse)
        .toList();
    return new ObservabilityReviewEventSearchResponse(items, query.limit(), query.offset(), items.size() == query.limit());
  }

  public ObservabilityAITaskRunSearchResponse searchAiTaskRuns(ObservabilityAITaskRunQuery query) {
    validateLimit(query.limit(), query.offset());
    validateRange(query.startedFrom(), query.startedTo(), "startedFrom", "startedTo");
    List<ObservabilityAITaskRunResponse> items = aiTaskRunReader.search(query)
        .stream()
        .map(ObservabilityReadService::aiTaskRunResponse)
        .toList();
    return new ObservabilityAITaskRunSearchResponse(items, query.limit(), query.offset(), items.size() == query.limit());
  }

  public ObservabilityDisclosureAuditExportResponse disclosureAuditExport(
      ObservabilityDisclosureAuditExportQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    Optional<DisclosureRecord> disclosure = disclosureRecordReader.findByRef(
        query.organizationId(),
        query.disclosureRecordRef());
    if (disclosure.isEmpty()) {
      return new ObservabilityDisclosureAuditExportResponse(
          query.disclosureRecordRef(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          List.of("missing_disclosure_record"),
          List.of(),
          List.of(),
          List.of());
    }
    DisclosureRecord record = disclosure.orElseThrow();
    Optional<ConsentRecord> consentRecord = consentRecordReader.findByRef(
        query.organizationId(),
        record.consentRecordRef());
    Optional<UnlockDecision> unlockDecision = unlockDecisionReader.findByRef(
        query.organizationId(),
        record.unlockDecisionRef());
    java.util.UUID disclosureEntityId = ConsentDisclosureWorkflowEntityIds.disclosureEntityId(
        query.organizationId(),
        record.disclosureRecordRef());
    ObservabilityWorkflowEventQuery workflowQuery = new ObservabilityWorkflowEventQuery(
        query.organizationId(),
        null,
        "DISCLOSURE",
        disclosureEntityId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        50,
        0);
    List<ObservabilityWorkflowEventResponse> workflowEvents = workflowEventReader.search(workflowQuery)
        .stream()
        .map(ObservabilityReadService::workflowEventResponse)
        .toList();
    ObservabilityAITaskRunQuery aiQuery = new ObservabilityAITaskRunQuery(
        query.organizationId(),
        null,
        null,
        "DISCLOSURE",
        disclosureEntityId,
        null,
        null,
        null,
        null,
        50,
        0);
    List<ObservabilityAITaskRunResponse> aiTaskRuns = aiTaskRunReader.search(aiQuery)
        .stream()
        .map(ObservabilityReadService::aiTaskRunResponse)
        .toList();
    ObservabilityReviewEventQuery reviewQuery = new ObservabilityReviewEventQuery(
        query.organizationId(),
        "DISCLOSURE",
        disclosureEntityId,
        null,
        null,
        null,
        null,
        null,
        50,
        0);
    List<ObservabilityReviewEventResponse> reviewEvents = reviewEventReader.search(reviewQuery)
        .stream()
        .map(ObservabilityReadService::reviewEventResponse)
        .toList();
    return new ObservabilityDisclosureAuditExportResponse(
        record.disclosureRecordRef(),
        record.status().wireValue(),
        record.disclosureLevel().wireValue(),
        record.redactionLevel().wireValue(),
        record.candidateRef(),
        record.candidateProfileRef(),
        record.jobRef(),
        record.clientRef(),
        record.consentRecordRef(),
        record.unlockDecisionRef(),
        null,
        null,
        unlockDecision.map(decision -> decision.approvedBy().role().wireValue()).orElse(null),
        unlockDecision.map(decision -> decision.approvedBy().userId().toString()).orElse(null),
        consentRecord.map(recordedConsent -> recordedConsent.status().wireValue()).orElse(null),
        consentRecord.map(ConsentRecord::consentTextVersion).orElse(null),
        consentRecord.map(ConsentRecord::profileVersion).orElse(null),
        record.workflowEventId().map(id -> id.value().toString()).orElse(null),
        record.decidedAt().toString(),
        missingReasonCodes(record, consentRecord, unlockDecision, workflowEvents, aiTaskRuns, reviewEvents),
        workflowEvents,
        aiTaskRuns,
        reviewEvents);
  }

  private static ObservabilityWorkflowEventResponse workflowEventResponse(WorkflowAuditRecord record) {
    return new ObservabilityWorkflowEventResponse(
        record.workflowEventId().value().toString(),
        record.entityType(),
        record.entityId().toString(),
        record.actionCode(),
        record.actorType().wireValue(),
        record.actorId().toString(),
        record.aiInvolvement().wireValue(),
        record.riskTier().wireValue(),
        safeReasonCode(record.reason()),
        record.correlationId() == null ? null : record.correlationId().value().toString(),
        record.causationId() == null ? null : record.causationId().value().toString(),
        record.occurredAt().toString());
  }

  private static ObservabilityReviewEventResponse reviewEventResponse(ObservabilityReviewEventRecord record) {
    return new ObservabilityReviewEventResponse(
        record.reviewEventId().toString(),
        record.reviewerUserId().toString(),
        record.targetEntityType(),
        record.targetEntityId().toString(),
        record.fieldPath(),
        record.riskTier(),
        record.decision(),
        record.status(),
        record.claimLedgerItemId() == null ? null : record.claimLedgerItemId().toString(),
        record.sourceSpanRef(),
        safeReasonCode(record.reason()),
        record.createdAt().toString());
  }

  private static ObservabilityAITaskRunResponse aiTaskRunResponse(AITaskRunRecord record) {
    return new ObservabilityAITaskRunResponse(
        record.aiTaskRunId().value().toString(),
        record.taskName(),
        record.taskVersion(),
        record.inputSchemaVersion(),
        record.outputSchemaVersion(),
        record.promptVersion(),
        record.status().wireValue(),
        record.humanReviewStatus(),
        record.writeBackTarget() == null ? null : record.writeBackTarget().value(),
        record.model().provider(),
        record.model().name(),
        record.costUnits(),
        latencyMs(record),
        record.traceRef(),
        record.errorCode(),
        record.replayedFromAiTaskRunId() == null ? null : record.replayedFromAiTaskRunId().value().toString(),
        record.requestedBy() == null ? null : record.requestedBy().role().wireValue(),
        record.requestedBy() == null ? null : record.requestedBy().userId().toString(),
        record.targetEntity().entityType(),
        record.targetEntity().entityId() == null ? null : record.targetEntity().entityId().toString(),
        record.correlationId() == null ? null : record.correlationId().value().toString(),
        record.causationId() == null ? null : record.causationId().value().toString(),
        record.startedAt().toString(),
        record.completedAt() == null ? null : record.completedAt().toString());
  }

  private static Long latencyMs(AITaskRunRecord record) {
    if (record.completedAt() == null) {
      return null;
    }
    return Duration.between(record.startedAt(), record.completedAt()).toMillis();
  }

  private static List<String> missingReasonCodes(
      DisclosureRecord record,
      Optional<ConsentRecord> consentRecord,
      Optional<UnlockDecision> unlockDecision,
      List<ObservabilityWorkflowEventResponse> workflowEvents,
      List<ObservabilityAITaskRunResponse> aiTaskRuns,
      List<ObservabilityReviewEventResponse> reviewEvents) {
    java.util.ArrayList<String> reasons = new java.util.ArrayList<>();
    reasons.add("missing_requester_link");
    if (consentRecord.isEmpty()) {
      reasons.add("missing_consent_record");
    }
    if (unlockDecision.isEmpty()) {
      reasons.add("missing_unlock_decision");
    }
    if (record.workflowEventId().isEmpty()) {
      reasons.add("missing_disclosure_workflow_event_id");
    }
    if (workflowEvents.isEmpty()) {
      reasons.add("missing_related_workflow_events");
    }
    if (aiTaskRuns.isEmpty()) {
      reasons.add("missing_related_ai_task_runs");
    }
    if (reviewEvents.isEmpty()) {
      reasons.add("missing_related_review_events");
    }
    return List.copyOf(reasons);
  }

  private static String safeReasonCode(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    String stripped = reason.strip();
    return SAFE_REASON_CODE.matcher(stripped).matches() ? stripped : "reason_redacted";
  }

  private static void validateLimit(int limit, int offset) {
    if (limit <= 0 || limit > 100) {
      throw new IllegalArgumentException("limit must be between 1 and 100");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("offset must not be negative");
    }
  }

  private static void validateRange(
      java.time.Instant from,
      java.time.Instant to,
      String fromName,
      String toName) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException(fromName + " must be before or equal to " + toName);
    }
  }
}
