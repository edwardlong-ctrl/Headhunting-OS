package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import java.util.Optional;

public enum DisclosureLevel {
  L0_TEASER("l0_teaser", RedactionLevel.L0_TEASER, false, false),
  L1_GENERALIZED("l1_generalized", RedactionLevel.L1_GENERALIZED, false, false),
  L2_CLIENT_SAFE("l2_client_safe", RedactionLevel.L2_CLIENT_SAFE, false, false),
  L3_CONSENTED_DETAIL("l3_consented_detail", RedactionLevel.L3_CONSENTED_DETAIL, false, false),
  L4_IDENTITY_DISCLOSED(
      "l4_identity_disclosed", RedactionLevel.L4_IDENTITY_DISCLOSED, true, false),
  RAW_CANDIDATE("raw_candidate", null, true, true),
  RAW_CANDIDATE_PROFILE("raw_candidate_profile", null, true, true);

  private final String wireValue;
  private final RedactionLevel redactionLevel;
  private final boolean identityLevel;
  private final boolean rawInternalExposure;

  DisclosureLevel(
      String wireValue,
      RedactionLevel redactionLevel,
      boolean identityLevel,
      boolean rawInternalExposure) {
    this.wireValue = wireValue;
    this.redactionLevel = redactionLevel;
    this.identityLevel = identityLevel;
    this.rawInternalExposure = rawInternalExposure;
  }

  public String wireValue() {
    return wireValue;
  }

  public Optional<RedactionLevel> redactionLevel() {
    return Optional.ofNullable(redactionLevel);
  }

  public boolean isAnonymousClientSafeLevel() {
    return !identityLevel && !rawInternalExposure;
  }

  public boolean requiresConfirmedConsent() {
    return this == L3_CONSENTED_DETAIL || this == L4_IDENTITY_DISCLOSED;
  }

  public boolean requiresUnlockAndDisclosure() {
    return this == L4_IDENTITY_DISCLOSED;
  }

  public boolean isRawInternalExposure() {
    return rawInternalExposure;
  }
}
