package com.recruitingtransactionos.coreapi.apiboundary.client;

import java.util.List;

public record ClientInterviewFeedbackRequest(
    String outcome,
    String decision,
    String interviewId,
    String rejectReasonTaxonomy,
    String notes,
    String strengths,
    String concerns,
    String interviewDate,
    Integer interviewRound,
    String interviewerName,
    String interviewerRole,
    List<DimensionRating> ratings) {

  public ClientInterviewFeedbackRequest {
    if (outcome == null || outcome.isBlank()) {
      throw new IllegalArgumentException("outcome must not be blank");
    }
    if (decision == null || decision.isBlank()) {
      throw new IllegalArgumentException("decision must not be blank");
    }
    if (notes == null || notes.isBlank()) {
      throw new IllegalArgumentException("notes must not be blank");
    }
    ratings = ratings == null ? List.of() : List.copyOf(ratings);
  }

  public record DimensionRating(
      String dimensionKey,
      String label,
      Integer score,
      String notes) {
  }
}
