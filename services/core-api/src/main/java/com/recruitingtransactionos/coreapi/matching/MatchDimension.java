package com.recruitingtransactionos.coreapi.matching;

public enum MatchDimension {
  TECHNICAL_FIT("technical_fit"),
  INDUSTRY_FIT("industry_fit"),
  SENIORITY_FIT("seniority_fit"),
  SALARY_FIT("salary_fit"),
  LOCATION_FIT("location_fit"),
  MOTIVATION_FIT("motivation_fit"),
  AVAILABILITY_FIT("availability_fit"),
  EVIDENCE_STRENGTH("evidence_strength"),
  CULTURE_OR_MANAGER_FIT("culture_or_manager_fit");

  private final String wireValue;

  MatchDimension(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
