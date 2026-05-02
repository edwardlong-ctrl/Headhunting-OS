package com.recruitingtransactionos.coreapi.apiboundary.client;

import java.util.List;

public record ClientJobClarificationRequest(
    List<String> clarificationAnswers,
    String description,
    String location,
    String compensation,
    String commercialTerms) {

  public ClientJobClarificationRequest {
    clarificationAnswers =
        clarificationAnswers == null ? List.of() : List.copyOf(clarificationAnswers);
  }
}
