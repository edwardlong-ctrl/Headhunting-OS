package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.Objects;

public record ReportingExportField(
    String name,
    String value,
    FieldVisibilityPolicy visibilityPolicy) {

  public ReportingExportField {
    name = requireNonBlank(name, "name");
    value = value == null ? "" : value;
    Objects.requireNonNull(visibilityPolicy, "visibilityPolicy must not be null");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return stripped;
  }
}
