package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanyDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanySummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({
    ConsultantCompanyController.class,
    ConsultantJobController.class,
    ConsultantShortlistController.class
})
class ConsultantControllerLeakageTest {

  private static final String ORG_ID = "00000000-0000-0000-0000-0000000018a1";
  private static final String COMPANY_ID = "00000000-0000-0000-0000-0000000018a2";
  private static final String JOB_ID = "00000000-0000-0000-0000-0000000018a3";
  private static final String SHORTLIST_ID = "00000000-0000-0000-0000-0000000018a4";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConsultantApiQueryService queryService;

  @MockBean
  private ConsultantApiCommandService commandService;

  // ── Company Controller ────────────────────────────────────────────────────

  @Nested
  class CompanyController {

    @Test
    void missingRoleHeaderReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/companies")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"))
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void wrongRoleClientReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/companies")
              .header("X-RTO-Actor-Role", "client")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"))
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void wrongRoleCandidateReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/companies")
              .header("X-RTO-Actor-Role", "candidate")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void missingOrganizationHeaderReturns400() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/companies")
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void listCompaniesSuccessReturnsPagedResult() throws Exception {
      ConsultantCompanySummaryResponse item = new ConsultantCompanySummaryResponse(
          COMPANY_ID, "test-corp", "active", 3, 5, "2025-01-01T00:00:00Z");
      PagedResult<ConsultantCompanySummaryResponse> result =
          PagedResult.of(List.of(item), 1, 20, 0);
      when(queryService.listCompanies(any(), any(), eq(null))).thenReturn(result);

      MvcResult mvcResult = mockMvc.perform(get("/api/consultant/companies")
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.items[0].companyId").value(COMPANY_ID))
          .andExpect(jsonPath("$.data.items[0].name").value("test-corp"))
          .andExpect(jsonPath("$.data.items[0].status").value("active"))
          .andExpect(jsonPath("$.data.totalCount").value(1))
          .andExpect(jsonPath("$.data.limit").value(20))
          .andExpect(jsonPath("$.data.offset").value(0))
          .andExpect(jsonPath("$.data.hasMore").value(false))
          .andReturn();

      String body = mvcResult.getResponse().getContentAsString();
      assertThat(body).doesNotContain("commercialTerms", "metadata");
      assertNoInternalLeakage(body);
    }

    @Test
    void getCompanyDetailNotFoundReturns404() throws Exception {
      when(queryService.getCompanyDetail(any(), any(), any())).thenReturn(Optional.empty());

      MvcResult result = mockMvc.perform(get("/api/consultant/companies/" + COMPANY_ID)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.errorCode").value("not_found"))
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void getCompanyDetailInvalidIdReturns400() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/companies/not-a-uuid")
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isBadRequest())
          .andReturn();
      assertSanitizedDenial(result);
    }
  }

  // ── Job Controller ───────────────────────────────────────────────────────

  @Nested
  class JobController {

    @Test
    void missingRoleHeaderReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/jobs")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void wrongRoleReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/jobs")
              .header("X-RTO-Actor-Role", "client")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void missingOrgReturns400() throws Exception {
      mockMvc.perform(get("/api/consultant/jobs")
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getJobDetailNotFoundReturns404() throws Exception {
      when(queryService.getJobDetail(any(), any(), any())).thenReturn(Optional.empty());

      MvcResult result = mockMvc.perform(get("/api/consultant/jobs/" + JOB_ID)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isNotFound())
          .andReturn();
      assertSanitizedDenial(result);
    }
  }

  // ── Shortlist Controller ──────────────────────────────────────────────────

  @Nested
  class ShortlistController {

    @Test
    void missingRoleHeaderReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/shortlists")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void wrongRoleReturns403() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/consultant/shortlists")
              .header("X-RTO-Actor-Role", "candidate")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andReturn();
      assertSanitizedDenial(result);
    }

    @Test
    void missingOrgReturns400() throws Exception {
      mockMvc.perform(get("/api/consultant/shortlists")
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getShortlistDetailNotFoundReturns404() throws Exception {
      when(queryService.getShortlistDetail(any(), any(), any())).thenReturn(Optional.empty());

      MvcResult result = mockMvc.perform(get("/api/consultant/shortlists/" + SHORTLIST_ID)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isNotFound())
          .andReturn();
      assertSanitizedDenial(result);
    }
  }

  // ── Company Write Controller ──────────────────────────────────────────────

  @Nested
  class CompanyWriteController {

    private static final ConsultantCompanyDetailResponse COMPANY_DETAIL =
        new ConsultantCompanyDetailResponse(
            COMPANY_ID, "testcorp", null, "tech", null,
            "asia", "medium", "active", "high", null,
            "2025-01-01T00:00:00Z", "2025-01-01T00:00:00Z",
            Collections.emptyList(), 0);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createCompanyMissingRoleReturns403() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("name", "NewCo", "status", "active"));

      mockMvc.perform(post("/api/consultant/companies")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
    }

    @Test
    void createCompanyMissingOrgReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("name", "NewCo", "status", "active"));

      mockMvc.perform(post("/api/consultant/companies")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void createCompanySuccessReturns201() throws Exception {
      when(commandService.createCompany(any(), any(), any()))
          .thenReturn(COMPANY_DETAIL);

      String body = objectMapper.writeValueAsString(
          Map.of("name", "NewCo", "status", "active"));

      mockMvc.perform(post("/api/consultant/companies")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.companyId").value(COMPANY_ID));
    }

    @Test
    void updateCompanyMissingRoleReturns403() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("name", "UpdatedCo", "status", "active", "version", 1));

      mockMvc.perform(put("/api/consultant/companies/" + COMPANY_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
    }

    @Test
    void updateCompanyMissingOrgReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("name", "UpdatedCo", "status", "active", "version", 1));

      mockMvc.perform(put("/api/consultant/companies/" + COMPANY_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void updateCompanySuccessReturns200() throws Exception {
      when(commandService.updateCompany(any(), any(), any(), any()))
          .thenReturn(COMPANY_DETAIL);

      String body = objectMapper.writeValueAsString(
          Map.of("name", "UpdatedCo", "status", "active", "version", 1));

      mockMvc.perform(put("/api/consultant/companies/" + COMPANY_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.companyId").value(COMPANY_ID));
    }

    @Test
    void createCompanyInvalidPayloadReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("name", "", "status", "active"));

      mockMvc.perform(post("/api/consultant/companies")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isBadRequest());
    }
  }

  // ── Job Write Controller ──────────────────────────────────────────────────

  @Nested
  class JobWriteController {

    private static final ConsultantJobDetailResponse JOB_DETAIL =
        new ConsultantJobDetailResponse(
            JOB_ID, COMPANY_ID, "engineer", null, null,
            null, null, null, null, "draft",
            null, null, null, null, "2025-01-01T00:00:00Z", "2025-01-01T00:00:00Z",
            Collections.emptyList(), null);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createJobMissingRoleReturns403() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Engineer", "status", "draft"));

      mockMvc.perform(post("/api/consultant/jobs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
    }

    @Test
    void createJobMissingOrgReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Engineer", "status", "draft"));

      mockMvc.perform(post("/api/consultant/jobs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void createJobSuccessReturns201() throws Exception {
      when(commandService.createJob(any(), any(), any()))
          .thenReturn(JOB_DETAIL);

      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Engineer", "status", "draft"));

      mockMvc.perform(post("/api/consultant/jobs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.jobId").value(JOB_ID));
    }

    @Test
    void updateJobMissingRoleReturns403() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Updated",
              "status", "active", "version", 1));

      mockMvc.perform(put("/api/consultant/jobs/" + JOB_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
    }

    @Test
    void updateJobMissingOrgReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Updated",
              "status", "active", "version", 1));

      mockMvc.perform(put("/api/consultant/jobs/" + JOB_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void updateJobSuccessReturns200() throws Exception {
      when(commandService.updateJob(any(), any(), any(), any()))
          .thenReturn(JOB_DETAIL);

      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "Updated",
              "status", "draft", "version", 1));

      mockMvc.perform(put("/api/consultant/jobs/" + JOB_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.jobId").value(JOB_ID));
    }

    @Test
    void createJobInvalidPayloadReturns400() throws Exception {
      String body = objectMapper.writeValueAsString(
          Map.of("companyId", COMPANY_ID, "title", "", "status", "draft"));

      mockMvc.perform(post("/api/consultant/jobs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
              .header("X-RTO-Actor-Role", "consultant")
              .header("X-RTO-Organization-Id", ORG_ID))
          .andExpect(status().isBadRequest());
    }
  }

  // ── Allowlist validation ──────────────────────────────────────────────────

  @Test
  void consultantCompanySummaryResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantCompanySummaryResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "companyId", "name", "status", "contactCount", "jobCount", "createdAt");
  }

  @Test
  void consultantCompanyDetailResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantCompanyDetailResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "companyId", "name", "displayName", "industry", "website", "headquartersLocation",
        "sizeBand", "status", "paymentReliability", "ownerConsultantId", "createdAt",
        "updatedAt", "contacts", "jobCount");
  }

  @Test
  void consultantJobSummaryResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantJobSummaryResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "jobId", "title", "companyId", "status", "createdAt");
  }

  @Test
  void consultantJobDetailResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantJobDetailResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "jobId", "companyId", "title", "description", "location", "seniorityBand",
        "roleFamily", "employmentType", "compensation", "status", "ownerConsultantId",
        "activatedAt", "closedAt", "closeReason", "createdAt", "updatedAt",
        "requirements", "scorecard");
  }

  @Test
  void consultantShortlistSummaryResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantShortlistSummaryResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "shortlistId", "title", "jobId", "status", "candidateCount", "createdAt");
  }

  @Test
  void consultantShortlistDetailResponseFieldsMatchAllowlist() {
    Set<String> fields = ApiBoundaryContractRules.consultantShortlistDetailResponseFieldNames();
    assertThat(fields).containsExactlyInAnyOrder(
        "shortlistId", "jobId", "title", "status", "sentAt", "clientViewedAt",
        "ownerConsultantId", "createdAt", "updatedAt", "cards");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static void assertSanitizedDenial(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    assertThat(body)
        .doesNotContain(
            "Candidate",
            "CandidateProfile",
            "candidateId",
            "candidateProfileId",
            "fullName",
            "email",
            "phone",
            "linkedin",
            "rawSourceText",
            "consultantNotes",
            "SourceItem",
            "InformationPacket",
            "ClaimLedgerItem",
            "ReviewEvent",
            "WorkflowEvent",
            "ConsentRecord",
            "DisclosureRecord",
            "com.recruitingtransactionos",
            "java.",
            "stack trace",
            "stacktrace",
            "\tat ",
            "Exception");
  }

  private static void assertNoInternalLeakage(String body) {
    assertThat(body)
        .doesNotContain(
            "rawCandidate",
            "rawProfile",
            "sourceItem",
            "informationPacket",
            "claimLedger",
            "reviewEvent",
            "workflowEvent",
            "consentRecord",
            "disclosureRecord",
            "CandidateProfile",
            "canonicalWrite");
  }
}
