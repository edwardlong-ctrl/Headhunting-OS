package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnlockWorkflowServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000331101");
  private static final UUID CLIENT_ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000331102");
  private static final UUID CONSULTANT_ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000331103");
  private static final CandidateId CANDIDATE_ID =
      new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000331104"));
  private static final UUID PROFILE_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000331105");
  private static final UUID JOB_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000331106");
  private static final UUID UNLOCK_WORKFLOW_ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-00000033110c");
  private static final ShortlistId SHORTLIST_ID =
      new ShortlistId(UUID.fromString("00000000-0000-0000-0000-000000331107"));
  private static final ShortlistCandidateCardId CARD_ID =
      new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000331108"));
  private static final Instant NOW = Instant.parse("2026-05-04T02:00:00Z");

  @Mock private ShortlistService shortlistService;
  @Mock private JobService jobService;
  @Mock private CompanyService companyService;
  @Mock private CandidateProfileService candidateProfileService;
  @Mock private ClientUnlockRequestPort clientUnlockRequestPort;
  @Mock private ConsentRecordPort consentRecordPort;
  @Mock private UnlockDecisionPort unlockDecisionPort;
  @Mock private DisclosureRecordPort disclosureRecordPort;
  @Mock private ConsentDisclosurePrerequisiteEvaluator prerequisiteEvaluator;
  @Mock private ConsentDisclosureService consentDisclosureService;
  @Mock private WorkflowTransitionAuditService workflowTransitionAuditService;

  private UnlockWorkflowService service;

  @BeforeEach
  void setUp() {
    service = new UnlockWorkflowService(
        shortlistService,
        jobService,
        companyService,
        candidateProfileService,
        clientUnlockRequestPort,
        consentRecordPort,
        unlockDecisionPort,
        disclosureRecordPort,
        prerequisiteEvaluator,
        consentDisclosureService,
        workflowTransitionAuditService,
        CanonicalWriteTransactionBoundary.immediate(),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createClientRequestPrecheckUsesRealClientActorIdInPrerequisiteRequest() {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(SHORTLIST_ID),
        eq(CARD_ID)))
        .thenReturn(Optional.empty());
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_ID.value().toString()),
        eq(PROFILE_UUID.toString()),
        eq(JOB_UUID.toString())))
        .thenReturn(Optional.of(consentRecord()));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(candidateProfile()));
    when(prerequisiteEvaluator.evaluate(any(), any(), any()))
        .thenReturn(new ConsentDisclosurePrerequisites(true, true, true, true, true));
    when(clientUnlockRequestPort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UnlockWorkflowService.UnlockWorkflowResult result = service.createClientRequest(
        ORGANIZATION_ID,
        CLIENT_ACTOR_ID,
        shortlist(),
        card(),
        "card_anon_3311",
        "Need identity for panel scheduling.");

    ArgumentCaptor<ConsentDisclosureServiceRequest> requestCaptor =
        ArgumentCaptor.forClass(ConsentDisclosureServiceRequest.class);
    verify(prerequisiteEvaluator).evaluate(requestCaptor.capture(), eq(Optional.empty()), eq(Optional.empty()));
    assertThat(requestCaptor.getValue().clientRef()).isEqualTo(CLIENT_ACTOR_ID.toString());
    assertThat(result.created()).isTrue();
    assertThat(result.unlockRequest()).isNotNull();
    InOrder inOrder = inOrder(workflowTransitionAuditService, clientUnlockRequestPort);
    inOrder.verify(workflowTransitionAuditService).record(any());
    inOrder.verify(clientUnlockRequestPort).create(any());

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService).record(auditCaptor.capture());
    assertThat(auditCaptor.getValue().entityType()).isEqualTo(WorkflowEntityType.UNLOCK_REQUEST.wireValue());
    assertThat(auditCaptor.getValue().entityId()).isEqualTo(result.unlockRequest().workflowEntityId());
    assertThat(auditCaptor.getValue().entityVersion()).isEqualTo(1);
    assertThat(auditCaptor.getValue().actionCode())
        .isEqualTo(WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED.wireValue());
  }

  @Test
  void createClientRequestPersistsRequestedQueueItemEvenWhenConsentIsMissing() {
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(SHORTLIST_ID),
        eq(CARD_ID)))
        .thenReturn(Optional.empty());
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_ID.value().toString()),
        eq(PROFILE_UUID.toString()),
        eq(JOB_UUID.toString())))
        .thenReturn(Optional.empty());
    when(clientUnlockRequestPort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UnlockWorkflowService.UnlockWorkflowResult result = service.createClientRequest(
        ORGANIZATION_ID,
        CLIENT_ACTOR_ID,
        shortlist(),
        card(),
        "card_anon_3311",
        "Need identity for panel scheduling.");

    assertThat(result.created()).isTrue();
    assertThat(result.unlockRequest()).isNotNull();
    assertThat(result.unlockRequest().status()).isEqualTo(ClientUnlockRequestStatus.REQUESTED);
    assertThat(result.blockers()).extracting(UnlockWorkflowService.UnlockBlocker::code)
        .containsExactly("CONSENT_MISSING");
    verify(clientUnlockRequestPort).create(any());
    verify(workflowTransitionAuditService).record(any());
  }

  @Test
  void approveRequestFailsClosedBeforePersistenceWhenReleasePreflightIsBlocked() {
    ClientUnlockRequest latestRequest = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000331109")))
        .workflowEntityId(UNLOCK_WORKFLOW_ENTITY_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .shortlistCandidateCardId(CARD_ID)
        .jobId(JOB_UUID)
        .clientActorId(CLIENT_ACTOR_ID)
        .anonymousCandidateCardRef("card_anon_3311")
        .requestReason("Need identity for panel scheduling.")
        .status(ClientUnlockRequestStatus.REQUESTED)
        .createdAt(NOW.minusSeconds(600))
        .updatedAt(NOW.minusSeconds(600))
        .build();
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(SHORTLIST_ID),
        eq(CARD_ID)))
        .thenReturn(Optional.of(latestRequest));
    when(shortlistService.findShortlistByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(SHORTLIST_ID)))
        .thenReturn(Optional.of(shortlist()));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(card()));
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_ID.value().toString()),
        eq(PROFILE_UUID.toString()),
        eq(JOB_UUID.toString())))
        .thenReturn(Optional.of(consentRecord()));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(candidateProfile()));
    when(prerequisiteEvaluator.evaluate(any(), any(), any()))
        .thenAnswer(invocation -> {
          Optional<?> unlockDecision = invocation.getArgument(1);
          Optional<?> disclosureRecord = invocation.getArgument(2);
          if (unlockDecision.isPresent() && disclosureRecord.isPresent()) {
            return new ConsentDisclosurePrerequisites(true, true, true, true, false);
          }
          return new ConsentDisclosurePrerequisites(true, true, true, true, true);
        });

    UnlockWorkflowService.UnlockApprovalResult result = service.approveRequest(
        ORGANIZATION_ID,
        CONSULTANT_ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        "Proceed to disclosure.");

    assertThat(result.unlockRequest()).isNull();
    assertThat(result.blockers())
        .extracting(UnlockWorkflowService.UnlockBlocker::code)
        .containsExactly("PRIVACY_RISK_GATE_REQUIRED");
    verify(unlockDecisionPort, never()).append(any());
    verify(disclosureRecordPort, never()).append(any());
    verify(clientUnlockRequestPort, never()).create(any());
    verify(consentDisclosureService, never()).evaluateDisclosureAttempt(any());
    verify(workflowTransitionAuditService, never()).record(any());
  }

  @Test
  void rejectRequestRecordsUnlockRejectedWorkflowEvent() {
    ClientUnlockRequest latestRequest = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000331109")))
        .workflowEntityId(UNLOCK_WORKFLOW_ENTITY_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .shortlistCandidateCardId(CARD_ID)
        .jobId(JOB_UUID)
        .clientActorId(CLIENT_ACTOR_ID)
        .anonymousCandidateCardRef("card_anon_3311")
        .requestReason("Need identity for panel scheduling.")
        .status(ClientUnlockRequestStatus.REQUESTED)
        .createdAt(NOW.minusSeconds(600))
        .updatedAt(NOW.minusSeconds(600))
        .build();
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(SHORTLIST_ID),
        eq(CARD_ID)))
        .thenReturn(Optional.of(latestRequest));
    when(shortlistService.findShortlistByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(SHORTLIST_ID)))
        .thenReturn(Optional.of(shortlist()));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(card()));
    when(unlockDecisionPort.append(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(clientUnlockRequestPort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UnlockWorkflowService.UnlockApprovalResult result = service.rejectRequest(
        ORGANIZATION_ID,
        CONSULTANT_ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        "Reject unlock because duplicate ownership risk.");

    assertThat(result.unlockRequest()).isNotNull();
    assertThat(result.unlockRequest().status()).isEqualTo(ClientUnlockRequestStatus.REJECTED);

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService).record(auditCaptor.capture());
    InOrder inOrder = inOrder(workflowTransitionAuditService, clientUnlockRequestPort);
    inOrder.verify(workflowTransitionAuditService).record(any());
    inOrder.verify(clientUnlockRequestPort).create(any());
    assertThat(auditCaptor.getValue().entityType()).isEqualTo(WorkflowEntityType.UNLOCK_REQUEST.wireValue());
    assertThat(auditCaptor.getValue().entityId()).isEqualTo(UNLOCK_WORKFLOW_ENTITY_ID);
    assertThat(auditCaptor.getValue().entityVersion()).isEqualTo(2);
    assertThat(auditCaptor.getValue().actionCode())
        .isEqualTo(WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED.wireValue());
    assertThat(result.unlockRequest().workflowEntityId()).isEqualTo(UNLOCK_WORKFLOW_ENTITY_ID);
    assertThat(result.unlockRequest().version()).isEqualTo(2);
  }

  @Test
  void approveRequestRecordsUnlockApprovedWorkflowEvent() {
    ClientUnlockRequest latestRequest = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.fromString("00000000-0000-0000-0000-000000331109")))
        .workflowEntityId(UNLOCK_WORKFLOW_ENTITY_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .shortlistCandidateCardId(CARD_ID)
        .jobId(JOB_UUID)
        .clientActorId(CLIENT_ACTOR_ID)
        .anonymousCandidateCardRef("card_anon_3311")
        .requestReason("Need identity for panel scheduling.")
        .status(ClientUnlockRequestStatus.REQUESTED)
        .createdAt(NOW.minusSeconds(600))
        .updatedAt(NOW.minusSeconds(600))
        .build();
    when(clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(SHORTLIST_ID),
        eq(CARD_ID)))
        .thenReturn(Optional.of(latestRequest));
    when(shortlistService.findShortlistByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(SHORTLIST_ID)))
        .thenReturn(Optional.of(shortlist()));
    when(shortlistService.findCardByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(CARD_ID)))
        .thenReturn(Optional.of(card()));
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_ID.value().toString()),
        eq(PROFILE_UUID.toString()),
        eq(JOB_UUID.toString())))
        .thenReturn(Optional.of(consentRecord()));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(candidateProfile()));
    when(prerequisiteEvaluator.evaluate(any(), any(), any()))
        .thenReturn(new ConsentDisclosurePrerequisites(true, true, true, true, true));
    when(unlockDecisionPort.append(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(disclosureRecordPort.append(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(consentDisclosureService.evaluateDisclosureAttempt(any()))
        .thenReturn(ConsentDisclosureServiceResult.allowed(DisclosureLevel.L4_IDENTITY_DISCLOSED));
    when(clientUnlockRequestPort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(shortlistService.updateCandidateCard(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(shortlistService.updateShortlist(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UnlockWorkflowService.UnlockApprovalResult result = service.approveRequest(
        ORGANIZATION_ID,
        CONSULTANT_ACTOR_ID,
        SHORTLIST_ID,
        CARD_ID,
        "Approve unlock after consent and risk review.");

    assertThat(result.unlockRequest()).isNotNull();
    assertThat(result.unlockRequest().status()).isEqualTo(ClientUnlockRequestStatus.APPROVED);

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService, times(3)).record(auditCaptor.capture());
    InOrder inOrder = inOrder(
        workflowTransitionAuditService,
        disclosureRecordPort,
        consentDisclosureService,
        clientUnlockRequestPort);
    inOrder.verify(workflowTransitionAuditService).record(argThat(request ->
        WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED.wireValue().equals(request.actionCode())));
    inOrder.verify(disclosureRecordPort).append(any());
    inOrder.verify(consentDisclosureService).evaluateDisclosureAttempt(any());
    inOrder.verify(workflowTransitionAuditService).record(argThat(request ->
        WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED.wireValue().equals(request.actionCode())));
    inOrder.verify(workflowTransitionAuditService).record(argThat(request ->
        WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED.wireValue().equals(request.actionCode())));
    inOrder.verify(clientUnlockRequestPort).create(any());
    assertThat(auditCaptor.getAllValues())
        .filteredOn(request -> request.actionCode().equals(WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED.wireValue()))
        .singleElement()
        .satisfies(request -> {
          assertThat(request.entityType()).isEqualTo(WorkflowEntityType.UNLOCK_REQUEST.wireValue());
          assertThat(request.entityId()).isEqualTo(UNLOCK_WORKFLOW_ENTITY_ID);
          assertThat(request.entityVersion()).isEqualTo(2);
        });
    assertThat(auditCaptor.getAllValues())
        .extracting(WorkflowTransitionAuditRequest::actionCode)
        .contains(WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED.wireValue())
        .contains(WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED.wireValue())
        .contains(WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED.wireValue());
    assertThat(result.unlockRequest().workflowEntityId()).isEqualTo(UNLOCK_WORKFLOW_ENTITY_ID);
    assertThat(result.unlockRequest().version()).isEqualTo(2);
  }

  private static Shortlist shortlist() {
    return Shortlist.builder()
        .shortlistId(SHORTLIST_ID)
        .organizationId(ORGANIZATION_ID)
        .jobId(new JobId(JOB_UUID))
        .title("Task33 shortlist")
        .status(ShortlistStatus.CANDIDATE_SELECTED)
        .createdAt(NOW.minusSeconds(3600))
        .updatedAt(NOW.minusSeconds(120))
        .version(1)
        .build();
  }

  private static ShortlistCandidateCard card() {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(CARD_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-00000033110a"))
        .candidateId(CANDIDATE_ID)
        .candidateProfileId(PROFILE_UUID)
        .sortOrder(1)
        .status(ShortlistCandidateCardStatus.SELECTED)
        .matchReportId(UUID.fromString("00000000-0000-0000-0000-00000033110b"))
        .metadata("{}")
        .createdAt(NOW.minusSeconds(3600))
        .updatedAt(NOW.minusSeconds(120))
        .version(1)
        .build();
  }

  private static ConsentRecord consentRecord() {
    return new ConsentRecord(
        "consent-task33-3311",
        ORGANIZATION_ID,
        CANDIDATE_ID.value().toString(),
        PROFILE_UUID.toString(),
        JOB_UUID.toString(),
        "7",
        "task33-v1",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(7200),
        NOW.plusSeconds(7200),
        false);
  }

  private static CandidateProfile candidateProfile() {
    return CandidateProfile.builder()
        .candidateProfileId(new CandidateProfileId(PROFILE_UUID))
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .profileVersion(new CandidateProfileVersion(7))
        .fields(List.of())
        .createdAt(NOW.minusSeconds(7200))
        .updatedAt(NOW.minusSeconds(300))
        .build();
  }
}
