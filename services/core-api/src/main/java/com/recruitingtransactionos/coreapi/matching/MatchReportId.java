package com.recruitingtransactionos.coreapi.matching;

public record MatchReportId(String value) {

  public MatchReportId {
    value = MatchingGuards.requireOpaqueRef(value, "matchReportId", "match_report_");
  }

  public static MatchReportId of(String value) {
    return new MatchReportId(value);
  }
}
