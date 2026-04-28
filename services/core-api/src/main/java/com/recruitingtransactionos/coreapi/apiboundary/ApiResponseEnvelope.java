package com.recruitingtransactionos.coreapi.apiboundary;

public record ApiResponseEnvelope<T extends ApiSafeResponseBody>(
    T data,
    ApiErrorResponse error) {

  public ApiResponseEnvelope {
    if ((data == null && error == null) || (data != null && error != null)) {
      throw new IllegalArgumentException("API response envelope must contain exactly one outcome");
    }
  }

  public static <T extends ApiSafeResponseBody> ApiResponseEnvelope<T> success(T data) {
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    return new ApiResponseEnvelope<>(data, null);
  }

  public static <T extends ApiSafeResponseBody> ApiResponseEnvelope<T> failure(
      ApiErrorResponse error) {
    if (error == null) {
      throw new IllegalArgumentException("error must not be null");
    }
    return new ApiResponseEnvelope<>(null, error);
  }
}
