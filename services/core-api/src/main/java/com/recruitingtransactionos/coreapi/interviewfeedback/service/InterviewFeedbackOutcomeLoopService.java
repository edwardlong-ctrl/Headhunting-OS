package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerOutput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerTaskService;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionScope;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionStatus;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionType;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcomeLabel;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignal;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignalId;
import com.recruitingtransactionos.coreapi.interviewfeedback.RejectReasonTaxonomy;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class InterviewFeedbackOutcomeLoopService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final InterviewFeedbackService interviewFeedbackService;
  private final InterviewFeedbackSuggestionService suggestionService;
  private final MatchCalibrationSignalService matchCalibrationSignalService;
  private final InterviewFeedbackStructurerTaskService structurerTaskService;

  public InterviewFeedbackOutcomeLoopService(
      InterviewFeedbackService interviewFeedbackService,
      InterviewFeedbackSuggestionService suggestionService,
      MatchCalibrationSignalService matchCalibrationSignalService,
      InterviewFeedbackStructurerTaskService structurerTaskService) {
    this.interviewFeedbackService = interviewFeedbackService;
    this.suggestionService = suggestionService;
    this.matchCalibrationSignalService = matchCalibrationSignalService;
    this.structurerTaskService = structurerTaskService;
  }

  public InterviewFeedbackOutcomeLoopResult processSubmission(
      UUID organizationId,
      UUID actorId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      Job job,
      CandidateCompanyInteraction interaction,
      InterviewFeedback feedback,
      String scorecardJson) {
    try {
      InterviewFeedbackStructurerResult result = structurerTaskService.execute(
          organizationId,
          new ActorRef(actorId, ActorRole.CLIENT),
          new EntityRef("interview_feedback", feedback.interviewFeedbackId().value()),
          List.of(feedback.interviewFeedbackId().value(), interaction.candidateCompanyInteractionId().value()),
          buildInput(shortlist, card, interaction, feedback, scorecardJson),
          new WorkflowCorrelationId(feedback.interviewFeedbackId().value()),
          new WorkflowCausationId(feedback.interviewFeedbackId().value()));

      InterviewFeedback updatedFeedback = interviewFeedbackService.updateFeedback(InterviewFeedback.builder()
          .interviewFeedbackId(feedback.interviewFeedbackId())
          .organizationId(feedback.organizationId())
          .candidateCompanyInteractionId(feedback.candidateCompanyInteractionId())
          .jobId(feedback.jobId())
          .interviewerName(feedback.interviewerName())
          .interviewerRole(feedback.interviewerRole())
          .interviewRound(feedback.interviewRound())
          .interviewDate(feedback.interviewDate())
          .outcome(feedback.outcome())
          .decision(feedback.decision())
          .rejectReasonTaxonomy(feedback.rejectReasonTaxonomy())
          .ratings(feedback.ratings())
          .ratingsSchemaVersion(feedback.ratingsSchemaVersion())
          .strengths(feedback.strengths())
          .concerns(feedback.concerns())
          .notes(feedback.notes())
          .submittedByRole(feedback.submittedByRole())
          .submittedByUserId(feedback.submittedByUserId())
          .aiTaskRunId(result.execution().runRecord().aiTaskRunId().value())
          .metadata(mergeFeedbackMetadata(feedback.metadata(), result.output()))
          .createdAt(feedback.createdAt())
          .updatedAt(Instant.now())
          .version(feedback.version() + 1)
          .build());

      List<InterviewFeedbackSuggestion> suggestions = persistSuggestions(result, updatedFeedback, interaction, job);
      List<MatchCalibrationSignal> signals = persistCalibrationSignal(result, updatedFeedback, interaction, job);
      return new InterviewFeedbackOutcomeLoopResult(
          updatedFeedback,
          suggestions,
          signals,
          result.output().structuredSummary(),
          true);
    } catch (RuntimeException exception) {
      InterviewFeedback updatedFeedback = interviewFeedbackService.updateFeedback(InterviewFeedback.builder()
          .interviewFeedbackId(feedback.interviewFeedbackId())
          .organizationId(feedback.organizationId())
          .candidateCompanyInteractionId(feedback.candidateCompanyInteractionId())
          .jobId(feedback.jobId())
          .interviewerName(feedback.interviewerName())
          .interviewerRole(feedback.interviewerRole())
          .interviewRound(feedback.interviewRound())
          .interviewDate(feedback.interviewDate())
          .outcome(feedback.outcome())
          .decision(feedback.decision())
          .rejectReasonTaxonomy(feedback.rejectReasonTaxonomy())
          .ratings(feedback.ratings())
          .ratingsSchemaVersion(feedback.ratingsSchemaVersion())
          .strengths(feedback.strengths())
          .concerns(feedback.concerns())
          .notes(feedback.notes())
          .submittedByRole(feedback.submittedByRole())
          .submittedByUserId(feedback.submittedByUserId())
          .metadata(mergeFailureMetadata(feedback.metadata(), exception.getMessage()))
          .createdAt(feedback.createdAt())
          .updatedAt(Instant.now())
          .version(feedback.version() + 1)
          .build());
      return new InterviewFeedbackOutcomeLoopResult(updatedFeedback, List.of(), List.of(), null, false);
    }
  }

  private InterviewFeedbackStructurerInput buildInput(
      Shortlist shortlist,
      ShortlistCandidateCard card,
      CandidateCompanyInteraction interaction,
      InterviewFeedback feedback,
      String scorecardJson) {
    return new InterviewFeedbackStructurerInput(
        shortlist.shortlistId().value().toString(),
        card.shortlistCandidateCardId().value().toString(),
        interaction.candidateCompanyInteractionId().value().toString(),
        feedback.jobId().value().toString(),
        interaction.candidateId().value().toString(),
        feedback.interviewFeedbackId().value().toString(),
        feedback.decision() != null ? feedback.decision().wireValue() : null,
        feedback.outcome().wireValue(),
        feedback.rejectReasonTaxonomy() != null ? feedback.rejectReasonTaxonomy().wireValue() : null,
        feedback.interviewerName(),
        feedback.interviewerRole(),
        feedback.interviewRound(),
        feedback.interviewDate() != null ? feedback.interviewDate().toString() : null,
        feedback.ratings(),
        feedback.strengths(),
        feedback.concerns(),
        feedback.notes(),
        scorecardJson,
        Stream.of(nonBlank(feedback.strengths()), nonBlank(feedback.concerns()), nonBlank(feedback.notes()))
            .filter(s -> s != null && !s.isBlank())
            .toList());
  }

  private List<InterviewFeedbackSuggestion> persistSuggestions(
      InterviewFeedbackStructurerResult result,
      InterviewFeedback feedback,
      CandidateCompanyInteraction interaction,
      Job job) {
    List<InterviewFeedbackSuggestion> saved = new ArrayList<>();
    for (InterviewFeedbackStructurerOutput.Suggestion suggestion : result.suggestions()) {
      InterviewFeedbackSuggestion savedSuggestion = suggestionService.create(new InterviewFeedbackSuggestion(
          new InterviewFeedbackSuggestionId(UUID.randomUUID()),
          feedback.organizationId(),
          feedback.interviewFeedbackId(),
          interaction.candidateCompanyInteractionId(),
          job.jobId(),
          interaction.candidateId(),
          result.execution().runRecord().aiTaskRunId().value(),
          InterviewFeedbackSuggestionScope.fromWireValue(suggestion.scope()),
          InterviewFeedbackSuggestionType.fromWireValue(suggestion.suggestionType()),
          InterviewFeedbackSuggestionStatus.PENDING_REVIEW,
          suggestion.outcomeLabel() != null ? InterviewOutcomeLabel.fromWireValue(suggestion.outcomeLabel()) : null,
          suggestion.rejectReasonTaxonomy() != null ? RejectReasonTaxonomy.fromWireValue(suggestion.rejectReasonTaxonomy()) : null,
          suggestion.title(),
          suggestion.rationale(),
          writeJson(suggestion.payload()),
          null,
          null,
          Instant.now(),
          Instant.now(),
          1));
      saved.add(savedSuggestion);
    }
    if (saved.stream().noneMatch(s -> s.scope() == InterviewFeedbackSuggestionScope.INTERACTION)) {
      saved.add(suggestionService.create(new InterviewFeedbackSuggestion(
          new InterviewFeedbackSuggestionId(UUID.randomUUID()),
          feedback.organizationId(),
          feedback.interviewFeedbackId(),
          interaction.candidateCompanyInteractionId(),
          job.jobId(),
          interaction.candidateId(),
          result.execution().runRecord().aiTaskRunId().value(),
          InterviewFeedbackSuggestionScope.INTERACTION,
          InterviewFeedbackSuggestionType.OUTCOME_LABEL,
          InterviewFeedbackSuggestionStatus.PENDING_REVIEW,
          InterviewOutcomeLabel.fromWireValue(result.output().outcomeLabel()),
          result.output().rejectReasonTaxonomy() != null ? RejectReasonTaxonomy.fromWireValue(result.output().rejectReasonTaxonomy()) : null,
          "Interview outcome recommendation",
          result.output().structuredSummary(),
          writeJson(fallbackSuggestionPayload(result.output())),
          null,
          null,
          Instant.now(),
          Instant.now(),
          1)));
    }
    return List.copyOf(saved);
  }

  private List<MatchCalibrationSignal> persistCalibrationSignal(
      InterviewFeedbackStructurerResult result,
      InterviewFeedback feedback,
      CandidateCompanyInteraction interaction,
      Job job) {
    MatchCalibrationSignal signal = matchCalibrationSignalService.create(new MatchCalibrationSignal(
        new MatchCalibrationSignalId(UUID.randomUUID()),
        feedback.organizationId(),
        feedback.interviewFeedbackId(),
        interaction.candidateCompanyInteractionId(),
        job.jobId(),
        interaction.candidateId(),
        job.industryPackId() != null ? job.industryPackId().toString() : null,
        feedback.decision(),
        InterviewOutcomeLabel.fromWireValue(result.output().outcomeLabel()),
        result.output().rejectReasonTaxonomy() != null ? RejectReasonTaxonomy.fromWireValue(result.output().rejectReasonTaxonomy()) : null,
        result.output().confidence(),
        writeJson(result.output().calibrationSignal()),
        Instant.now(),
        1));
    return List.of(signal);
  }

  private static String nonBlank(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String mergeFeedbackMetadata(String current, InterviewFeedbackStructurerOutput output) {
    Map<String, Object> metadata = readJsonMap(current);
    Map<String, Object> structurer = new LinkedHashMap<>();
    structurer.put("structuredSummary", output.structuredSummary());
    structurer.put("outcomeLabel", output.outcomeLabel());
    structurer.put("rejectReasonTaxonomy", output.rejectReasonTaxonomy());
    structurer.put("confidence", output.confidence());
    structurer.put("evidence", output.evidence());
    metadata.put("interviewFeedbackStructurer", structurer);
    return writeJson(metadata);
  }

  private static Map<String, Object> fallbackSuggestionPayload(InterviewFeedbackStructurerOutput output) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("outcomeLabel", output.outcomeLabel());
    payload.put("rejectReasonTaxonomy", output.rejectReasonTaxonomy());
    payload.put("confidence", output.confidence());
    return payload;
  }

  private static String mergeFailureMetadata(String current, String errorMessage) {
    Map<String, Object> metadata = readJsonMap(current);
    metadata.put("interviewFeedbackStructurer", Map.of(
        "status", "failed",
        "error", errorMessage == null ? "task_failed" : errorMessage));
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
      throw new IllegalStateException("Failed to write json", exception);
    }
  }
}
