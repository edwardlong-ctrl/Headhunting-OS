package com.recruitingtransactionos.coreapi.candidateprofile;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProfileFieldLineage(
    ProfileFieldLineageId profileFieldLineageId,
    UUID organizationId,
    CandidateProfileId candidateProfileId,
    CandidateId candidateId,
    CandidateProfileFieldPath fieldPath,
    CandidateProfileFieldSourceType sourceType,
    String sourceId,
    String sourceTrust,
    String provenanceLabel,
    Instant recordedAt,
    Instant createdAt) {

  public ProfileFieldLineage {
    Objects.requireNonNull(profileFieldLineageId, "profileFieldLineageId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    sourceId = CandidateProfileGuards.requireNonBlank(sourceId, "sourceId");
    sourceTrust = CandidateProfileGuards.optionalNonBlank(sourceTrust, "sourceTrust");
    provenanceLabel = CandidateProfileGuards.optionalNonBlank(provenanceLabel, "provenanceLabel");
    Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ProfileFieldLineageId profileFieldLineageId;
    private UUID organizationId;
    private CandidateProfileId candidateProfileId;
    private CandidateId candidateId;
    private CandidateProfileFieldPath fieldPath;
    private CandidateProfileFieldSourceType sourceType;
    private String sourceId;
    private String sourceTrust;
    private String provenanceLabel;
    private Instant recordedAt;
    private Instant createdAt;

    private Builder() {
    }

    public Builder profileFieldLineageId(ProfileFieldLineageId profileFieldLineageId) {
      this.profileFieldLineageId = profileFieldLineageId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder candidateProfileId(CandidateProfileId candidateProfileId) {
      this.candidateProfileId = candidateProfileId;
      return this;
    }

    public Builder candidateId(CandidateId candidateId) {
      this.candidateId = candidateId;
      return this;
    }

    public Builder fieldPath(CandidateProfileFieldPath fieldPath) {
      this.fieldPath = fieldPath;
      return this;
    }

    public Builder sourceType(CandidateProfileFieldSourceType sourceType) {
      this.sourceType = sourceType;
      return this;
    }

    public Builder sourceId(String sourceId) {
      this.sourceId = sourceId;
      return this;
    }

    public Builder sourceTrust(String sourceTrust) {
      this.sourceTrust = sourceTrust;
      return this;
    }

    public Builder provenanceLabel(String provenanceLabel) {
      this.provenanceLabel = provenanceLabel;
      return this;
    }

    public Builder recordedAt(Instant recordedAt) {
      this.recordedAt = recordedAt;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ProfileFieldLineage build() {
      return new ProfileFieldLineage(
          profileFieldLineageId,
          organizationId,
          candidateProfileId,
          candidateId,
          fieldPath,
          sourceType,
          sourceId,
          sourceTrust,
          provenanceLabel,
          recordedAt,
          createdAt);
    }
  }
}
