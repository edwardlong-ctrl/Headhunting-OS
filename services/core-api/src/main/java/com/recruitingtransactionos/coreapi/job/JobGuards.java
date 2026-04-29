package com.recruitingtransactionos.coreapi.job;

final class JobGuards {

  private JobGuards() {
  }

  static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be null or blank");
    }
    return value;
  }

  static String optionalNonBlank(String value, String name) {
    if (value != null && value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank if provided");
    }
    return value;
  }
}
