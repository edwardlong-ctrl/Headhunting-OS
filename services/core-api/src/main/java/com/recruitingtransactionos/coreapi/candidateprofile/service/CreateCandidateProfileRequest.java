package com.recruitingtransactionos.coreapi.candidateprofile.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateCandidateProfileRequest(
    UUID organizationId,
    CandidateProfileId candidateProfileId,
    CandidateId candidateId,
    CandidateProfileVersion profileVersion,
    List<CandidateProfileField> initialFields) {

  public CreateCandidateProfileRequest(
      UUID organizationId,
      CandidateId candidateId,
      CandidateProfileVersion profileVersion,
      List<CandidateProfileField> initialFields) {
    this(organizationId, null, candidateId, profileVersion, initialFields);
  }

  public CreateCandidateProfileRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(profileVersion, "profileVersion must not be null");
    initialFields = initialFields == null ? List.of() : List.copyOf(initialFields);
    initialFields.forEach(field -> Objects.requireNonNull(
        field,
        "initialFields must not contain null values"));
  }
}
