package com.recruitingtransactionos.coreapi.interviewfeedback;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MatchCalibrationSignal(
    MatchCalibrationSignalId matchCalibrationSignalId,
    UUID organizationId,
    InterviewFeedbackId interviewFeedbackId,
    CandidateCompanyInteractionId candidateCompanyInteractionId,
    JobId jobId,
    CandidateId candidateId,
    String industryPackKey,
    InterviewFeedbackDecision decision,
    InterviewOutcomeLabel outcomeLabel,
    RejectReasonTaxonomy rejectReasonTaxonomy,
    String confidence,
    String metadata,
    Instant createdAt,
    int version) {

  public MatchCalibrationSignal {
    Objects.requireNonNull(matchCalibrationSignalId, "matchCalibrationSignalId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interviewFeedbackId, "interviewFeedbackId must not be null");
    Objects.requireNonNull(candidateCompanyInteractionId, "candidateCompanyInteractionId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }
}
