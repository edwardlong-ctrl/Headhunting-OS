package com.recruitingtransactionos.coreapi.apiboundary.client;

import java.util.List;
import java.util.Objects;

public record ClientPreferenceUpsertRequest(
    List<PreferenceItem> preferences) {

  public ClientPreferenceUpsertRequest {
    preferences = List.copyOf(Objects.requireNonNull(preferences, "preferences must not be null"));
  }

  public record PreferenceItem(String preferenceKey, String preferenceValue, String notes) {
    public PreferenceItem {
      if (preferenceKey == null || preferenceKey.isBlank()) {
        throw new IllegalArgumentException("preferenceKey must not be blank");
      }
      if (preferenceValue == null) {
        throw new IllegalArgumentException("preferenceValue must not be null");
      }
    }
  }
}
