package com.recruitingtransactionos.coreapi.placement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record PlacementOfferDetails(
    BigDecimal salaryAmount,
    String salaryCurrency,
    BigDecimal feeRatePercentage,
    String notes,
    Boolean feeAgreementActive,
    String feeAgreementReference,
    String paymentTerms) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public PlacementOfferDetails(
      BigDecimal salaryAmount,
      String salaryCurrency,
      BigDecimal feeRatePercentage,
      String notes) {
    this(salaryAmount, salaryCurrency, feeRatePercentage, notes, false, null, null);
  }

  public String toJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to serialize placement offer details", exception);
    }
  }

  public static PlacementOfferDetails fromJson(String json) {
    if (json == null || json.isBlank()) {
      return new PlacementOfferDetails(null, null, null, null);
    }
    try {
      return OBJECT_MAPPER.readValue(json, PlacementOfferDetails.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to parse placement offer details", exception);
    }
  }

  public BigDecimal expectedFeeAmount() {
    if (salaryAmount == null || feeRatePercentage == null) {
      return null;
    }
    return salaryAmount
        .multiply(feeRatePercentage)
        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
  }

  public boolean hasActiveFeeAgreement() {
    return Boolean.TRUE.equals(feeAgreementActive);
  }
}
