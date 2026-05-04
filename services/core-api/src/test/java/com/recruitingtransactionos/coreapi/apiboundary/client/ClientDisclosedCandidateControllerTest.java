package com.recruitingtransactionos.coreapi.apiboundary.client;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestId;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
@WebMvcTest(ClientDisclosedCandidateController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ClientDisclosedCandidateControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330201");
  private static final UUID CLIENT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000330202");
  private static final ShortlistId SHORTLIST_ID =
      new ShortlistId(UUID.fromString("00000000-0000-0000-0000-000000330203"));
  private static final ShortlistCandidateCardId CARD_ID =
      new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000330204"));
  private static final CandidateId CANDIDATE_ID =
      new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000330205"));
  private static final UUID PROFILE_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000330206");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ShortlistService shortlistService;

  @MockBean
  private CandidateService candidateService;

  @MockBean
  private CandidateProfileService candidateProfileService;

  @MockBean
  private ClientUnlockRequestPort clientUnlockRequestPort;

  @MockBean
  private DisclosureRecordPort disclosureRecordPort;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void disclosedCandidateRequiresApprovedDisclosureRecord() throws Exception {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(eq(ORG_ID), eq(SHORTLIST_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ClientUnlockRequest.builder()
            .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000330207")))
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .shortlistCandidateCardId(CARD_ID)
            .jobId(UUID.fromString("00000000-0000-0000-0000-000000330208"))
            .clientActorId(CLIENT_USER_ID)
            .anonymousCandidateCardRef("card_anon_1")
            .requestReason("Need direct coordination.")
            .status(ClientUnlockRequestStatus.REQUESTED)
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T01:00:00Z"))
            .build()));

    mockMvc.perform(get("/api/client/disclosed-candidates/{shortlistId}/{cardId}",
            SHORTLIST_ID.value(),
            CARD_ID.value())
            .with(authentication(auth())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("identity_disclosure_required"));
  }

  @Test
  void disclosedCandidateReturnsApprovedIdentityReleasedPayload() throws Exception {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(eq(ORG_ID), eq(SHORTLIST_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ClientUnlockRequest.builder()
            .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000330207")))
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .shortlistCandidateCardId(CARD_ID)
            .jobId(UUID.fromString("00000000-0000-0000-0000-000000330208"))
            .clientActorId(CLIENT_USER_ID)
            .anonymousCandidateCardRef("card_anon_1")
            .requestReason("Need direct coordination.")
            .status(ClientUnlockRequestStatus.APPROVED)
            .approvedDisclosureRecordRef("disclosure-3302")
            .unlockDecisionRef("unlock-decision-3302")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(disclosureRecordPort.findByRefAndOrganizationId(eq(ORG_ID), eq("disclosure-3302")))
        .thenReturn(Optional.of(new DisclosureRecord(
            "disclosure-3302",
            ORG_ID,
            CANDIDATE_ID.value().toString(),
            PROFILE_UUID.toString(),
            "00000000-0000-0000-0000-000000330208",
            CLIENT_USER_ID.toString(),
            DisclosureStatus.IDENTITY_DISCLOSED,
            DisclosureLevel.L4_IDENTITY_DISCLOSED,
            RedactionLevel.L4_IDENTITY_DISCLOSED,
            "unlock-decision-3302",
            "consent-3302",
            Optional.empty(),
            Instant.parse("2026-05-04T02:00:00Z"))));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORG_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ShortlistCandidateCard.builder()
            .shortlistCandidateCardId(CARD_ID)
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000330209"))
            .candidateId(CANDIDATE_ID)
            .candidateProfileId(PROFILE_UUID)
            .sortOrder(1)
            .status(ShortlistCandidateCardStatus.UNLOCKED)
            .metadata("{}")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(candidateService.findCandidateByIdAndOrganizationId(eq(ORG_ID), eq(CANDIDATE_ID)))
        .thenReturn(Optional.of(Candidate.builder()
            .candidateId(CANDIDATE_ID)
            .organizationId(ORG_ID)
            .status(CandidateStatus.IDENTITY_DISCLOSED)
            .currentProfileId(new CandidateProfileId(PROFILE_UUID))
            .privacyStatus("internal_only")
            .metadata("{}")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(eq(ORG_ID), eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(CandidateProfile.builder()
            .candidateProfileId(new CandidateProfileId(PROFILE_UUID))
            .organizationId(ORG_ID)
            .candidateId(CANDIDATE_ID)
            .profileVersion(new CandidateProfileVersion(9))
            .fields(List.of())
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));

    mockMvc.perform(get("/api/client/disclosed-candidates/{shortlistId}/{cardId}",
            SHORTLIST_ID.value(),
            CARD_ID.value())
            .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.disclosureRecordRef").value("disclosure-3302"))
        .andExpect(jsonPath("$.data.candidateId").value(CANDIDATE_ID.value().toString()))
        .andExpect(jsonPath("$.data.profileVersion").value("9"));
  }

  @Test
  void disclosedCandidateRequiresIdentityDisclosedRecordInsteadOfApprovedRefAlone() throws Exception {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(eq(ORG_ID), eq(SHORTLIST_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ClientUnlockRequest.builder()
            .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000330207")))
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .shortlistCandidateCardId(CARD_ID)
            .jobId(UUID.fromString("00000000-0000-0000-0000-000000330208"))
            .clientActorId(CLIENT_USER_ID)
            .anonymousCandidateCardRef("card_anon_1")
            .requestReason("Need direct coordination.")
            .status(ClientUnlockRequestStatus.APPROVED)
            .unlockDecisionRef("unlock-decision-3302")
            .approvedDisclosureRecordRef("disclosure-3302")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(disclosureRecordPort.findByRefAndOrganizationId(eq(ORG_ID), eq("disclosure-3302")))
        .thenReturn(Optional.of(new DisclosureRecord(
            "disclosure-3302",
            ORG_ID,
            CANDIDATE_ID.value().toString(),
            PROFILE_UUID.toString(),
            "00000000-0000-0000-0000-000000330208",
            CLIENT_USER_ID.toString(),
            DisclosureStatus.APPROVED,
            DisclosureLevel.L4_IDENTITY_DISCLOSED,
            RedactionLevel.L4_IDENTITY_DISCLOSED,
            "unlock-decision-3302",
            "consent-3302",
            Optional.empty(),
            Instant.parse("2026-05-04T02:00:00Z"))));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORG_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ShortlistCandidateCard.builder()
            .shortlistCandidateCardId(CARD_ID)
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000330209"))
            .candidateId(CANDIDATE_ID)
            .candidateProfileId(PROFILE_UUID)
            .sortOrder(1)
            .status(ShortlistCandidateCardStatus.UNLOCKED)
            .metadata("{}")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));

    mockMvc.perform(get("/api/client/disclosed-candidates/{shortlistId}/{cardId}",
            SHORTLIST_ID.value(),
            CARD_ID.value())
            .with(authentication(auth())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("identity_disclosure_required"));
  }

  @Test
  void disclosedCandidateRejectsDisclosureRecordWithMismatchedScope() throws Exception {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(eq(ORG_ID), eq(SHORTLIST_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ClientUnlockRequest.builder()
            .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000330207")))
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .shortlistCandidateCardId(CARD_ID)
            .jobId(UUID.fromString("00000000-0000-0000-0000-000000330208"))
            .clientActorId(CLIENT_USER_ID)
            .anonymousCandidateCardRef("card_anon_1")
            .requestReason("Need direct coordination.")
            .status(ClientUnlockRequestStatus.APPROVED)
            .unlockDecisionRef("unlock-decision-3302")
            .approvedDisclosureRecordRef("disclosure-3302")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORG_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(ShortlistCandidateCard.builder()
            .shortlistCandidateCardId(CARD_ID)
            .organizationId(ORG_ID)
            .shortlistId(SHORTLIST_ID)
            .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000330209"))
            .candidateId(CANDIDATE_ID)
            .candidateProfileId(PROFILE_UUID)
            .sortOrder(1)
            .status(ShortlistCandidateCardStatus.UNLOCKED)
            .metadata("{}")
            .createdAt(Instant.parse("2026-05-04T01:00:00Z"))
            .updatedAt(Instant.parse("2026-05-04T02:00:00Z"))
            .build()));
    when(disclosureRecordPort.findByRefAndOrganizationId(eq(ORG_ID), eq("disclosure-3302")))
        .thenReturn(Optional.of(new DisclosureRecord(
            "disclosure-3302",
            ORG_ID,
            UUID.fromString("00000000-0000-0000-0000-000000330299").toString(),
            PROFILE_UUID.toString(),
            "00000000-0000-0000-0000-000000330208",
            CLIENT_USER_ID.toString(),
            DisclosureStatus.IDENTITY_DISCLOSED,
            DisclosureLevel.L4_IDENTITY_DISCLOSED,
            RedactionLevel.L4_IDENTITY_DISCLOSED,
            "unlock-decision-3302",
            "consent-3302",
            Optional.empty(),
            Instant.parse("2026-05-04T02:00:00Z"))));

    mockMvc.perform(get("/api/client/disclosed-candidates/{shortlistId}/{cardId}",
            SHORTLIST_ID.value(),
            CARD_ID.value())
            .with(authentication(auth())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("identity_disclosure_required"));
  }

  private static Authentication auth() {
    return new RtoAuthenticationToken(new RtoAuthenticatedPrincipal(
        CLIENT_USER_ID,
        ORG_ID,
        PortalRole.CLIENT,
        "Task33 Client Tester",
        UUID.fromString("00000000-0000-0000-0000-0000003302ff")));
  }
}
