package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record CandidateConsentSummaryResponse(
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String jobTitle,
    String consentRecordRef,
    String consentStatus,
    String consentTextVersion,
    String currentProfileVersion,
    boolean profileVersionMatches,
    boolean revoked,
    String expiresAt,
    List<SharedField> sharedFields) implements ApiSafeResponseBody {

  public CandidateConsentSummaryResponse {
    candidateRef = ApiBoundaryContractRules.requireNonBlank(candidateRef, "candidateRef");
    candidateProfileRef = ApiBoundaryContractRules.requireNonBlank(candidateProfileRef, "candidateProfileRef");
    jobRef = ApiBoundaryContractRules.requireNonBlank(jobRef, "jobRef");
    jobTitle = ApiBoundaryContractRules.requireNonBlank(jobTitle, "jobTitle");
    consentRecordRef = ApiBoundaryContractRules.requireNonBlank(consentRecordRef, "consentRecordRef");
    consentStatus = ApiBoundaryContractRules.requireNonBlank(consentStatus, "consentStatus");
    consentTextVersion = ApiBoundaryContractRules.requireNonBlank(consentTextVersion, "consentTextVersion");
    currentProfileVersion = ApiBoundaryContractRules.requireNonBlank(currentProfileVersion, "currentProfileVersion");
    sharedFields = List.copyOf(Objects.requireNonNull(sharedFields, "sharedFields must not be null"));
  }

  public record SharedField(
      String fieldPath,
      String jsonValue) {

    public SharedField {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      jsonValue = ApiBoundaryContractRules.requireNonBlank(jsonValue, "jsonValue");
    }
  }
}
