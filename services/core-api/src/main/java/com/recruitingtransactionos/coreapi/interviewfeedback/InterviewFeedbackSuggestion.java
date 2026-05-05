package com.recruitingtransactionos.coreapi.interviewfeedback;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InterviewFeedbackSuggestion(
    InterviewFeedbackSuggestionId interviewFeedbackSuggestionId,
    UUID organizationId,
    InterviewFeedbackId interviewFeedbackId,
    CandidateCompanyInteractionId candidateCompanyInteractionId,
    JobId jobId,
    CandidateId candidateId,
    UUID aiTaskRunId,
    InterviewFeedbackSuggestionScope scope,
    InterviewFeedbackSuggestionType suggestionType,
    InterviewFeedbackSuggestionStatus status,
    InterviewOutcomeLabel outcomeLabel,
    RejectReasonTaxonomy rejectReasonTaxonomy,
    String title,
    String rationale,
    String payload,
    UUID reviewedByUserId,
    Instant reviewedAt,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public InterviewFeedbackSuggestion {
    Objects.requireNonNull(interviewFeedbackSuggestionId, "interviewFeedbackSuggestionId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interviewFeedbackId, "interviewFeedbackId must not be null");
    Objects.requireNonNull(candidateCompanyInteractionId, "candidateCompanyInteractionId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(scope, "scope must not be null");
    Objects.requireNonNull(suggestionType, "suggestionType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }
}
