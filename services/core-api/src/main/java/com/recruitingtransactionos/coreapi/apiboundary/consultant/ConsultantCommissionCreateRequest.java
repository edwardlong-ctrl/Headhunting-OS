package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import java.math.BigDecimal;

public record ConsultantCommissionCreateRequest(
    String placementId,
    String commissionType,
    BigDecimal amount,
    String currency,
    BigDecimal splitPercentage,
    BigDecimal salaryAmount,
    BigDecimal feeRatePercentage) {}
