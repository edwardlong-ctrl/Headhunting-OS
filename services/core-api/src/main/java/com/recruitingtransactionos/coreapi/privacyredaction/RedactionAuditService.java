package com.recruitingtransactionos.coreapi.privacyredaction;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeSummaryPipeline;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskDecision;
import com.recruitingtransactionos.coreapi.privacyredaction.port.ReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Task 30 redaction-and-audit orchestrator.
 *
 * <p>Given an internal candidate projection snapshot:
 * <ol>
 *   <li>Runs the {@link ClientSafeSummaryPipeline} to redact employer / chip
 *       / project / rare-title-year combinations.</li>
 *   <li>Runs the {@link ReidentificationRiskAssessmentService} on the
 *       redacted snapshot to produce a final
 *       {@link ReidentificationRiskAssessment} with risk score and
 *       decision.</li>
 *   <li>Persists the assessment via
 *       {@link ReidentificationRiskAssessmentPort} (Task 30 spec deliverable
 *       "ReidentificationRiskAssessment persistence").</li>
 *   <li>Emits one of:
 *       {@link WorkflowActionCode#REIDENTIFICATION_RISK_ASSESSED} when the
 *       projection is allowed, generalized, or queued for review; or
 *       {@link WorkflowActionCode#CLIENT_SAFE_REDACTION_BLOCKED} when the
 *       redaction pipeline cannot reduce risk below the BLOCK threshold
 *       (Task 30 spec deliverable "Redaction audit event").</li>
 * </ol>
 *
 * <p>The service is the single chokepoint for audit-trailed re-identification
 * decisions. Higher-level callers (the client-safe projection query port,
 * the shortlist builder pre-send check) call into this service rather than
 * the assessor directly so that every decision lands in
 * {@code privacy.reidentification_risk_assessment} and
 * {@code workflow.workflow_event}.
 */
public final class RedactionAuditService {

  private final ReidentificationRiskAssessmentService assessmentService;
  private final ReidentificationRiskAssessmentPort assessmentPort;
  private final WorkflowEventService workflowEventService;

  public RedactionAuditService(
      ReidentificationRiskAssessmentService assessmentService,
      ReidentificationRiskAssessmentPort assessmentPort,
      WorkflowEventService workflowEventService) {
    this.assessmentService = Objects.requireNonNull(
        assessmentService, "assessmentService must not be null");
    this.assessmentPort = Objects.requireNonNull(
        assessmentPort, "assessmentPort must not be null");
    this.workflowEventService = Objects.requireNonNull(
        workflowEventService, "workflowEventService must not be null");
  }

  /**
   * Result of {@link #evaluate(RedactionAuditRequest)}.
   *
   * <p>{@code persistedAssessment} is the just-written row.
   * {@code workflowEventId} is the audit event id.
   * {@code redactedSnapshot} is the post-pipeline snapshot the caller should
   * use to build the client-safe card. {@code blocked} is true when the
   * decision was BLOCK.
   */
  public record RedactionAuditResult(
      PersistedReidentificationRiskAssessment persistedAssessment,
      WorkflowEventId workflowEventId,
      InternalCandidateProjectionSnapshot redactedSnapshot,
      boolean blocked) {

    public RedactionAuditResult {
      Objects.requireNonNull(persistedAssessment, "persistedAssessment must not be null");
      Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
      Objects.requireNonNull(redactedSnapshot, "redactedSnapshot must not be null");
    }

    public ReidentificationRiskAssessment assessment() {
      return persistedAssessment.assessment();
    }
  }

  public RedactionAuditResult evaluate(RedactionAuditRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    ReidentificationRiskAssessmentService.PipelineResult pipelineResult =
        assessmentService.assessWithPipeline(request.snapshot());

    boolean blocked =
        pipelineResult.assessment().decision() == ReidentificationRiskDecision.BLOCK;

    PersistedReidentificationRiskAssessment toPersist =
        new PersistedReidentificationRiskAssessment(
            request.reidentificationRiskAssessmentRef(),
            request.organizationId(),
            request.candidateRef(),
            request.jobRef(),
            pipelineResult.assessment(),
            Optional.empty(),
            request.occurredAt());

    PersistedReidentificationRiskAssessment persisted =
        assessmentPort.append(toPersist);

    WorkflowEventAppendCommand command = buildAuditCommand(
        request,
        pipelineResult,
        blocked);
    WorkflowEventAppendResult appendResult = workflowEventService.append(command);
    WorkflowEventId workflowEventId = appendResult.workflowEventId();

    PersistedReidentificationRiskAssessment finalRecord =
        new PersistedReidentificationRiskAssessment(
            persisted.reidentificationRiskAssessmentRef(),
            persisted.organizationId(),
            persisted.candidateRef(),
            persisted.jobRef(),
            persisted.assessment(),
            Optional.of(workflowEventId),
            persisted.recordedAt());

    return new RedactionAuditResult(
        finalRecord,
        workflowEventId,
        pipelineResult.pipeline().redactedSnapshot(),
        blocked);
  }

  private WorkflowEventAppendCommand buildAuditCommand(
      RedactionAuditRequest request,
      ReidentificationRiskAssessmentService.PipelineResult pipelineResult,
      boolean blocked) {
    WorkflowActionCode actionCode = blocked
        ? WorkflowActionCode.CLIENT_SAFE_REDACTION_BLOCKED
        : WorkflowActionCode.REIDENTIFICATION_RISK_ASSESSED;

    UUID entityId = RedactionAuditWorkflowEntityIds.assessmentEntityId(
        request.organizationId(),
        request.reidentificationRiskAssessmentRef());

    String reasonOverride = blocked
        ? "Client-safe projection blocked: "
            + pipelineResult.assessment().explanation()
        : "Re-identification risk assessment recorded: "
            + pipelineResult.assessment().decision().wireValue()
            + " (risk_score=" + pipelineResult.assessment().riskScore() + ")";

    return new WorkflowEventAppendCommand(
        request.organizationId(),
        "privacy",
        new EntityRef(WorkflowEntityType.REIDENTIFICATION_ASSESSMENT.wireValue(), entityId),
        null,
        actionCode.wireValue(),
        new WorkflowStateSnapshot(buildBeforeState(request)),
        new WorkflowStateSnapshot(buildAfterState(request, pipelineResult, blocked)),
        new ActorRef(request.actorId(), request.actorRole()),
        request.sourceType(),
        null,
        null,
        null,
        request.reason() == null || request.reason().isBlank()
            ? reasonOverride
            : request.reason(),
        null,
        null,
        null,
        request.occurredAt());
  }

  private static String buildBeforeState(RedactionAuditRequest request) {
    return "{"
        + "\"boundary\":\"reidentification_risk_assessment\","
        + "\"status\":\"requested\","
        + "\"candidate_card_id\":\"" + escapeJson(request.snapshot().cardId().value()) + "\","
        + "\"redaction_level\":\""
        + request.snapshot().redactionLevel().wireValue() + "\""
        + "}";
  }

  private static String buildAfterState(
      RedactionAuditRequest request,
      ReidentificationRiskAssessmentService.PipelineResult pipelineResult,
      boolean blocked) {
    return "{"
        + "\"boundary\":\"reidentification_risk_assessment\","
        + "\"status\":\"" + (blocked ? "blocked" : "recorded") + "\","
        + "\"candidate_card_id\":\""
        + escapeJson(request.snapshot().cardId().value()) + "\","
        + "\"redaction_level\":\""
        + pipelineResult.assessment().redactionLevel().wireValue() + "\","
        + "\"risk_level\":\""
        + pipelineResult.assessment().riskLevel().wireValue() + "\","
        + "\"decision\":\""
        + pipelineResult.assessment().decision().wireValue() + "\","
        + "\"risk_score\":" + pipelineResult.assessment().riskScore() + ","
        + "\"unsafe_feature_count\":"
        + pipelineResult.assessment().unsafeFeatures().size()
        + "}";
  }

  private static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder result = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> result.append("\\\"");
        case '\\' -> result.append("\\\\");
        case '\b' -> result.append("\\b");
        case '\f' -> result.append("\\f");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        default -> result.append(character);
      }
    }
    return result.toString();
  }

  /**
   * Request payload for {@link #evaluate(RedactionAuditRequest)}.
   */
  public record RedactionAuditRequest(
      UUID organizationId,
      String reidentificationRiskAssessmentRef,
      String candidateRef,
      String jobRef,
      InternalCandidateProjectionSnapshot snapshot,
      UUID actorId,
      ActorRole actorRole,
      String sourceType,
      String reason,
      Instant occurredAt) {

    public RedactionAuditRequest {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(reidentificationRiskAssessmentRef,
          "reidentificationRiskAssessmentRef must not be null");
      if (reidentificationRiskAssessmentRef.isBlank()) {
        throw new IllegalArgumentException(
            "reidentificationRiskAssessmentRef must not be blank");
      }
      Objects.requireNonNull(snapshot, "snapshot must not be null");
      Objects.requireNonNull(actorId, "actorId must not be null");
      Objects.requireNonNull(actorRole, "actorRole must not be null");
      Objects.requireNonNull(sourceType, "sourceType must not be null");
      if (sourceType.isBlank()) {
        throw new IllegalArgumentException("sourceType must not be blank");
      }
      Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
  }
}
