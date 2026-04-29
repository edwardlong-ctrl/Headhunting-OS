package com.recruitingtransactionos.coreapi.job;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JobScorecard(
    JobScorecardId jobScorecardId,
    UUID organizationId,
    JobId jobId,
    String dimensions,
    String scoringGuidance,
    String status,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public JobScorecard {
    Objects.requireNonNull(jobScorecardId, "jobScorecardId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(dimensions, "dimensions must not be null");
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
    private JobScorecardId jobScorecardId;
    private UUID organizationId;
    private JobId jobId;
    private String dimensions;
    private String scoringGuidance;
    private String status = "draft";
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder jobScorecardId(JobScorecardId id) { this.jobScorecardId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder jobId(JobId jobId) { this.jobId = jobId; return this; }
    public Builder dimensions(String dimensions) { this.dimensions = dimensions; return this; }
    public Builder scoringGuidance(String guidance) { this.scoringGuidance = guidance; return this; }
    public Builder status(String status) { this.status = status; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public JobScorecard build() {
      return new JobScorecard(jobScorecardId, organizationId, jobId, dimensions,
          scoringGuidance, status, metadata, createdAt, updatedAt, version);
    }
  }
}
