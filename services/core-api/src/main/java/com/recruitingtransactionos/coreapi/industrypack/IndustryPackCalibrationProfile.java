package com.recruitingtransactionos.coreapi.industrypack;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record IndustryPackCalibrationProfile(
    IndustryPack industryPack,
    OntologyVersion ontologyVersion,
    Instant calibrationReviewBy,
    List<String> goldCases,
    List<String> negativeCases,
    List<String> antiPatterns,
    List<String> scoreCaps,
    List<String> driftSignals) {

  public IndustryPackCalibrationProfile {
    Objects.requireNonNull(industryPack, "industryPack must not be null");
    Objects.requireNonNull(ontologyVersion, "ontologyVersion must not be null");
    Objects.requireNonNull(calibrationReviewBy, "calibrationReviewBy must not be null");
    goldCases = List.copyOf(goldCases == null ? List.of() : goldCases);
    negativeCases = List.copyOf(negativeCases == null ? List.of() : negativeCases);
    antiPatterns = List.copyOf(antiPatterns == null ? List.of() : antiPatterns);
    scoreCaps = List.copyOf(scoreCaps == null ? List.of() : scoreCaps);
    driftSignals = List.copyOf(driftSignals == null ? List.of() : driftSignals);
  }
}
