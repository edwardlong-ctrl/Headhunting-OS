package com.recruitingtransactionos.coreapi.candidateprofile.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflict;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStaleness;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UpsertCandidateProfileFieldRequest(
    UUID organizationId,
    CandidateProfileId candidateProfileId,
    CandidateProfileFieldPath fieldPath,
    CandidateProfileFieldValue value,
    CandidateProfileFieldStatus fieldStatus,
    CandidateProfileFieldLineage lineage,
    CandidateProfileFieldConflict conflict,
    CandidateProfileFieldStaleness staleness,
    Instant lastReviewedAt,
    UUID confirmedByActorId,
    CandidateProfileVersion confirmedAgainstProfileVersion,
    ClaimId sourceClaimId,
    ReviewEventId sourceReviewEventId,
    WorkflowEventId sourceWorkflowEventId,
    String notes,
    boolean bulkApproval) {

  public UpsertCandidateProfileFieldRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(fieldStatus, "fieldStatus must not be null");
    Objects.requireNonNull(lineage, "lineage must not be null");
    if (bulkApproval
        && (fieldStatus == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED
            || fieldStatus == CandidateProfileFieldStatus.EXTERNAL_VERIFIED)) {
      throw new IllegalArgumentException(
          "bulk approval cannot persist candidate_confirmed or external_verified");
    }
  }

  public CandidateProfileField toCandidateProfileField() {
    return CandidateProfileField.builder()
        .fieldPath(fieldPath)
        .value(value)
        .fieldStatus(fieldStatus)
        .lineage(lineage)
        .conflict(conflict)
        .staleness(staleness)
        .lastReviewedAt(lastReviewedAt)
        .confirmedByActorId(confirmedByActorId)
        .confirmedAgainstProfileVersion(confirmedAgainstProfileVersion)
        .sourceClaimId(sourceClaimId)
        .sourceReviewEventId(sourceReviewEventId)
        .sourceWorkflowEventId(sourceWorkflowEventId)
        .notes(notes)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private CandidateProfileId candidateProfileId;
    private CandidateProfileFieldPath fieldPath;
    private CandidateProfileFieldValue value;
    private CandidateProfileFieldStatus fieldStatus;
    private CandidateProfileFieldLineage lineage;
    private CandidateProfileFieldConflict conflict;
    private CandidateProfileFieldStaleness staleness;
    private Instant lastReviewedAt;
    private UUID confirmedByActorId;
    private CandidateProfileVersion confirmedAgainstProfileVersion;
    private ClaimId sourceClaimId;
    private ReviewEventId sourceReviewEventId;
    private WorkflowEventId sourceWorkflowEventId;
    private String notes;
    private boolean bulkApproval;

    private Builder() {
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder candidateProfileId(CandidateProfileId candidateProfileId) {
      this.candidateProfileId = candidateProfileId;
      return this;
    }

    public Builder fieldPath(CandidateProfileFieldPath fieldPath) {
      this.fieldPath = fieldPath;
      return this;
    }

    public Builder value(CandidateProfileFieldValue value) {
      this.value = value;
      return this;
    }

    public Builder fieldStatus(CandidateProfileFieldStatus fieldStatus) {
      this.fieldStatus = fieldStatus;
      return this;
    }

    public Builder lineage(CandidateProfileFieldLineage lineage) {
      this.lineage = lineage;
      return this;
    }

    public Builder conflict(CandidateProfileFieldConflict conflict) {
      this.conflict = conflict;
      return this;
    }

    public Builder staleness(CandidateProfileFieldStaleness staleness) {
      this.staleness = staleness;
      return this;
    }

    public Builder lastReviewedAt(Instant lastReviewedAt) {
      this.lastReviewedAt = lastReviewedAt;
      return this;
    }

    public Builder confirmedByActorId(UUID confirmedByActorId) {
      this.confirmedByActorId = confirmedByActorId;
      return this;
    }

    public Builder confirmedAgainstProfileVersion(
        CandidateProfileVersion confirmedAgainstProfileVersion) {
      this.confirmedAgainstProfileVersion = confirmedAgainstProfileVersion;
      return this;
    }

    public Builder sourceClaimId(ClaimId sourceClaimId) {
      this.sourceClaimId = sourceClaimId;
      return this;
    }

    public Builder sourceReviewEventId(ReviewEventId sourceReviewEventId) {
      this.sourceReviewEventId = sourceReviewEventId;
      return this;
    }

    public Builder sourceWorkflowEventId(WorkflowEventId sourceWorkflowEventId) {
      this.sourceWorkflowEventId = sourceWorkflowEventId;
      return this;
    }

    public Builder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder bulkApproval(boolean bulkApproval) {
      this.bulkApproval = bulkApproval;
      return this;
    }

    public UpsertCandidateProfileFieldRequest build() {
      return new UpsertCandidateProfileFieldRequest(
          organizationId,
          candidateProfileId,
          fieldPath,
          value,
          fieldStatus,
          lineage,
          conflict,
          staleness,
          lastReviewedAt,
          confirmedByActorId,
          confirmedAgainstProfileVersion,
          sourceClaimId,
          sourceReviewEventId,
          sourceWorkflowEventId,
          notes,
          bulkApproval);
    }
  }
}
