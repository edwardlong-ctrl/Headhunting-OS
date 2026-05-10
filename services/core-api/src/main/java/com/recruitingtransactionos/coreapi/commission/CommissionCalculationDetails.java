package com.recruitingtransactionos.coreapi.commission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

public record CommissionCalculationDetails(
    BigDecimal salaryAmount,
    BigDecimal feeRatePercentage,
    BigDecimal expectedFeeAmount,
    String feeAgreementReference,
    String paymentTerms,
    String calculationSource) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public CommissionCalculationDetails(
      BigDecimal salaryAmount,
      BigDecimal feeRatePercentage,
      BigDecimal expectedFeeAmount) {
    this(salaryAmount, feeRatePercentage, expectedFeeAmount, null, null, null);
  }

  public String toJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to serialize commission calculation details", exception);
    }
  }

  public static CommissionCalculationDetails fromJson(String json) {
    if (json == null || json.isBlank()) {
      return new CommissionCalculationDetails(null, null, null, null, null, null);
    }
    try {
      return OBJECT_MAPPER.readValue(json, CommissionCalculationDetails.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to parse commission calculation details", exception);
    }
  }
}
