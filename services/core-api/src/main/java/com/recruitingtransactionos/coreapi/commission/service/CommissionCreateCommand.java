package com.recruitingtransactionos.coreapi.commission.service;

import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CommissionCreateCommand(
    UUID organizationId,
    PlacementId placementId,
    UUID consultantId,
    UUID actorId,
    CommissionType commissionType,
    BigDecimal amount,
    String currency,
    BigDecimal splitPercentage,
    BigDecimal salaryAmount,
    BigDecimal feeRatePercentage,
    String feeAgreementReference,
    String paymentTerms,
    String calculationSource) {

  public CommissionCreateCommand(
      UUID organizationId,
      PlacementId placementId,
      UUID consultantId,
      UUID actorId,
      CommissionType commissionType,
      BigDecimal amount,
      String currency,
      BigDecimal splitPercentage,
      BigDecimal salaryAmount,
      BigDecimal feeRatePercentage) {
    this(
        organizationId,
        placementId,
        consultantId,
        actorId,
        commissionType,
        amount,
        currency,
        splitPercentage,
        salaryAmount,
        feeRatePercentage,
        null,
        null,
        null);
  }

  public CommissionCreateCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(consultantId, "consultantId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(commissionType, "commissionType must not be null");
  }
}
