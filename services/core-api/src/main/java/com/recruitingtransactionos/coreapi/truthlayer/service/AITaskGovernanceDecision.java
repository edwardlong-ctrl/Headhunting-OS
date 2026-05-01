package com.recruitingtransactionos.coreapi.truthlayer.service;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record AITaskGovernanceDecision(
    boolean allowed,
    String reasonCode,
    String safeExplanation,
    boolean humanReviewRequired,
    boolean canonicalGateRequired,
    boolean consentDisclosureUnlockGateRequired,
    boolean workflowActionGateRequired,
    boolean commercialPlacementGateRequired) {

  private static final Pattern SAFE_REASON_CODE = Pattern.compile("[a-z0-9_.-]+");

  public AITaskGovernanceDecision {
    reasonCode = requireSafeReasonCode(reasonCode);
    safeExplanation = requireSafeExplanation(safeExplanation);
  }

  static AITaskGovernanceDecision allow(
      String reasonCode,
      String safeExplanation,
      boolean humanReviewRequired,
      boolean canonicalGateRequired,
      boolean consentDisclosureUnlockGateRequired,
      boolean workflowActionGateRequired,
      boolean commercialPlacementGateRequired) {
    return new AITaskGovernanceDecision(
        true,
        reasonCode,
        safeExplanation,
        humanReviewRequired,
        canonicalGateRequired,
        consentDisclosureUnlockGateRequired,
        workflowActionGateRequired,
        commercialPlacementGateRequired);
  }

  static AITaskGovernanceDecision deny(
      String reasonCode,
      String safeExplanation,
      boolean humanReviewRequired,
      boolean canonicalGateRequired,
      boolean consentDisclosureUnlockGateRequired,
      boolean workflowActionGateRequired,
      boolean commercialPlacementGateRequired) {
    return new AITaskGovernanceDecision(
        false,
        reasonCode,
        safeExplanation,
        humanReviewRequired,
        canonicalGateRequired,
        consentDisclosureUnlockGateRequired,
        workflowActionGateRequired,
        commercialPlacementGateRequired);
  }

  private static String requireSafeReasonCode(String reasonCode) {
    Objects.requireNonNull(reasonCode, "reasonCode must not be null");
    String normalized = reasonCode.strip().toLowerCase(Locale.ROOT);
    if (normalized.isBlank() || !SAFE_REASON_CODE.matcher(normalized).matches()) {
      throw new IllegalArgumentException("reasonCode must be a safe reason code");
    }
    return normalized;
  }

  private static String requireSafeExplanation(String safeExplanation) {
    Objects.requireNonNull(safeExplanation, "safeExplanation must not be null");
    String stripped = safeExplanation.strip();
    String lower = stripped.toLowerCase(Locale.ROOT);
    if (stripped.isBlank()
        || stripped.contains("\n")
        || stripped.contains("\r")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("java.")) {
      throw new IllegalArgumentException("safeExplanation must be safe single-line text");
    }
    return stripped;
  }
}
