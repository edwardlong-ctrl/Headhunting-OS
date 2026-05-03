package com.recruitingtransactionos.coreapi.apiboundary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestId;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.service.JobActivationGateResult;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientApiQueryServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000032c001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000032c002");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000032c003");
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000032c004"));
  private static final JobId JOB_ID = new JobId(UUID.fromString("00000000-0000-0000-0000-00000032c005"));
  private static final ShortlistId SHORTLIST_ID = new ShortlistId(UUID.fromString("00000000-0000-0000-0000-00000032c006"));
  private static final ShortlistCandidateCardId CARD_ID = new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-00000032c007"));
  private static final UUID ANONYMOUS_CARD_ID = UUID.fromString("00000000-0000-0000-0000-00000032c008");
  private static final CandidateId CANDIDATE_ID = new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000032c009"));
  private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000032c00a");
  private static final Instant CREATED_AT = Instant.parse("2026-05-04T01:30:00Z");
  private static final String CLIENT_METADATA = """
      {
        "anonymousCandidateRef": "anon_card_01",
        "projectionVersion": "task29-shortlist-v1",
        "redactionLevel": "l2_client_safe",
        "generalizedHeadline": "Semiconductor DV Leader",
        "generalizedRoleFamily": "Digital Verification",
        "generalizedSeniorityBand": "principal",
        "generalizedLocationRegion": "apac",
        "safeSummary": "Leads complex verification programs without revealing identity.",
        "safeSkillSummary": "UVM, low power, SoC verification",
        "safeEvidenceSummaries": ["Owns DV execution"],
        "safeMatchNarratives": ["Most aligned on UVM depth"],
        "overallScore": 91,
        "confidence": "high",
        "reidentificationRiskSignal": "low",
        "dimensionScores": []
      }
      """;

  @Mock private CompanyService companyService;
  @Mock private JobService jobService;
  @Mock private JobIntakeApplicationService jobIntakeApplicationService;
  @Mock private ShortlistService shortlistService;
  @Mock private ClientUnlockRequestPort clientUnlockRequestPort;
  @Mock private InterviewFeedbackService interviewFeedbackService;
  @Mock private com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer permissionEnforcer;

  private ClientApiQueryService service;
  private AccessRequest shortlistReadAccessRequest;

  @BeforeEach
  void setUp() {
    service = new ClientApiQueryService(
        companyService,
        jobService,
        jobIntakeApplicationService,
        shortlistService,
        clientUnlockRequestPort,
        interviewFeedbackService,
        permissionEnforcer);
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed_for_test", "Allowed for focused unit test."));
    shortlistReadAccessRequest = new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  @Test
  void getShortlistDetail_hidesClientNotesFromClientResponse() {
    Job visibleJob = visibleJob();
    Shortlist shortlist = shortlist();
    ShortlistCandidateCard card = shortlistCard();

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(List.of(card));
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID, CARD_ID))
        .thenReturn(Optional.empty());

    var response = service.getShortlistDetail(
        shortlistReadAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID);

    assertThat(response).isPresent();
    assertThat(response.orElseThrow().cards()).hasSize(1);
    assertThat(response.orElseThrow().cards().get(0).generalizedHeadline()).isEqualTo("Semiconductor DV Leader");
    assertThat(response.orElseThrow().cards().get(0).clientNotes()).isNull();
  }

  @Test
  void getShortlistDetail_surfacesUnlockDecisionAndDisclosureRefsWithoutAssumingDisclosure() {
    Job visibleJob = visibleJob();
    Shortlist shortlist = shortlist();
    ShortlistCandidateCard card = shortlistCard();
    ClientUnlockRequest approvedRequest = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-00000032c00c")))
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .shortlistCandidateCardId(CARD_ID)
        .jobId(JOB_ID.value())
        .clientActorId(ACTOR_ID)
        .anonymousCandidateCardRef("card_" + ANONYMOUS_CARD_ID.toString().replace("-", ""))
        .requestReason("Need disclosure review.")
        .status(ClientUnlockRequestStatus.APPROVED)
        .unlockDecisionRef("unlock_decision_approved_001")
        .approvedDisclosureRecordRef(null)
        .createdAt(CREATED_AT.plusSeconds(200))
        .updatedAt(CREATED_AT.plusSeconds(220))
        .version(2)
        .build();

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(List.of(card));
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID, CARD_ID))
        .thenReturn(Optional.of(approvedRequest));

    var response = service.getShortlistDetail(
        shortlistReadAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID);

    assertThat(response).isPresent();
    assertThat(response.orElseThrow().cards()).hasSize(1);
    assertThat(response.orElseThrow().cards().get(0).unlockRequestStatus()).isEqualTo("approved");
    assertThat(response.orElseThrow().cards().get(0).unlockDecisionRef()).isEqualTo("unlock_decision_approved_001");
    assertThat(response.orElseThrow().cards().get(0).approvedDisclosureRecordRef()).isNull();
  }

  @Test
  void listShortlists_candidateCountExcludesRemovedCardsToMatchDetailVisibility() {
    Job visibleJob = visibleJob();
    Shortlist shortlist = shortlist();
    ShortlistCandidateCard visibleCard = shortlistCard();
    ShortlistCandidateCard removedCard = shortlistCard(ShortlistCandidateCardStatus.REMOVED);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findAllShortlistsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(List.of(visibleCard, removedCard));

    var response = service.listShortlists(shortlistReadAccessRequest, ORGANIZATION_ID, ACTOR_ID);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).candidateCount()).isEqualTo(1);
  }

  @Test
  void getDashboard_recentShortlistsCandidateCountExcludesRemovedCards() {
    Job visibleJob = visibleJob();
    Shortlist shortlist = shortlist();
    ShortlistCandidateCard visibleCard = shortlistCard();
    ShortlistCandidateCard removedCard = shortlistCard(ShortlistCandidateCardStatus.REMOVED);

    when(companyService.findAllCompaniesByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(clientCompany()));
    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findAllShortlistsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
        .thenReturn(List.of(visibleCard, removedCard));
    when(jobIntakeApplicationService.activationGate(ORGANIZATION_ID, JOB_ID))
        .thenReturn(new JobActivationGateResult(true, List.of(), List.of(), true, true, true));
    when(interviewFeedbackService.findFeedbackByJobIdAndOrganizationId(ORGANIZATION_ID, JOB_ID))
        .thenReturn(List.of());
    when(clientUnlockRequestPort.findByClientActorId(ORGANIZATION_ID, ACTOR_ID)).thenReturn(List.of());

    var response = service.getDashboard(shortlistReadAccessRequest, ORGANIZATION_ID, ACTOR_ID);

    assertThat(response.recentShortlists()).hasSize(1);
    assertThat(response.recentShortlists().get(0).candidateCount()).isEqualTo(1);
  }

  private Job visibleJob() {
    return Job.builder()
        .jobId(JOB_ID)
        .organizationId(ORGANIZATION_ID)
        .companyId(COMPANY_ID)
        .title("Principal DV Engineer")
        .status(JobStatus.ACTIVATED)
        .ownerConsultantId(CONSULTANT_ID)
        .metadata("{\"clientActorId\":\"" + ACTOR_ID + "\"}")
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }

  private com.recruitingtransactionos.coreapi.company.Company clientCompany() {
    return com.recruitingtransactionos.coreapi.company.Company.builder()
        .companyId(COMPANY_ID)
        .organizationId(ORGANIZATION_ID)
        .name("Acme Semiconductors")
        .displayName("Acme Semiconductors")
        .status(CompanyStatus.ACTIVE)
        .metadata("{\"clientActorId\":\"" + ACTOR_ID + "\"}")
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }

  private Shortlist shortlist() {
    return Shortlist.builder()
        .shortlistId(SHORTLIST_ID)
        .organizationId(ORGANIZATION_ID)
        .jobId(JOB_ID)
        .title("DV shortlist")
        .status(ShortlistStatus.CLIENT_VIEWED)
        .sentAt(CREATED_AT.plusSeconds(60))
        .clientViewedAt(CREATED_AT.plusSeconds(120))
        .ownerConsultantId(CONSULTANT_ID)
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT.plusSeconds(180))
        .version(2)
        .build();
  }

  private ShortlistCandidateCard shortlistCard() {
    return shortlistCard(ShortlistCandidateCardStatus.SELECTED);
  }

  private ShortlistCandidateCard shortlistCard(ShortlistCandidateCardStatus status) {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(CARD_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .anonymousCandidateCardId(ANONYMOUS_CARD_ID)
        .candidateId(CANDIDATE_ID)
        .candidateProfileId(PROFILE_ID)
        .sortOrder(1)
        .status(status)
        .matchReportId(UUID.fromString("00000000-0000-0000-0000-00000032c00b"))
        .clientNotes("Candidate is Alice from TSMC in Hsinchu.")
        .metadata(CLIENT_METADATA)
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }
}
