package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConsentDisclosureServiceRequest(
    UUID organizationId,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String clientRef,
    String consentRecordRef,
    String unlockDecisionRef,
    String approvedDisclosureRecordRef,
    PortalRole requestedByRole,
    ActorRef actor,
    DisclosureLevel requestedLevel,
    ConsentDisclosurePrerequisites prerequisites,
    String reason,
    Instant requestedAt) {

  public ConsentDisclosureServiceRequest {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    candidateRef = requireNonBlank(candidateRef, "candidateRef");
    candidateProfileRef = requireNonBlank(candidateProfileRef, "candidateProfileRef");
    jobRef = requireNonBlank(jobRef, "jobRef");
    clientRef = requireNonBlank(clientRef, "clientRef");
    consentRecordRef = requireNonBlank(consentRecordRef, "consentRecordRef");
    unlockDecisionRef = requireNonBlank(unlockDecisionRef, "unlockDecisionRef");
    approvedDisclosureRecordRef =
        requireNonBlank(approvedDisclosureRecordRef, "approvedDisclosureRecordRef");
    Objects.requireNonNull(requestedByRole, "requestedByRole must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
    Objects.requireNonNull(requestedLevel, "requestedLevel must not be null");
    Objects.requireNonNull(prerequisites, "prerequisites must not be null");
    reason = requireNonBlank(reason, "reason");
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID organizationId;
    private String candidateRef;
    private String candidateProfileRef;
    private String jobRef;
    private String clientRef;
    private String consentRecordRef;
    private String unlockDecisionRef;
    private String approvedDisclosureRecordRef;
    private PortalRole requestedByRole;
    private ActorRef actor;
    private DisclosureLevel requestedLevel;
    private ConsentDisclosurePrerequisites prerequisites;
    private String reason;
    private Instant requestedAt;

    private Builder() {}

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder candidateRef(String candidateRef) {
      this.candidateRef = candidateRef;
      return this;
    }

    public Builder candidateProfileRef(String candidateProfileRef) {
      this.candidateProfileRef = candidateProfileRef;
      return this;
    }

    public Builder jobRef(String jobRef) {
      this.jobRef = jobRef;
      return this;
    }

    public Builder clientRef(String clientRef) {
      this.clientRef = clientRef;
      return this;
    }

    public Builder consentRecordRef(String consentRecordRef) {
      this.consentRecordRef = consentRecordRef;
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

    public Builder requestedByRole(PortalRole requestedByRole) {
      this.requestedByRole = requestedByRole;
      return this;
    }

    public Builder actor(ActorRef actor) {
      this.actor = actor;
      return this;
    }

    public Builder requestedLevel(DisclosureLevel requestedLevel) {
      this.requestedLevel = requestedLevel;
      return this;
    }

    public Builder prerequisites(ConsentDisclosurePrerequisites prerequisites) {
      this.prerequisites = prerequisites;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder requestedAt(Instant requestedAt) {
      this.requestedAt = requestedAt;
      return this;
    }

    public ConsentDisclosureServiceRequest build() {
      return new ConsentDisclosureServiceRequest(
          organizationId,
          candidateRef,
          candidateProfileRef,
          jobRef,
          clientRef,
          consentRecordRef,
          unlockDecisionRef,
          approvedDisclosureRecordRef,
          requestedByRole,
          actor,
          requestedLevel,
          prerequisites,
          reason,
          requestedAt);
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
