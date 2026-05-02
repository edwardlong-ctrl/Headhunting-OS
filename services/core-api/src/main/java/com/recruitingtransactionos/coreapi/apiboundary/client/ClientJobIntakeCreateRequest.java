package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import java.util.List;

public record ClientJobIntakeCreateRequest(
    String companyId,
    String title,
    String description,
    String location,
    String compensation,
    String commercialTerms,
    List<String> clarificationQuestions) {

  public ClientJobIntakeCreateRequest {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    clarificationQuestions =
        clarificationQuestions == null ? List.of() : List.copyOf(clarificationQuestions);
  }
}
