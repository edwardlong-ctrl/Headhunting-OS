package com.recruitingtransactionos.coreapi.candidateprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

public record CandidateProfileFieldValue(String jsonValue) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public CandidateProfileFieldValue {
    jsonValue = CandidateProfileGuards.requireJsonValue(jsonValue, "jsonValue");
  }

  public static CandidateProfileFieldValue ofJson(String jsonValue) {
    return new CandidateProfileFieldValue(jsonValue);
  }

  public static CandidateProfileFieldValue ofString(String value) {
    Objects.requireNonNull(value, "value must not be null");
    try {
      return new CandidateProfileFieldValue(OBJECT_MAPPER.writeValueAsString(value));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("value must be JSON serializable", exception);
    }
  }
}
