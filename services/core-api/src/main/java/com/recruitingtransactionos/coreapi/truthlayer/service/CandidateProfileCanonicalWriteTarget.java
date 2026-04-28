package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import java.util.Objects;

public record CandidateProfileCanonicalWriteTarget(
    CandidateProfileId candidateProfileId,
    CandidateProfileFieldPath fieldPath,
    CandidateProfileFieldValue value,
    CandidateProfileFieldStatus fieldStatus) {

  public CandidateProfileCanonicalWriteTarget {
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(fieldStatus, "fieldStatus must not be null");
  }
}
