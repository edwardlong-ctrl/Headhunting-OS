package com.recruitingtransactionos.coreapi.reportingexport;

import com.recruitingtransactionos.coreapi.apiboundary.OwnerRevenueSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.owner.OwnerPlacementQueryService;
import com.recruitingtransactionos.coreapi.apiboundary.owner.OwnerRevenueQueryService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class OwnerReportExportAdapter implements ReportingExportAdapter {

  private final OwnerRevenueQueryService ownerRevenueQueryService;
  private final OwnerPlacementQueryService ownerPlacementQueryService;

  public OwnerReportExportAdapter(
      OwnerRevenueQueryService ownerRevenueQueryService,
      OwnerPlacementQueryService ownerPlacementQueryService) {
    this.ownerRevenueQueryService = Objects.requireNonNull(
        ownerRevenueQueryService,
        "ownerRevenueQueryService must not be null");
    this.ownerPlacementQueryService = Objects.requireNonNull(
        ownerPlacementQueryService,
        "ownerPlacementQueryService must not be null");
  }

  @Override
  public ReportingExportPayload export(ReportingExportRequest request) {
    OwnerRevenueSummaryResponse revenue = ownerRevenueQueryService.load(
        access(ResourceType.REVENUE_REPORT),
        request.organizationId());
    PagedResult<?> placements = ownerPlacementQueryService.listPlacements(
        access(ResourceType.PLACEMENT),
        PagedQuery.builder(request.organizationId()).limit(100).build());
    ArrayList<ReportingExportField> fields = new ArrayList<>();
    fields.add(field("placementCount", String.valueOf(revenue.placementCount())));
    fields.add(field("totalExpectedFee", String.valueOf(revenue.totalExpectedFee())));
    fields.add(field("totalPaidFee", String.valueOf(revenue.totalPaidFee())));
    fields.add(field("invoiceInFlightCount", String.valueOf(revenue.invoiceInFlightCount())));
    fields.add(field("placementRowsIncluded", String.valueOf(placements.items().size())));
    return new ReportingExportPayload(
        "json",
        "owner_operating_report",
        "owner_same_organization_operating_supervision",
        false,
        List.of(new ReportingExportSection("owner-report", fields)),
        List.of("OwnerRevenueQueryService", "OwnerPlacementQueryService"),
        List.of());
  }

  private static ReportingExportField field(String name, String value) {
    return new ReportingExportField(name, value, FieldVisibilityPolicy.OWNER_INTERNAL);
  }

  private static AccessRequest access(ResourceType resourceType) {
    return new AccessRequest(
        PortalRole.OWNER,
        resourceType,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }
}
