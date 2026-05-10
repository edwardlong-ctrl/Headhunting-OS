package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;
import java.util.UUID;

public record OutboundIntegrationCommand(
    UUID organizationId,
    UUID actorId,
    UUID actorOrganizationId,
    String reason,
    IntegrationChannel channel,
    OutboundIntegrationTarget target,
    IntegrationPayloadKind payloadKind,
    String subject,
    String safeSummaryPayload,
    String rawSensitivePayload,
    RedactionDecision redactionDecision,
    DisclosureState disclosureState,
    String idempotencyKey) {

  public OutboundIntegrationCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(actorOrganizationId, "actorOrganizationId must not be null");
    reason = requireNonBlank(reason, "reason");
    Objects.requireNonNull(channel, "channel must not be null");
    Objects.requireNonNull(target, "target must not be null");
    Objects.requireNonNull(payloadKind, "payloadKind must not be null");
    subject = requireNonBlank(subject, "subject");
    safeSummaryPayload = requireNonBlank(safeSummaryPayload, "safeSummaryPayload");
    rawSensitivePayload = optionalNonBlank(rawSensitivePayload, "rawSensitivePayload");
    Objects.requireNonNull(redactionDecision, "redactionDecision must not be null");
    Objects.requireNonNull(disclosureState, "disclosureState must not be null");
    idempotencyKey = optionalNonBlank(idempotencyKey, "idempotencyKey");
  }

  boolean hasRawSensitivePayload() {
    return rawSensitivePayload != null;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
