package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityDisclosureAuditExportResponse;
import com.recruitingtransactionos.coreapi.observability.ObservabilityDisclosureAuditExportQuery;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReadService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DisclosureAuditExportAdapter implements ReportingExportAdapter {

  private final ObservabilityReadService observabilityReadService;

  public DisclosureAuditExportAdapter(ObservabilityReadService observabilityReadService) {
    this.observabilityReadService = Objects.requireNonNull(
        observabilityReadService,
        "observabilityReadService must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    ObservabilityDisclosureAuditExportResponse audit =
        observabilityReadService.disclosureAuditExport(new ObservabilityDisclosureAuditExportQuery(
            request.organizationId(),
            request.targetScope().targetEntityRef()));
    ArrayList<ReportingExportField> fields = new ArrayList<>();
    fields.add(field("disclosureRecordRef", audit.disclosureRecordRef()));
    fields.add(field("status", audit.status()));
    fields.add(field("disclosureLevel", audit.disclosureLevel()));
    fields.add(field("redactionLevel", audit.redactionLevel()));
    fields.add(field("workflowEventId", audit.workflowEventId()));
    fields.add(field("decidedAt", audit.decidedAt()));
    fields.add(field("missingReasonCodes", String.join("|", audit.missingReasonCodes())));
    return new ReportingExportPayload(
        "json",
        "disclosure_legal_audit_package",
        "admin_same_organization_disclosure_audit",
        false,
        List.of(new ReportingExportSection("disclosure-audit", fields)),
        List.of(
            "ObservabilityReadService.disclosureAuditExport",
            "workflowEvents=" + audit.workflowEvents().size(),
            "aiTaskRuns=" + audit.aiTaskRuns().size(),
            "reviewEvents=" + audit.reviewEvents().size()),
        List.of());
  }

  private static ReportingExportField field(String name, String value) {
    return new ReportingExportField(
        name,
        value == null ? "missing" : value,
        FieldVisibilityPolicy.SYSTEM_GOVERNANCE);
  }
}
