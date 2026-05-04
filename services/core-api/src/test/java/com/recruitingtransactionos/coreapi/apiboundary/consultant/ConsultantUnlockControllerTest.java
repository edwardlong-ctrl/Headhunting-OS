package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService.CandidateConsentView;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestId;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService.UnlockApprovalResult;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService.UnlockBlocker;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService.UnlockReviewItem;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
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
@WebMvcTest(ConsultantUnlockController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ConsultantUnlockControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330101");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330102");
  private static final UUID CLIENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330103");
  private static final ShortlistId SHORTLIST_ID =
      new ShortlistId(UUID.fromString("00000000-0000-0000-0000-000000330104"));
  private static final ShortlistCandidateCardId CARD_ID =
      new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000330105"));

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UnlockWorkflowService unlockWorkflowService;

  @MockBean
  private CandidateConsentWorkflowService candidateConsentWorkflowService;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void listPendingReturnsUnlockQueueWithBlockers() throws Exception {
    when(unlockWorkflowService.listPendingRequests(eq(ORG_ID), eq(USER_ID)))
        .thenReturn(List.of(new UnlockReviewItem(
            "unlock-request-1",
            SHORTLIST_ID.value().toString(),
            CARD_ID.value().toString(),
            "requested",
            "Need to schedule final panel interview.",
            Instant.parse("2026-05-04T01:00:00Z"),
            "card_anon_1",
            "Platform architect",
            "Northstar Semi",
            "requested",
            List.of(new UnlockBlocker("CONSENT_NOT_CONFIRMED", "候选人 consent 仍未确认。")))));

    mockMvc.perform(get("/api/consultant/unlock-requests")
            .with(authentication(auth(PortalRole.CONSULTANT))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].unlockRequestId").value("unlock-request-1"))
        .andExpect(jsonPath("$.data.items[0].blockers[0].code").value("CONSENT_NOT_CONFIRMED"));
  }

  @Test
  void approveReturnsDecisionRefsAfterConsultantApproval() throws Exception {
    when(unlockWorkflowService.approveRequest(eq(ORG_ID), eq(USER_ID), eq(SHORTLIST_ID), eq(CARD_ID), eq("Proceed to disclosure.")))
        .thenReturn(new UnlockApprovalResult(
            ClientUnlockRequest.builder()
                .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000330106")))
                .organizationId(ORG_ID)
                .shortlistId(SHORTLIST_ID)
                .shortlistCandidateCardId(CARD_ID)
                .jobId(UUID.fromString("00000000-0000-0000-0000-000000330107"))
                .clientActorId(CLIENT_ID)
                .anonymousCandidateCardRef("card_anon_1")
                .requestReason("Need to schedule final panel interview.")
                .status(ClientUnlockRequestStatus.APPROVED)
                .unlockDecisionRef("unlock-decision-1")
                .approvedDisclosureRecordRef("disclosure-1")
                .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
                .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
                .build(),
            List.of()));

    mockMvc.perform(post("/api/consultant/unlock-requests/{shortlistId}/{cardId}/approve",
            SHORTLIST_ID.value(),
            CARD_ID.value())
            .with(authentication(auth(PortalRole.CONSULTANT)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reason":"Proceed to disclosure."}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("approved"))
        .andExpect(jsonPath("$.data.unlockDecisionRef").value("unlock-decision-1"))
        .andExpect(jsonPath("$.data.approvedDisclosureRecordRef").value("disclosure-1"));
  }

  @Test
  void requestConsentUsesSnapshotWithoutRecordingCandidateViewedEvent() throws Exception {
    when(candidateConsentWorkflowService.requestConsent(
        eq(ORG_ID),
        eq(USER_ID),
        eq("candidate-ref-1"),
        eq("profile-ref-1"),
        eq("job-ref-1"),
        eq("task33-v1"),
        eq(Instant.parse("2026-05-18T00:00:00Z"))))
        .thenReturn(new ConsentRecord(
            "consent-task33-1",
            ORG_ID,
            "candidate-ref-1",
            "profile-ref-1",
            "job-ref-1",
            "7",
            "task33-v1",
            ConsentStatus.REQUESTED,
            Set.of(DisclosureLevel.L4_IDENTITY_DISCLOSED),
            Instant.parse("2026-05-04T00:00:00Z"),
            Instant.parse("2026-05-18T00:00:00Z"),
            false));
    when(candidateConsentWorkflowService.latestConsentSnapshot(
        eq(ORG_ID),
        eq("candidate-ref-1"),
        eq("profile-ref-1"),
        eq("job-ref-1")))
        .thenReturn(new CandidateConsentView(
            new ConsentRecord(
                "consent-task33-1",
                ORG_ID,
                "candidate-ref-1",
                "profile-ref-1",
                "job-ref-1",
                "7",
                "task33-v1",
                ConsentStatus.REQUESTED,
                Set.of(DisclosureLevel.L4_IDENTITY_DISCLOSED),
                Instant.parse("2026-05-04T00:00:00Z"),
                Instant.parse("2026-05-18T00:00:00Z"),
                false),
            "7",
            "Platform architect",
            List.of()));

    mockMvc.perform(post("/api/consultant/unlock-requests/consent-requests")
            .with(authentication(auth(PortalRole.CONSULTANT)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "candidateRef":"candidate-ref-1",
                  "candidateProfileRef":"profile-ref-1",
                  "jobRef":"job-ref-1",
                  "consentTextVersion":"task33-v1",
                  "expiresAt":"2026-05-18T00:00:00Z"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.consentStatus").value("requested"));

    verify(candidateConsentWorkflowService).latestConsentSnapshot(
        ORG_ID,
        "candidate-ref-1",
        "profile-ref-1",
        "job-ref-1");
    verify(candidateConsentWorkflowService, never()).viewLatestConsent(
        eq(ORG_ID),
        eq("candidate-ref-1"),
        eq("profile-ref-1"),
        eq("job-ref-1"),
        eq(USER_ID));
  }

  private static Authentication auth(PortalRole portalRole) {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        USER_ID,
        ORG_ID,
        portalRole,
        "Task33 Consultant Tester",
        UUID.fromString("00000000-0000-0000-0000-0000003301ff")));
  }
}
