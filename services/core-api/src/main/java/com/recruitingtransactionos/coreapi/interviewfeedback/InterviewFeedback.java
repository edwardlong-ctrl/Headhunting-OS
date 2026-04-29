package com.recruitingtransactionos.coreapi.interviewfeedback;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InterviewFeedback(
    InterviewFeedbackId interviewFeedbackId,
    UUID organizationId,
    CandidateCompanyInteractionId candidateCompanyInteractionId,
    JobId jobId,
    String interviewerName,
    String interviewerRole,
    Integer interviewRound,
    Instant interviewDate,
    InterviewOutcome outcome,
    String ratings,
    String strengths,
    String concerns,
    String notes,
    String submittedByRole,
    UUID submittedByUserId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public InterviewFeedback {
    Objects.requireNonNull(interviewFeedbackId, "interviewFeedbackId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateCompanyInteractionId,
        "candidateCompanyInteractionId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(outcome, "outcome must not be null");
    Objects.requireNonNull(submittedByRole, "submittedByRole must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private InterviewFeedbackId interviewFeedbackId;
    private UUID organizationId;
    private CandidateCompanyInteractionId candidateCompanyInteractionId;
    private JobId jobId;
    private String interviewerName;
    private String interviewerRole;
    private Integer interviewRound;
    private Instant interviewDate;
    private InterviewOutcome outcome;
    private String ratings = "{}";
    private String strengths;
    private String concerns;
    private String notes;
    private String submittedByRole;
    private UUID submittedByUserId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder interviewFeedbackId(InterviewFeedbackId id) {
      this.interviewFeedbackId = id; return this;
    }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder candidateCompanyInteractionId(CandidateCompanyInteractionId id) {
      this.candidateCompanyInteractionId = id; return this;
    }
    public Builder jobId(JobId id) { this.jobId = id; return this; }
    public Builder interviewerName(String name) { this.interviewerName = name; return this; }
    public Builder interviewerRole(String role) { this.interviewerRole = role; return this; }
    public Builder interviewRound(Integer round) { this.interviewRound = round; return this; }
    public Builder interviewDate(Instant date) { this.interviewDate = date; return this; }
    public Builder outcome(InterviewOutcome outcome) { this.outcome = outcome; return this; }
    public Builder ratings(String ratings) { this.ratings = ratings; return this; }
    public Builder strengths(String strengths) { this.strengths = strengths; return this; }
    public Builder concerns(String concerns) { this.concerns = concerns; return this; }
    public Builder notes(String notes) { this.notes = notes; return this; }
    public Builder submittedByRole(String role) { this.submittedByRole = role; return this; }
    public Builder submittedByUserId(UUID id) { this.submittedByUserId = id; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public InterviewFeedback build() {
      return new InterviewFeedback(interviewFeedbackId, organizationId,
          candidateCompanyInteractionId, jobId, interviewerName, interviewerRole,
          interviewRound, interviewDate, outcome, ratings, strengths, concerns, notes,
          submittedByRole, submittedByUserId, metadata, createdAt, updatedAt, version);
    }
  }
}
