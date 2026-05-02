package com.recruitingtransactionos.coreapi.candidate;

final class CandidateGuards {

  private CandidateGuards() {}

  static String requireNonBlank(String value, String fieldName) {
    if (value == null) {
      throw new NullPointerException(fieldName + " must not be null");
    }
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }

  static String optionalNonBlank(String value, String fieldName) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }
}
