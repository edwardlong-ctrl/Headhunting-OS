package com.recruitingtransactionos.coreapi.interviewfeedback;

public enum RejectReasonTaxonomy {
  COMPENSATION_MISMATCH("compensation_mismatch"),
  SKILL_GAP("skill_gap"),
  EXPERIENCE_GAP("experience_gap"),
  CULTURE_MISMATCH("culture_mismatch"),
  COMMUNICATION_CONCERN("communication_concern"),
  AVAILABILITY_CONSTRAINT("availability_constraint"),
  LOCATION_MISMATCH("location_mismatch"),
  OTHER("other");

  private final String wireValue;

  RejectReasonTaxonomy(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static RejectReasonTaxonomy fromWireValue(String wireValue) {
    for (RejectReasonTaxonomy taxonomy : values()) {
      if (taxonomy.wireValue.equals(wireValue)) {
        return taxonomy;
      }
    }
    throw new IllegalArgumentException("Unknown reject reason taxonomy: " + wireValue);
  }
}
