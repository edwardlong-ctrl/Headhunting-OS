package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityReviewEventSearchResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityWorkflowEventSearchResponse;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReadService;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReviewEventQuery;
import com.recruitingtransactionos.coreapi.observability.ObservabilityWorkflowEventQuery;
import java.util.List;
import java.util.Objects;

public final class ConsultantActivityExportAdapter implements ReportingExportAdapter {

  private final ObservabilityReadService observabilityReadService;

  public ConsultantActivityExportAdapter(ObservabilityReadService observabilityReadService) {
    this.observabilityReadService = Objects.requireNonNull(
        observabilityReadService,
        "observabilityReadService must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    ObservabilityWorkflowEventSearchResponse workflowEvents =
        observabilityReadService.searchWorkflowEvents(new ObservabilityWorkflowEventQuery(
            request.organizationId(),
            null,
            null,
            null,
            null,
            "consultant",
            null,
            null,
            null,
            null,
            null,
            100,
            0));
    ObservabilityReviewEventSearchResponse reviewEvents =
        observabilityReadService.searchReviewEvents(new ObservabilityReviewEventQuery(
            request.organizationId(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            100,
            0));
    return new ReportingExportPayload(
        "json",
        "consultant_activity_report",
        "same_organization_consultant_activity_supervision",
        false,
        List.of(new ReportingExportSection("consultant-activity", List.of(
            field("workflowEventCount", String.valueOf(workflowEvents.items().size())),
            field("reviewEventCount", String.valueOf(reviewEvents.items().size()))))),
        List.of("ObservabilityReadService.searchWorkflowEvents", "ObservabilityReadService.searchReviewEvents"),
        List.of());
  }

  private static ReportingExportField field(String name, String value) {
    return new ReportingExportField(name, value, FieldVisibilityPolicy.CONSULTANT_INTERNAL);
  }
}
