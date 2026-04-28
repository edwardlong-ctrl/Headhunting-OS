package com.recruitingtransactionos.coreapi.matching;

public record MatchJobRef(String value) {

  public MatchJobRef {
    value = MatchingGuards.requireOpaqueRef(value, "jobRef", "job_ref_");
  }

  public static MatchJobRef of(String value) {
    return new MatchJobRef(value);
  }
}
