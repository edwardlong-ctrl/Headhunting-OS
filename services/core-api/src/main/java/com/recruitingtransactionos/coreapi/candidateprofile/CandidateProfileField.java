package com.recruitingtransactionos.coreapi.candidateprofile;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateProfileField(
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
    String notes) {

  public CandidateProfileField {
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(fieldStatus, "fieldStatus must not be null");
    Objects.requireNonNull(lineage, "lineage must not be null");
    if (!lineage.hasAnyReference()) {
      throw new IllegalArgumentException("lineage must contain at least one source reference");
    }
    notes = CandidateProfileGuards.optionalNonBlank(notes, "notes");
    requireStatusInvariants(
        fieldPath,
        fieldStatus,
        lineage,
        conflict,
        confirmedByActorId,
        confirmedAgainstProfileVersion);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static void requireStatusInvariants(
      CandidateProfileFieldPath fieldPath,
      CandidateProfileFieldStatus fieldStatus,
      CandidateProfileFieldLineage lineage,
      CandidateProfileFieldConflict conflict,
      UUID confirmedByActorId,
      CandidateProfileVersion confirmedAgainstProfileVersion) {
    if (fieldStatus == CandidateProfileFieldStatus.CONFLICTING
        && (conflict == null || !conflict.hasMultipleSourceBackedValues())) {
      throw new IllegalArgumentException(
          "conflicting field requires source-backed conflict metadata");
    }
    if (conflict != null && !conflict.fieldPath().equals(fieldPath)) {
      throw new IllegalArgumentException("conflict fieldPath must match fieldPath");
    }
    if (fieldStatus == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED
        && (confirmedByActorId == null || confirmedAgainstProfileVersion == null)) {
      throw new IllegalArgumentException(
          "candidate_confirmed field requires confirmedByActorId and confirmedAgainstProfileVersion");
    }
    if (fieldStatus == CandidateProfileFieldStatus.CONSULTANT_ATTESTED
        && confirmedByActorId == null) {
      throw new IllegalArgumentException(
          "consultant_attested field requires confirmedByActorId");
    }
    if (fieldStatus == CandidateProfileFieldStatus.EXTERNAL_VERIFIED
        && !lineage.hasReferenceType(CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE)) {
      throw new IllegalArgumentException(
          "external_verified field requires EXTERNAL_EVIDENCE lineage reference");
    }
  }

  public static final class Builder {
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

    private Builder() {
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

    public CandidateProfileField build() {
      return new CandidateProfileField(
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
          notes);
    }
  }
}
