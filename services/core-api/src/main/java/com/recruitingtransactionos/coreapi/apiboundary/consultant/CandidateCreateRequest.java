package com.recruitingtransactionos.coreapi.apiboundary.consultant;

public record CandidateCreateRequest(
    String status,
    String privacyStatus,
    String currentProfileId,
    String ownerConsultantId,
    String doNotContactReason,
    String mergedIntoCandidateId,
    String lastActivityAt,
    String defaultIndustryPackId,
    String metadata) {

  public CandidateCreateRequest {
    status = optionalNonBlank(status, "status");
    privacyStatus = optionalNonBlank(privacyStatus, "privacyStatus");
    currentProfileId = optionalNonBlank(currentProfileId, "currentProfileId");
    ownerConsultantId = optionalNonBlank(ownerConsultantId, "ownerConsultantId");
    doNotContactReason = optionalNonBlank(doNotContactReason, "doNotContactReason");
    mergedIntoCandidateId = optionalNonBlank(mergedIntoCandidateId, "mergedIntoCandidateId");
    lastActivityAt = optionalNonBlank(lastActivityAt, "lastActivityAt");
    defaultIndustryPackId = optionalNonBlank(defaultIndustryPackId, "defaultIndustryPackId");
    metadata = optionalNonBlank(metadata, "metadata");
  }

  private static String optionalNonBlank(String value, String fieldName) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank if provided");
    }
    return stripped;
  }
}
