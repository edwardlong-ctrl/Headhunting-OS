package com.recruitingtransactionos.coreapi.apiboundary.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconsole.GovernanceConsoleReadService;
import com.recruitingtransactionos.coreapi.governancequery.GovernanceReadService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OwnerGovernanceControllerPolicyTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000370301");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000370302");
  private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000370303");

  private GovernanceReadService governanceReadService;
  private GovernanceConsoleReadService governanceConsoleReadService;
  private PermissionEnforcer permissionEnforcer;
  private OwnerGovernanceController controller;

  @BeforeEach
  void setUp() {
    governanceReadService = mock(GovernanceReadService.class);
    governanceConsoleReadService = mock(GovernanceConsoleReadService.class);
    permissionEnforcer = mock(PermissionEnforcer.class);
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed", "allowed"));
    controller = new OwnerGovernanceController(
        governanceReadService,
        governanceConsoleReadService,
        permissionEnforcer);
  }

  @Test
  void ownerGovernanceReadsUseCentralPermissionPolicy() {
    when(governanceReadService.loadOwnerSection(ORGANIZATION_ID, "risk"))
        .thenReturn(section("risk"));

    controller.loadSection(principal(), request("/api/owner/risk"));

    ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);
    verify(permissionEnforcer).requireAllowed(captor.capture());
    AccessRequest accessRequest = captor.getValue();
    assertThat(accessRequest.actorRole()).isEqualTo(PortalRole.OWNER);
    assertThat(accessRequest.resourceType()).isEqualTo(ResourceType.ADMIN_GOVERNANCE);
    assertThat(accessRequest.action()).isEqualTo(AccessAction.READ);
    assertThat(accessRequest.fieldClassification()).isEqualTo(FieldClassification.SYSTEM_GOVERNANCE);
  }

  @Test
  void ownerAiQualityUsesTask50GovernanceConsoleSummary() {
    when(governanceConsoleReadService.loadOwnerSummary(ORGANIZATION_ID))
        .thenReturn(section("ai-quality"));

    controller.loadSection(principal(), request("/api/owner/ai-quality"));

    verify(governanceConsoleReadService).loadOwnerSummary(ORGANIZATION_ID);
  }

  private static GovernanceSectionResponse section(String sectionKey) {
    return new GovernanceSectionResponse(
        sectionKey,
        "Section",
        "",
        List.of(),
        List.of(),
        List.of(),
        false,
        "{}",
        "");
  }

  private static RtoAuthenticatedPrincipal principal() {
    return new RtoAuthenticatedPrincipal(
        USER_ID,
        ORGANIZATION_ID,
        PortalRole.OWNER,
        "Task37 Owner",
        SESSION_ID);
  }

  private static HttpServletRequest request(String uri) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    return request;
  }
}
