package com.recruitingtransactionos.coreapi.matching;

public record MatchSubjectRef(String value) {

  public MatchSubjectRef {
    value = MatchingGuards.requireOpaqueRef(value, "candidateCardRef", "match_subject_");
  }

  public static MatchSubjectRef of(String value) {
    return new MatchSubjectRef(value);
  }
}
