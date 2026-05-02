package com.recruitingtransactionos.coreapi.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobIntakeApplicationServiceTest {

  private final JobService jobService = mock(JobService.class);
  private final CompanyService companyService = mock(CompanyService.class);
  private final JobActivationGateService activationGateService = mock(JobActivationGateService.class);
  private final WorkflowTransitionAuditService workflowTransitionAuditService =
      mock(WorkflowTransitionAuditService.class);
  private final JobIntakeApplicationService service = new JobIntakeApplicationService(
      jobService,
      companyService,
      activationGateService,
      workflowTransitionAuditService);

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

    verify(jobService, never()).updateJob(org.mockito.ArgumentMatchers.any());
    verify(workflowTransitionAuditService, never()).record(org.mockito.ArgumentMatchers.any());
  }
}
