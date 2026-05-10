package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.util.Objects;
import java.util.UUID;

public record ReportingExportRequest(
    UUID organizationId,
    PortalRole actorRole,
    ReportingExportTargetScope targetScope,
    ReportingExportType exportType,
    FieldVisibilityPolicy fieldVisibilityPolicy,
    String auditId) {

  public ReportingExportRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(targetScope, "targetScope must not be null");
    Objects.requireNonNull(exportType, "exportType must not be null");
    Objects.requireNonNull(fieldVisibilityPolicy, "fieldVisibilityPolicy must not be null");
    auditId = requireNonBlank(auditId, "auditId");
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
