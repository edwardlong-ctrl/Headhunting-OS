package com.recruitingtransactionos.coreapi.apiboundary.client;

public record ClientInterviewFeedbackRequest(
    String outcome,
    String notes,
    String strengths,
    String concerns,
    Integer interviewRound,
    String interviewerName,
    String interviewerRole) {

  public ClientInterviewFeedbackRequest {
    if (outcome == null || outcome.isBlank()) {
      throw new IllegalArgumentException("outcome must not be blank");
    }
    if (notes == null || notes.isBlank()) {
      throw new IllegalArgumentException("notes must not be blank");
    }
  }
}
