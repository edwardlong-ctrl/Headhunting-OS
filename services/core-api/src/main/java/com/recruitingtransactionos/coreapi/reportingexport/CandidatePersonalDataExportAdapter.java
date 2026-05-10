package com.recruitingtransactionos.coreapi.reportingexport;

import java.util.List;
import java.util.Objects;

public final class CandidatePersonalDataExportAdapter implements ReportingExportAdapter {

  private final CandidatePersonalDataSource source;

  public CandidatePersonalDataExportAdapter(CandidatePersonalDataSource source) {
    this.source = Objects.requireNonNull(source, "source must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    List<ReportingExportSection> sections = source.selfScopedCandidateSections(request)
        .stream()
        .map(CandidatePersonalDataExportAdapter::candidateSelfOnly)
        .toList();
    return new ReportingExportPayload(
        "json",
        "candidate_personal_data_export",
        "candidate_self_access_profile_documents_consents_status",
        false,
        sections,
        List.of("Candidate self-scoped profile/document/consent/status source"),
        List.of());
  }

  private static ReportingExportSection candidateSelfOnly(ReportingExportSection section) {
    return new ReportingExportSection(
        section.title(),
        section.fields().stream()
            .map(field -> new ReportingExportField(
                field.name(),
                field.value(),
                field.visibilityPolicy() == FieldVisibilityPolicy.RAW_PII
                    ? FieldVisibilityPolicy.CANDIDATE_SELF
                    : field.visibilityPolicy()))
            .toList());
  }

  public interface CandidatePersonalDataSource {

    List<ReportingExportSection> selfScopedCandidateSections(ReportingExportRequest request);
  }
}
