package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantAuditDrawerResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowBlockerResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEntityStateResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEventResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTimelineResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTransitionOptionResponse;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosurePrerequisiteEvaluator;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosurePrerequisites;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureServiceRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionBlocker;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionDecision;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionLegalityPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantWorkflowSurfaceService {

  private static final ActorRef WORKFLOW_PREVIEW_ACTOR =
      new ActorRef(UUID.fromString("00000000-0000-0000-0000-000000000026"), ActorRole.CONSULTANT);

  private final WorkflowAuditQueryService workflowAuditQueryService;
  private final WorkflowEntityStatePort workflowEntityStatePort;
  private final WorkflowTransitionLegalityPolicy legalityPolicy;
  private final ConsentDisclosurePrerequisiteEvaluator consentDisclosurePrerequisiteEvaluator;
  private final DisclosureRecordPort disclosureRecordPort;
  private final UnlockDecisionPort unlockDecisionPort;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantWorkflowSurfaceService(
      WorkflowAuditQueryService workflowAuditQueryService,
      WorkflowEntityStatePort workflowEntityStatePort,
      ConsentDisclosurePrerequisiteEvaluator consentDisclosurePrerequisiteEvaluator,
      DisclosureRecordPort disclosureRecordPort,
      UnlockDecisionPort unlockDecisionPort) {
    this(
        workflowAuditQueryService,
        workflowEntityStatePort,
        WorkflowTransitionLegalityPolicy.standard(),
        consentDisclosurePrerequisiteEvaluator,
        disclosureRecordPort,
        unlockDecisionPort,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantWorkflowSurfaceService(
      WorkflowAuditQueryService workflowAuditQueryService,
      WorkflowEntityStatePort workflowEntityStatePort,
      WorkflowTransitionLegalityPolicy legalityPolicy,
      ConsentDisclosurePrerequisiteEvaluator consentDisclosurePrerequisiteEvaluator,
      DisclosureRecordPort disclosureRecordPort,
      UnlockDecisionPort unlockDecisionPort,
      PermissionEnforcer permissionEnforcer) {
    this.workflowAuditQueryService = Objects.requireNonNull(workflowAuditQueryService, "workflowAuditQueryService must not be null");
    this.workflowEntityStatePort = Objects.requireNonNull(workflowEntityStatePort, "workflowEntityStatePort must not be null");
    this.legalityPolicy = Objects.requireNonNull(legalityPolicy, "legalityPolicy must not be null");
    this.consentDisclosurePrerequisiteEvaluator = Objects.requireNonNull(
        consentDisclosurePrerequisiteEvaluator,
        "consentDisclosurePrerequisiteEvaluator must not be null");
    this.disclosureRecordPort = Objects.requireNonNull(disclosureRecordPort, "disclosureRecordPort must not be null");
    this.unlockDecisionPort = Objects.requireNonNull(unlockDecisionPort, "unlockDecisionPort must not be null");
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
    List<ConsultantWorkflowEntityStateResponse> entityStates =
        entityType != null && !entityType.isBlank() && entityId != null
            ? List.of(entityState(accessRequest, organizationId, entityType, entityId))
            : List.of();
    return new ConsultantWorkflowTimelineResponse(items, entityStates, limit, offset, records.size() == limit);
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

  public ConsultantWorkflowEntityStateResponse entityState(
      AccessRequest accessRequest,
      UUID organizationId,
      String entityType,
      UUID entityId) {
    requireRead(accessRequest);
    String normalizedEntityType = normalizeEntityType(entityType);
    String namespace = namespaceFor(normalizedEntityType);
    Optional<String> stateJson = workflowEntityStatePort.getCurrentStateJson(
        organizationId,
        namespace,
        normalizedEntityType,
        entityId);
    String currentStatus = stateJson.map(ConsultantWorkflowSurfaceService::extractStatus).orElse(null);
    List<ConsultantWorkflowTransitionOptionResponse> transitionOptions = previewTransitions(
        organizationId,
        normalizedEntityType,
        entityId,
        currentStatus).stream()
            .map(this::toTransitionOptionResponse)
            .toList();
    return new ConsultantWorkflowEntityStateResponse(
        normalizedEntityType.toUpperCase(),
        entityId.toString(),
        currentStatus,
        transitionOptions);
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
        extractStatus(record.beforeState().json()),
        extractStatus(record.afterState().json()),
        record.reason(),
        record.occurredAt().toString());
  }

  private ConsultantWorkflowTransitionOptionResponse toTransitionOptionResponse(
      WorkflowTransitionDecision decision) {
    return new ConsultantWorkflowTransitionOptionResponse(
        decision.actionCode(),
        decision.currentStatus(),
        decision.targetStatus(),
        decision.allowed(),
        decision.blockers().stream()
            .map(blocker -> new ConsultantWorkflowBlockerResponse(
                blocker.code(),
                blocker.safeReason()))
            .toList());
  }

  private List<WorkflowTransitionDecision> previewTransitions(
      UUID organizationId,
      String normalizedEntityType,
      UUID entityId,
      String currentStatus) {
    List<WorkflowTransitionDecision> decisions =
        legalityPolicy.previewTransitions(normalizedEntityType, currentStatus);
    if (!"disclosure".equals(normalizedEntityType)) {
      return decisions;
    }
    return applyDisclosureGateBlockers(organizationId, entityId, decisions);
  }

  private List<WorkflowTransitionDecision> applyDisclosureGateBlockers(
      UUID organizationId,
      UUID entityId,
      List<WorkflowTransitionDecision> decisions) {
    Optional<DisclosureRecord> disclosureRecord =
        disclosureRecordPort.findByWorkflowEntityId(organizationId, entityId);
    if (disclosureRecord.isEmpty()) {
      return decisions;
    }
    DisclosureRecord record = disclosureRecord.orElseThrow();
    Optional<UnlockDecision> unlockDecision =
        unlockDecisionPort.findByRefAndOrganizationId(organizationId, record.unlockDecisionRef());
    ConsentDisclosureServiceRequest previewRequest = ConsentDisclosureServiceRequest.builder()
        .organizationId(organizationId)
        .candidateRef(record.candidateRef())
        .candidateProfileRef(record.candidateProfileRef())
        .jobRef(record.jobRef())
        .clientRef(record.clientRef())
        .consentRecordRef(record.consentRecordRef())
        .unlockDecisionRef(record.unlockDecisionRef())
        .approvedDisclosureRecordRef(record.disclosureRecordRef())
        .requestedByRole(PortalRole.CONSULTANT)
        .actor(WORKFLOW_PREVIEW_ACTOR)
        .requestedLevel(record.disclosureLevel())
        .prerequisites(new ConsentDisclosurePrerequisites(false, false, false, false, false))
        .reason("workflow_state_preview")
        .requestedAt(Instant.EPOCH)
        .build();
    ConsentDisclosurePrerequisites prerequisites =
        consentDisclosurePrerequisiteEvaluator.evaluate(
            previewRequest,
            unlockDecision,
            disclosureRecord);
    List<WorkflowTransitionBlocker> blockers = disclosureGateBlockers(
        prerequisites,
        record.disclosureLevel());
    if (blockers.isEmpty()) {
      return decisions;
    }
    return decisions.stream()
        .map(decision -> applyDisclosureGateBlockers(decision, blockers))
        .toList();
  }

  private WorkflowTransitionDecision applyDisclosureGateBlockers(
      WorkflowTransitionDecision decision,
      List<WorkflowTransitionBlocker> blockers) {
    if (!decision.allowed() || !"DISCLOSURE_IDENTITY_DISCLOSED".equals(decision.actionCode())) {
      return decision;
    }
    return WorkflowTransitionDecision.blocked(
        decision.actionCode(),
        decision.currentStatus(),
        decision.targetStatus(),
        blockers);
  }

  private static List<WorkflowTransitionBlocker> disclosureGateBlockers(
      ConsentDisclosurePrerequisites prerequisites,
      DisclosureLevel requestedLevel) {
    if (!requestedLevel.requiresUnlockAndDisclosure()) {
      return List.of();
    }
    java.util.ArrayList<WorkflowTransitionBlocker> blockers = new java.util.ArrayList<>();
    if (!prerequisites.jobActivated()) {
      blockers.add(new WorkflowTransitionBlocker(
          "job_activation_gate_required",
          "Job activation is required before identity disclosure."));
    }
    if (!prerequisites.feeAgreementActive()) {
      blockers.add(new WorkflowTransitionBlocker(
          "fee_agreement_gate_required",
          "Active fee agreement is required before identity disclosure."));
    }
    if (!prerequisites.priorContactCleared()) {
      blockers.add(new WorkflowTransitionBlocker(
          "prior_contact_review_required",
          "Prior contact review must be cleared before identity disclosure."));
    }
    if (!prerequisites.priorApplicationCleared()) {
      blockers.add(new WorkflowTransitionBlocker(
          "prior_application_review_required",
          "Prior application review must be cleared before identity disclosure."));
    }
    if (!prerequisites.privacyRiskCleared()) {
      blockers.add(new WorkflowTransitionBlocker(
          "privacy_risk_gate_required",
          "Privacy risk review must be cleared before identity disclosure."));
    }
    return List.copyOf(blockers);
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

  private static String normalizeEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new IllegalArgumentException("entityType must not be blank");
    }
    return entityType.strip().toLowerCase();
  }

  private static String namespaceFor(String normalizedEntityType) {
    return switch (normalizedEntityType) {
      case "consent", "disclosure" -> "workflow";
      default -> "recruiting";
    };
  }

  private static String extractStatus(String stateJson) {
    if (stateJson == null || stateJson.isBlank()) {
      return null;
    }
    try {
      com.fasterxml.jackson.databind.JsonNode node =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(stateJson);
      if (node.has("status") && node.get("status").isTextual()) {
        return node.get("status").asText();
      }
    } catch (Exception exception) {
      throw new IllegalArgumentException("workflow state must be valid JSON with a status field", exception);
    }
    return null;
  }
}
