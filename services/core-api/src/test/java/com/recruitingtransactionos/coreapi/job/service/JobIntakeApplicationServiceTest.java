package com.recruitingtransactionos.coreapi.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class JobIntakeApplicationServiceTest {

  private final JobService jobService = mock(JobService.class);
  private final CompanyService companyService = mock(CompanyService.class);
  private final JobActivationGateService activationGateService = mock(JobActivationGateService.class);
  private final WorkflowTransitionAuditService workflowTransitionAuditService =
      mock(WorkflowTransitionAuditService.class);
  private final WorkflowEventService workflowEventService = mock(WorkflowEventService.class);
  private final NotificationService notificationService = mock(NotificationService.class);
  private final JobIntakeApplicationService service = new JobIntakeApplicationService(
      jobService,
      companyService,
      activationGateService,
      workflowTransitionAuditService,
      workflowEventService,
      notificationService);

  @Test
  void metadataContainsActorRequiresExactClientActorIdField() {
    UUID ownerActorId = UUID.randomUUID();
    UUID otherActorId = UUID.randomUUID();
    String metadata = """
        {
          "clientActorId":"%s",
          "clarificationQuestions":["Please ask %s to confirm compensation"]
        }
        """.formatted(ownerActorId, otherActorId);

    assertThat(JobIntakeApplicationService.metadataContainsActor(metadata, ownerActorId)).isTrue();
    assertThat(JobIntakeApplicationService.metadataContainsActor(metadata, otherActorId)).isFalse();
  }

  @Test
  void answerClarificationRejectsActorMentionedOnlyInFreeTextMetadata() {
    UUID organizationId = UUID.randomUUID();
    UUID ownerActorId = UUID.randomUUID();
    UUID otherActorId = UUID.randomUUID();
    JobId jobId = new JobId(UUID.randomUUID());
    Job existing = Job.builder()
        .jobId(jobId)
        .organizationId(organizationId)
        .companyId(new CompanyId(UUID.randomUUID()))
        .title("Staff Engineer")
        .status(JobStatus.SUBMITTED)
        .metadata("""
            {
              "clientActorId":"%s",
              "clarificationQuestions":["follow up with actor %s before activation"]
            }
            """.formatted(ownerActorId, otherActorId))
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    when(jobService.findJobByIdAndOrganizationId(organizationId, jobId))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.answerClarification(
        organizationId,
        otherActorId,
        jobId,
        List.of("Compensation confirmed"),
        "Updated brief",
        "Shanghai",
        "200k",
        "{\"feeModel\":\"retained\"}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("client_job_submission_not_owned_by_actor");

    verify(jobService, never()).updateJob(any());
    verify(workflowTransitionAuditService, never()).record(any());
    verify(workflowEventService, never()).append(any());
  }

  @Test
  void createClientJobSubmission_recordsClarificationEventWhenQuestionsPresent() {
    UUID organizationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    UUID consultantId = UUID.randomUUID();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    var company = com.recruitingtransactionos.coreapi.company.Company.builder()
        .companyId(companyId)
        .organizationId(organizationId)
        .name("Acme Semi")
        .status(CompanyStatus.ACTIVE)
        .ownerConsultantId(consultantId)
        .metadata("{\"clientActorId\":\"" + actorId + "\"}")
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    when(companyService.findCompanyByIdAndOrganizationId(organizationId, companyId))
        .thenReturn(Optional.of(company));
    when(jobService.createJob(any())).thenAnswer(invocation -> invocation.getArgument(0, Job.class));

    Job created = service.createClientJobSubmission(
        organizationId,
        actorId,
        companyId,
        "DV Lead",
        "Own DV roadmap",
        "Shanghai",
        "Open",
        "{}",
        List.of("Confirm compensation band"));

    assertThat(created.status()).isEqualTo(JobStatus.SUBMITTED);
    assertThat(created.location()).isEqualTo("\"Shanghai\"");
    assertThat(created.compensation()).isEqualTo("\"Open\"");
    assertThat(created.commercialTerms()).isEqualTo("{}");
    verify(workflowEventService, times(1)).append(any());
  }

  @Test
  void answerClarification_notifiesResponsibleConsultantAndAppendsWorkflowEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    UUID consultantId = UUID.randomUUID();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    JobId jobId = new JobId(UUID.randomUUID());
    Job existing = Job.builder()
        .jobId(jobId)
        .organizationId(organizationId)
        .companyId(companyId)
        .title("Staff Engineer")
        .status(JobStatus.SUBMITTED)
        .metadata("""
            {
              "clientActorId":"%s",
              "clarificationQuestions":["Confirm compensation","Confirm start date"]
            }
            """.formatted(actorId))
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    var company = com.recruitingtransactionos.coreapi.company.Company.builder()
        .companyId(companyId)
        .organizationId(organizationId)
        .name("Acme Semi")
        .status(CompanyStatus.ACTIVE)
        .ownerConsultantId(consultantId)
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    when(jobService.findJobByIdAndOrganizationId(organizationId, jobId))
        .thenReturn(Optional.of(existing));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0, Job.class));
    when(companyService.findCompanyByIdAndOrganizationId(organizationId, companyId))
        .thenReturn(Optional.of(company));

    Job persisted = service.answerClarification(
        organizationId,
        actorId,
        jobId,
        List.of("200k", "2 weeks"),
        "Updated brief",
        "Shanghai",
        "200k",
        "{\"feeModel\":\"retained\"}");

    assertThat(persisted.status()).isEqualTo(JobStatus.INTAKE_REVIEW);
    verify(workflowEventService, times(1)).append(any());
    ArgumentCaptor<NotificationService.CreateNotificationCommand> notificationCaptor =
        ArgumentCaptor.forClass(NotificationService.CreateNotificationCommand.class);
    verify(notificationService, times(2)).createNotification(notificationCaptor.capture());
    assertThat(notificationCaptor.getAllValues()).anySatisfy(command -> {
      assertThat(command.recipientUserAccountId()).isEqualTo(consultantId);
      assertThat(command.recipientPortalRole()).isEqualTo(PortalRole.CONSULTANT);
      assertThat(command.notificationType()).isEqualTo("client_clarification_answered");
    });
  }

  @Test
  void activateJobAuditsCommercialPendingBeforeContractPendingAndActivated() {
    UUID organizationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    JobId jobId = new JobId(UUID.randomUUID());
    Job existing = Job.builder()
        .jobId(jobId)
        .organizationId(organizationId)
        .companyId(new CompanyId(UUID.randomUUID()))
        .title("Staff Engineer")
        .description("Own verification planning")
        .location("Shanghai")
        .status(JobStatus.INTAKE_REVIEW)
        .commercialTerms("""
            {"feeModel":"success_fee","feeRangeOrRate":"25%","paymentTerms":"replacement_days:90","contractStatus":"consultant_confirmed"}
            """)
        .metadata("{}")
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    when(jobService.findJobByIdAndOrganizationId(organizationId, jobId))
        .thenReturn(Optional.of(existing));
    when(jobService.findRequirementsByJobIdAndOrganizationId(organizationId, jobId))
        .thenReturn(List.of());
    when(jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, jobId))
        .thenReturn(Optional.empty());
    when(activationGateService.evaluate(any(), any(), any()))
        .thenReturn(new JobActivationGateResult(true, List.of(), List.of(), true, true, true));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0, Job.class));

    Job activated = service.activateJob(
        organizationId,
        actorId,
        jobId,
        "activation evidence");

    assertThat(activated.status()).isEqualTo(JobStatus.ACTIVATED);
    ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobService, times(3)).updateJob(jobCaptor.capture());
    assertThat(jobCaptor.getAllValues().stream().map(Job::status).toList())
        .containsExactly(
            JobStatus.COMMERCIAL_PENDING,
            JobStatus.CONTRACT_PENDING,
            JobStatus.ACTIVATED);

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService, times(3)).record(auditCaptor.capture());
    assertThat(auditCaptor.getAllValues().stream().map(WorkflowTransitionAuditRequest::actionCode).toList())
        .containsExactly(
            WorkflowActionCode.JOB_COMMERCIAL_PENDING.wireValue(),
            WorkflowActionCode.JOB_CONTRACT_PENDING.wireValue(),
            WorkflowActionCode.JOB_ACTIVATED.wireValue());
  }
}
