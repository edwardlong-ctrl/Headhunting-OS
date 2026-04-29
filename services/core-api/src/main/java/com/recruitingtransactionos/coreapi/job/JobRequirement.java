package com.recruitingtransactionos.coreapi.job;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JobRequirement(
    JobRequirementId jobRequirementId,
    UUID organizationId,
    JobId jobId,
    String requirementType,
    String label,
    JobRequirementImportance importance,
    String detail,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public JobRequirement {
    Objects.requireNonNull(jobRequirementId, "jobRequirementId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    requirementType = JobGuards.requireNonBlank(requirementType, "requirementType");
    label = JobGuards.requireNonBlank(label, "label");
    Objects.requireNonNull(importance, "importance must not be null");
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
    private JobRequirementId jobRequirementId;
    private UUID organizationId;
    private JobId jobId;
    private String requirementType;
    private String label;
    private JobRequirementImportance importance;
    private String detail;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder jobRequirementId(JobRequirementId id) { this.jobRequirementId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder jobId(JobId jobId) { this.jobId = jobId; return this; }
    public Builder requirementType(String type) { this.requirementType = type; return this; }
    public Builder label(String label) { this.label = label; return this; }
    public Builder importance(JobRequirementImportance importance) { this.importance = importance; return this; }
    public Builder detail(String detail) { this.detail = detail; return this; }
    public Builder sortOrder(int sortOrder) { this.sortOrder = sortOrder; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public JobRequirement build() {
      return new JobRequirement(jobRequirementId, organizationId, jobId, requirementType,
          label, importance, detail, sortOrder, createdAt, updatedAt, version);
    }
  }
}
