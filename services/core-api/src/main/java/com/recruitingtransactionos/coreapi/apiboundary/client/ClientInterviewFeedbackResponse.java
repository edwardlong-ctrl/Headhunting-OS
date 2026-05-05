package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ClientInterviewFeedbackResponse(
    String interviewFeedbackId,
    String interviewId,
    String shortlistId,
    String shortlistCandidateCardId,
    String outcome,
    String decision,
    String rejectReasonTaxonomy,
    String notes,
    String strengths,
    String concerns,
    String ratings,
    Integer interviewRound,
    String interviewerName,
    String interviewerRole,
    String structuredSummary,
    Integer pendingSuggestionCount,
    boolean aiStructured,
    String createdAt) implements ApiSafeResponseBody {

  public ClientInterviewFeedbackResponse {
    interviewFeedbackId = ApiBoundaryContractRules.requireNonBlank(interviewFeedbackId, "interviewFeedbackId");
    interviewId = ApiBoundaryContractRules.requireNonBlank(interviewId, "interviewId");
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    outcome = ApiBoundaryContractRules.requireNonBlank(outcome, "outcome");
    decision = ApiBoundaryContractRules.requireNonBlank(decision, "decision");
    notes = ApiBoundaryContractRules.requireNonBlank(notes, "notes");
    ratings = ApiBoundaryContractRules.requireNonBlank(ratings, "ratings");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
