package com.recruitingtransactionos.coreapi.matching;

import java.util.Objects;

public record MatchReportGenerationResult(
    MatchReport matchReport,
    MatchEvidenceSummary evidenceSummary) {

  public MatchReportGenerationResult {
    Objects.requireNonNull(matchReport, "matchReport must not be null");
    Objects.requireNonNull(evidenceSummary, "evidenceSummary must not be null");
  }
}
