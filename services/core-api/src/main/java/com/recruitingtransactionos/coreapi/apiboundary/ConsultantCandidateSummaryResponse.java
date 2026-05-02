package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantCandidateSummaryResponse(
    String candidateId,
    String status,
    String privacyStatus,
    String currentProfileId,
    String ownerConsultantId,
    String lastActivityAt,
    String createdAt) implements ApiSafeResponseBody {

  public ConsultantCandidateSummaryResponse {
    candidateId = ApiBoundaryContractRules.requireNonBlank(candidateId, "candidateId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    privacyStatus = ApiBoundaryContractRules.requireNonBlank(privacyStatus, "privacyStatus");
    currentProfileId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(currentProfileId, null);
    ownerConsultantId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(ownerConsultantId, null);
    lastActivityAt = ApiBoundaryContractRules.sanitizeConsultantVisibleText(lastActivityAt, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
