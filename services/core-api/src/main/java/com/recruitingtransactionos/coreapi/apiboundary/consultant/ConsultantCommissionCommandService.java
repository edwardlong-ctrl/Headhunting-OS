package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCommissionSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.CommissionApiViewMapper;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.commission.service.CommissionCreateCommand;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantCommissionCommandService {

  private final CommissionWorkflowService commissionWorkflowService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantCommissionCommandService(CommissionWorkflowService commissionWorkflowService) {
    this(commissionWorkflowService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantCommissionCommandService(
      CommissionWorkflowService commissionWorkflowService,
      PermissionEnforcer permissionEnforcer) {
    this.commissionWorkflowService = Objects.requireNonNull(commissionWorkflowService, "commissionWorkflowService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ConsultantCommissionSummaryResponse createCommission(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ConsultantCommissionCreateRequest request) {
    requireWrite(accessRequest);
    Commission commission = commissionWorkflowService.createCommission(new CommissionCreateCommand(
        organizationId,
        new PlacementId(UUID.fromString(request.placementId())),
        actorId,
        actorId,
        CommissionType.fromWireValue(request.commissionType()),
        request.amount(),
        request.currency(),
        request.splitPercentage(),
        request.salaryAmount(),
        request.feeRatePercentage()));
    return CommissionApiViewMapper.toConsultantSummary(commission);
  }

  public ConsultantCommissionSummaryResponse markPaid(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      String commissionId,
      int version) {
    requireWrite(accessRequest);
    return CommissionApiViewMapper.toConsultantSummary(
        commissionWorkflowService.markPaid(organizationId, new CommissionId(UUID.fromString(commissionId)), actorId, version));
  }

  public ConsultantCommissionSummaryResponse withhold(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      String commissionId,
      ConsultantCommissionWithholdRequest request) {
    requireWrite(accessRequest);
    return CommissionApiViewMapper.toConsultantSummary(
        commissionWorkflowService.withhold(organizationId, new CommissionId(UUID.fromString(commissionId)), actorId, request.version(), request.reason()));
  }

  private void requireWrite(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.COMMISSION
        || accessRequest.action() != AccessAction.UPDATE
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "consultant_commission_write_context_required", "Consultant commission command API requires a commission write context."));
    }
  }
}
