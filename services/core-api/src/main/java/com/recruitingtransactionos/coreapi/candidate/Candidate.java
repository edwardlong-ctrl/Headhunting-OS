package com.recruitingtransactionos.coreapi.candidate;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Candidate(
    CandidateId candidateId,
    UUID organizationId,
    CandidateStatus status,
    CandidateProfileId currentProfileId,
    String privacyStatus,
    UUID ownerConsultantId,
    String doNotContactReason,
    CandidateId mergedIntoCandidateId,
    Instant lastActivityAt,
    UUID defaultIndustryPackId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Candidate {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    privacyStatus = CandidateGuards.requireNonBlank(privacyStatus, "privacyStatus");
    doNotContactReason = CandidateGuards.optionalNonBlank(
        doNotContactReason, "doNotContactReason");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private CandidateId candidateId;
    private UUID organizationId;
    private CandidateStatus status;
    private CandidateProfileId currentProfileId;
    private String privacyStatus = "internal_only";
    private UUID ownerConsultantId;
    private String doNotContactReason;
    private CandidateId mergedIntoCandidateId;
    private Instant lastActivityAt;
    private UUID defaultIndustryPackId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {}

    public Builder candidateId(CandidateId candidateId) { this.candidateId = candidateId; return this; }
    public Builder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
    public Builder status(CandidateStatus status) { this.status = status; return this; }
    public Builder currentProfileId(CandidateProfileId currentProfileId) { this.currentProfileId = currentProfileId; return this; }
    public Builder privacyStatus(String privacyStatus) { this.privacyStatus = privacyStatus; return this; }
    public Builder ownerConsultantId(UUID ownerConsultantId) { this.ownerConsultantId = ownerConsultantId; return this; }
    public Builder doNotContactReason(String doNotContactReason) { this.doNotContactReason = doNotContactReason; return this; }
    public Builder mergedIntoCandidateId(CandidateId mergedIntoCandidateId) { this.mergedIntoCandidateId = mergedIntoCandidateId; return this; }
    public Builder lastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; return this; }
    public Builder defaultIndustryPackId(UUID defaultIndustryPackId) { this.defaultIndustryPackId = defaultIndustryPackId; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Candidate build() {
      return new Candidate(
          candidateId,
          organizationId,
          status,
          currentProfileId,
          privacyStatus,
          ownerConsultantId,
          doNotContactReason,
          mergedIntoCandidateId,
          lastActivityAt,
          defaultIndustryPackId,
          metadata,
          createdAt,
          updatedAt,
          version);
    }
  }
}
