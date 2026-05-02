package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEntityStateResponse;
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
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.workflowaudit.WorkflowTransitionLegalityPolicy;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultantWorkflowSurfaceServiceTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026a001");
  private static final UUID DISCLOSURE_ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026a002");

  @Test
  void disclosurePreviewIncludesRealGateBlockers() {
    WorkflowEntityStatePort statePort = (organizationId, namespace, entityType, entityId) ->
        Optional.of("{\"status\":\"approved\"}");
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
        evaluator,
        disclosureRecordPort,
        unlockDecisionPort,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantWorkflowEntityStateResponse response = service.entityState(
        consultantWorkflowReadAccess(),
        ORG_ID,
        "DISCLOSURE",
        DISCLOSURE_ENTITY_ID);

    assertThat(response.currentStatus()).isEqualTo("approved");
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
        DisclosureStatus.APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock-ref-26",
        "consent-ref-26",
        Optional.empty(),
        Instant.parse("2026-05-02T00:00:00Z"));
  }
}
