package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowBlockerResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEntityStateResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowEventResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTimelineResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantWorkflowTransitionOptionResponse;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@WebMvcTest(ConsultantWorkflowController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ConsultantWorkflowControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026b001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026b002");
  private static final UUID ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026b003");
  private static final UUID EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-00000026b004");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConsultantWorkflowSurfaceService workflowSurfaceService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void entityStateRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/consultant/workflow/entity-state")
            .param("entityType", "DISCLOSURE")
            .param("entityId", ENTITY_ID.toString()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void entityStateReturnsBlockersForConsultant() throws Exception {
    when(workflowSurfaceService.entityState(any(), eq(ORG_ID), eq("DISCLOSURE"), eq(ENTITY_ID)))
        .thenReturn(new ConsultantWorkflowEntityStateResponse(
            "DISCLOSURE",
            ENTITY_ID.toString(),
            "approved",
            List.of(new ConsultantWorkflowTransitionOptionResponse(
                "DISCLOSURE_IDENTITY_DISCLOSED",
                "approved",
                "identity_disclosed",
                false,
                List.of(new ConsultantWorkflowBlockerResponse(
                    "job_activation_gate_required",
                    "Job activation is required before identity disclosure."))))));

    mockMvc.perform(get("/api/consultant/workflow/entity-state")
            .param("entityType", "DISCLOSURE")
            .param("entityId", ENTITY_ID.toString())
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.entityType").value("DISCLOSURE"))
        .andExpect(jsonPath("$.data.currentStatus").value("approved"))
        .andExpect(jsonPath("$.data.transitionOptions[0].actionCode").value("DISCLOSURE_IDENTITY_DISCLOSED"))
        .andExpect(jsonPath("$.data.transitionOptions[0].allowed").value(false))
        .andExpect(jsonPath("$.data.transitionOptions[0].blockers[0].code").value("job_activation_gate_required"));
  }

  @Test
  void timelineReturnsEntityStatesAndTransitionStatuses() throws Exception {
    when(workflowSurfaceService.timeline(any(), eq(ORG_ID), eq("JOB"), eq(ENTITY_ID), eq(20), eq(0)))
        .thenReturn(new ConsultantWorkflowTimelineResponse(
            List.of(new ConsultantWorkflowEventResponse(
                EVENT_ID.toString(),
                "JOB",
                ENTITY_ID.toString(),
                "JOB_ACTIVATED",
                "consultant",
                "none",
                "t2_operational",
                "contract_pending",
                "activated",
                "Ready for launch",
                "2026-05-02T01:00:00Z")),
            List.of(new ConsultantWorkflowEntityStateResponse(
                "JOB",
                ENTITY_ID.toString(),
                "activated",
                List.of())),
            20,
            0,
            false));

    mockMvc.perform(get("/api/consultant/workflow")
            .param("entityType", "JOB")
            .param("entityId", ENTITY_ID.toString())
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].beforeStatus").value("contract_pending"))
        .andExpect(jsonPath("$.data.items[0].afterStatus").value("activated"))
        .andExpect(jsonPath("$.data.entityStates[0].currentStatus").value("activated"));
  }

  private static Authentication auth(String portalRole) {
    return new RtoAuthenticationToken(principal(portalRole));
  }

  private static RtoAuthenticatedPrincipal principal(String portalRole) {
    return new RtoAuthenticatedPrincipal(
        USER_ID,
        ORG_ID,
        PortalRole.valueOf(portalRole.toUpperCase()),
        "Task26 Workflow Tester",
        UUID.fromString("00000000-0000-0000-0000-00000026b00f"));
  }
}
