package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsultantPlacementCreateRequest(
    String jobId,
    String candidateId,
    String companyId,
    BigDecimal salaryAmount,
    String salaryCurrency,
    BigDecimal feeRatePercentage,
    LocalDate startDate,
    Integer guaranteeDays,
    String notes,
    Boolean feeAgreementActive,
    String feeAgreementReference,
    String paymentTerms) {}
