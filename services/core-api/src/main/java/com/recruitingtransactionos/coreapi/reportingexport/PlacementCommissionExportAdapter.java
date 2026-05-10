package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.apiboundary.OwnerAccountingExportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.owner.OwnerRevenueQueryService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PlacementCommissionExportAdapter implements ReportingExportAdapter {

  private final OwnerRevenueQueryService ownerRevenueQueryService;

  public PlacementCommissionExportAdapter(OwnerRevenueQueryService ownerRevenueQueryService) {
    this.ownerRevenueQueryService = Objects.requireNonNull(
        ownerRevenueQueryService,
        "ownerRevenueQueryService must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    OwnerAccountingExportResponse accounting = ownerRevenueQueryService.exportAccountingHandoff(
        access(),
        request.organizationId());
    return new ReportingExportPayload(
        accounting.format(),
        accounting.process(),
        accounting.disclaimer(),
        false,
        List.of(new ReportingExportSection("placement-commission-accounting-handoff", List.of(
            field("generatedAt", accounting.generatedAt()),
            field("content", accounting.content())))),
        List.of("OwnerRevenueQueryService.exportAccountingHandoff"),
        List.of());
  }

  private static ReportingExportField field(String name, String value) {
    return new ReportingExportField(name, value, FieldVisibilityPolicy.COMMERCIAL_READ_ONLY);
  }

  private static AccessRequest access() {
    return new AccessRequest(
        PortalRole.OWNER,
        ResourceType.REVENUE_REPORT,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
