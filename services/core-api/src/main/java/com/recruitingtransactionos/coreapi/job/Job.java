package com.recruitingtransactionos.coreapi.job;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Job(
    JobId jobId,
    UUID organizationId,
    CompanyId companyId,
    String title,
    String description,
    String location,
    String seniorityBand,
    String roleFamily,
    String employmentType,
    String compensation,
    JobStatus status,
    String commercialTerms,
    UUID ownerConsultantId,
    Instant activatedAt,
    Instant closedAt,
    String closeReason,
    UUID industryPackId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Job {
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    title = JobGuards.requireNonBlank(title, "title");
    closeReason = JobGuards.optionalNonBlank(closeReason, "closeReason");
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
    private JobId jobId;
    private UUID organizationId;
    private CompanyId companyId;
    private String title;
    private String description;
    private String location;
    private String seniorityBand;
    private String roleFamily;
    private String employmentType;
    private String compensation;
    private JobStatus status;
    private String commercialTerms;
    private UUID ownerConsultantId;
    private Instant activatedAt;
    private Instant closedAt;
    private String closeReason;
    private UUID industryPackId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder jobId(JobId jobId) { this.jobId = jobId; return this; }
    public Builder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
    public Builder companyId(CompanyId companyId) { this.companyId = companyId; return this; }
    public Builder title(String title) { this.title = title; return this; }
    public Builder description(String description) { this.description = description; return this; }
    public Builder location(String location) { this.location = location; return this; }
    public Builder seniorityBand(String seniorityBand) { this.seniorityBand = seniorityBand; return this; }
    public Builder roleFamily(String roleFamily) { this.roleFamily = roleFamily; return this; }
    public Builder employmentType(String employmentType) { this.employmentType = employmentType; return this; }
    public Builder compensation(String compensation) { this.compensation = compensation; return this; }
    public Builder status(JobStatus status) { this.status = status; return this; }
    public Builder commercialTerms(String commercialTerms) { this.commercialTerms = commercialTerms; return this; }
    public Builder ownerConsultantId(UUID ownerConsultantId) { this.ownerConsultantId = ownerConsultantId; return this; }
    public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
    public Builder closedAt(Instant closedAt) { this.closedAt = closedAt; return this; }
    public Builder closeReason(String closeReason) { this.closeReason = closeReason; return this; }
    public Builder industryPackId(UUID industryPackId) { this.industryPackId = industryPackId; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Job build() {
      return new Job(jobId, organizationId, companyId, title, description, location,
          seniorityBand, roleFamily, employmentType, compensation, status, commercialTerms,
          ownerConsultantId, activatedAt, closedAt, closeReason, industryPackId,
          metadata, createdAt, updatedAt, version);
    }
  }
}
