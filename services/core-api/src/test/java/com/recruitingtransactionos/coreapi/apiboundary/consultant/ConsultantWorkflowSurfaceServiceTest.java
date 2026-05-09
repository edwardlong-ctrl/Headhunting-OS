package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEntityStateResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTimelineResponse;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosurePrerequisiteEvaluator;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosurePrerequisites;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureServiceRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.workflowautomation.WorkflowAutomationPolicy;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionLegalityPolicy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class ConsultantWorkflowSurfaceServiceTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026a001");
  private static final UUID DISCLOSURE_ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026a002");

  @Test
  void disclosurePreviewIncludesRealGateBlockers() {
    WorkflowEntityStatePort statePort = (organizationId, namespace, entityType, entityId) ->
        Optional.of("{\"status\":\"consultant_approved\"}");
    ConsentDisclosurePrerequisiteEvaluator evaluator =
        new ConsentDisclosurePrerequisiteEvaluator() {
          @Override
          public ConsentDisclosurePrerequisites evaluate(
              ConsentDisclosureServiceRequest request,
              Optional<UnlockDecision> unlockDecision,
              Optional<DisclosureRecord> disclosureRecord) {
            return new ConsentDisclosurePrerequisites(false, true, true, true, false);
          }
        };
    DisclosureRecordPort disclosureRecordPort = new DisclosureRecordPort() {
      @Override
      public DisclosureRecord append(DisclosureRecord disclosureRecord) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Optional<DisclosureRecord> findByRefAndOrganizationId(UUID organizationId, String disclosureRecordRef) {
        return Optional.empty();
      }

      @Override
      public Optional<DisclosureRecord> findByWorkflowEntityId(UUID organizationId, UUID workflowEntityId) {
        return Optional.of(sampleDisclosureRecord());
      }

      @Override
      public DisclosureRecord transitionToIdentityDisclosed(
          UUID organizationId,
          String disclosureRecordRef,
          com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId workflowEventId,
          Instant decidedAt) {
        throw new UnsupportedOperationException();
      }
    };
    UnlockDecisionPort unlockDecisionPort = new UnlockDecisionPort() {
      @Override
      public UnlockDecision append(UnlockDecision unlockDecision) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Optional<UnlockDecision> findByRefAndOrganizationId(UUID organizationId, String unlockDecisionRef) {
        return Optional.empty();
      }
    };
    ConsultantWorkflowSurfaceService service = new ConsultantWorkflowSurfaceService(
        mock(WorkflowAuditQueryService.class),
        statePort,
        WorkflowTransitionLegalityPolicy.standard(),
        WorkflowAutomationPolicy.standard(),
        evaluator,
        disclosureRecordPort,
        unlockDecisionPort,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantWorkflowEntityStateResponse response = service.entityState(
        consultantWorkflowReadAccess(),
        ORG_ID,
        "DISCLOSURE",
        DISCLOSURE_ENTITY_ID);

    assertThat(response.currentStatus()).isEqualTo("consultant_approved");
    assertThat(response.transitionOptions())
        .anySatisfy(option -> {
          if ("DISCLOSURE_IDENTITY_DISCLOSED".equals(option.actionCode())) {
            assertThat(option.allowed()).isFalse();
            assertThat(option.blockers())
                .extracting(blocker -> blocker.code())
                .contains("job_activation_gate_required", "privacy_risk_gate_required");
          }
        });
  }

  @Test
  void timelineExposesCardStatusTransitionWhenWorkflowSnapshotContainsIt() {
    WorkflowAuditQueryService workflowAuditQueryService = mock(WorkflowAuditQueryService.class);
    when(workflowAuditQueryService.search(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(sampleShortlistCardAuditRecord()));
    ConsultantWorkflowSurfaceService service = new ConsultantWorkflowSurfaceService(
        workflowAuditQueryService,
        (organizationId, namespace, entityType, entityId) -> Optional.empty(),
        WorkflowTransitionLegalityPolicy.standard(),
        WorkflowAutomationPolicy.standard(),
        mock(ConsentDisclosurePrerequisiteEvaluator.class),
        mock(DisclosureRecordPort.class),
        mock(UnlockDecisionPort.class),
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantWorkflowTimelineResponse response = service.timeline(
        consultantWorkflowReadAccess(),
        ORG_ID,
        "SHORTLIST",
        DISCLOSURE_ENTITY_ID,
        20,
        0);

    assertThat(response.items()).singleElement().satisfies(item -> {
      assertThat(item.beforeStatus()).isEqualTo("draft");
      assertThat(item.afterStatus()).isEqualTo("draft");
      assertThat(item.beforeCardStatus()).isEqualTo("included");
      assertThat(item.afterCardStatus()).isEqualTo("removed");
    });
  }

  @Test
  void automationQueueSurfacesStalledConsultantOwnedWorkflowActions() {
    WorkflowAuditQueryService workflowAuditQueryService = mock(WorkflowAuditQueryService.class);
    when(workflowAuditQueryService.search(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(sampleConsentAuditRecord()));
    ConsultantWorkflowSurfaceService service = new ConsultantWorkflowSurfaceService(
        workflowAuditQueryService,
        (organizationId, namespace, entityType, entityId) -> Optional.empty(),
        WorkflowTransitionLegalityPolicy.standard(),
        WorkflowAutomationPolicy.standard(),
        mock(ConsentDisclosurePrerequisiteEvaluator.class),
        mock(DisclosureRecordPort.class),
        mock(UnlockDecisionPort.class),
        new PermissionEnforcer(new PermissionEvaluator()));

    var response = service.automationQueue(
        consultantWorkflowReadAccess(),
        ORG_ID,
        50,
        Instant.parse("2026-05-04T01:00:00Z"));

    assertThat(response.items()).singleElement().satisfies(item -> {
      assertThat(item.workflowFamily()).isEqualTo("consent");
      assertThat(item.status()).isEqualTo("ESCALATED");
      assertThat(item.dueAt()).isEqualTo("2026-05-03T00:00:00Z");
      assertThat(item.nextBestAction()).contains("consent follow-up");
    });
    ArgumentCaptor<WorkflowAuditQuery> queryCaptor = ArgumentCaptor.forClass(WorkflowAuditQuery.class);
    verify(workflowAuditQueryService).search(queryCaptor.capture());
    assertThat(queryCaptor.getValue().limit()).isEqualTo(WorkflowAuditQuery.MAX_LIMIT);
  }

  @Test
  void automationQueueAppliesLimitAfterFilteringEligibleConsultantActions() {
    WorkflowAuditQueryService workflowAuditQueryService = mock(WorkflowAuditQueryService.class);
    when(workflowAuditQueryService.search(any()))
        .thenReturn(List.of(
            sampleClientFeedbackAuditRecord(),
            sampleConsentAuditRecord(),
            sampleInterviewAuditRecord()));
    ConsultantWorkflowSurfaceService service = new ConsultantWorkflowSurfaceService(
        workflowAuditQueryService,
        (organizationId, namespace, entityType, entityId) -> Optional.empty(),
        WorkflowTransitionLegalityPolicy.standard(),
        WorkflowAutomationPolicy.standard(),
        mock(ConsentDisclosurePrerequisiteEvaluator.class),
        mock(DisclosureRecordPort.class),
        mock(UnlockDecisionPort.class),
        new PermissionEnforcer(new PermissionEvaluator()));

    var response = service.automationQueue(
        consultantWorkflowReadAccess(),
        ORG_ID,
        1,
        Instant.parse("2026-05-04T01:00:00Z"));

    assertThat(response.items()).singleElement().satisfies(item -> {
      assertThat(item.workflowFamily()).isEqualTo("consent");
      assertThat(item.ownerRole()).isEqualTo("consultant");
    });
  }

  @Test
  void timelineExportReturnsCsvWithAutomationDueDates() {
    WorkflowAuditQueryService workflowAuditQueryService = mock(WorkflowAuditQueryService.class);
    when(workflowAuditQueryService.search(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(sampleConsentAuditRecord()));
    ConsultantWorkflowSurfaceService service = new ConsultantWorkflowSurfaceService(
        workflowAuditQueryService,
        (organizationId, namespace, entityType, entityId) -> Optional.empty(),
        WorkflowTransitionLegalityPolicy.standard(),
        WorkflowAutomationPolicy.standard(),
        mock(ConsentDisclosurePrerequisiteEvaluator.class),
        mock(DisclosureRecordPort.class),
        mock(UnlockDecisionPort.class),
        new PermissionEnforcer(new PermissionEvaluator()));

    var response = service.timelineExport(
        consultantWorkflowReadAccess(),
        ORG_ID,
        "CANDIDATE",
        DISCLOSURE_ENTITY_ID,
        100,
        Instant.parse("2026-05-04T01:00:00Z"));

    assertThat(response.format()).isEqualTo("csv");
    assertThat(response.content())
        .contains("CONSENT_REQUESTED")
        .contains("2026-05-03T00:00:00Z")
        .doesNotContain("raw_candidate");
  }

  private static AccessRequest consultantWorkflowReadAccess() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.WORKFLOW_EVENT,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static DisclosureRecord sampleDisclosureRecord() {
    return new DisclosureRecord(
        "disclosure-ref-26",
        ORG_ID,
        "candidate-ref-26",
        "candidate-profile-ref-26",
        "job-ref-26",
        "client-ref-26",
        DisclosureStatus.CONSULTANT_APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock-ref-26",
        "consent-ref-26",
        Optional.empty(),
        Instant.parse("2026-05-02T00:00:00Z"));
  }

  private static WorkflowAuditRecord sampleShortlistCardAuditRecord() {
    Instant occurredAt = Instant.parse("2026-05-02T03:00:00Z");
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000026a101")),
        ORG_ID,
        "workflow",
        "SHORTLIST",
        DISCLOSURE_ENTITY_ID,
        "SHORTLIST_CARD_REMOVED",
        ActorRole.CONSULTANT,
        UUID.fromString("00000000-0000-0000-0000-00000026a102"),
        WorkflowAiInvolvement.NONE,
        RiskTier.T2_MEDIUM_RISK,
        new WorkflowStateSnapshot("{\"status\":\"draft\",\"cardStatus\":\"included\"}"),
        new WorkflowStateSnapshot("{\"status\":\"draft\",\"cardStatus\":\"removed\"}"),
        "candidate card removed from shortlist builder",
        new WorkflowIdempotencyKey("workflow-test-idempotency"),
        WorkflowCorrelationId.fromWireValue("00000000-0000-0000-0000-00000026a104"),
        WorkflowCausationId.fromWireValue("00000000-0000-0000-0000-00000026a105"),
        null,
        "shortlist_builder",
        UUID.fromString("00000000-0000-0000-0000-00000026a103"),
        occurredAt,
        occurredAt);
  }

  private static WorkflowAuditRecord sampleConsentAuditRecord() {
    Instant occurredAt = Instant.parse("2026-05-01T00:00:00Z");
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000026a201")),
        ORG_ID,
        "workflow",
        "CANDIDATE",
        DISCLOSURE_ENTITY_ID,
        "CONSENT_REQUESTED",
        ActorRole.CONSULTANT,
        UUID.fromString("00000000-0000-0000-0000-00000026a202"),
        WorkflowAiInvolvement.AI_ASSISTED,
        RiskTier.T2_MEDIUM_RISK,
        new WorkflowStateSnapshot("{\"status\":\"not_requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        "candidate consent requested",
        new WorkflowIdempotencyKey("workflow-consent-sla-test"),
        WorkflowCorrelationId.fromWireValue("00000000-0000-0000-0000-00000026a204"),
        WorkflowCausationId.fromWireValue("00000000-0000-0000-0000-00000026a205"),
        null,
        "consent_workflow",
        UUID.fromString("00000000-0000-0000-0000-00000026a203"),
        occurredAt,
        occurredAt);
  }

  private static WorkflowAuditRecord sampleInterviewAuditRecord() {
    Instant occurredAt = Instant.parse("2026-05-01T00:00:00Z");
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000026a301")),
        ORG_ID,
        "workflow",
        "CANDIDATE",
        DISCLOSURE_ENTITY_ID,
        "CANDIDATE_INTERVIEWING",
        ActorRole.CONSULTANT,
        UUID.fromString("00000000-0000-0000-0000-00000026a302"),
        WorkflowAiInvolvement.AI_ASSISTED,
        RiskTier.T2_MEDIUM_RISK,
        new WorkflowStateSnapshot("{\"status\":\"screening\"}"),
        new WorkflowStateSnapshot("{\"status\":\"interviewing\"}"),
        "candidate moved to interview",
        new WorkflowIdempotencyKey("workflow-interview-sla-test"),
        WorkflowCorrelationId.fromWireValue("00000000-0000-0000-0000-00000026a304"),
        WorkflowCausationId.fromWireValue("00000000-0000-0000-0000-00000026a305"),
        null,
        "interview_workflow",
        UUID.fromString("00000000-0000-0000-0000-00000026a303"),
        occurredAt,
        occurredAt);
  }

  private static WorkflowAuditRecord sampleClientFeedbackAuditRecord() {
    Instant occurredAt = Instant.parse("2026-05-01T00:00:00Z");
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000026a401")),
        ORG_ID,
        "workflow",
        "SHORTLIST",
        DISCLOSURE_ENTITY_ID,
        "SHORTLIST_CLIENT_FEEDBACK_PENDING",
        ActorRole.CLIENT,
        UUID.fromString("00000000-0000-0000-0000-00000026a402"),
        WorkflowAiInvolvement.AI_ASSISTED,
        RiskTier.T2_MEDIUM_RISK,
        new WorkflowStateSnapshot("{\"status\":\"client_viewed\"}"),
        new WorkflowStateSnapshot("{\"status\":\"client_feedback_pending\"}"),
        "client feedback pending",
        new WorkflowIdempotencyKey("workflow-client-feedback-sla-test"),
        WorkflowCorrelationId.fromWireValue("00000000-0000-0000-0000-00000026a404"),
        WorkflowCausationId.fromWireValue("00000000-0000-0000-0000-00000026a405"),
        null,
        "feedback_workflow",
        UUID.fromString("00000000-0000-0000-0000-00000026a403"),
        occurredAt,
        occurredAt);
  }
}
