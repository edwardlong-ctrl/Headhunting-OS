package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClientUnlockRequest(
    ClientUnlockRequestId clientUnlockRequestId,
    UUID workflowEntityId,
    UUID organizationId,
    ShortlistId shortlistId,
    ShortlistCandidateCardId shortlistCandidateCardId,
    UUID jobId,
    UUID clientActorId,
    String anonymousCandidateCardRef,
    String requestReason,
    ClientUnlockRequestStatus status,
    String unlockDecisionRef,
    String approvedDisclosureRecordRef,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public ClientUnlockRequest {
    Objects.requireNonNull(clientUnlockRequestId, "clientUnlockRequestId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    workflowEntityId = workflowEntityId == null
        ? ConsentDisclosureWorkflowEntityIds.unlockRequestEntityId(
            organizationId,
            clientUnlockRequestId.value().toString())
        : workflowEntityId;
    Objects.requireNonNull(workflowEntityId, "workflowEntityId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Objects.requireNonNull(shortlistCandidateCardId, "shortlistCandidateCardId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(clientActorId, "clientActorId must not be null");
    anonymousCandidateCardRef = requireNonBlank(anonymousCandidateCardRef, "anonymousCandidateCardRef");
    requestReason = requireNonBlank(requestReason, "requestReason");
    Objects.requireNonNull(status, "status must not be null");
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
    private ClientUnlockRequestId clientUnlockRequestId;
    private UUID workflowEntityId;
    private UUID organizationId;
    private ShortlistId shortlistId;
    private ShortlistCandidateCardId shortlistCandidateCardId;
    private UUID jobId;
    private UUID clientActorId;
    private String anonymousCandidateCardRef;
    private String requestReason;
    private ClientUnlockRequestStatus status;
    private String unlockDecisionRef;
    private String approvedDisclosureRecordRef;
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {}

    public Builder clientUnlockRequestId(ClientUnlockRequestId clientUnlockRequestId) {
      this.clientUnlockRequestId = clientUnlockRequestId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder workflowEntityId(UUID workflowEntityId) {
      this.workflowEntityId = workflowEntityId;
      return this;
    }

    public Builder shortlistId(ShortlistId shortlistId) {
      this.shortlistId = shortlistId;
      return this;
    }

    public Builder shortlistCandidateCardId(ShortlistCandidateCardId shortlistCandidateCardId) {
      this.shortlistCandidateCardId = shortlistCandidateCardId;
      return this;
    }

    public Builder jobId(UUID jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder clientActorId(UUID clientActorId) {
      this.clientActorId = clientActorId;
      return this;
    }

    public Builder anonymousCandidateCardRef(String anonymousCandidateCardRef) {
      this.anonymousCandidateCardRef = anonymousCandidateCardRef;
      return this;
    }

    public Builder requestReason(String requestReason) {
      this.requestReason = requestReason;
      return this;
    }

    public Builder status(ClientUnlockRequestStatus status) {
      this.status = status;
      return this;
    }

    public Builder unlockDecisionRef(String unlockDecisionRef) {
      this.unlockDecisionRef = unlockDecisionRef;
      return this;
    }

    public Builder approvedDisclosureRecordRef(String approvedDisclosureRecordRef) {
      this.approvedDisclosureRecordRef = approvedDisclosureRecordRef;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder version(int version) {
      this.version = version;
      return this;
    }

    public ClientUnlockRequest build() {
      return new ClientUnlockRequest(
          clientUnlockRequestId,
          workflowEntityId,
          organizationId,
          shortlistId,
          shortlistCandidateCardId,
          jobId,
          clientActorId,
          anonymousCandidateCardRef,
          requestReason,
          status,
          unlockDecisionRef,
          approvedDisclosureRecordRef,
          createdAt,
          updatedAt,
          version);
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
