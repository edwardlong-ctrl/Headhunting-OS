package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantShortlistDetailResponse(
    String shortlistId,
    String jobId,
    String title,
    String status,
    String sentAt,
    String clientViewedAt,
    String ownerConsultantId,
    String createdAt,
    String updatedAt,
    List<Card> cards) implements ApiSafeResponseBody {

  public ConsultantShortlistDetailResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    cards = ApiBoundaryContractRules.requireNonNullList(cards, "cards");
  }

  public record Card(
      String cardId,
      String anonymousCandidateCardId,
      int sortOrder,
      String status,
      String matchReportId) {}
}
