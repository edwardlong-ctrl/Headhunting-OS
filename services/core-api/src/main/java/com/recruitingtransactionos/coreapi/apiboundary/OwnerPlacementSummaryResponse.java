package com.recruitingtransactionos.coreapi.apiboundary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record OwnerPlacementSummaryResponse(
    String placementId,
    String jobId,
    String candidateId,
    String companyId,
    String status,
    BigDecimal salaryAmount,
    String salaryCurrency,
    BigDecimal feeRatePercentage,
    BigDecimal expectedFeeAmount,
    boolean feeAgreementActive,
    String feeAgreementReference,
    String paymentTerms,
    String invoiceReadiness,
    String accountingExportStatus,
    List<String> commissionStatuses,
    LocalDate startDate,
    Integer guaranteeDays,
    LocalDate guaranteeExpiresAt,
    Instant createdAt,
    Instant updatedAt) implements ApiSafeResponseBody {

  public OwnerPlacementSummaryResponse {
    Objects.requireNonNull(placementId, "placementId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(commissionStatuses, "commissionStatuses must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
