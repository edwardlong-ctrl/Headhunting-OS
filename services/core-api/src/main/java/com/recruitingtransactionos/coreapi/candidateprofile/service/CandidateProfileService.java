package com.recruitingtransactionos.coreapi.candidateprofile.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CandidateProfileService {

  private final CandidateProfilePersistencePort candidateProfilePersistencePort;

  public CandidateProfileService(CandidateProfilePersistencePort candidateProfilePersistencePort) {
    this.candidateProfilePersistencePort = Objects.requireNonNull(
        candidateProfilePersistencePort,
        "candidateProfilePersistencePort must not be null");
  }

  public CandidateProfile createCandidateProfile(CreateCandidateProfileRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    Instant now = Instant.now();
    CandidateProfile candidateProfile = CandidateProfile.builder()
        .candidateProfileId(request.candidateProfileId() != null
            ? request.candidateProfileId()
            : new CandidateProfileId(UUID.randomUUID()))
        .organizationId(request.organizationId())
        .candidateId(request.candidateId())
        .profileVersion(request.profileVersion())
        .fields(request.initialFields())
        .createdAt(now)
        .updatedAt(now)
        .build();
    return candidateProfilePersistencePort.createCandidateProfile(candidateProfile);
  }

  public Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    return candidateProfilePersistencePort.findCandidateProfileByIdAndOrganizationId(
        organizationId,
        candidateProfileId);
  }

  public Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
      UUID organizationId,
      CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return candidateProfilePersistencePort.findCandidateProfileByCandidateIdAndOrganizationId(
        organizationId,
        candidateId);
  }

  public CandidateProfileField upsertCandidateProfileField(
      UpsertCandidateProfileFieldRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    CandidateProfileField field = request.toCandidateProfileField();
    return candidateProfilePersistencePort.upsertCandidateProfileField(
        request.organizationId(),
        request.candidateProfileId(),
        field);
  }

  public List<CandidateProfileField> listCandidateProfileFields(
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    return candidateProfilePersistencePort.listCandidateProfileFields(
        organizationId,
        candidateProfileId);
  }
}
