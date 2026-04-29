package com.recruitingtransactionos.coreapi.job;

public enum JobRequirementImportance {
  MUST_HAVE("must_have"),
  NICE_TO_HAVE("nice_to_have"),
  PREFERRED("preferred");

  private final String wireValue;

  JobRequirementImportance(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static JobRequirementImportance fromWireValue(String wireValue) {
    for (JobRequirementImportance importance : values()) {
      if (importance.wireValue.equals(wireValue)) {
        return importance;
      }
    }
    throw new IllegalArgumentException("Unknown job requirement importance: " + wireValue);
  }
}
