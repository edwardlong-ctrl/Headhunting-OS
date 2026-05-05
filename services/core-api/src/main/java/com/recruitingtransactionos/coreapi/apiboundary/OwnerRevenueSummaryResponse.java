package com.recruitingtransactionos.coreapi.apiboundary;

import java.math.BigDecimal;

public record OwnerRevenueSummaryResponse(
    BigDecimal totalExpectedFee,
    BigDecimal totalPaidFee,
    int placementCount,
    int unknownExpectedFeePlacementCount,
    int pendingCommissionCount,
    int paidCommissionCount,
    int paidCommissionMissingAmountCount,
    int activeGuaranteeCount,
    int replacementRequiredCount,
    int invoiceInFlightCount) implements ApiSafeResponseBody {}
