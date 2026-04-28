package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record ProvenanceSummary(
    ProvenanceCategory strongestProvenanceCategory,
    ProvenanceSourceStrength strongestSourceStrength,
    ProvenanceWeight provenanceWeight,
    EvidenceAssertionStrength assertionStrength,
    AuthenticityRiskLevel authenticityRisk) {

  public ProvenanceSummary {
    Objects.requireNonNull(
        strongestProvenanceCategory, "strongestProvenanceCategory must not be null");
    Objects.requireNonNull(strongestSourceStrength, "strongestSourceStrength must not be null");
    Objects.requireNonNull(provenanceWeight, "provenanceWeight must not be null");
    Objects.requireNonNull(assertionStrength, "assertionStrength must not be null");
    Objects.requireNonNull(authenticityRisk, "authenticityRisk must not be null");
  }
}
