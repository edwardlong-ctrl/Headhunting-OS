package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderService;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderState;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistDeliveryPreview;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistPreSendCheck;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultantApiCommandServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
  private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000112");
  private static final UUID SHORTLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000113");
  private static final Instant NOW = Instant.parse("2026-05-03T08:00:00Z");

  @Mock private CompanyService companyService;
  @Mock private JobService jobService;
  @Mock private ShortlistService shortlistService;
  @Mock private ShortlistBuilderService shortlistBuilderService;
  @Mock private IndustryPackService industryPackService;
  @Mock private WorkflowTransitionAuditService workflowTransitionAuditService;

  @Test
  void updateShortlistRejectsSentToClientStatusOutsideSendCommand() {
    Shortlist existing = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    Job job = Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000115")))
        .title("Platform engineer")
        .status(JobStatus.ACTIVATED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, existing.shortlistId()))
        .thenReturn(Optional.of(existing));
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.of(job));

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ShortlistUpdateRequest request = new ShortlistUpdateRequest(
        JOB_ID.toString(),
        "Task 29 shortlist",
        ShortlistStatus.SENT_TO_CLIENT.wireValue(),
        null,
        "{}",
        1);

    assertThatThrownBy(() -> service.updateShortlist(
        shortlistUpdateAccessRequest(),
        ORG_ID,
        UUID.fromString("00000000-0000-0000-0000-000000000114"),
        existing.shortlistId(),
        request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_status_change_requires_send_command");

    verify(shortlistService, never()).updateShortlist(any());
  }

  @Test
  void updateShortlistRejectsMutationAfterShortlistWasSent() {
    Shortlist existing = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.SENT_TO_CLIENT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(2)
        .build();
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, existing.shortlistId()))
        .thenReturn(Optional.of(existing));

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ShortlistUpdateRequest request = new ShortlistUpdateRequest(
        JOB_ID.toString(),
        "Task 29 shortlist reopened",
        ShortlistStatus.READY_FOR_REVIEW.wireValue(),
        null,
        "{}",
        2);

    assertThatThrownBy(() -> service.updateShortlist(
        shortlistUpdateAccessRequest(),
        ORG_ID,
        UUID.fromString("00000000-0000-0000-0000-000000000114"),
        existing.shortlistId(),
        request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shortlist_builder_locked_after_send");

    verify(shortlistService, never()).updateShortlist(any());
    verify(jobService, never()).findJobByIdAndOrganizationId(any(), any());
  }

  @Test
  void updateShortlistRejectsCreateAccessContext() {
    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ShortlistUpdateRequest request = new ShortlistUpdateRequest(
        JOB_ID.toString(),
        "Task 29 shortlist",
        ShortlistStatus.DRAFT.wireValue(),
        null,
        "{}",
        1);

    assertThatThrownBy(() -> service.updateShortlist(
        shortlistCreateAccessRequest(),
        ORG_ID,
        UUID.fromString("00000000-0000-0000-0000-000000000114"),
        new ShortlistId(SHORTLIST_ID),
        request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("shortlist_write_context_required");

    verify(shortlistService, never()).findShortlistByIdAndOrganizationId(any(), any());
  }

  @Test
  void updateShortlistCandidateCardRejectsDownstreamWorkflowStatuses() {
    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    assertThatThrownBy(() -> service.updateShortlistCandidateCard(
        shortlistUpdateAccessRequest(),
        ORG_ID,
        UUID.fromString("00000000-0000-0000-0000-000000000114"),
        new ShortlistId(SHORTLIST_ID),
        new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000116")),
        new ShortlistCandidateCardUpdateRequest(
            null,
            ShortlistCandidateCardStatus.SELECTED.wireValue(),
            null,
            1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_builder_card_status_requires_dedicated_workflow");

    verify(shortlistBuilderService, never()).updateCandidateCard(any(), any(), any(), any(), anyInt(), any(), any(), any());
  }

  @Test
  void updateShortlistCandidateCardPassesActorIdToBuilderService() {
    UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000114");
    Shortlist updated = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(2)
        .build();
    ShortlistBuilderState builderState = new ShortlistBuilderState(
        updated,
        List.of(),
        List.of(
            new ShortlistPreSendCheck("status_ready_for_review", "ready", false),
            new ShortlistPreSendCheck("has_included_cards", "cards", false),
            new ShortlistPreSendCheck("anonymous_cards_generated", "anonymous", false),
            new ShortlistPreSendCheck("delivery_preview_ready", "preview", false),
            new ShortlistPreSendCheck("reidentification_risk_within_gate", "risk", false),
            new ShortlistPreSendCheck("client_safe_consent_confirmed", "consent", false)),
        new ShortlistDeliveryPreview("summary", "pdf", "email", "wechat"));
    when(shortlistBuilderService.updateCandidateCard(
        eq(ORG_ID),
        eq(actorId),
        eq(new ShortlistId(SHORTLIST_ID)),
        eq(new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000116"))),
        eq(1),
        eq(null),
        eq(ShortlistCandidateCardStatus.REMOVED),
        eq(null)))
        .thenReturn(builderState);

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantShortlistDetailResponse response = service.updateShortlistCandidateCard(
        shortlistUpdateAccessRequest(),
        ORG_ID,
        actorId,
        new ShortlistId(SHORTLIST_ID),
        new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000116")),
        new ShortlistCandidateCardUpdateRequest(
            null,
            ShortlistCandidateCardStatus.REMOVED.wireValue(),
            null,
            1));

    verify(shortlistBuilderService).updateCandidateCard(
        ORG_ID,
        actorId,
        new ShortlistId(SHORTLIST_ID),
        new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000116")),
        1,
        null,
        ShortlistCandidateCardStatus.REMOVED,
        null);
    assertThat(response.status()).isEqualTo(ShortlistStatus.DRAFT.wireValue());
  }

  @Test
  void updateShortlistRecordsRollbackAuditWhenReturningToDraft() {
    UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000114");
    Shortlist existing = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.READY_FOR_REVIEW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(2)
        .build();
    Shortlist updated = Shortlist.builder()
        .shortlistId(existing.shortlistId())
        .organizationId(ORG_ID)
        .jobId(existing.jobId())
        .title("Task 29 shortlist revised")
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(3)
        .build();
    Job job = Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000115")))
        .title("Platform engineer")
        .status(JobStatus.ACTIVATED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    ShortlistBuilderState builderState = new ShortlistBuilderState(
        updated,
        List.of(),
        List.of(
            new ShortlistPreSendCheck("status_ready_for_review", "ready", false),
            new ShortlistPreSendCheck("has_included_cards", "cards", false),
            new ShortlistPreSendCheck("anonymous_cards_generated", "anonymous", true),
            new ShortlistPreSendCheck("delivery_preview_ready", "preview", false),
            new ShortlistPreSendCheck("reidentification_risk_within_gate", "risk", true),
            new ShortlistPreSendCheck("client_safe_consent_confirmed", "consent", true)),
        new ShortlistDeliveryPreview("summary", "pdf", "email", "wechat"));

    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, existing.shortlistId()))
        .thenReturn(Optional.of(existing));
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.of(job));
    when(shortlistService.updateShortlist(any())).thenReturn(updated);
    when(shortlistBuilderService.getBuilderState(ORG_ID, existing.shortlistId())).thenReturn(builderState);

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantShortlistDetailResponse response = service.updateShortlist(
        shortlistUpdateAccessRequest(),
        ORG_ID,
        actorId,
        existing.shortlistId(),
        new ShortlistUpdateRequest(
            JOB_ID.toString(),
            "Task 29 shortlist revised",
            ShortlistStatus.DRAFT.wireValue(),
            null,
            "{}",
            2));

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService).record(auditCaptor.capture());
    assertThat(auditCaptor.getValue().actionCode())
        .isEqualTo(WorkflowActionCode.SHORTLIST_RETURNED_TO_DRAFT.wireValue());
    assertThat(response.preSendChecks()).hasSize(6);
  }

  @Test
  void createShortlistReturnsBuilderBackedDetailWhenBuilderServiceExists() {
    UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000114");
    Job job = Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000115")))
        .title("Platform engineer")
        .status(JobStatus.ACTIVATED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    Shortlist created = Shortlist.builder()
        .shortlistId(new ShortlistId(SHORTLIST_ID))
        .organizationId(ORG_ID)
        .jobId(new JobId(JOB_ID))
        .title("Task 29 shortlist")
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    ShortlistBuilderState builderState = new ShortlistBuilderState(
        created,
        List.of(),
        List.of(
            new ShortlistPreSendCheck("status_ready_for_review", "ready", false),
            new ShortlistPreSendCheck("has_included_cards", "cards", false),
            new ShortlistPreSendCheck("anonymous_cards_generated", "anonymous", true),
            new ShortlistPreSendCheck("delivery_preview_ready", "preview", false),
            new ShortlistPreSendCheck("reidentification_risk_within_gate", "risk", true),
            new ShortlistPreSendCheck("client_safe_consent_confirmed", "consent", true)),
        new ShortlistDeliveryPreview("summary", "pdf", "email", "wechat"));

    when(jobService.findJobByIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.of(job));
    when(shortlistService.createShortlist(any())).thenReturn(created);
    when(shortlistBuilderService.getBuilderState(ORG_ID, created.shortlistId())).thenReturn(builderState);

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    ConsultantShortlistDetailResponse response = service.createShortlist(
        shortlistCreateAccessRequest(),
        ORG_ID,
        actorId,
        new ShortlistCreateRequest(JOB_ID.toString(), "Task 29 shortlist", ShortlistStatus.DRAFT.wireValue(), null, "{}"));

    assertThat(response.preSendChecks()).hasSize(6);
    assertThat(response.status()).isEqualTo(ShortlistStatus.DRAFT.wireValue());
    assertThat(response.shortlistId()).isEqualTo(SHORTLIST_ID.toString());
  }

  @Test
  void updateJobNormalizesPilotCommercialTermsForActivationGate() {
    CompanyId companyId = new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000115"));
    Job existing = Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORG_ID)
        .companyId(companyId)
        .title("Platform engineer")
        .status(JobStatus.INTAKE_REVIEW)
        .metadata("{\"clientActorId\":\"00000000-0000-0000-0000-000000380103\"}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    when(companyService.findCompanyByIdAndOrganizationId(ORG_ID, companyId))
        .thenReturn(Optional.of(com.recruitingtransactionos.coreapi.company.Company.builder()
            .companyId(companyId)
            .organizationId(ORG_ID)
            .name("Pilot Client")
            .status(com.recruitingtransactionos.coreapi.company.CompanyStatus.ACTIVE)
            .metadata("{}")
            .createdAt(NOW)
            .updatedAt(NOW)
            .version(1)
            .build()));
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.of(existing));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(jobService.findRequirementsByJobIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(List.of());
    when(jobService.findActiveScorecardByJobIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.empty());
    when(industryPackService.findIndustryPackById(null)).thenReturn(Optional.empty());

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    service.updateJob(
        jobUpdateAccessRequest(),
        ORG_ID,
        new JobId(JOB_ID),
        new JobUpdateRequest(
            companyId.value().toString(),
            "Platform engineer",
            "Lead verification planning.",
            "Shanghai",
            "staff",
            "ASIC verification",
            "full_time",
            "700k-950k RMB",
            JobStatus.INTAKE_REVIEW.wireValue(),
            "{\"feeRate\":\"25%\",\"replacementDays\":90,\"approval\":\"consultant_confirmed\"}",
            null,
            "{}",
            1));

    ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobService).updateJob(jobCaptor.capture());
    assertThat(jobCaptor.getValue().commercialTerms())
        .contains("\"feeModel\":\"success_fee\"")
        .contains("\"feeRangeOrRate\":\"25%\"")
        .contains("\"paymentTerms\":\"replacement_days:90\"")
        .contains("\"contractStatus\":\"consultant_confirmed\"");
    assertThat(jobCaptor.getValue().metadata())
        .contains("\"clientActorId\":\"00000000-0000-0000-0000-000000380103\"");
  }

  @Test
  void createJobScorecardNormalizesPilotConfirmedStatusAndJsonbPayloads() {
    Job job = Job.builder()
        .jobId(new JobId(JOB_ID))
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000115")))
        .title("Platform engineer")
        .status(JobStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.of(job));
    when(jobService.findRequirementsByJobIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(List.of());
    when(jobService.findActiveScorecardByJobIdAndOrganizationId(ORG_ID, new JobId(JOB_ID)))
        .thenReturn(Optional.empty());
    when(industryPackService.findIndustryPackById(null)).thenReturn(Optional.empty());

    ConsultantApiCommandService service = new ConsultantApiCommandService(
        companyService,
        jobService,
        null,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));

    service.createJobScorecard(
        jobCreateAccessRequest(),
        ORG_ID,
        new JobId(JOB_ID),
        new JobScorecardCreateRequest(
            "[{\"label\":\"Technical fit\"}]",
            "Prioritize evidence-backed semiconductor verification experience.",
            "confirmed",
            null));

    ArgumentCaptor<JobScorecard> scorecardCaptor = ArgumentCaptor.forClass(JobScorecard.class);
    verify(jobService).createScorecard(scorecardCaptor.capture());
    JobScorecard scorecard = scorecardCaptor.getValue();
    assertThat(scorecard.status()).isEqualTo("active");
    assertThat(scorecard.dimensions()).isEqualTo("[{\"label\":\"Technical fit\"}]");
    assertThat(scorecard.metadata()).isEqualTo("{}");
  }

  private static AccessRequest shortlistUpdateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.SHORTLIST,
        AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }

  private static AccessRequest shortlistCreateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.SHORTLIST,
        AccessAction.CREATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }

  private static AccessRequest jobUpdateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.JOB,
        AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }

  private static AccessRequest jobCreateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.JOB,
        AccessAction.CREATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }
}
