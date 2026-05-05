package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.CandidateOpportunityDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateMeResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateProfileReviewResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateTimelineResponse;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflict;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflictResolutionStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflictSeverity;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflictValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStaleness;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.followup.FollowUpSubmissionService;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidatePortalQueryServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000031aa01");
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-00000031aa02");
  private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000031aa03");
  private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-00000031aa04");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000031aa05");

  @Mock private CandidateService candidateService;
  @Mock private CandidateProfileService candidateProfileService;
  @Mock private CandidateDocumentService candidateDocumentService;
  @Mock private CandidateCompanyInteractionService interactionService;
  @Mock private DocumentUploadService documentUploadService;
  @Mock private WorkflowAuditQueryService workflowAuditQueryService;
  @Mock private JobService jobService;
  @Mock private CompanyService companyService;
  @Mock private CandidateConsentWorkflowService candidateConsentWorkflowService;
  @Mock private ConsentRecordPort consentRecordPort;
  @Mock private WorkflowEventService workflowEventService;
  @Mock private FollowUpSubmissionService followUpSubmissionService;
  @Mock private NotificationService notificationService;

  private CandidatePortalQueryService service;

  @BeforeEach
  void setUp() {
    service = new CandidatePortalQueryService(
        candidateService,
        candidateProfileService,
        candidateDocumentService,
        interactionService,
        documentUploadService,
        workflowAuditQueryService,
        jobService,
        companyService,
        candidateConsentWorkflowService,
        consentRecordPort,
        workflowEventService,
        followUpSubmissionService,
        notificationService);
  }

  @Test
  void buildMe_usesCandidateReadableNameAndCountsPendingFollowUps() {
    CandidateProfile profile = candidateProfile(List.of(
        profileField("identity.full_name", "Alice Chen", CandidateProfileFieldStatus.AI_EXTRACTED, null, null),
        profileField("profile.headline", "Principal DV Engineer", CandidateProfileFieldStatus.NEEDS_CONFIRMATION, null, null),
        profileField("location.current_city", "Shanghai", CandidateProfileFieldStatus.CANDIDATE_CONFIRMED, null, null),
        profileField("skills.primary_skills", "[\"UVM\",\"SystemVerilog\"]", CandidateProfileFieldStatus.CONFLICTING, conflict(), null),
        profileField("profile.summary", "Verification lead", CandidateProfileFieldStatus.AI_EXTRACTED, null, stale())));
    when(candidateService.findCandidateByIdAndOrganizationId(ORGANIZATION_ID, new CandidateId(USER_ID)))
        .thenReturn(Optional.of(candidate()));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(Optional.of(profile));
    when(candidateDocumentService.findDocumentsByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(List.of(
            org.mockito.Mockito.mock(CandidateDocument.class),
            org.mockito.Mockito.mock(CandidateDocument.class)));
    when(interactionService.findInteractionsByCandidateId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(List.of(interaction(), interaction()));

    CandidateMeResponse response = service.buildMe(ORGANIZATION_ID, USER_ID, "Session Candidate");

    assertThat(response.displayName()).isEqualTo("Alice Chen");
    assertThat(response.pendingFollowUpCount()).isEqualTo(3);
    assertThat(response.documentCount()).isEqualTo(2);
    assertThat(response.activeOpportunityCount()).isEqualTo(2);
  }

  @Test
  void confirmProfileFields_marksRequestedActionableFieldsAsCandidateConfirmed() {
    CandidateProfile initialProfile = candidateProfile(List.of(
        profileField("profile.headline", "Principal DV Engineer", CandidateProfileFieldStatus.NEEDS_CONFIRMATION, null, stale()),
        profileField("location.current_city", "Shanghai", CandidateProfileFieldStatus.CANDIDATE_CONFIRMED, null, null)));
    CandidateProfile updatedProfile = candidateProfile(List.of(
        profileField("profile.headline", "Principal DV Engineer", CandidateProfileFieldStatus.CANDIDATE_CONFIRMED, null, null),
        profileField("location.current_city", "Shanghai", CandidateProfileFieldStatus.CANDIDATE_CONFIRMED, null, null)));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(Optional.of(initialProfile), Optional.of(updatedProfile));
    when(candidateProfileService.upsertCandidateProfileField(any())).thenAnswer(invocation -> invocation.getArgument(0, UpsertCandidateProfileFieldRequest.class).toCandidateProfileField());

    CandidateProfileReviewResponse response = service.confirmProfileFields(
        ORGANIZATION_ID,
        USER_ID.toString(),
        USER_ID,
        "profile.headline");

    ArgumentCaptor<UpsertCandidateProfileFieldRequest> captor =
        ArgumentCaptor.forClass(UpsertCandidateProfileFieldRequest.class);
    verify(candidateProfileService, times(1)).upsertCandidateProfileField(captor.capture());
    assertThat(captor.getValue().fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED);
    assertThat(captor.getValue().confirmedByActorId()).isEqualTo(USER_ID);
    assertThat(captor.getValue().staleness()).isNull();
    verify(workflowEventService, times(1)).append(any());
    assertThat(response.fields()).hasSize(2);
    assertThat(response.fields().get(0).status()).isEqualTo("candidate_confirmed");
  }

  @Test
  void buildTimeline_mergesCandidateAndConsentWorkflowEvents() {
    ConsentRecord consentRecord = new ConsentRecord(
        "consent-task31-1",
        ORGANIZATION_ID,
        USER_ID.toString(),
        PROFILE_ID.toString(),
        JOB_ID.toString(),
        "7",
        "task31-v1",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L3_CONSENTED_DETAIL),
        Instant.parse("2026-05-04T10:20:00Z"),
        Instant.parse("2026-05-18T00:00:00Z"),
        false);
    when(consentRecordPort.listByCandidateRef(ORGANIZATION_ID, USER_ID.toString()))
        .thenReturn(List.of(consentRecord));
    when(workflowAuditQueryService.search(any(WorkflowAuditQuery.class))).thenAnswer(invocation -> {
      WorkflowAuditQuery query = invocation.getArgument(0);
      if (WorkflowEntityType.CANDIDATE.wireValue().equals(query.entityType())) {
        return List.of(workflowRecord(
            "CANDIDATE_CONSENT_CONFIRMED",
            WorkflowEntityType.CANDIDATE.wireValue(),
            ConsentDisclosureWorkflowEntityIds.candidateEntityId(ORGANIZATION_ID, USER_ID.toString()),
            "consent_confirmed",
            Instant.parse("2026-05-04T10:30:00Z")));
      }
      return List.of(workflowRecord(
          "CONSENT_VIEWED_BY_CANDIDATE",
          WorkflowEntityType.CONSENT.wireValue(),
          ConsentDisclosureWorkflowEntityIds.consentEntityId(ORGANIZATION_ID, consentRecord.consentRecordRef()),
          "viewed_by_candidate",
          Instant.parse("2026-05-04T10:15:00Z")));
    });

    CandidateTimelineResponse response = service.buildTimeline(ORGANIZATION_ID, USER_ID.toString());

    assertThat(response.events()).hasSize(2);
    assertThat(response.events().get(0).actionCode()).isEqualTo("CANDIDATE_CONSENT_CONFIRMED");
    assertThat(response.events().get(0).status()).isEqualTo("consent_confirmed");
    assertThat(response.events().get(1).actionCode()).isEqualTo("CONSENT_VIEWED_BY_CANDIDATE");
  }

  @Test
  void submitFollowUpAnswer_preservesCanonicalFieldAndLeavesConsultantGateIntact() {
    CandidateProfile initialProfile = candidateProfile(List.of(
        profileField("availability.notice_period", "30 days", CandidateProfileFieldStatus.NEEDS_CONFIRMATION, null, stale())));
    when(candidateService.findCandidateByIdAndOrganizationId(ORGANIZATION_ID, new CandidateId(USER_ID)))
        .thenReturn(Optional.of(candidate()));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(Optional.of(initialProfile), Optional.of(initialProfile));
    when(workflowEventService.append(any())).thenReturn(
        new WorkflowEventAppendResult(new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000031aa99"))));
    when(followUpSubmissionService.create(any())).thenAnswer(invocation -> {
      FollowUpSubmissionService.CreateFollowUpSubmissionCommand command =
          invocation.getArgument(0, FollowUpSubmissionService.CreateFollowUpSubmissionCommand.class);
      return new FollowUpSubmissionService.FollowUpSubmission(
          command.followUpSubmissionId(),
          command.organizationId(),
          command.candidateId(),
          command.candidateProfileId(),
          command.formId(),
          command.fieldPath(),
          command.answerJson(),
          command.status(),
          command.submittedByUserId(),
          null,
          command.workflowEventId(),
          command.submittedAt(),
          null,
          command.notes(),
          command.submittedAt(),
          command.submittedAt(),
          1);
    });

    service.submitFollowUpAnswer(
        ORGANIZATION_ID,
        USER_ID.toString(),
        USER_ID,
        "current-profile",
        "availability.notice_period",
        "Available in 2 weeks");

    verify(candidateProfileService, never()).upsertCandidateProfileField(any());
    ArgumentCaptor<WorkflowEventAppendCommand> workflowEventCaptor =
        ArgumentCaptor.forClass(WorkflowEventAppendCommand.class);
    verify(workflowEventService).append(workflowEventCaptor.capture());
    ArgumentCaptor<FollowUpSubmissionService.CreateFollowUpSubmissionCommand> submissionCaptor =
        ArgumentCaptor.forClass(FollowUpSubmissionService.CreateFollowUpSubmissionCommand.class);
    verify(followUpSubmissionService).create(submissionCaptor.capture());
    ArgumentCaptor<NotificationService.CreateNotificationCommand> notificationCaptor =
        ArgumentCaptor.forClass(NotificationService.CreateNotificationCommand.class);
    verify(notificationService, times(2)).createNotification(notificationCaptor.capture());
    UUID submissionId = submissionCaptor.getValue().followUpSubmissionId();
    assertThat(notificationCaptor.getAllValues()).anySatisfy(command -> {
      assertThat(command.recipientUserAccountId()).isEqualTo(USER_ID);
      assertThat(command.recipientPortalRole()).isEqualTo(com.recruitingtransactionos.coreapi.identityaccess.PortalRole.CANDIDATE);
      assertThat(command.entityType()).isEqualTo(WorkflowEntityType.FOLLOW_UP_SUBMISSION.wireValue());
      assertThat(command.entityId()).isEqualTo(submissionId);
    });
    assertThat(notificationCaptor.getAllValues()).anySatisfy(command -> {
      assertThat(command.recipientUserAccountId()).isEqualTo(CONSULTANT_ID);
      assertThat(command.recipientPortalRole()).isEqualTo(com.recruitingtransactionos.coreapi.identityaccess.PortalRole.CONSULTANT);
      assertThat(command.notificationType()).isEqualTo("candidate_follow_up_submitted");
      assertThat(command.deepLink()).isEqualTo("/consultant/follow-ups");
      assertThat(command.entityId()).isEqualTo(submissionId);
    });
    assertThat(workflowEventCaptor.getValue().entity().entityType())
        .isEqualTo(WorkflowEntityType.FOLLOW_UP_SUBMISSION.wireValue());
    assertThat(workflowEventCaptor.getValue().entity().entityId())
        .isEqualTo(submissionId);
  }

  @Test
  void buildOpportunityDetail_redactsCompanyIdentityForCandidateView() {
    CandidateProfile profile = candidateProfile(List.of(
        profileField("identity.full_name", "Alice Chen", CandidateProfileFieldStatus.AI_EXTRACTED, null, null)));
    CandidateCompanyInteraction interaction = interaction();
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(Optional.of(profile));
    when(interactionService.findInteractionByIdAndOrganizationId(
        ORGANIZATION_ID,
        interaction.candidateCompanyInteractionId())).thenReturn(Optional.of(interaction));
    when(jobService.findJobByIdAndOrganizationId(ORGANIZATION_ID, interaction.jobId()))
        .thenReturn(Optional.of(job(interaction.companyId())));
    when(companyService.findCompanyByIdAndOrganizationId(ORGANIZATION_ID, interaction.companyId()))
        .thenReturn(Optional.of(company(interaction.companyId(), "TSMC")));
    when(candidateConsentWorkflowService.latestConsentSnapshot(
        ORGANIZATION_ID,
        USER_ID.toString(),
        PROFILE_ID.toString(),
        JOB_ID.toString())).thenThrow(new IllegalArgumentException("consent_request_not_found"));

    CandidateOpportunityDetailResponse response = service.buildOpportunityDetail(
        ORGANIZATION_ID,
        USER_ID,
        USER_ID.toString(),
        interaction.candidateCompanyInteractionId().value().toString());

    assertThat(response.companyName()).isEqualTo("Top semiconductor foundry");
    assertThat(response.companyName()).isNotEqualTo("TSMC");
  }

  @Test
  void recordOpportunityInterest_keepsInteractionStatusAndWritesInterestMetadataOnly() {
    CandidateProfile profile = candidateProfile(List.of(
        profileField("identity.full_name", "Alice Chen", CandidateProfileFieldStatus.AI_EXTRACTED, null, null)));
    CandidateCompanyInteraction interaction = interaction();
    CandidateCompanyInteraction updatedInteraction = CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(interaction.candidateCompanyInteractionId())
        .organizationId(interaction.organizationId())
        .candidateId(interaction.candidateId())
        .companyId(interaction.companyId())
        .jobId(interaction.jobId())
        .interactionType(interaction.interactionType())
        .status(interaction.status())
        .startedAt(interaction.startedAt())
        .endedAt(interaction.endedAt())
        .notes(interaction.notes())
        .metadata("{\"candidateInterestStatus\":\"declined\",\"interestUpdatedAt\":\"2026-05-04T00:00:00Z\"}")
        .createdAt(interaction.createdAt())
        .updatedAt(interaction.updatedAt())
        .version(interaction.version() + 1)
        .build();
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        ORGANIZATION_ID, new CandidateId(USER_ID))).thenReturn(Optional.of(profile));
    when(interactionService.findInteractionByIdAndOrganizationId(
        ORGANIZATION_ID,
        interaction.candidateCompanyInteractionId())).thenReturn(Optional.of(interaction), Optional.of(updatedInteraction));
    when(interactionService.updateInteraction(any())).thenReturn(updatedInteraction);
    when(jobService.findJobByIdAndOrganizationId(ORGANIZATION_ID, interaction.jobId()))
        .thenReturn(Optional.of(job(interaction.companyId())));
    when(companyService.findCompanyByIdAndOrganizationId(ORGANIZATION_ID, interaction.companyId()))
        .thenReturn(Optional.of(company(interaction.companyId(), "TSMC")));
    when(candidateConsentWorkflowService.latestConsentSnapshot(
        ORGANIZATION_ID,
        USER_ID.toString(),
        PROFILE_ID.toString(),
        JOB_ID.toString())).thenThrow(new IllegalArgumentException("consent_request_not_found"));

    CandidateOpportunityDetailResponse response = service.recordOpportunityInterest(
        ORGANIZATION_ID,
        USER_ID,
        USER_ID.toString(),
        interaction.candidateCompanyInteractionId().value().toString(),
        "declined",
        "Timing is not right");

    ArgumentCaptor<CandidateCompanyInteraction> captor =
        ArgumentCaptor.forClass(CandidateCompanyInteraction.class);
    verify(interactionService).updateInteraction(captor.capture());
    assertThat(captor.getValue().status()).isEqualTo(InteractionStatus.ACTIVE);
    assertThat(captor.getValue().endedAt()).isNull();
    assertThat(captor.getValue().metadata()).contains("\"candidateInterestStatus\":\"declined\"");
    verify(workflowEventService, times(1)).append(any());
    assertThat(response.interestStatus()).isEqualTo("declined");
    assertThat(response.companyName()).isEqualTo("Top semiconductor foundry");
  }

  private static Candidate candidate() {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return Candidate.builder()
        .candidateId(new CandidateId(USER_ID))
        .organizationId(ORGANIZATION_ID)
        .status(CandidateStatus.CONSENT_PENDING)
        .currentProfileId(new CandidateProfileId(PROFILE_ID))
        .privacyStatus("internal_only")
        .ownerConsultantId(CONSULTANT_ID)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private static CandidateProfile candidateProfile(List<CandidateProfileField> fields) {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return CandidateProfile.builder()
        .candidateProfileId(new CandidateProfileId(PROFILE_ID))
        .organizationId(ORGANIZATION_ID)
        .candidateId(new CandidateId(USER_ID))
        .profileVersion(new CandidateProfileVersion(7))
        .fields(fields)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private static CandidateProfileField profileField(
      String fieldPath,
      String value,
      CandidateProfileFieldStatus status,
      CandidateProfileFieldConflict conflict,
      CandidateProfileFieldStaleness staleness) {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return CandidateProfileField.builder()
        .fieldPath(new CandidateProfileFieldPath(fieldPath))
        .value(CandidateProfileFieldValue.ofJson(value.startsWith("[") ? value : "\"" + value + "\""))
        .fieldStatus(status)
        .lineage(new CandidateProfileFieldLineage(
            List.of(CandidateProfileFieldSourceReference.sourceSpan("source-span-1", "high", now)),
            "candidate_portal_test",
            now))
        .conflict(conflict)
        .staleness(staleness)
        .lastReviewedAt(now)
        .confirmedByActorId(status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED ? USER_ID : null)
        .confirmedAgainstProfileVersion(status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED
            ? new CandidateProfileVersion(7)
            : null)
        .notes("test field")
        .build();
  }

  private static CandidateProfileFieldConflict conflict() {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return new CandidateProfileFieldConflict(
        new CandidateProfileFieldPath("skills.primary_skills"),
        List.of(
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("UVM"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan("source-span-a", "high", now))),
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("SystemVerilog"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan("source-span-b", "high", now)))),
        CandidateProfileFieldConflictSeverity.MEDIUM,
        CandidateProfileFieldConflictResolutionStatus.UNRESOLVED,
        now,
        "conflict");
  }

  private static CandidateProfileFieldStaleness stale() {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return new CandidateProfileFieldStaleness(true, "Needs refresh", now.minusSeconds(7200), now.minusSeconds(3600), now.plusSeconds(3600), now);
  }

  private static CandidateCompanyInteraction interaction() {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(new CandidateCompanyInteractionId(UUID.randomUUID()))
        .organizationId(ORGANIZATION_ID)
        .candidateId(new CandidateId(USER_ID))
        .companyId(new CompanyId(UUID.randomUUID()))
        .jobId(new JobId(JOB_ID))
        .interactionType(InteractionType.SUBMISSION)
        .status(InteractionStatus.ACTIVE)
        .startedAt(now)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private static Job job(CompanyId companyId) {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORGANIZATION_ID)
        .companyId(companyId)
        .title("Principal Verification Engineer")
        .description("Own verification strategy for a leading compute platform.")
        .location("Singapore")
        .roleFamily("semiconductor")
        .compensation("SGD 180k - 220k")
        .status(JobStatus.ACTIVATED)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private static Company company(CompanyId companyId, String name) {
    Instant now = Instant.parse("2026-05-04T00:00:00Z");
    return Company.builder()
        .companyId(companyId)
        .organizationId(ORGANIZATION_ID)
        .name(name)
        .industry("semiconductor")
        .status(CompanyStatus.ACTIVE)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private static WorkflowAuditRecord workflowRecord(
      String actionCode,
      String entityType,
      UUID entityId,
      String status,
      Instant occurredAt) {
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.randomUUID()),
        ORGANIZATION_ID,
        "workflow",
        entityType,
        entityId,
        actionCode,
        ActorRole.CANDIDATE,
        USER_ID,
        WorkflowAiInvolvement.NONE,
        RiskTier.T3_HIGH_RISK,
        new WorkflowStateSnapshot("{\"status\":\"pending\"}"),
        new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}"),
        "timeline test event",
        null,
        null,
        null,
        null,
        "portal_api",
        entityId,
        occurredAt,
        occurredAt);
  }
}
