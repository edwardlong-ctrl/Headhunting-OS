package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerOutput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerTaskService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackDecision;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcome;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InterviewFeedbackOutcomeLoopServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID ACTOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final JobId JOB_ID = new JobId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  private static final CandidateId CANDIDATE_ID = new CandidateId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  private static final ShortlistId SHORTLIST_ID =
      new ShortlistId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  private static final ShortlistCandidateCardId CARD_ID =
      new ShortlistCandidateCardId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  private static final CandidateCompanyInteractionId INTERACTION_ID =
      new CandidateCompanyInteractionId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
  private static final InterviewFeedbackId FEEDBACK_ID =
      new InterviewFeedbackId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
  private static final Instant NOW = Instant.parse("2026-05-09T00:00:00Z");

  @Test
  void nullRejectReasonTaxonomyDoesNotFailOutcomeLoop() {
    InterviewFeedbackService feedbackService = mock(InterviewFeedbackService.class);
    InterviewFeedbackSuggestionService suggestionService = mock(InterviewFeedbackSuggestionService.class);
    MatchCalibrationSignalService calibrationSignalService = mock(MatchCalibrationSignalService.class);
    InterviewFeedbackStructurerTaskService taskService = mock(InterviewFeedbackStructurerTaskService.class);
    InterviewFeedbackOutcomeLoopService service = new InterviewFeedbackOutcomeLoopService(
        feedbackService,
        suggestionService,
        calibrationSignalService,
        taskService);
    InterviewFeedbackStructurerOutput output = new InterviewFeedbackStructurerOutput(
        "Client feedback is positive and should be reviewed by the consultant.",
        "strong_fit",
        null,
        "medium",
        List.of(),
        List.of("client_feedback_text"),
        Map.of("source", "unit-test"));
    when(taskService.execute(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new InterviewFeedbackStructurerResult(execution(output), output, output.suggestions()));
    when(feedbackService.updateFeedback(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(suggestionService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(calibrationSignalService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

    InterviewFeedbackOutcomeLoopResult result = service.processSubmission(
        ORGANIZATION_ID,
        ACTOR_ID,
        shortlist(),
        card(),
        job(),
        interaction(),
        feedback(),
        "{}");

    assertThat(result.aiStructured())
        .as(result.feedback().metadata())
        .isTrue();
    assertThat(result.feedback().metadata())
        .contains("\"outcomeLabel\":\"strong_fit\"")
        .contains("\"rejectReasonTaxonomy\":null");
    assertThat(result.suggestions()).hasSize(1);
    assertThat(result.suggestions().get(0).rejectReasonTaxonomy()).isNull();
    assertThat(result.calibrationSignals()).hasSize(1);
    assertThat(result.calibrationSignals().get(0).rejectReasonTaxonomy()).isNull();
  }

  private static AITaskExecutionResult execution(InterviewFeedbackStructurerOutput output) {
    AITaskRunId runId = new AITaskRunId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
    return new AITaskExecutionResult(
        new AITaskRunRecord(
            runId,
            ORGANIZATION_ID,
            "interview-feedback-structurer",
            "interview-feedback-structurer.v1",
            "interview-feedback-structurer-input.v1",
            "interview-feedback-structurer-output.v1",
            "interview-feedback-structurer.v1",
            new ModelRef("deterministic", "deterministic-pilot-local", null),
            AITaskRunStatus.SUCCEEDED,
            "pending",
            new WriteBackTarget("interview_feedback_suggestion"),
            new ActorRef(ACTOR_ID, ActorRole.CLIENT),
            new WorkflowCorrelationId(FEEDBACK_ID.value()),
            new WorkflowCausationId(FEEDBACK_ID.value()),
            new EntityRef("interview_feedback", FEEDBACK_ID.value()),
            List.of(FEEDBACK_ID.value(), INTERACTION_ID.value()),
            NOW,
            NOW,
            null,
            NOW),
        OBJECT_MAPPER.valueToTree(output),
        Duration.ZERO);
  }

  private static Shortlist shortlist() {
    return Shortlist.builder()
        .shortlistId(SHORTLIST_ID)
        .organizationId(ORGANIZATION_ID)
        .jobId(JOB_ID)
        .title("Pilot shortlist")
        .status(ShortlistStatus.INTERVIEWING)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static ShortlistCandidateCard card() {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(CARD_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .anonymousCandidateCardId(UUID.fromString("99999999-9999-9999-9999-999999999999"))
        .candidateId(CANDIDATE_ID)
        .candidateProfileId(UUID.fromString("12121212-1212-1212-1212-121212121212"))
        .status(ShortlistCandidateCardStatus.UNLOCKED)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static Job job() {
    return Job.builder()
        .jobId(JOB_ID)
        .organizationId(ORGANIZATION_ID)
        .companyId(COMPANY_ID)
        .title("ASIC Verification Lead")
        .status(JobStatus.INTERVIEWING)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static CandidateCompanyInteraction interaction() {
    return CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(INTERACTION_ID)
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .companyId(COMPANY_ID)
        .jobId(JOB_ID)
        .interactionType(InteractionType.INTERVIEW)
        .status(InteractionStatus.ACTIVE)
        .startedAt(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static InterviewFeedback feedback() {
    return InterviewFeedback.builder()
        .interviewFeedbackId(FEEDBACK_ID)
        .organizationId(ORGANIZATION_ID)
        .candidateCompanyInteractionId(INTERACTION_ID)
        .jobId(JOB_ID)
        .outcome(InterviewOutcome.YES)
        .decision(InterviewFeedbackDecision.HOLD)
        .notes("Strong technical evidence.")
        .submittedByRole("client")
        .submittedByUserId(ACTOR_ID)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }
}
