package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantPlacementSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.PlacementApiViewMapper;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
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
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.service.PlacementCreateCommand;
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantPlacementCommandService {

  private final PlacementWorkflowService placementWorkflowService;
  private final CommissionService commissionService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantPlacementCommandService(
      PlacementWorkflowService placementWorkflowService,
      CommissionService commissionService) {
    this(placementWorkflowService, commissionService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantPlacementCommandService(
      PlacementWorkflowService placementWorkflowService,
      CommissionService commissionService,
      PermissionEnforcer permissionEnforcer) {
    this.placementWorkflowService = Objects.requireNonNull(placementWorkflowService, "placementWorkflowService must not be null");
    this.commissionService = Objects.requireNonNull(commissionService, "commissionService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ConsultantPlacementSummaryResponse createPlacement(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ConsultantPlacementCreateRequest request) {
    requireWrite(accessRequest);
    Placement placement = placementWorkflowService.createPlacement(new PlacementCreateCommand(
        organizationId,
        new JobId(UUID.fromString(request.jobId())),
        new CandidateId(UUID.fromString(request.candidateId())),
        new CompanyId(UUID.fromString(request.companyId())),
        actorId,
        request.salaryAmount(),
        request.salaryCurrency(),
        request.feeRatePercentage(),
        request.startDate(),
        request.guaranteeDays(),
        request.notes()));
    return mapPlacement(placement);
  }

  public ConsultantPlacementSummaryResponse acceptOffer(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markOfferAccepted(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse onboard(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markOnboarded(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse markInvoiceReady(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markInvoiceReady(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse markInvoiceSent(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markInvoiceSent(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse markPaymentPaid(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markPaymentPaid(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse activateGuarantee(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.activateGuarantee(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse completeGuarantee(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.completeGuarantee(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  public ConsultantPlacementSummaryResponse requireReplacement(AccessRequest accessRequest, UUID organizationId, UUID actorId, String placementId, int version) {
    requireWrite(accessRequest);
    return mapPlacement(placementWorkflowService.markReplacementRequired(organizationId, new PlacementId(UUID.fromString(placementId)), actorId, version));
  }

  private ConsultantPlacementSummaryResponse mapPlacement(Placement placement) {
    List<Commission> commissions = commissionService.findCommissionsByPlacementIdAndOrganizationId(
        placement.organizationId(), placement.placementId());
    return PlacementApiViewMapper.toConsultantSummary(placement, commissions);
  }

  private void requireWrite(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.PLACEMENT
        || accessRequest.action() != AccessAction.UPDATE
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "consultant_placement_write_context_required", "Consultant placement command API requires a placement write context."));
    }
  }
}
