package com.recruitingtransactionos.coreapi.apiboundary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.job.Job;
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
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientApiCommandServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000032a001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000032a002");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000032a003");
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000032a004"));
  private static final JobId JOB_ID = new JobId(UUID.fromString("00000000-0000-0000-0000-00000032a005"));
  private static final ShortlistId SHORTLIST_ID = new ShortlistId(UUID.fromString("00000000-0000-0000-0000-00000032a006"));
  private static final ShortlistCandidateCardId CARD_ID = new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-00000032a007"));
  private static final UUID ANONYMOUS_CARD_ID = UUID.fromString("00000000-0000-0000-0000-00000032a008");
  private static final CandidateId CANDIDATE_ID = new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000032a009"));
  private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000032a00a");
  private static final Instant CREATED_AT = Instant.parse("2026-05-03T10:00:00Z");
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

  @Mock private CompanyIntakeApplicationService companyIntakeApplicationService;
  @Mock private JobIntakeApplicationService jobIntakeApplicationService;
  @Mock private CompanyService companyService;
  @Mock private JobService jobService;
  @Mock private ShortlistService shortlistService;
  @Mock private ClientUnlockRequestPort clientUnlockRequestPort;
  @Mock private CandidateCompanyInteractionService interactionService;
  @Mock private InterviewFeedbackService interviewFeedbackService;
  @Mock private WorkflowTransitionAuditService workflowTransitionAuditService;
  @Mock private com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer permissionEnforcer;

  private ClientApiCommandService service;
  private AccessRequest shortlistUpdateAccessRequest;

  @BeforeEach
  void setUp() {
    service = new ClientApiCommandService(
        companyIntakeApplicationService,
        jobIntakeApplicationService,
        companyService,
        jobService,
        shortlistService,
        clientUnlockRequestPort,
        interactionService,
        interviewFeedbackService,
        workflowTransitionAuditService,
        permissionEnforcer);
    when(permissionEnforcer.requireAllowed(any()))
        .thenReturn(new AccessDecision(true, "allowed_for_test", "Allowed for focused unit test."));
    shortlistUpdateAccessRequest = new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false);
  }

  @Test
  void selectShortlistCandidate_transitionsThroughViewedBeforeSelected_whenStartingFromSentToClient() {
    Job visibleJob = visibleJob();
    Shortlist sent = shortlist(ShortlistStatus.SENT_TO_CLIENT, CREATED_AT.plusSeconds(60), null, 1);
    Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
    Shortlist selected = shortlist(ShortlistStatus.CANDIDATE_SELECTED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 3);
    ShortlistCandidateCard card = shortlistCard(ShortlistCandidateCardStatus.INCLUDED, CLIENT_METADATA);
    ShortlistCandidateCard selectedCard = shortlistCard(ShortlistCandidateCardStatus.SELECTED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(sent));
    when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID)).thenReturn(Optional.of(card));
    when(shortlistService.updateShortlist(any())).thenReturn(viewed, selected);
    when(shortlistService.updateCandidateCard(any())).thenReturn(selectedCard);

    var response = service.selectShortlistCandidate(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID);

    ArgumentCaptor<WorkflowTransitionAuditRequest> captor = ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService, org.mockito.Mockito.times(2)).record(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(WorkflowTransitionAuditRequest::actionCode)
        .containsExactly("SHORTLIST_VIEWED_BY_CLIENT", "SHORTLIST_CANDIDATE_SELECTED");
    assertThat(response.shortlistStatus()).isEqualTo("candidate_selected");
    assertThat(response.cardStatus()).isEqualTo("selected");
  }

  @Test
  void viewShortlist_returnsProjectedMetadataInsteadOfFallbackPlaceholders() {
    Job visibleJob = visibleJob();
    Shortlist sent = shortlist(ShortlistStatus.SENT_TO_CLIENT, CREATED_AT.plusSeconds(60), null, 1);
    Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
    ShortlistCandidateCard card = shortlistCard(ShortlistCandidateCardStatus.INCLUDED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(sent));
    when(shortlistService.updateShortlist(any())).thenReturn(viewed);
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(List.of(card));
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID, CARD_ID))
        .thenReturn(Optional.empty());

    var response = service.viewShortlist(shortlistUpdateAccessRequest, ORGANIZATION_ID, ACTOR_ID, SHORTLIST_ID);

    assertThat(response.cards()).hasSize(1);
    assertThat(response.cards().get(0).generalizedHeadline()).isEqualTo("Semiconductor DV Leader");
    assertThat(response.cards().get(0).safeSummary()).contains("without revealing identity");
    assertThat(response.cards().get(0).reidentificationRiskSignal()).isEqualTo("low");
    assertThat(response.cards().get(0).clientNotes()).isNull();
  }

  @Test
  void createUnlockRequest_requiresSelectedCardBeforeRequestingUnlock() {
    Job visibleJob = visibleJob();
    Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
    ShortlistCandidateCard includedCard = shortlistCard(ShortlistCandidateCardStatus.INCLUDED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(viewed));
    when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID)).thenReturn(Optional.of(includedCard));

    assertThatThrownBy(() -> service.createUnlockRequest(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        new ClientUnlockRequestCreateRequest("Need identity for interview scheduling.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_candidate_must_be_selected_before_unlock_request");
  }

  @Test
  void viewShortlist_rejectsShortlistThatIsNotYetClientVisible() {
    Job visibleJob = visibleJob();
    Shortlist draft = shortlist(ShortlistStatus.DRAFT, null, null, 1);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> service.viewShortlist(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_not_found_in_client_scope");
  }

  @Test
  void selectShortlistCandidate_rejectsRemovedCardEvenIfCardIdIsKnown() {
    Job visibleJob = visibleJob();
    Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
    ShortlistCandidateCard removedCard = shortlistCard(ShortlistCandidateCardStatus.REMOVED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(viewed));
    when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID)).thenReturn(Optional.of(removedCard));

    assertThatThrownBy(() -> service.selectShortlistCandidate(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_card_not_found_in_client_scope");
  }

  @Test
  void submitInterviewFeedback_rejectsAnonymousPhaseShortlistBeforeIdentityUnlock() {
    Job visibleJob = visibleJob();
    Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
    ShortlistCandidateCard selectedCard = shortlistCard(ShortlistCandidateCardStatus.SELECTED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(viewed));
    when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID)).thenReturn(Optional.of(selectedCard));

    assertThatThrownBy(() -> service.submitInterviewFeedback(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        new ClientInterviewFeedbackRequest("maybe", "Anonymous shortlist review note", null, null, 1, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("interview_feedback_requires_unlocked_candidate_and_post_unlock_shortlist");
  }

  @Test
  void submitInterviewFeedback_requiresUnlockedCardAfterContactUnlock() {
    Job visibleJob = visibleJob();
    Shortlist contactUnlocked = shortlist(ShortlistStatus.CONTACT_UNLOCKED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 3);
    ShortlistCandidateCard selectedCard = shortlistCard(ShortlistCandidateCardStatus.SELECTED, CLIENT_METADATA);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(contactUnlocked));
    when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID)).thenReturn(Optional.of(selectedCard));

    assertThatThrownBy(() -> service.submitInterviewFeedback(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        new ClientInterviewFeedbackRequest("yes", "Needs actual unlocked candidate before interview feedback.", null, null, 1, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("interview_feedback_requires_unlocked_candidate_and_post_unlock_shortlist");
  }

  @Test
  void selectShortlistCandidate_rejectsClosedShortlist() {
    Job visibleJob = visibleJob();
    Shortlist closed = shortlist(ShortlistStatus.CLOSED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 4);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(closed));

    assertThatThrownBy(() -> service.selectShortlistCandidate(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_selection_not_allowed_in_current_status");
  }

  @Test
  void createUnlockRequest_rejectsContactUnlockedShortlist() {
    Job visibleJob = visibleJob();
    Shortlist contactUnlocked = shortlist(ShortlistStatus.CONTACT_UNLOCKED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 4);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(contactUnlocked));

    assertThatThrownBy(() -> service.createUnlockRequest(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        new ClientUnlockRequestCreateRequest("Contact is already unlocked.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_unlock_request_not_allowed_in_current_status");
  }

  @Test
  void createUnlockRequest_rejectsInterviewingShortlist() {
    Job visibleJob = visibleJob();
    Shortlist interviewing = shortlist(ShortlistStatus.INTERVIEWING, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 5);

    when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID)).thenReturn(Optional.of(interviewing));

    assertThatThrownBy(() -> service.createUnlockRequest(
        shortlistUpdateAccessRequest,
        ORGANIZATION_ID,
        ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        new ClientUnlockRequestCreateRequest("Interviewing stage should be read-only for unlock.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_unlock_request_not_allowed_in_current_status");
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

  private Shortlist shortlist(ShortlistStatus status, Instant sentAt, Instant clientViewedAt, int version) {
    return Shortlist.builder()
        .shortlistId(SHORTLIST_ID)
        .organizationId(ORGANIZATION_ID)
        .jobId(JOB_ID)
        .title("DV shortlist")
        .status(status)
        .sentAt(sentAt)
        .clientViewedAt(clientViewedAt)
        .ownerConsultantId(CONSULTANT_ID)
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT.plusSeconds(version))
        .version(version)
        .build();
  }

  private ShortlistCandidateCard shortlistCard(ShortlistCandidateCardStatus status, String metadata) {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(CARD_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .anonymousCandidateCardId(ANONYMOUS_CARD_ID)
        .candidateId(CANDIDATE_ID)
        .candidateProfileId(PROFILE_ID)
        .sortOrder(1)
        .status(status)
        .matchReportId(UUID.fromString("00000000-0000-0000-0000-00000032a00b"))
        .clientNotes("Client-safe note")
        .metadata(metadata)
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }
}
