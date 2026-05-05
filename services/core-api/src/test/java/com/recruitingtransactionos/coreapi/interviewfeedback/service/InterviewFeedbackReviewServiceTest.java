package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionScope;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionStatus;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionType;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewFeedbackReviewServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-00000035f001");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-00000035f002");
  private static final InterviewFeedbackSuggestionId SUGGESTION_ID =
      new InterviewFeedbackSuggestionId(UUID.fromString("00000000-0000-0000-0000-00000035f003"));

  @Mock
  private InterviewFeedbackSuggestionService suggestionService;

  @Mock
  private com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService
      interactionService;

  @Mock
  private WorkflowTransitionAuditService workflowTransitionAuditService;

  @Test
  void review_rejectsAlreadyReviewedSuggestion() {
    InterviewFeedbackReviewService service = new InterviewFeedbackReviewService(
        suggestionService,
        interactionService,
        workflowTransitionAuditService);
    when(suggestionService.findByIdAndOrganizationId(ORGANIZATION_ID, SUGGESTION_ID))
        .thenReturn(Optional.of(suggestion(InterviewFeedbackSuggestionStatus.APPROVED)));

    assertThatThrownBy(() -> service.review(
        ORGANIZATION_ID,
        ACTOR_ID,
        SUGGESTION_ID,
        "approve",
        "Do not allow second review"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("interview_feedback_suggestion_already_reviewed");

    verify(suggestionService, never()).update(org.mockito.ArgumentMatchers.any());
    verify(interactionService, never()).findInteractionByIdAndOrganizationId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(workflowTransitionAuditService, never()).record(org.mockito.ArgumentMatchers.any());
  }

  private InterviewFeedbackSuggestion suggestion(InterviewFeedbackSuggestionStatus status) {
    return new InterviewFeedbackSuggestion(
        SUGGESTION_ID,
        ORGANIZATION_ID,
        new InterviewFeedbackId(UUID.fromString("00000000-0000-0000-0000-00000035f004")),
        new CandidateCompanyInteractionId(UUID.fromString("00000000-0000-0000-0000-00000035f005")),
        new JobId(UUID.fromString("00000000-0000-0000-0000-00000035f006")),
        new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000035f007")),
        UUID.fromString("00000000-0000-0000-0000-00000035f008"),
        InterviewFeedbackSuggestionScope.INTERACTION,
        InterviewFeedbackSuggestionType.OUTCOME_LABEL,
        status,
        null,
        null,
        "Interview outcome recommendation",
        "Structured review rationale",
        "{\"outcomeLabel\":\"hold\"}",
        null,
        status == InterviewFeedbackSuggestionStatus.PENDING_REVIEW ? null : Instant.parse("2026-05-05T01:00:00Z"),
        Instant.parse("2026-05-05T00:00:00Z"),
        Instant.parse("2026-05-05T00:00:00Z"),
        1);
  }
}
