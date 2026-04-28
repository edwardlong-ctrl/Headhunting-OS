package com.recruitingtransactionos.coreapi.candidateprofile;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CandidateProfile(
    CandidateProfileId candidateProfileId,
    UUID organizationId,
    CandidateId candidateId,
    CandidateProfileVersion profileVersion,
    List<CandidateProfileField> fields,
    Instant createdAt,
    Instant updatedAt) {

  public CandidateProfile {
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(profileVersion, "profileVersion must not be null");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
    fields.forEach(field -> Objects.requireNonNull(field, "fields must not contain null values"));
    requireUniqueFieldPaths(fields);
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("updatedAt must not be before createdAt");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static void requireUniqueFieldPaths(List<CandidateProfileField> fields) {
    Set<CandidateProfileFieldPath> seen = new HashSet<>();
    for (CandidateProfileField field : fields) {
      if (!seen.add(field.fieldPath())) {
        throw new IllegalArgumentException("fields must not contain duplicate fieldPath values");
      }
    }
  }

  public static final class Builder {
    private CandidateProfileId candidateProfileId;
    private UUID organizationId;
    private CandidateId candidateId;
    private CandidateProfileVersion profileVersion;
    private List<CandidateProfileField> fields = List.of();
    private Instant createdAt;
    private Instant updatedAt;

    private Builder() {
    }

    public Builder candidateProfileId(CandidateProfileId candidateProfileId) {
      this.candidateProfileId = candidateProfileId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder candidateId(CandidateId candidateId) {
      this.candidateId = candidateId;
      return this;
    }

    public Builder profileVersion(CandidateProfileVersion profileVersion) {
      this.profileVersion = profileVersion;
      return this;
    }

    public Builder fields(List<CandidateProfileField> fields) {
      this.fields = fields;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public CandidateProfile build() {
      return new CandidateProfile(
          candidateProfileId,
          organizationId,
          candidateId,
          profileVersion,
          fields,
          createdAt,
          updatedAt);
    }
  }
}
