package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantPlacementSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.PlacementApiViewMapper;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.service.CommissionService;
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
public final class ConsultantPlacementQueryService {

  private final PlacementWorkflowService placementWorkflowService;
  private final CommissionService commissionService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantPlacementQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionService commissionService) {
    this(placementWorkflowService, commissionService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantPlacementQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionService commissionService,
      PermissionEnforcer permissionEnforcer) {
    this.placementWorkflowService = Objects.requireNonNull(placementWorkflowService, "placementWorkflowService must not be null");
    this.commissionService = Objects.requireNonNull(commissionService, "commissionService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public PagedResult<ConsultantPlacementSummaryResponse> listPlacements(
      AccessRequest accessRequest,
      PagedQuery pagedQuery) {
    requireRead(accessRequest);
    List<Placement> all = placementWorkflowService.listPlacements(pagedQuery.organizationId());
    Map<PlacementId, List<Commission>> commissionsByPlacement = commissionService.findAllCommissionsByOrganizationId(
            pagedQuery.organizationId())
        .stream()
        .collect(Collectors.groupingBy(Commission::placementId));
    List<ConsultantPlacementSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(placement -> PlacementApiViewMapper.toConsultantSummary(
            placement,
            commissionsByPlacement.getOrDefault(placement.placementId(), List.of())))
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  private void requireRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.PLACEMENT
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "consultant_placement_read_context_required", "Consultant placement API requires a placement read context."));
    }
  }
}
