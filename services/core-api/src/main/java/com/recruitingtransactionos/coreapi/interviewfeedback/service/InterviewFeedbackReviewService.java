package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantInterviewFeedbackReviewResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantInterviewFeedbackSuggestionResponse;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionScope;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class InterviewFeedbackReviewService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final InterviewFeedbackSuggestionService suggestionService;
  private final CandidateCompanyInteractionService interactionService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  public InterviewFeedbackReviewService(
      InterviewFeedbackSuggestionService suggestionService,
      CandidateCompanyInteractionService interactionService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.suggestionService = suggestionService;
    this.interactionService = interactionService;
    this.workflowTransitionAuditService = workflowTransitionAuditService;
  }

  public ConsultantInterviewFeedbackSuggestionResponse getSuggestion(
      UUID organizationId,
      InterviewFeedbackSuggestionId suggestionId) {
    InterviewFeedbackSuggestion suggestion = suggestionService.findByIdAndOrganizationId(organizationId, suggestionId)
        .orElseThrow(InterviewFeedbackSuggestionNotFoundException::new);
    return toResponse(suggestion);
  }

  public ConsultantInterviewFeedbackReviewResponse review(
      UUID organizationId,
      UUID actorId,
      InterviewFeedbackSuggestionId suggestionId,
      String decision,
      String note) {
    InterviewFeedbackSuggestion suggestion = suggestionService.findByIdAndOrganizationId(organizationId, suggestionId)
        .orElseThrow(InterviewFeedbackSuggestionNotFoundException::new);
    if (suggestion.status() != InterviewFeedbackSuggestionStatus.PENDING_REVIEW) {
      throw new IllegalArgumentException("interview_feedback_suggestion_already_reviewed");
    }
    InterviewFeedbackSuggestionStatus targetStatus = switch (decision) {
      case "approve" -> InterviewFeedbackSuggestionStatus.APPROVED;
      case "reject" -> InterviewFeedbackSuggestionStatus.REJECTED;
      case "defer" -> InterviewFeedbackSuggestionStatus.DEFERRED;
      default -> throw new IllegalArgumentException("unsupported_review_decision");
    };
    Instant now = Instant.now();
    InterviewFeedbackSuggestion updated = suggestionService.update(new InterviewFeedbackSuggestion(
        suggestion.interviewFeedbackSuggestionId(),
        suggestion.organizationId(),
        suggestion.interviewFeedbackId(),
        suggestion.candidateCompanyInteractionId(),
        suggestion.jobId(),
        suggestion.candidateId(),
        suggestion.aiTaskRunId(),
        suggestion.scope(),
        suggestion.suggestionType(),
        targetStatus,
        suggestion.outcomeLabel(),
        suggestion.rejectReasonTaxonomy(),
        suggestion.title(),
        mergeRationale(suggestion.rationale(), note),
        suggestion.payload(),
        actorId,
        now,
        suggestion.createdAt(),
        now,
        suggestion.version() + 1));

    boolean interactionUpdated = false;
    if (targetStatus == InterviewFeedbackSuggestionStatus.APPROVED
        && updated.scope() == InterviewFeedbackSuggestionScope.INTERACTION) {
      CandidateCompanyInteraction interaction = interactionService.findInteractionByIdAndOrganizationId(
              organizationId, updated.candidateCompanyInteractionId())
          .orElseThrow(() -> new IllegalArgumentException("interaction_not_found_for_suggestion"));
      interactionService.updateInteraction(CandidateCompanyInteraction.builder()
          .candidateCompanyInteractionId(interaction.candidateCompanyInteractionId())
          .organizationId(interaction.organizationId())
          .candidateId(interaction.candidateId())
          .companyId(interaction.companyId())
          .jobId(interaction.jobId())
          .interactionType(interaction.interactionType())
          .status(interaction.status())
          .startedAt(interaction.startedAt())
          .endedAt(interaction.endedAt())
          .notes(interaction.notes())
          .metadata(mergeInteractionMetadata(interaction.metadata(), updated))
          .createdAt(interaction.createdAt())
          .updatedAt(now)
          .version(interaction.version() + 1)
          .build());
      interactionUpdated = true;
    }

    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(organizationId)
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.REVIEW_EVENT.wireValue())
        .entityId(updated.interviewFeedbackSuggestionId().value())
        .entityVersion(updated.version())
        .actionCode(WorkflowActionCode.REVIEW_EVENT_APPENDED.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.AI_ASSISTED)
        .beforeState(new WorkflowStateSnapshot(
            "{\"status\":\"" + suggestion.status().wireValue() + "\"}"))
        .afterState(new WorkflowStateSnapshot(
            "{\"status\":\"" + updated.status().wireValue() + "\"}"))
        .reason(decision + (note == null || note.isBlank() ? "" : ": " + note))
        .sourceType("consultant_api")
        .sourceRefId(updated.interviewFeedbackSuggestionId().value())
        .occurredAt(now)
        .build());

    return new ConsultantInterviewFeedbackReviewResponse(
        updated.interviewFeedbackSuggestionId().value().toString(),
        updated.status().wireValue(),
        now.toString(),
        updated.candidateCompanyInteractionId().value().toString(),
        updated.outcomeLabel() != null ? updated.outcomeLabel().wireValue() : null,
        updated.rejectReasonTaxonomy() != null ? updated.rejectReasonTaxonomy().wireValue() : null,
        interactionUpdated);
  }

  private static String mergeRationale(String rationale, String note) {
    if (note == null || note.isBlank()) {
      return rationale;
    }
    if (rationale == null || rationale.isBlank()) {
      return note;
    }
    return rationale + "\nConsultant note: " + note;
  }

  private static String mergeInteractionMetadata(String metadataRaw, InterviewFeedbackSuggestion suggestion) {
    Map<String, Object> metadata = readJsonMap(metadataRaw);
    metadata.put("interviewFeedbackReview", Map.of(
        "suggestionId", suggestion.interviewFeedbackSuggestionId().value().toString(),
        "status", suggestion.status().wireValue(),
        "outcomeLabel", suggestion.outcomeLabel() != null ? suggestion.outcomeLabel().wireValue() : null,
        "rejectReasonTaxonomy", suggestion.rejectReasonTaxonomy() != null ? suggestion.rejectReasonTaxonomy().wireValue() : null,
        "reviewedAt", suggestion.reviewedAt() != null ? suggestion.reviewedAt().toString() : null));
    return writeJson(metadata);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> readJsonMap(String raw) {
    if (raw == null || raw.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return OBJECT_MAPPER.readValue(raw, LinkedHashMap.class);
    } catch (JsonProcessingException exception) {
      return new LinkedHashMap<>();
    }
  }

  private static String writeJson(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize interaction metadata", exception);
    }
  }

  private static ConsultantInterviewFeedbackSuggestionResponse toResponse(InterviewFeedbackSuggestion suggestion) {
    return new ConsultantInterviewFeedbackSuggestionResponse(
        suggestion.interviewFeedbackSuggestionId().value().toString(),
        suggestion.interviewFeedbackId().value().toString(),
        suggestion.candidateCompanyInteractionId().value().toString(),
        suggestion.jobId().value().toString(),
        suggestion.candidateId() != null ? suggestion.candidateId().value().toString() : null,
        suggestion.scope().wireValue(),
        suggestion.suggestionType().wireValue(),
        suggestion.status().wireValue(),
        suggestion.outcomeLabel() != null ? suggestion.outcomeLabel().wireValue() : null,
        suggestion.rejectReasonTaxonomy() != null ? suggestion.rejectReasonTaxonomy().wireValue() : null,
        suggestion.title(),
        suggestion.rationale(),
        suggestion.payload(),
        suggestion.createdAt().toString(),
        suggestion.reviewedAt() != null ? suggestion.reviewedAt().toString() : null);
  }
}
