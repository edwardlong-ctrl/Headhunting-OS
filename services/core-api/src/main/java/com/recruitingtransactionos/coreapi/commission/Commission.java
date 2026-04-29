package com.recruitingtransactionos.coreapi.commission;

import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Commission(
    CommissionId commissionId,
    UUID organizationId,
    PlacementId placementId,
    UUID consultantId,
    CommissionStatus status,
    CommissionType commissionType,
    BigDecimal amount,
    String currency,
    BigDecimal splitPercentage,
    String calculationDetails,
    Instant paidAt,
    String withheldReason,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public Commission {
    Objects.requireNonNull(commissionId, "commissionId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(consultantId, "consultantId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(commissionType, "commissionType must not be null");
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
    private CommissionId commissionId;
    private UUID organizationId;
    private PlacementId placementId;
    private UUID consultantId;
    private CommissionStatus status;
    private CommissionType commissionType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal splitPercentage;
    private String calculationDetails;
    private Instant paidAt;
    private String withheldReason;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder commissionId(CommissionId id) { this.commissionId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder placementId(PlacementId id) { this.placementId = id; return this; }
    public Builder consultantId(UUID id) { this.consultantId = id; return this; }
    public Builder status(CommissionStatus status) { this.status = status; return this; }
    public Builder commissionType(CommissionType type) {
      this.commissionType = type; return this;
    }
    public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
    public Builder currency(String currency) { this.currency = currency; return this; }
    public Builder splitPercentage(BigDecimal pct) { this.splitPercentage = pct; return this; }
    public Builder calculationDetails(String details) {
      this.calculationDetails = details; return this;
    }
    public Builder paidAt(Instant paidAt) { this.paidAt = paidAt; return this; }
    public Builder withheldReason(String reason) { this.withheldReason = reason; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public Commission build() {
      return new Commission(commissionId, organizationId, placementId, consultantId,
          status, commissionType, amount, currency, splitPercentage, calculationDetails,
          paidAt, withheldReason, metadata, createdAt, updatedAt, version);
    }
  }
}
