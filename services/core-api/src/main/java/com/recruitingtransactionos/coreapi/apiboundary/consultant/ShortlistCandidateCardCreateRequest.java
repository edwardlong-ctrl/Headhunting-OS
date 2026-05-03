package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record ShortlistCandidateCardCreateRequest(
    String candidateId,
    Integer sortOrder,
    String clientNotes) {

  public ShortlistCandidateCardCreateRequest {
    candidateId = ApiBoundaryContractRules.requireNonBlank(candidateId, "candidateId");
    if (sortOrder != null && sortOrder < 0) {
      throw new IllegalArgumentException("sortOrder must be >= 0");
    }
    clientNotes = clientNotes == null ? null : clientNotes.strip();
  }
}
