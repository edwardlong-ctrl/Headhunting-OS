package com.recruitingtransactionos.coreapi.candidateprofile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CandidateProfileFieldConflict(
    CandidateProfileFieldPath fieldPath,
    List<CandidateProfileFieldConflictValue> conflictingValues,
    CandidateProfileFieldConflictSeverity severity,
    CandidateProfileFieldConflictResolutionStatus resolutionStatus,
    Instant detectedAt,
    String notes) {

  public CandidateProfileFieldConflict {
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    conflictingValues = List.copyOf(
        Objects.requireNonNull(conflictingValues, "conflictingValues must not be null"));
    conflictingValues.forEach(value -> Objects.requireNonNull(
        value,
        "conflictingValues must not contain null values"));
    if (conflictingValues.size() < 2) {
      throw new IllegalArgumentException("conflictingValues must contain at least two values");
    }
    Objects.requireNonNull(severity, "severity must not be null");
    Objects.requireNonNull(resolutionStatus, "resolutionStatus must not be null");
    Objects.requireNonNull(detectedAt, "detectedAt must not be null");
    notes = CandidateProfileGuards.optionalNonBlank(notes, "notes");
  }

  public boolean hasMultipleSourceBackedValues() {
    return conflictingValues.size() > 1
        && conflictingValues.stream().allMatch(CandidateProfileFieldConflictValue::hasSourceBacking);
  }
}
