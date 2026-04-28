package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.regex.Pattern;

public record AnonymousCandidateRef(String value) {

  private static final Pattern RAW_UUID = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public AnonymousCandidateRef {
    value = ClientSafeProjectionGuards.requireNonBlank(value, "anonymousCandidateRef");
    if (RAW_UUID.matcher(value).matches()) {
      throw new IllegalArgumentException("anonymousCandidateRef must not be a raw UUID");
    }
    if (!value.startsWith("anon_candidate_")) {
      throw new IllegalArgumentException(
          "anonymousCandidateRef must use the anon_candidate_ opaque prefix");
    }
  }

  public static AnonymousCandidateRef of(String value) {
    return new AnonymousCandidateRef(value);
  }
}
