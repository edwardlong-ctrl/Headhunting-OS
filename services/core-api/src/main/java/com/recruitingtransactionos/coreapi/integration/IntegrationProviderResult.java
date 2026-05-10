package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;

public record IntegrationProviderResult(
    String providerKey,
    IntegrationProviderStatus status,
    String safeStatusCode,
    String safeMessage,
    String externalMessageId) {

  public IntegrationProviderResult {
    providerKey = requireNonBlank(providerKey, "providerKey");
    Objects.requireNonNull(status, "status must not be null");
    safeStatusCode = requireNonBlank(safeStatusCode, "safeStatusCode");
    safeMessage = optionalNonBlank(safeMessage, "safeMessage");
    externalMessageId = optionalNonBlank(externalMessageId, "externalMessageId");
  }

  public static IntegrationProviderResult accepted(String providerKey, String externalMessageId) {
    return new IntegrationProviderResult(
        providerKey,
        IntegrationProviderStatus.ACCEPTED,
        "accepted",
        "Provider accepted the audited integration command.",
        externalMessageId);
  }

  public static IntegrationProviderResult unconfigured(String providerKey, String safeStatusCode) {
    return new IntegrationProviderResult(
        providerKey,
        IntegrationProviderStatus.UNCONFIGURED,
        safeStatusCode,
        "Provider is not configured in this environment.",
        null);
  }

  public static IntegrationProviderResult placeholder(String providerKey, String safeStatusCode) {
    return new IntegrationProviderResult(
        providerKey,
        IntegrationProviderStatus.PRODUCTION_PLACEHOLDER,
        safeStatusCode,
        "Provider boundary exists, but production execution is intentionally not configured.",
        null);
  }

  public static IntegrationProviderResult failedClosed(String providerKey, String safeStatusCode) {
    return new IntegrationProviderResult(
        providerKey,
        IntegrationProviderStatus.FAILED_CLOSED,
        safeStatusCode,
        "Integration command was blocked before external provider execution.",
        null);
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
