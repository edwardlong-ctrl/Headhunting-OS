package com.recruitingtransactionos.coreapi.interviewfeedback.port;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackPersistencePort {

  InterviewFeedback create(InterviewFeedback feedback);

  InterviewFeedback update(InterviewFeedback feedback);

  Optional<InterviewFeedback> findByIdAndOrganizationId(
      UUID organizationId, InterviewFeedbackId feedbackId);

  List<InterviewFeedback> findByInteractionIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId);

  List<InterviewFeedback> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId);
}
