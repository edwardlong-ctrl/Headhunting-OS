package com.recruitingtransactionos.coreapi.candidateprofile.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.port.ProfileFieldLineagePort;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ProfileFieldLineageService {

  private final ProfileFieldLineagePort profileFieldLineagePort;

  public ProfileFieldLineageService(ProfileFieldLineagePort profileFieldLineagePort) {
    this.profileFieldLineagePort = Objects.requireNonNull(
        profileFieldLineagePort, "profileFieldLineagePort must not be null");
  }

  public ProfileFieldLineage append(ProfileFieldLineage lineage) {
    Objects.requireNonNull(lineage, "lineage must not be null");
    return profileFieldLineagePort.append(lineage);
  }

  public List<ProfileFieldLineage> findByProfileAndFieldPath(
      UUID organizationId,
      CandidateProfileId candidateProfileId,
      CandidateProfileFieldPath fieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    return profileFieldLineagePort.findByProfileAndFieldPath(
        organizationId, candidateProfileId, fieldPath);
  }

  public List<ProfileFieldLineage> findByCandidateAndFieldPath(
      UUID organizationId,
      CandidateId candidateId,
      CandidateProfileFieldPath fieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    return profileFieldLineagePort.findByCandidateAndFieldPath(
        organizationId, candidateId, fieldPath);
  }

  public List<ProfileFieldLineage> findBySourceTypeAndSourceId(
      UUID organizationId,
      CandidateProfileFieldSourceType sourceType,
      String sourceId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(sourceId, "sourceId must not be null");
    return profileFieldLineagePort.findBySourceTypeAndSourceId(
        organizationId, sourceType, sourceId);
  }
}
