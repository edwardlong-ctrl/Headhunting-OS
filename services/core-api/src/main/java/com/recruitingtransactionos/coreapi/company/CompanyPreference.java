package com.recruitingtransactionos.coreapi.company;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CompanyPreference(
    CompanyPreferenceId companyPreferenceId,
    UUID organizationId,
    CompanyId companyId,
    String preferenceKey,
    String preferenceValue,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public CompanyPreference {
    Objects.requireNonNull(companyPreferenceId, "companyPreferenceId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    preferenceKey = CompanyGuards.requireNonBlank(preferenceKey, "preferenceKey");
    Objects.requireNonNull(preferenceValue, "preferenceValue must not be null");
    notes = CompanyGuards.optionalNonBlank(notes, "notes");
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
    private CompanyPreferenceId companyPreferenceId;
    private UUID organizationId;
    private CompanyId companyId;
    private String preferenceKey;
    private String preferenceValue;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder companyPreferenceId(CompanyPreferenceId id) { this.companyPreferenceId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder companyId(CompanyId companyId) { this.companyId = companyId; return this; }
    public Builder preferenceKey(String key) { this.preferenceKey = key; return this; }
    public Builder preferenceValue(String value) { this.preferenceValue = value; return this; }
    public Builder notes(String notes) { this.notes = notes; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public CompanyPreference build() {
      return new CompanyPreference(companyPreferenceId, organizationId, companyId,
          preferenceKey, preferenceValue, notes, createdAt, updatedAt, version);
    }
  }
}
