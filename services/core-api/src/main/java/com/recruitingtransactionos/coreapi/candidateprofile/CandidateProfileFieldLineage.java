package com.recruitingtransactionos.coreapi.candidateprofile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CandidateProfileFieldLineage(
    List<CandidateProfileFieldSourceReference> sourceReferences,
    String provenanceLabel,
    Instant createdAt) {

  public CandidateProfileFieldLineage {
    sourceReferences = List.copyOf(
        Objects.requireNonNull(sourceReferences, "sourceReferences must not be null"));
    sourceReferences.forEach(reference -> Objects.requireNonNull(
        reference,
        "sourceReferences must not contain null values"));
    if (sourceReferences.isEmpty()) {
      throw new IllegalArgumentException("sourceReferences must not be empty");
    }
    provenanceLabel = CandidateProfileGuards.optionalNonBlank(provenanceLabel, "provenanceLabel");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public boolean hasAnyReference() {
    return !sourceReferences.isEmpty();
  }

  public boolean hasReferenceType(CandidateProfileFieldSourceType sourceType) {
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    return sourceReferences.stream().anyMatch(reference -> reference.sourceType() == sourceType);
  }
}
