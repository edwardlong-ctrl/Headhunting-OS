package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.List;
import java.util.Objects;

public final class RetentionEvidenceExportAdapter implements ReportingExportAdapter {

  private final RetentionEvidenceSource source;

  public RetentionEvidenceExportAdapter(RetentionEvidenceSource source) {
    this.source = Objects.requireNonNull(source, "source must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    return new ReportingExportPayload(
        "json",
        "retention_delete_evidence_package",
        "retention_policy_evidence",
        false,
        source.retentionEvidenceSections(request),
        List.of("DataLifecycleService retention decision evidence"),
        List.of());
  }

  public interface RetentionEvidenceSource {

    List<ReportingExportSection> retentionEvidenceSections(ReportingExportRequest request);
  }
}
