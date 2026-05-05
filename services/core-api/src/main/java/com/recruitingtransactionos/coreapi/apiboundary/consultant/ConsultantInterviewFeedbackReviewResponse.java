package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantInterviewFeedbackReviewResponse(
    String suggestionId,
    String status,
    String reviewedAt,
    String interactionId,
    String outcomeLabel,
    String rejectReasonTaxonomy,
    boolean interactionUpdated) implements ApiSafeResponseBody {

  public ConsultantInterviewFeedbackReviewResponse {
    suggestionId = ApiBoundaryContractRules.requireNonBlank(suggestionId, "suggestionId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    reviewedAt = ApiBoundaryContractRules.requireNonBlank(reviewedAt, "reviewedAt");
    interactionId = ApiBoundaryContractRules.requireNonBlank(interactionId, "interactionId");
  }
}
