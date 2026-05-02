package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantAuditDrawerResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEventResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTimelineResponse;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantWorkflowSurfaceService {

  private final WorkflowAuditQueryService workflowAuditQueryService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantWorkflowSurfaceService(WorkflowAuditQueryService workflowAuditQueryService) {
    this(workflowAuditQueryService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantWorkflowSurfaceService(
      WorkflowAuditQueryService workflowAuditQueryService,
      PermissionEnforcer permissionEnforcer) {
    this.workflowAuditQueryService = Objects.requireNonNull(workflowAuditQueryService, "workflowAuditQueryService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ConsultantWorkflowTimelineResponse timeline(
      AccessRequest accessRequest,
      UUID organizationId,
      String entityType,
      UUID entityId,
      int limit,
      int offset) {
    requireRead(accessRequest);
    WorkflowAuditQuery.Builder builder = WorkflowAuditQuery.builder(organizationId)
        .limit(limit)
        .offset(offset);
    if (entityType != null && !entityType.isBlank()) {
      builder.entityType(entityType);
    }
    if (entityId != null) {
      builder.entityId(entityId);
    }
    List<WorkflowAuditRecord> records = workflowAuditQueryService.search(builder.build());
    List<ConsultantWorkflowEventResponse> items = records.stream().map(this::toResponse).toList();
    return new ConsultantWorkflowTimelineResponse(items, limit, offset, records.size() == limit);
  }

  public ConsultantAuditDrawerResponse auditDrawer(
      AccessRequest accessRequest,
      UUID organizationId,
      String entityType,
      UUID entityId,
      int limit) {
    requireRead(accessRequest);
    List<ConsultantWorkflowEventResponse> items = workflowAuditQueryService.search(
            WorkflowAuditQuery.builder(organizationId)
                .entityType(entityType)
                .entityId(entityId)
                .limit(limit)
                .offset(0)
                .build())
        .stream()
        .map(this::toResponse)
        .toList();
    return new ConsultantAuditDrawerResponse(entityType, entityId.toString(), items);
  }

  private ConsultantWorkflowEventResponse toResponse(WorkflowAuditRecord record) {
    return new ConsultantWorkflowEventResponse(
        record.workflowEventId().value().toString(),
        record.entityType(),
        record.entityId().toString(),
        record.actionCode(),
        record.actorType().name().toLowerCase(),
        record.aiInvolvement().wireValue(),
        record.riskTier().wireValue(),
        null,
        null,
        record.reason(),
        record.occurredAt().toString());
  }

  private void requireRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.WORKFLOW_EVENT || accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "workflow_read_context_required",
          "Consultant workflow API requires a workflow read context."));
    }
  }
}
