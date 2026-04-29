package com.recruitingtransactionos.coreapi.company;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Company(
    CompanyId companyId,
    UUID organizationId,
    String name,
    String displayName,
    String industry,
    String website,
    String headquartersLocation,
    String sizeBand,
    CompanyStatus status,
    String paymentReliability,
    UUID ownerConsultantId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Company {
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    name = CompanyGuards.requireNonBlank(name, "name");
    displayName = CompanyGuards.optionalNonBlank(displayName, "displayName");
    industry = CompanyGuards.optionalNonBlank(industry, "industry");
    website = CompanyGuards.optionalNonBlank(website, "website");
    headquartersLocation = CompanyGuards.optionalNonBlank(
        headquartersLocation, "headquartersLocation");
    sizeBand = CompanyGuards.optionalNonBlank(sizeBand, "sizeBand");
    Objects.requireNonNull(status, "status must not be null");
    paymentReliability = CompanyGuards.optionalNonBlank(
        paymentReliability, "paymentReliability");
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
    private CompanyId companyId;
    private UUID organizationId;
    private String name;
    private String displayName;
    private String industry;
    private String website;
    private String headquartersLocation;
    private String sizeBand;
    private CompanyStatus status;
    private String paymentReliability;
    private UUID ownerConsultantId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder companyId(CompanyId companyId) { this.companyId = companyId; return this; }
    public Builder organizationId(UUID organizationId) { this.organizationId = organizationId; return this; }
    public Builder name(String name) { this.name = name; return this; }
    public Builder displayName(String displayName) { this.displayName = displayName; return this; }
    public Builder industry(String industry) { this.industry = industry; return this; }
    public Builder website(String website) { this.website = website; return this; }
    public Builder headquartersLocation(String headquartersLocation) { this.headquartersLocation = headquartersLocation; return this; }
    public Builder sizeBand(String sizeBand) { this.sizeBand = sizeBand; return this; }
    public Builder status(CompanyStatus status) { this.status = status; return this; }
    public Builder paymentReliability(String paymentReliability) { this.paymentReliability = paymentReliability; return this; }
    public Builder ownerConsultantId(UUID ownerConsultantId) { this.ownerConsultantId = ownerConsultantId; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Company build() {
      return new Company(companyId, organizationId, name, displayName, industry, website,
          headquartersLocation, sizeBand, status, paymentReliability, ownerConsultantId,
          metadata, createdAt, updatedAt, version);
    }
  }
}
