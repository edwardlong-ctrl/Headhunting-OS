package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.apiboundary.ReportingExportResult;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReportingExportService {

  private static final AccessDecision GENERIC_EXPORT_DENIAL = new AccessDecision(
      false,
      "export_not_available",
      "Requested export is not available for this actor, scope, or field policy.");

  private final ReportingExportAdapterRegistry adapterRegistry;
  private final Clock clock;

  public ReportingExportService(
      ReportingExportAdapterRegistry adapterRegistry,
      Clock clock) {
    this.adapterRegistry = Objects.requireNonNull(adapterRegistry, "adapterRegistry must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public ReportingExportResult export(ReportingExportRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    requireAllowed(request);
    ReportingExportAdapter adapter = adapterRegistry.adapterFor(request.exportType())
        .orElseThrow(ReportingExportService::denied);
    ReportingExportPayload rawPayload = adapter.export(request);
    ReportingExportPayload safePayload = filterPayload(request, rawPayload);
    requirePackageEvidence(request, safePayload);
    Instant generatedAt = Instant.now(clock);
    return new ReportingExportResult(
        "export-" + request.auditId() + "-" + request.exportType().wireValue(),
        request.exportType().wireValue(),
        request.organizationId().toString(),
        request.actorRole().wireValue(),
        request.targetScope().targetEntityType(),
        request.targetScope().targetEntityRef(),
        request.fieldVisibilityPolicy().wireValue(),
        request.auditId(),
        generatedAt.toString(),
        safePayload.format(),
        safePayload.semantics(),
        safePayload.legalBasis(),
        safePayload.mutationPerformed(),
        safePayload.sections(),
        safePayload.provenance(),
        safePayload.redactedOrWithheldFields());
  }

  private static void requireAllowed(ReportingExportRequest request) {
    if (!request.organizationId().equals(request.targetScope().organizationId())) {
      throw denied();
    }
    if (!rolePolicyAllows(request)) {
      throw denied();
    }
    if (request.actorRole() == PortalRole.CANDIDATE
        && request.exportType() == ReportingExportType.CANDIDATE_PERSONAL_DATA) {
      if (!"candidate".equalsIgnoreCase(request.targetScope().targetEntityType())
          || request.targetScope().actorSubjectRef() == null
          || !request.targetScope().actorSubjectRef().equals(request.targetScope().targetEntityRef())) {
        throw denied();
      }
    }
  }

  private static boolean rolePolicyAllows(ReportingExportRequest request) {
    return switch (request.exportType()) {
      case OWNER_REPORT -> request.actorRole() == PortalRole.OWNER
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.OWNER_INTERNAL;
      case CONSULTANT_ACTIVITY -> request.actorRole() == PortalRole.CONSULTANT
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.CONSULTANT_INTERNAL;
      case CLIENT_SHORTLIST_FEEDBACK -> request.actorRole() == PortalRole.CLIENT
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.CLIENT_SAFE;
      case CANDIDATE_PERSONAL_DATA -> request.actorRole() == PortalRole.CANDIDATE
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.CANDIDATE_SELF;
      case DISCLOSURE_AUDIT -> request.actorRole() == PortalRole.ADMIN
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.SYSTEM_GOVERNANCE;
      case PLACEMENT_COMMISSION -> request.actorRole() == PortalRole.OWNER
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.COMMERCIAL_READ_ONLY;
      case RETENTION_DELETE_EVIDENCE -> request.actorRole() == PortalRole.ADMIN
          && request.fieldVisibilityPolicy() == FieldVisibilityPolicy.RETENTION_EVIDENCE;
    };
  }

  private static ReportingExportPayload filterPayload(
      ReportingExportRequest request,
      ReportingExportPayload payload) {
    Objects.requireNonNull(payload, "payload must not be null");
    ArrayList<String> withheld = new ArrayList<>(payload.redactedOrWithheldFields());
    List<ReportingExportSection> sections = payload.sections().stream()
        .map(section -> filterSection(request.fieldVisibilityPolicy(), section, withheld))
        .toList();
    boolean mutationPerformed = request.exportType() == ReportingExportType.RETENTION_DELETE_EVIDENCE
        ? false
        : payload.mutationPerformed();
    return new ReportingExportPayload(
        payload.format(),
        payload.semantics(),
        payload.legalBasis(),
        mutationPerformed,
        sections,
        payload.provenance(),
        withheld);
  }

  private static ReportingExportSection filterSection(
      FieldVisibilityPolicy exportPolicy,
      ReportingExportSection section,
      List<String> withheld) {
    List<ReportingExportField> fields = section.fields().stream()
        .filter(field -> {
          boolean allowed = field.visibilityPolicy().canAppearUnder(exportPolicy);
          if (!allowed) {
            withheld.add(field.name());
          }
          return allowed;
        })
        .toList();
    return new ReportingExportSection(section.title(), fields);
  }

  private static void requirePackageEvidence(
      ReportingExportRequest request,
      ReportingExportPayload payload) {
    if (request.exportType() == ReportingExportType.DISCLOSURE_AUDIT
        && payload.provenance().isEmpty()) {
      throw new IllegalStateException(
          "disclosure audit export requires provenance evidence");
    }
    if (request.exportType() == ReportingExportType.PLACEMENT_COMMISSION
        && payload.mutationPerformed()) {
      throw new IllegalStateException(
          "placement and commission export must remain read-only");
    }
    if (request.exportType() == ReportingExportType.RETENTION_DELETE_EVIDENCE
        && payload.mutationPerformed()) {
      throw new IllegalStateException(
          "retention evidence export must not silently perform deletion");
    }
  }

  private static AccessDeniedException denied() {
    return new AccessDeniedException(GENERIC_EXPORT_DENIAL);
  }
}
