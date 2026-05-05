package com.recruitingtransactionos.coreapi.apiboundary;

public record ClientInterviewFeedbackContextResponse(
    String interviewId,
    String shortlistId,
    String shortlistCandidateCardId,
    String jobId,
    String shortlistStatus,
    String scorecard,
    Integer existingFeedbackCount,
    String routeHint) implements ApiSafeResponseBody {

  public ClientInterviewFeedbackContextResponse {
    interviewId = ApiBoundaryContractRules.requireNonBlank(interviewId, "interviewId");
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    shortlistStatus = ApiBoundaryContractRules.requireNonBlank(shortlistStatus, "shortlistStatus");
    scorecard = ApiBoundaryContractRules.requireNonBlank(scorecard, "scorecard");
    routeHint = ApiBoundaryContractRules.requireNonBlank(routeHint, "routeHint");
  }
}
