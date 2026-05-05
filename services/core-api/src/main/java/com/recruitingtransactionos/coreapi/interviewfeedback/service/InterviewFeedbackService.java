package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackPersistencePort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InterviewFeedbackService {

  private final InterviewFeedbackPersistencePort feedbackPort;

  public InterviewFeedbackService(InterviewFeedbackPersistencePort feedbackPort) {
    this.feedbackPort = Objects.requireNonNull(feedbackPort, "feedbackPort must not be null");
  }

  public InterviewFeedback createFeedback(InterviewFeedback feedback) {
    Objects.requireNonNull(feedback, "feedback must not be null");
    return feedbackPort.create(feedback);
  }

  public InterviewFeedback updateFeedback(InterviewFeedback feedback) {
    Objects.requireNonNull(feedback, "feedback must not be null");
    return feedbackPort.update(feedback);
  }

  public Optional<InterviewFeedback> findFeedbackByIdAndOrganizationId(
      UUID organizationId, InterviewFeedbackId feedbackId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(feedbackId, "feedbackId must not be null");
    return feedbackPort.findByIdAndOrganizationId(organizationId, feedbackId);
  }

  public List<InterviewFeedback> findFeedbackByInteractionIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    return feedbackPort.findByInteractionIdAndOrganizationId(organizationId, interactionId);
  }

  public List<InterviewFeedback> findFeedbackByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    return feedbackPort.findByJobIdAndOrganizationId(organizationId, jobId);
  }
}
