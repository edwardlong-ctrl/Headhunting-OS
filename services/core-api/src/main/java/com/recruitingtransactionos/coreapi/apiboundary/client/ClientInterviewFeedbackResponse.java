package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ClientInterviewFeedbackResponse(
    String interviewFeedbackId,
    String shortlistId,
    String shortlistCandidateCardId,
    String outcome,
    String notes,
    String strengths,
    String concerns,
    Integer interviewRound,
    String interviewerName,
    String interviewerRole,
    String createdAt) implements ApiSafeResponseBody {

  public ClientInterviewFeedbackResponse {
    interviewFeedbackId = ApiBoundaryContractRules.requireNonBlank(interviewFeedbackId, "interviewFeedbackId");
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    outcome = ApiBoundaryContractRules.requireNonBlank(outcome, "outcome");
    notes = ApiBoundaryContractRules.requireNonBlank(notes, "notes");
    strengths = strengths;
    concerns = concerns;
    interviewerName = interviewerName;
    interviewerRole = interviewerRole;
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
