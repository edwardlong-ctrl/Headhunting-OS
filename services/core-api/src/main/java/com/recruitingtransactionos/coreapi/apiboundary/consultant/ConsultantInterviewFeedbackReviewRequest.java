package com.recruitingtransactionos.coreapi.apiboundary.consultant;

public record ConsultantInterviewFeedbackReviewRequest(
    String decision,
    String note) {

  public ConsultantInterviewFeedbackReviewRequest {
    if (decision == null || decision.isBlank()) {
      throw new IllegalArgumentException("decision must not be blank");
    }
  }
}
