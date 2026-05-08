package com.recruitingtransactionos.coreapi.apiboundary.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityDisclosureAuditExportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityWorkflowEventSearchResponse;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReadService;
import com.recruitingtransactionos.coreapi.observability.ObservabilityWorkflowEventQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminObservabilityControllerPolicyTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000400101");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000400102");
  private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000400103");

  private ObservabilityReadService observabilityReadService;
  private PermissionEnforcer permissionEnforcer;
  private AdminObservabilityController controller;

  @BeforeEach
  void setUp() {
    observabilityReadService = mock(ObservabilityReadService.class);
    permissionEnforcer = mock(PermissionEnforcer.class);
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed", "allowed"));
    controller = new AdminObservabilityController(observabilityReadService, permissionEnforcer);
  }

  @Test
  void workflowEventSearchUsesAdminGovernanceReadPolicy() {
    when(observabilityReadService.searchWorkflowEvents(any()))
        .thenReturn(new ObservabilityWorkflowEventSearchResponse(List.of(), 50, 0, false));

    controller.workflowEvents(
        principal(PortalRole.ADMIN),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        50,
        0);

    AccessRequest accessRequest = capturedAccessRequest();
    assertThat(accessRequest.actorRole()).isEqualTo(PortalRole.ADMIN);
    assertThat(accessRequest.resourceType()).isEqualTo(ResourceType.ADMIN_GOVERNANCE);
    assertThat(accessRequest.action()).isEqualTo(AccessAction.READ);
    assertThat(accessRequest.fieldClassification()).isEqualTo(FieldClassification.SYSTEM_GOVERNANCE);
  }

  @Test
  void clientRoleIsDeniedBeforeObservabilitySearchRuns() {
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed", "allowed"));

    var response = controller.workflowEvents(
        principal(PortalRole.CLIENT), null, null, null, null, null, null, null, null, null, null, 50, 0);

    assertThat(response.getStatusCode().value()).isEqualTo(403);
  }

  @Test
  void workflowEventSearchAcceptsActorAndTimeRangeFilters() {
    UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000400104");
    Instant occurredFrom = Instant.parse("2026-05-08T01:00:00Z");
    Instant occurredTo = Instant.parse("2026-05-08T02:00:00Z");
    when(observabilityReadService.searchWorkflowEvents(any()))
        .thenReturn(new ObservabilityWorkflowEventSearchResponse(List.of(), 50, 0, false));

    controller.workflowEvents(
        principal(PortalRole.ADMIN),
        null,
        null,
        null,
        null,
        "admin",
        actorId,
        null,
        null,
        occurredFrom,
        occurredTo,
        50,
        0);

    ArgumentCaptor<ObservabilityWorkflowEventQuery> captor =
        ArgumentCaptor.forClass(ObservabilityWorkflowEventQuery.class);
    verify(observabilityReadService).searchWorkflowEvents(captor.capture());
    assertThat(captor.getValue().actorId()).isEqualTo(actorId);
    assertThat(captor.getValue().occurredFrom()).isEqualTo(occurredFrom);
    assertThat(captor.getValue().occurredTo()).isEqualTo(occurredTo);
  }

  @Test
  void disclosureAuditExportUsesExplicitAdminExportPolicy() {
    when(observabilityReadService.disclosureAuditExport(any()))
        .thenReturn(new ObservabilityDisclosureAuditExportResponse(
            "disclosure_ref_1",
            "missing_disclosure_record",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("missing_disclosure_record"),
            List.of(),
            List.of(),
            List.of()));

    controller.disclosureAuditExport(principal(PortalRole.ADMIN), "disclosure_ref_1");

    AccessRequest accessRequest = capturedAccessRequest();
    assertThat(accessRequest.actorRole()).isEqualTo(PortalRole.ADMIN);
    assertThat(accessRequest.resourceType()).isEqualTo(ResourceType.DISCLOSURE_RECORD);
    assertThat(accessRequest.action()).isEqualTo(AccessAction.EXPORT);
    assertThat(accessRequest.fieldClassification()).isEqualTo(FieldClassification.SYSTEM_GOVERNANCE);
  }

  private AccessRequest capturedAccessRequest() {
    ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);
    verify(permissionEnforcer).requireAllowed(captor.capture());
    return captor.getValue();
  }

  private static RtoAuthenticatedPrincipal principal(PortalRole role) {
    return new RtoAuthenticatedPrincipal(
        USER_ID,
        ORGANIZATION_ID,
        role,
        "Task40 Admin",
        SESSION_ID);
  }
}
