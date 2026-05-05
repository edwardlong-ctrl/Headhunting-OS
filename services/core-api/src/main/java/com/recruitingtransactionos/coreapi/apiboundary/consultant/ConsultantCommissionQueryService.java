package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCommissionSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.CommissionApiViewMapper;
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
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantCommissionQueryService {

  private final CommissionWorkflowService commissionWorkflowService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantCommissionQueryService(CommissionWorkflowService commissionWorkflowService) {
    this(commissionWorkflowService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantCommissionQueryService(
      CommissionWorkflowService commissionWorkflowService,
      PermissionEnforcer permissionEnforcer) {
    this.commissionWorkflowService = Objects.requireNonNull(commissionWorkflowService, "commissionWorkflowService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public PagedResult<ConsultantCommissionSummaryResponse> listCommissions(AccessRequest accessRequest, PagedQuery pagedQuery) {
    requireRead(accessRequest);
    List<Commission> all = commissionWorkflowService.listCommissions(pagedQuery.organizationId());
    List<ConsultantCommissionSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(CommissionApiViewMapper::toConsultantSummary)
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  private void requireRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.COMMISSION
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "consultant_commission_read_context_required", "Consultant commission API requires a commission read context."));
    }
  }
}
