package com.recruitingtransactionos.coreapi.placement;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Placement(
    PlacementId placementId,
    UUID organizationId,
    JobId jobId,
    CandidateId candidateId,
    CompanyId companyId,
    PlacementStatus status,
    String offerDetails,
    Instant offerAcceptedAt,
    LocalDate startDate,
    Instant onboardedAt,
    Integer guaranteeDays,
    LocalDate guaranteeExpiresAt,
    Instant cancelledAt,
    String cancelReason,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Placement {
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(status, "status must not be null");
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
    private PlacementId placementId;
    private UUID organizationId;
    private JobId jobId;
    private CandidateId candidateId;
    private CompanyId companyId;
    private PlacementStatus status;
    private String offerDetails;
    private Instant offerAcceptedAt;
    private LocalDate startDate;
    private Instant onboardedAt;
    private Integer guaranteeDays;
    private LocalDate guaranteeExpiresAt;
    private Instant cancelledAt;
    private String cancelReason;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder placementId(PlacementId id) { this.placementId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder jobId(JobId id) { this.jobId = id; return this; }
    public Builder candidateId(CandidateId id) { this.candidateId = id; return this; }
    public Builder companyId(CompanyId id) { this.companyId = id; return this; }
    public Builder status(PlacementStatus status) { this.status = status; return this; }
    public Builder offerDetails(String details) { this.offerDetails = details; return this; }
    public Builder offerAcceptedAt(Instant at) { this.offerAcceptedAt = at; return this; }
    public Builder startDate(LocalDate date) { this.startDate = date; return this; }
    public Builder onboardedAt(Instant at) { this.onboardedAt = at; return this; }
    public Builder guaranteeDays(Integer days) { this.guaranteeDays = days; return this; }
    public Builder guaranteeExpiresAt(LocalDate date) {
      this.guaranteeExpiresAt = date; return this;
    }
    public Builder cancelledAt(Instant at) { this.cancelledAt = at; return this; }
    public Builder cancelReason(String reason) { this.cancelReason = reason; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Placement build() {
      return new Placement(placementId, organizationId, jobId, candidateId, companyId,
          status, offerDetails, offerAcceptedAt, startDate, onboardedAt, guaranteeDays,
          guaranteeExpiresAt, cancelledAt, cancelReason, metadata, createdAt, updatedAt, version);
    }
  }
}
