package com.recruitingtransactionos.coreapi.company;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompanyContact(
    CompanyContactId companyContactId,
    UUID organizationId,
    CompanyId companyId,
    String name,
    String title,
    String email,
    String phone,
    String roleType,
    boolean isPrimary,
    String status,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public CompanyContact {
    Objects.requireNonNull(companyContactId, "companyContactId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    name = CompanyGuards.requireNonBlank(name, "name");
    title = CompanyGuards.optionalNonBlank(title, "title");
    email = CompanyGuards.optionalNonBlank(email, "email");
    phone = CompanyGuards.optionalNonBlank(phone, "phone");
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
    private CompanyContactId companyContactId;
    private UUID organizationId;
    private CompanyId companyId;
    private String name;
    private String title;
    private String email;
    private String phone;
    private String roleType;
    private boolean isPrimary;
    private String status = "active";
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder companyContactId(CompanyContactId id) { this.companyContactId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder companyId(CompanyId companyId) { this.companyId = companyId; return this; }
    public Builder name(String name) { this.name = name; return this; }
    public Builder title(String title) { this.title = title; return this; }
    public Builder email(String email) { this.email = email; return this; }
    public Builder phone(String phone) { this.phone = phone; return this; }
    public Builder roleType(String roleType) { this.roleType = roleType; return this; }
    public Builder isPrimary(boolean isPrimary) { this.isPrimary = isPrimary; return this; }
    public Builder status(String status) { this.status = status; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public CompanyContact build() {
      return new CompanyContact(companyContactId, organizationId, companyId, name, title,
          email, phone, roleType, isPrimary, status, metadata, createdAt, updatedAt, version);
    }
  }
}
