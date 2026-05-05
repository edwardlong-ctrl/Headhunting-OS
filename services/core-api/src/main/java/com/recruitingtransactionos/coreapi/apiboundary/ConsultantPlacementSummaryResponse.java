package com.recruitingtransactionos.coreapi.apiboundary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ConsultantPlacementSummaryResponse(
    String placementId,
    int version,
    String jobId,
    String candidateId,
    String companyId,
    String status,
    BigDecimal salaryAmount,
    String salaryCurrency,
    BigDecimal feeRatePercentage,
    BigDecimal expectedFeeAmount,
    LocalDate startDate,
    Integer guaranteeDays,
    LocalDate guaranteeExpiresAt,
    Instant offerAcceptedAt,
    Instant onboardedAt,
    Instant createdAt,
    Instant updatedAt,
    String notes) implements ApiSafeResponseBody {

  public ConsultantPlacementSummaryResponse {
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
