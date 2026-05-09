package com.recruitingtransactionos.coreapi.apiboundary.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.GovernanceConfigUpdateResponse;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
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
import java.time.Instant;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.bind.annotation.PutMapping;

class AdminGovernanceControllerMappingTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000370201");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000370202");
  private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000370203");

  private GovernanceReadService governanceReadService;
  private GovernanceConfigService governanceConfigService;
  private PermissionEnforcer permissionEnforcer;
  private AdminGovernanceController controller;

  @BeforeEach
  void setUp() {
    governanceReadService = mock(GovernanceReadService.class);
    governanceConfigService = mock(GovernanceConfigService.class);
    permissionEnforcer = mock(PermissionEnforcer.class);
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed", "allowed"));
    controller = new AdminGovernanceController(
        governanceReadService,
        governanceConfigService,
        permissionEnforcer);
  }

  @Test
  void onlyRuntimeWiredGovernanceConfigSectionsExposePutMapping() throws NoSuchMethodException {
    Method method = AdminGovernanceController.class.getDeclaredMethod(
        "saveSectionConfig",
        com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal.class,
        jakarta.servlet.http.HttpServletRequest.class,
        AdminGovernanceConfigRequest.class);

    assertThat(method.getAnnotation(PutMapping.class).value())
        .containsExactly("/model-routing");
  }

  @Test
  void adminGovernanceReadsUseCentralPermissionPolicy() {
    when(governanceReadService.loadAdminSection(ORGANIZATION_ID, "integrations"))
        .thenReturn(section("integrations"));

    controller.loadSection(principal(PortalRole.ADMIN), request("/api/admin/integrations"));

    AccessRequest accessRequest = capturedAccessRequest();
    assertThat(accessRequest.actorRole()).isEqualTo(PortalRole.ADMIN);
    assertThat(accessRequest.resourceType()).isEqualTo(ResourceType.ADMIN_GOVERNANCE);
    assertThat(accessRequest.action()).isEqualTo(AccessAction.READ);
    assertThat(accessRequest.fieldClassification()).isEqualTo(FieldClassification.SYSTEM_GOVERNANCE);
  }

  @Test
  void adminGovernanceWritesUseCentralPermissionPolicy() {
    GovernanceConfigRecord record = new GovernanceConfigRecord(
        UUID.fromString("00000000-0000-0000-0000-000000370204"),
        ORGANIZATION_ID,
        "model-routing",
        "default",
        "{}",
        true,
        USER_ID,
        USER_ID,
        Instant.parse("2026-05-07T00:00:00Z"),
        Instant.parse("2026-05-07T00:00:00Z"),
        1);
    when(governanceConfigService.save(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        "{}",
        true,
        USER_ID,
        PortalRole.ADMIN))
        .thenReturn(record);

    controller.saveSectionConfig(
        principal(PortalRole.ADMIN),
        request("/api/admin/model-routing"),
        new AdminGovernanceConfigRequest("{}", true));

    AccessRequest accessRequest = capturedAccessRequest();
    assertThat(accessRequest.actorRole()).isEqualTo(PortalRole.ADMIN);
    assertThat(accessRequest.resourceType()).isEqualTo(ResourceType.ADMIN_GOVERNANCE);
    assertThat(accessRequest.action()).isEqualTo(AccessAction.UPDATE);
    assertThat(accessRequest.fieldClassification()).isEqualTo(FieldClassification.SYSTEM_GOVERNANCE);
  }

  private AccessRequest capturedAccessRequest() {
    ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);
    verify(permissionEnforcer).requireAllowed(captor.capture());
    return captor.getValue();
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

  private static RtoAuthenticatedPrincipal principal(PortalRole role) {
    return new RtoAuthenticatedPrincipal(
        USER_ID,
        ORGANIZATION_ID,
        role,
        "Task37 Admin",
        SESSION_ID);
  }

  private static HttpServletRequest request(String uri) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(uri);
    return request;
  }
}
