package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.List;
import java.util.Objects;

public record CandidateProfileFieldConflictValue(
    CandidateProfileFieldValue value,
    List<CandidateProfileFieldSourceReference> sourceReferences) {

  public CandidateProfileFieldConflictValue {
    Objects.requireNonNull(value, "value must not be null");
    sourceReferences = List.copyOf(
        Objects.requireNonNull(sourceReferences, "sourceReferences must not be null"));
    sourceReferences.forEach(reference -> Objects.requireNonNull(
        reference,
        "sourceReferences must not contain null values"));
    if (sourceReferences.isEmpty()) {
      throw new IllegalArgumentException("sourceReferences must not be empty");
    }
  }

  public boolean hasSourceBacking() {
    return !sourceReferences.isEmpty();
  }
}
