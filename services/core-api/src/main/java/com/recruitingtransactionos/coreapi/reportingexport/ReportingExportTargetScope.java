package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.Objects;
import java.util.UUID;

public record ReportingExportTargetScope(
    UUID organizationId,
    String targetEntityType,
    String targetEntityRef,
    String actorSubjectRef,
    String clientRef) {

  public ReportingExportTargetScope {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    targetEntityType = requireNonBlank(targetEntityType, "targetEntityType");
    targetEntityRef = requireNonBlank(targetEntityRef, "targetEntityRef");
    actorSubjectRef = actorSubjectRef == null ? null : actorSubjectRef.strip();
    clientRef = clientRef == null ? null : clientRef.strip();
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
