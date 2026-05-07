package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record GovernanceSectionResponse(
    String sectionKey,
    String title,
    String description,
    List<GovernanceMetricResponse> metrics,
    List<GovernanceItemResponse> items,
    List<String> warnings,
    boolean editable,
    String configJson,
    String updatedAt) implements ApiSafeResponseBody {

  public GovernanceSectionResponse {
    sectionKey = requireNonBlank(sectionKey, "sectionKey");
    title = requireNonBlank(title, "title");
    description = description == null ? "" : description;
    metrics = metrics == null ? List.of() : List.copyOf(metrics);
    items = items == null ? List.of() : List.copyOf(items);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
    configJson = configJson == null ? "{}" : configJson;
    updatedAt = updatedAt == null ? "" : updatedAt;
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}
