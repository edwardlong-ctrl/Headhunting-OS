package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantCandidateDetailResponse(
    String candidateId,
    String status,
    String privacyStatus,
    String currentProfileId,
    String ownerConsultantId,
    String lastActivityAt,
    String doNotContactReason,
    String mergedIntoCandidateId,
    String defaultIndustryPackId,
    String createdAt,
    String updatedAt) implements ApiSafeResponseBody {

  public ConsultantCandidateDetailResponse {
    candidateId = ApiBoundaryContractRules.requireNonBlank(candidateId, "candidateId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    privacyStatus = ApiBoundaryContractRules.requireNonBlank(privacyStatus, "privacyStatus");
    currentProfileId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(currentProfileId, null);
    ownerConsultantId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(ownerConsultantId, null);
    lastActivityAt = ApiBoundaryContractRules.sanitizeConsultantVisibleText(lastActivityAt, null);
    doNotContactReason = ApiBoundaryContractRules.sanitizeConsultantVisibleText(doNotContactReason, null);
    mergedIntoCandidateId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(mergedIntoCandidateId, null);
    defaultIndustryPackId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(defaultIndustryPackId, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
  }
}
