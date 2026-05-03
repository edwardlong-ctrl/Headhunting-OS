package com.recruitingtransactionos.coreapi.clientsafeprojection;

public enum ReidentificationRiskFeature {
  EXACT_COMPANY_RARE_TITLE_EXACT_YEAR(
      "exact_company_rare_title_exact_year",
      ReidentificationRiskDecision.GENERALIZE),
  EXACT_CURRENT_EMPLOYER(
      "exact_current_employer",
      ReidentificationRiskDecision.BLOCK),
  EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME(
      "exact_project_product_chip_code_name",
      ReidentificationRiskDecision.BLOCK),
  PUBLIC_IDENTIFIER_BEFORE_CONSENT(
      "public_identifier_before_consent",
      ReidentificationRiskDecision.BLOCK),
  EXACT_LOCATION_OR_ADDRESS(
      "exact_location_or_address",
      ReidentificationRiskDecision.BLOCK),
  DIRECT_CONTACT_OR_PROFILE_URL(
      "direct_contact_or_profile_url",
      ReidentificationRiskDecision.BLOCK),
  SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM(
      "small_team_unique_ownership_claim",
      ReidentificationRiskDecision.GENERALIZE),
  OVERLY_SPECIFIC_IDENTIFYING_ACHIEVEMENT_NUMBER(
      "overly_specific_identifying_achievement_number",
      ReidentificationRiskDecision.REVIEW);

  private final String wireValue;
  private final ReidentificationRiskDecision recommendedDecision;

  ReidentificationRiskFeature(
      String wireValue,
      ReidentificationRiskDecision recommendedDecision) {
    this.wireValue = wireValue;
    this.recommendedDecision = recommendedDecision;
  }

  public String wireValue() {
    return wireValue;
  }

  public ReidentificationRiskDecision recommendedDecision() {
    return recommendedDecision;
  }

  public static ReidentificationRiskFeature fromWireValue(String wireValue) {
    if (wireValue == null) {
      throw new NullPointerException("wireValue must not be null");
    }
    for (ReidentificationRiskFeature feature : values()) {
      if (feature.wireValue.equals(wireValue)) {
        return feature;
      }
    }
    throw new IllegalArgumentException("unknown re-identification risk feature: " + wireValue);
  }
}
