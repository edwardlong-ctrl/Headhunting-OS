package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public record SupportImpersonationDecision(
    boolean allowed,
    String reasonCode,
    String safeExplanation,
    String ticketRef) {

  public SupportImpersonationDecision {
    reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null").strip();
    safeExplanation = Objects.requireNonNull(safeExplanation, "safeExplanation must not be null").strip();
    ticketRef = ticketRef == null ? "" : ticketRef.strip();
    if (reasonCode.isBlank()) {
      throw new IllegalArgumentException("reasonCode must not be blank");
    }
    if (safeExplanation.isBlank()) {
      throw new IllegalArgumentException("safeExplanation must not be blank");
    }
  }

  static SupportImpersonationDecision allow(String reasonCode, String safeExplanation, String ticketRef) {
    return new SupportImpersonationDecision(true, reasonCode, safeExplanation, ticketRef);
  }

  static SupportImpersonationDecision deny(String reasonCode, String safeExplanation, String ticketRef) {
    return new SupportImpersonationDecision(false, reasonCode, safeExplanation, ticketRef);
  }
}
