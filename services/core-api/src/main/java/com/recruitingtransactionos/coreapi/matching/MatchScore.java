package com.recruitingtransactionos.coreapi.matching;

public record MatchScore(int value) {

  public MatchScore {
    if (value < 1 || value > 5) {
      throw new IllegalArgumentException("match score must be between 1 and 5");
    }
  }

  public static MatchScore of(int value) {
    return new MatchScore(value);
  }
}
