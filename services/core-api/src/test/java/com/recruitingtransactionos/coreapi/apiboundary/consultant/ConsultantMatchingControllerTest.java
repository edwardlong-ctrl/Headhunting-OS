package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.DimensionScore;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.EvidenceCoverageSummary;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.ProvenanceSummaryResponse;
import com.recruitingtransactionos.coreapi.consultantmatching.ConsultantMatchingSurfaceService;
import com.recruitingtransactionos.coreapi.consultantmatching.ConsultantMatchingSurfaceService.ConsultantMatchSelection;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@WebMvcTest(ConsultantMatchingController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ConsultantMatchingControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027b001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027b002");
  private static final UUID JOB_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027b003");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ConsultantMatchingSurfaceService consultantMatchingSurfaceService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void generateMatchReportUsesMinimalSubjectSelectionPayload() throws Exception {
    when(consultantMatchingSurfaceService.generateMatchReport(
        any(),
        eq(ORG_ID),
        eq(new com.recruitingtransactionos.coreapi.job.JobId(JOB_ID)),
        eq(new ConsultantMatchSelection("00000000-0000-0000-0000-00000027b010", null))))
        .thenReturn(sampleResponse("match_report_00000000000000000000000027b101"));

    mockMvc.perform(post("/api/consultant/jobs/{jobId}/matching/generate", JOB_ID)
            .with(authentication(auth("consultant")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RequestBody(
                "00000000-0000-0000-0000-00000027b010",
                null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.matchReportId").value("match_report_00000000000000000000000027b101"))
        .andExpect(jsonPath("$.data.subjectType").value("candidate"))
        .andExpect(jsonPath("$.data.dimensionScores[0].dimension").value("TECHNICAL_FIT"));
  }

  @Test
  void listMatchReportsReturnsPersistedReportsForConsultantPortal() throws Exception {
    when(consultantMatchingSurfaceService.listMatchReports(
        any(),
        eq(ORG_ID),
        eq(new com.recruitingtransactionos.coreapi.job.JobId(JOB_ID))))
        .thenReturn(List.of(sampleResponse("match_report_00000000000000000000000027b102")));

    mockMvc.perform(get("/api/consultant/jobs/{jobId}/matching", JOB_ID)
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reports[0].matchReportId").value("match_report_00000000000000000000000027b102"))
        .andExpect(jsonPath("$.data.reports[0].capSafeExplanation").value("Evidence remains consultant-safe and reviewable."))
        .andExpect(jsonPath("$.data.reports[0].interviewQuestions[0]").value("Which recent projects best prove fit?"));
  }

  private static ConsultantMatchReportResponse sampleResponse(String matchReportId) {
    return new ConsultantMatchReportResponse(
        matchReportId,
        "candidate",
        "match_subject_candidate_00000000000000000000000027b010",
        4,
        true,
        "INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE",
        "Evidence remains consultant-safe and reviewable.",
        "MEDIUM",
        "LOW",
        "LOW",
        "ontology-v2.1",
        "industry-pack-v1",
        "2026-05-03T00:00:00Z",
        List.of(new DimensionScore("TECHNICAL_FIT", 4)),
        new EvidenceCoverageSummary(0.66d, "PARTIAL", 3, 1),
        new ProvenanceSummaryResponse("CONSULTANT_ATTESTED", "HIGH", 0.8d, "IMPLIED"),
        List.of("Evidence coverage is partial across the required dimensions."),
        List.of("Which recent projects best prove fit?"));
  }

  private static Authentication auth(String portalRole) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        USER_ID,
        ORG_ID,
        PortalRole.valueOf(portalRole.toUpperCase()),
        "Task27 Matching Tester",
        UUID.fromString("00000000-0000-0000-0000-00000027b0ff")));
  }

  private record RequestBody(
      String candidateId,
      String shortlistCandidateCardId) {
  }
}
