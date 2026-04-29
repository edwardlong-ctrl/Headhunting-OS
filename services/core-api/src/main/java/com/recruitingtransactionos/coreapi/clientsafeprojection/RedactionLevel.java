package com.recruitingtransactionos.coreapi.clientsafeprojection;

public enum RedactionLevel {
  L0_TEASER(
      "l0_teaser",
      0,
      "Only role family, seniority band, and broad capability direction; no company, project, exact achievement, identity clue, or precise timeline."),
  L1_GENERALIZED(
      "l1_generalized",
      1,
      "Generalized company type and generalized project or capability description, without exact organization or project names."),
  L2_CLIENT_SAFE(
      "l2_client_safe",
      2,
      "Default anonymous shortlist level with redacted capability evidence and risks while unique identity details stay hidden."),
  L3_CONSENTED_DETAIL(
      "l3_consented_detail",
      3,
      "Candidate-authorized detail before full identity disclosure; more specific, but still without full identity or contact exposure."),
  L4_IDENTITY_DISCLOSED(
      "l4_identity_disclosed",
      4,
      "Full identity and contact detail level that requires unlock approval and DisclosureRecord generation; vocabulary only in this contract.");

  private final String wireValue;
  private final int order;
  private final String description;

  RedactionLevel(String wireValue, int order, String description) {
    this.wireValue = wireValue;
    this.order = order;
    this.description = description;
  }

  public String wireValue() {
    return wireValue;
  }

  public int order() {
    return order;
  }

  public String description() {
    return description;
  }

  public static RedactionLevel fromWireValue(String wireValue) {
    for (RedactionLevel redactionLevel : values()) {
      if (redactionLevel.wireValue.equals(wireValue)) {
        return redactionLevel;
      }
    }
    throw new IllegalArgumentException("unknown redaction level: " + wireValue);
  }

  public boolean isAnonymousClientSafeLevel() {
    return this != L4_IDENTITY_DISCLOSED;
  }

  public boolean requiresDisclosureRecord() {
    return this == L4_IDENTITY_DISCLOSED;
  }
}
