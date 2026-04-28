package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public final class AccessDeniedException extends RuntimeException {

  private final AccessDecision decision;

  public AccessDeniedException(AccessDecision decision) {
    super(message(decision));
    this.decision = Objects.requireNonNull(decision, "decision must not be null");
    if (decision.allowed()) {
      throw new IllegalArgumentException("decision must be a denial");
    }
  }

  public AccessDecision decision() {
    return decision;
  }

  private static String message(AccessDecision decision) {
    Objects.requireNonNull(decision, "decision must not be null");
    return decision.reasonCode() + ": " + decision.safeExplanation();
  }
}
