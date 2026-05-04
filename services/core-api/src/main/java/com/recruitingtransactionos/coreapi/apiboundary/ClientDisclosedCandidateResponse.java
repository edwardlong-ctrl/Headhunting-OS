package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record ClientDisclosedCandidateResponse(
    String shortlistId,
    String shortlistCandidateCardId,
    String disclosureRecordRef,
    String candidateId,
    String candidateProfileId,
    String candidateStatus,
    String profileVersion,
    List<DisclosedField> disclosedFields) implements ApiSafeResponseBody {

  public ClientDisclosedCandidateResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    disclosureRecordRef = ApiBoundaryContractRules.requireNonBlank(disclosureRecordRef, "disclosureRecordRef");
    candidateId = ApiBoundaryContractRules.requireNonBlank(candidateId, "candidateId");
    candidateProfileId = ApiBoundaryContractRules.requireNonBlank(candidateProfileId, "candidateProfileId");
    candidateStatus = ApiBoundaryContractRules.requireNonBlank(candidateStatus, "candidateStatus");
    profileVersion = ApiBoundaryContractRules.requireNonBlank(profileVersion, "profileVersion");
    disclosedFields = List.copyOf(Objects.requireNonNull(disclosedFields, "disclosedFields must not be null"));
  }

  public record DisclosedField(String fieldPath, String jsonValue) {
    public DisclosedField {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      jsonValue = ApiBoundaryContractRules.requireNonBlank(jsonValue, "jsonValue");
    }
  }
}
