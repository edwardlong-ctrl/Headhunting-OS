package com.recruitingtransactionos.coreapi.interaction;

public enum InteractionType {
  SUBMISSION("submission"),
  PRIOR_CONTACT("prior_contact"),
  PRIOR_APPLICATION("prior_application"),
  INTERVIEW("interview"),
  PLACEMENT("placement");

  private final String wireValue;

  InteractionType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static InteractionType fromWireValue(String wireValue) {
    for (InteractionType type : values()) {
      if (type.wireValue.equals(wireValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown interaction type: " + wireValue);
  }
}
