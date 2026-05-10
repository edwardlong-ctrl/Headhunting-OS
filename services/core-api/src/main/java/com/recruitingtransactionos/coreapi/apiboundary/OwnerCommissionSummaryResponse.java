package com.recruitingtransactionos.coreapi.apiboundary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record OwnerCommissionSummaryResponse(
    String commissionId,
    String placementId,
    String consultantId,
    String status,
    String commissionType,
    BigDecimal amount,
    String currency,
    BigDecimal splitPercentage,
    BigDecimal salaryAmount,
    BigDecimal feeRatePercentage,
    BigDecimal expectedFeeAmount,
    String feeAgreementReference,
    String paymentTerms,
    String calculationSource,
    Instant paidAt,
    String withheldReason,
    Instant createdAt,
    Instant updatedAt) implements ApiSafeResponseBody {

  public OwnerCommissionSummaryResponse {
    Objects.requireNonNull(commissionId, "commissionId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(consultantId, "consultantId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(commissionType, "commissionType must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
