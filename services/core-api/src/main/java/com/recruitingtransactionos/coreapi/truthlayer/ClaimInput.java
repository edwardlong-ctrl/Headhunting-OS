package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.Objects;

public record ClaimInput(
    ClaimType type,
    AssertionStrength assertionStrength,
    VerificationStatus verificationStatus,
    ClientShareability clientShareability,
    boolean bulkApproved) {

  public ClaimInput {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(assertionStrength, "assertionStrength must not be null");
    Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    Objects.requireNonNull(clientShareability, "clientShareability must not be null");
  }
}
