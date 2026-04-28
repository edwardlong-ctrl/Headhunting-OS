package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Locale;
import java.util.Optional;

public enum AITaskWriteBackTarget {
  NONE("none"),
  NO_WRITE_BACK("no_write_back"),
  CLAIM_LEDGER_PROPOSAL("claim_ledger_proposal"),
  REVIEW_QUEUE("review_queue"),
  HUMAN_REVIEW_REQUIRED("human_review_required"),
  CANONICAL_CANDIDATE_PROFILE("canonical_candidate_profile"),
  CLIENT_SAFE_PROJECTION("client_safe_projection"),
  JOB_PROFILE("job_profile"),
  COMPANY_PROFILE("company_profile"),
  CONSENT_DISCLOSURE("consent_disclosure"),
  WORKFLOW_ACTION("workflow_action"),
  COMMERCIAL_OR_PLACEMENT("commercial_or_placement");

  private final String wireValue;

  AITaskWriteBackTarget(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static Optional<AITaskWriteBackTarget> fromWireValue(String wireValue) {
    if (wireValue == null || wireValue.isBlank()) {
      return Optional.empty();
    }
    String normalized = wireValue.strip().toLowerCase(Locale.ROOT);
    for (AITaskWriteBackTarget target : values()) {
      if (target.wireValue.equals(normalized)) {
        return Optional.of(target);
      }
    }
    return Optional.empty();
  }
}
