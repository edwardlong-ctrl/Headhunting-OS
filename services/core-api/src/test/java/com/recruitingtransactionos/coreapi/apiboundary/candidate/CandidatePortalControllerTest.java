package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.apiboundary.CandidateDocumentSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateMeResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateOpportunityResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateProfileReviewResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateTimelineResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDocumentUploadResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@org.springframework.context.annotation.Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
@WebMvcTest(CandidatePortalController.class)
class CandidatePortalControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000310001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000310002");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CandidatePortalQueryService portalQueryService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  // === me endpoint ===

  @Test
  void meRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void meRequiresCandidateRole() throws Exception {
    mockMvc.perform(get("/api/candidate/me")
            .with(authentication(auth(PortalRole.CONSULTANT, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void meReturnsCandidateSummary() throws Exception {
    when(portalQueryService.buildMe(eq(ORG_ID), eq(USER_ID)))
        .thenReturn(new CandidateMeResponse(
            USER_ID.toString(), "Alice Chen", ORG_ID.toString(), "3", 2, 1, 0));

    mockMvc.perform(get("/api/candidate/me")
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.candidateRef").value(USER_ID.toString()))
        .andExpect(jsonPath("$.data.displayName").value("Alice Chen"))
        .andExpect(jsonPath("$.data.currentProfileVersion").value("3"))
        .andExpect(jsonPath("$.data.documentCount").value(2))
        .andExpect(jsonPath("$.data.activeOpportunityCount").value(1));
  }

  // === profile endpoint ===

  @Test
  void profileRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/profile/{candidateRef}", USER_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void profileRequiresCandidateRole() throws Exception {
    mockMvc.perform(get("/api/candidate/profile/{candidateRef}", USER_ID)
            .with(authentication(auth(PortalRole.CLIENT, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void profileFailsClosedWhenCandidateRefDoesNotMatchPrincipal() throws Exception {
    UUID otherCandidate = UUID.fromString("00000000-0000-0000-0000-000000310099");
    mockMvc.perform(get("/api/candidate/profile/{candidateRef}", otherCandidate)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void profileReturnsSelfScopedProfileReview() throws Exception {
    when(portalQueryService.buildProfileReview(eq(ORG_ID), eq(USER_ID.toString())))
        .thenReturn(new CandidateProfileReviewResponse(
            USER_ID.toString(), "3", List.of(
                new CandidateProfileReviewResponse.ProfileField(
                    "profile.headline", "\"Senior Verification Engineer\"", "confirmed", "ai_extracted", "2024-01-01T00:00:00Z"))));

    mockMvc.perform(get("/api/candidate/profile/{candidateRef}", USER_ID)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.candidateRef").value(USER_ID.toString()))
        .andExpect(jsonPath("$.data.profileVersion").value("3"))
        .andExpect(jsonPath("$.data.fields[0].fieldPath").value("profile.headline"))
        .andExpect(jsonPath("$.data.fields[0].jsonValue").value("\"Senior Verification Engineer\""));
  }

  // === documents endpoint ===

  @Test
  void documentsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/documents"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void documentsRequiresCandidateRole() throws Exception {
    mockMvc.perform(get("/api/candidate/documents")
            .with(authentication(auth(PortalRole.ADMIN, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void documentsReturnsPagedDocumentList() throws Exception {
    when(portalQueryService.listDocuments(eq(ORG_ID), eq(USER_ID), anyInt(), anyInt()))
        .thenReturn(PagedResult.of(List.of(
            new CandidateDocumentSummaryResponse(
                UUID.randomUUID(), "resume", "Alice_Resume.pdf", "active", 204800L, "application/pdf", Instant.now())
        ), 1, 20, 0));

    mockMvc.perform(get("/api/candidate/documents")
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].documentType").value("resume"))
        .andExpect(jsonPath("$.data.items[0].title").value("Alice_Resume.pdf"));
  }

  // === opportunities endpoint ===

  @Test
  void opportunitiesRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/opportunities"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void opportunitiesRequiresCandidateRole() throws Exception {
    mockMvc.perform(get("/api/candidate/opportunities")
            .with(authentication(auth(PortalRole.SYSTEM, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void opportunitiesReturnsSelfScopedOpportunityList() throws Exception {
    when(portalQueryService.listOpportunities(eq(ORG_ID), eq(USER_ID)))
        .thenReturn(List.of(
            new CandidateOpportunityResponse(
                UUID.randomUUID().toString(), "Principal Engineer", "TechCorp", "active", "interview_scheduled", Instant.now(), Instant.now())));

    mockMvc.perform(get("/api/candidate/opportunities")
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].jobTitle").value("Principal Engineer"))
        .andExpect(jsonPath("$.data.items[0].companyName").value("TechCorp"));
  }

  // === timeline endpoint ===

  @Test
  void timelineRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/candidate/timeline/{candidateRef}", USER_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void timelineRequiresCandidateRole() throws Exception {
    mockMvc.perform(get("/api/candidate/timeline/{candidateRef}", USER_ID)
            .with(authentication(auth(PortalRole.OWNER, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void timelineFailsClosedWhenCandidateRefDoesNotMatchPrincipal() throws Exception {
    UUID otherCandidate = UUID.fromString("00000000-0000-0000-0000-000000310099");
    mockMvc.perform(get("/api/candidate/timeline/{candidateRef}", otherCandidate)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void timelineReturnsSelfScopedTimeline() throws Exception {
    Instant now = Instant.now();
    when(portalQueryService.buildTimeline(eq(ORG_ID), eq(USER_ID.toString())))
        .thenReturn(new CandidateTimelineResponse(USER_ID.toString(), List.of(
            new CandidateTimelineResponse.TimelineEvent(
                "workflow", "CONSENT_REQUESTED", "requested", "consultant requested candidate consent", now))));

    mockMvc.perform(get("/api/candidate/timeline/{candidateRef}", USER_ID)
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.candidateRef").value(USER_ID.toString()))
        .andExpect(jsonPath("$.data.events[0].actionCode").value("CONSENT_REQUESTED"));
  }

  // === documents/upload endpoint ===

  @Test
  void uploadDocumentRequiresAuthentication() throws Exception {
    mockMvc.perform(multipart("/api/candidate/documents/upload")
            .file("file", "content".getBytes()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"));
  }

  @Test
  void uploadDocumentRequiresCandidateRole() throws Exception {
    mockMvc.perform(multipart("/api/candidate/documents/upload")
            .file("file", "content".getBytes())
            .with(authentication(auth(PortalRole.CONSULTANT, USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void uploadDocumentReturnsUploadResult() throws Exception {
    when(portalQueryService.uploadDocument(eq(ORG_ID), eq(USER_ID), any(), anyString()))
        .thenReturn(new ConsultantDocumentUploadResponse(
            UUID.randomUUID().toString(), "packet-1", "not_scanned"));

    mockMvc.perform(multipart("/api/candidate/documents/upload")
            .file("file", "content".getBytes())
            .param("documentType", "resume")
            .with(authentication(auth(PortalRole.CANDIDATE, USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.scanStatus").value("not_scanned"));
  }

  // === helpers ===

  private static Authentication auth(PortalRole role, UUID userId) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        userId, ORG_ID, role, "Alice", UUID.randomUUID()));
  }
}
