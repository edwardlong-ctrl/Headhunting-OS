package com.recruitingtransactionos.coreapi.interaction;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateCompanyInteraction(
    CandidateCompanyInteractionId candidateCompanyInteractionId,
    UUID organizationId,
    CandidateId candidateId,
    CompanyId companyId,
    JobId jobId,
    InteractionType interactionType,
    InteractionStatus status,
    Instant startedAt,
    Instant endedAt,
    String notes,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public CandidateCompanyInteraction {
    Objects.requireNonNull(candidateCompanyInteractionId,
        "candidateCompanyInteractionId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(interactionType, "interactionType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(startedAt, "startedAt must not be null");
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
    private CandidateCompanyInteractionId candidateCompanyInteractionId;
    private UUID organizationId;
    private CandidateId candidateId;
    private CompanyId companyId;
    private JobId jobId;
    private InteractionType interactionType;
    private InteractionStatus status;
    private Instant startedAt;
    private Instant endedAt;
    private String notes;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder candidateCompanyInteractionId(CandidateCompanyInteractionId id) {
      this.candidateCompanyInteractionId = id; return this;
    }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder candidateId(CandidateId id) { this.candidateId = id; return this; }
    public Builder companyId(CompanyId id) { this.companyId = id; return this; }
    public Builder jobId(JobId id) { this.jobId = id; return this; }
    public Builder interactionType(InteractionType type) {
      this.interactionType = type; return this;
    }
    public Builder status(InteractionStatus status) { this.status = status; return this; }
    public Builder startedAt(Instant startedAt) { this.startedAt = startedAt; return this; }
    public Builder endedAt(Instant endedAt) { this.endedAt = endedAt; return this; }
    public Builder notes(String notes) { this.notes = notes; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public CandidateCompanyInteraction build() {
      return new CandidateCompanyInteraction(candidateCompanyInteractionId, organizationId,
          candidateId, companyId, jobId, interactionType, status, startedAt, endedAt,
          notes, metadata, createdAt, updatedAt, version);
    }
  }
}
