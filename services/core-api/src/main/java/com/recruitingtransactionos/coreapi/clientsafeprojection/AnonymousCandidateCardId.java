package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.regex.Pattern;

public record AnonymousCandidateCardId(String value) {

  private static final Pattern RAW_UUID = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public AnonymousCandidateCardId {
    value = ClientSafeProjectionGuards.requireNonBlank(value, "anonymousCandidateCardId");
    if (RAW_UUID.matcher(value).matches()) {
      throw new IllegalArgumentException("anonymousCandidateCardId must not be a raw UUID");
    }
    if (!value.startsWith("card_")) {
      throw new IllegalArgumentException("anonymousCandidateCardId must use the card_ opaque prefix");
    }
  }

  public static AnonymousCandidateCardId of(String value) {
    return new AnonymousCandidateCardId(value);
  }
}
