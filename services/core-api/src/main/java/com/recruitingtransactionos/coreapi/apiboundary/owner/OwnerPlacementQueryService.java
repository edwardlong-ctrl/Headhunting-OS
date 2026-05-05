package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.OwnerCommissionSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.OwnerPlacementSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.CommissionApiViewMapper;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.PlacementApiViewMapper;
import com.recruitingtransactionos.coreapi.commission.Commission;
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
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class OwnerPlacementQueryService {

  private final PlacementWorkflowService placementWorkflowService;
  private final CommissionWorkflowService commissionWorkflowService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public OwnerPlacementQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService) {
    this(placementWorkflowService, commissionWorkflowService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  OwnerPlacementQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService,
      PermissionEnforcer permissionEnforcer) {
    this.placementWorkflowService = Objects.requireNonNull(placementWorkflowService, "placementWorkflowService must not be null");
    this.commissionWorkflowService = Objects.requireNonNull(commissionWorkflowService, "commissionWorkflowService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public PagedResult<OwnerPlacementSummaryResponse> listPlacements(AccessRequest accessRequest, PagedQuery pagedQuery) {
    requirePlacementRead(accessRequest);
    List<Placement> all = placementWorkflowService.listPlacements(pagedQuery.organizationId());
    List<Commission> commissions = commissionWorkflowService.listCommissions(pagedQuery.organizationId());
    Map<PlacementId, List<Commission>> commissionsByPlacement = commissions.stream()
        .collect(Collectors.groupingBy(Commission::placementId));
    List<OwnerPlacementSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(placement -> PlacementApiViewMapper.toOwnerSummary(
            placement,
            commissionsByPlacement.getOrDefault(placement.placementId(), List.of())))
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public PagedResult<OwnerCommissionSummaryResponse> listCommissions(AccessRequest accessRequest, PagedQuery pagedQuery) {
    requireCommissionRead(accessRequest);
    List<Commission> all = commissionWorkflowService.listCommissions(pagedQuery.organizationId());
    List<OwnerCommissionSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(CommissionApiViewMapper::toOwnerSummary)
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  private void requirePlacementRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.PLACEMENT
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_placement_read_context_required", "Owner placement API requires a placement read context."));
    }
  }

  private void requireCommissionRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.COMMISSION
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_commission_read_context_required", "Owner commission API requires a commission read context."));
    }
  }
}
