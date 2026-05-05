package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.OwnerRevenueSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.PlacementApiViewMapper;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class OwnerRevenueQueryService {

  private final PlacementWorkflowService placementWorkflowService;
  private final CommissionWorkflowService commissionWorkflowService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public OwnerRevenueQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService) {
    this(placementWorkflowService, commissionWorkflowService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  OwnerRevenueQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService,
      PermissionEnforcer permissionEnforcer) {
    this.placementWorkflowService = Objects.requireNonNull(placementWorkflowService, "placementWorkflowService must not be null");
    this.commissionWorkflowService = Objects.requireNonNull(commissionWorkflowService, "commissionWorkflowService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public OwnerRevenueSummaryResponse load(AccessRequest accessRequest, java.util.UUID organizationId) {
    requireRevenueRead(accessRequest);
    List<Placement> placements = placementWorkflowService.listPlacements(organizationId);
    List<Commission> commissions = commissionWorkflowService.listCommissions(organizationId);
    Map<PlacementId, List<Commission>> commissionsByPlacement = commissions.stream()
        .collect(Collectors.groupingBy(Commission::placementId));
    BigDecimal totalExpectedFee = placements.stream()
        .map(placement -> expectedFeeAmount(placement, commissionsByPlacement))
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int unknownExpectedFeePlacementCount = (int) placements.stream()
        .filter(placement -> expectedFeeAmount(placement, commissionsByPlacement) == null)
        .count();
    List<Commission> paidCommissions = commissions.stream()
        .filter(commission -> commission.status() == CommissionStatus.PAID)
        .toList();
    BigDecimal totalPaidFee = paidCommissions.stream()
        .map(Commission::amount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int paidCommissionMissingAmountCount = (int) paidCommissions.stream()
        .filter(commission -> commission.amount() == null)
        .count();
    int activeGuaranteeCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.GUARANTEE_ACTIVE).count();
    int replacementRequiredCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.REPLACEMENT_REQUIRED).count();
    int invoiceInFlightCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.INVOICE_READY || placement.status() == PlacementStatus.INVOICE_SENT).count();
    int paidCommissionCount = paidCommissions.size();
    int pendingCommissionCount = (int) commissions.stream().filter(commission -> commission.status() == CommissionStatus.PENDING || commission.status() == CommissionStatus.CALCULATED).count();
    return new OwnerRevenueSummaryResponse(
        totalExpectedFee,
        totalPaidFee,
        placements.size(),
        unknownExpectedFeePlacementCount,
        pendingCommissionCount,
        paidCommissionCount,
        paidCommissionMissingAmountCount,
        activeGuaranteeCount,
        replacementRequiredCount,
        invoiceInFlightCount);
  }

  private static BigDecimal expectedFeeAmount(
      Placement placement,
      Map<PlacementId, List<Commission>> commissionsByPlacement) {
    return PlacementApiViewMapper.expectedFeeAmount(
        placement,
        commissionsByPlacement.getOrDefault(placement.placementId(), List.of()));
  }

  private void requireRevenueRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.REVENUE_REPORT
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_revenue_read_context_required", "Owner revenue API requires a revenue read context."));
    }
  }
}
