package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.List;
import java.util.Objects;

public record ReportingExportSection(
    String title,
    List<ReportingExportField> fields) {

  public ReportingExportSection {
    title = requireNonBlank(title, "title");
    fields = List.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
    fields.forEach(field -> Objects.requireNonNull(field, "fields must not contain null values"));
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
