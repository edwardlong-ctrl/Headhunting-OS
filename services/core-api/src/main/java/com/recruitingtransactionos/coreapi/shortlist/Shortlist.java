package com.recruitingtransactionos.coreapi.shortlist;

import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Shortlist(
    ShortlistId shortlistId,
    UUID organizationId,
    JobId jobId,
    String title,
    ShortlistStatus status,
    Instant sentAt,
    Instant clientViewedAt,
    UUID ownerConsultantId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Shortlist {
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
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
    private ShortlistId shortlistId;
    private UUID organizationId;
    private JobId jobId;
    private String title;
    private ShortlistStatus status;
    private Instant sentAt;
    private Instant clientViewedAt;
    private UUID ownerConsultantId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder shortlistId(ShortlistId id) { this.shortlistId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder jobId(JobId id) { this.jobId = id; return this; }
    public Builder title(String title) { this.title = title; return this; }
    public Builder status(ShortlistStatus status) { this.status = status; return this; }
    public Builder sentAt(Instant sentAt) { this.sentAt = sentAt; return this; }
    public Builder clientViewedAt(Instant clientViewedAt) {
      this.clientViewedAt = clientViewedAt; return this;
    }
    public Builder ownerConsultantId(UUID id) { this.ownerConsultantId = id; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Shortlist build() {
      return new Shortlist(shortlistId, organizationId, jobId, title, status,
          sentAt, clientViewedAt, ownerConsultantId, metadata, createdAt, updatedAt, version);
    }
  }
}
