package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.reportingexport.ReportingExportSection;
import java.util.List;
import java.util.Objects;

public record ReportingExportResult(
    String exportId,
    String exportType,
    String organizationId,
    String actorRole,
    String targetEntityType,
    String targetEntityRef,
    String fieldVisibilityPolicy,
    String auditId,
    String generatedAt,
    String format,
    String semantics,
    String legalBasis,
    boolean mutationPerformed,
    List<ReportingExportSection> sections,
    List<String> provenance,
    List<String> redactedOrWithheldFields) implements ApiSafeResponseBody {

  public ReportingExportResult {
    exportId = requireNonBlank(exportId, "exportId");
    exportType = requireNonBlank(exportType, "exportType");
    organizationId = requireNonBlank(organizationId, "organizationId");
    actorRole = requireNonBlank(actorRole, "actorRole");
    targetEntityType = requireNonBlank(targetEntityType, "targetEntityType");
    targetEntityRef = requireNonBlank(targetEntityRef, "targetEntityRef");
    fieldVisibilityPolicy = requireNonBlank(fieldVisibilityPolicy, "fieldVisibilityPolicy");
    auditId = requireNonBlank(auditId, "auditId");
    generatedAt = requireNonBlank(generatedAt, "generatedAt");
    format = requireNonBlank(format, "format");
    semantics = requireNonBlank(semantics, "semantics");
    legalBasis = requireNonBlank(legalBasis, "legalBasis");
    sections = List.copyOf(Objects.requireNonNull(sections, "sections must not be null"));
    provenance = copyNonBlankList(provenance, "provenance");
    redactedOrWithheldFields = copyNonBlankList(
        redactedOrWithheldFields,
        "redactedOrWithheldFields");
  }

  private static List<String> copyNonBlankList(List<String> values, String name) {
    return List.copyOf(Objects.requireNonNull(values, name + " must not be null").stream()
        .map(value -> requireNonBlank(value, name + " entry"))
        .toList());
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
