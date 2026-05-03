package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;

public record ShortlistCandidateCardUpdateRequest(
    Integer sortOrder,
    String status,
    String clientNotes,
    int version) {

  public ShortlistCandidateCardUpdateRequest {
    if (sortOrder != null && sortOrder < 0) {
      throw new IllegalArgumentException("sortOrder must be >= 0");
    }
    status = status == null ? null : status.strip();
    clientNotes = clientNotes == null ? null : clientNotes.strip();
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }
}
