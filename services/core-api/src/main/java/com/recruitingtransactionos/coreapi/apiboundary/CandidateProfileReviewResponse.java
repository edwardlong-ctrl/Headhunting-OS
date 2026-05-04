package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record CandidateProfileReviewResponse(
    String candidateRef,
    String profileVersion,
    List<ProfileField> fields) implements ApiSafeResponseBody {

  public CandidateProfileReviewResponse {
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(profileVersion, "profileVersion must not be null");
    fields = fields == null ? List.of() : List.copyOf(fields);
  }

  public record ProfileField(
      String fieldPath,
      String jsonValue,
      String status,
      String sourceType,
      String updatedAt) {

    public ProfileField {
      Objects.requireNonNull(fieldPath, "fieldPath must not be null");
      Objects.requireNonNull(jsonValue, "jsonValue must not be null");
    }
  }
}
