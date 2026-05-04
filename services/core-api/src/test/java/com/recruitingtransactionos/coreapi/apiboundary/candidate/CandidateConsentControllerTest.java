package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService.CandidateConsentView;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService.SharedFieldPreview;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@WebMvcTest(CandidateConsentController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class CandidateConsentControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330002");
  private static final String CONSENT_RECORD_REF = "consent-task33-1";
  private static final String PROFILE_REF = "00000000-0000-0000-0000-000000330003";
  private static final String JOB_REF = "00000000-0000-0000-0000-000000330004";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CandidateConsentWorkflowService consentWorkflowService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void latestConsentRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/consent/{candidateRef}/requests/{consentRecordRef}",
            USER_ID,
            CONSENT_RECORD_REF))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void latestConsentReturnsCandidateScopedSummary() throws Exception {
    when(consentWorkflowService.viewConsentByRef(
        eq(ORG_ID),
        eq(USER_ID.toString()),
        eq(CONSENT_RECORD_REF),
        eq(USER_ID))).thenReturn(sampleView(ConsentStatus.REQUESTED));

    mockMvc.perform(get("/api/candidate/consent/{candidateRef}/requests/{consentRecordRef}",
            USER_ID,
            CONSENT_RECORD_REF)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.candidateRef").value(USER_ID.toString()))
        .andExpect(jsonPath("$.data.consentStatus").value("requested"))
        .andExpect(jsonPath("$.data.jobTitle").value("Principal Verification Engineer"))
        .andExpect(jsonPath("$.data.sharedFields[0].fieldPath").value("profile.headline"));
  }

  @Test
  void respondFailsClosedWhenCandidateRefDoesNotMatchPrincipal() throws Exception {
    mockMvc.perform(post("/api/candidate/consent/{candidateRef}/requests/{consentRecordRef}/respond",
            UUID.fromString("00000000-0000-0000-0000-000000330099"),
            CONSENT_RECORD_REF)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"approve":true}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("candidate_self_scope_required"));
  }

  private static CandidateConsentView sampleView(ConsentStatus status) {
    return new CandidateConsentView(
        new ConsentRecord(
            CONSENT_RECORD_REF,
            ORG_ID,
            USER_ID.toString(),
            PROFILE_REF,
            JOB_REF,
            "7",
            "task33-v1",
            status,
            Set.of(DisclosureLevel.L3_CONSENTED_DETAIL, DisclosureLevel.L4_IDENTITY_DISCLOSED),
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-18T00:00:00Z"),
            false),
        "7",
        "Principal Verification Engineer",
        List.of(new SharedFieldPreview("profile.headline", "\"Senior verification lead\"")));
  }

  private static Authentication auth(PortalRole portalRole, UUID userId) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        userId,
        ORG_ID,
        portalRole,
        "Task33 Candidate Tester",
        UUID.fromString("00000000-0000-0000-0000-0000003300ff")));
  }
}
