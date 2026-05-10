package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;

public final class InboundIntegrationBoundaryService {

  private final InboundIntegrationSink intakeSink;

  public InboundIntegrationBoundaryService(InboundIntegrationSink intakeSink) {
    this.intakeSink = Objects.requireNonNull(intakeSink, "intakeSink must not be null");
  }

  public InboundIntegrationResult acceptInbound(InboundIntegrationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    if (!command.organizationId().equals(command.actorOrganizationId())) {
      return new InboundIntegrationResult(
          InboundIntegrationStatus.BLOCKED_CROSS_ORG,
          null,
          null,
          false,
          "cross_org_inbound_blocked");
    }
    if (command.purpose() == InboundIntegrationPurpose.CONFIRMED_FACT_WRITE) {
      return new InboundIntegrationResult(
          InboundIntegrationStatus.BLOCKED_CONFIRMED_FACT_WRITE,
          null,
          null,
          false,
          "confirmed_fact_write_blocked");
    }
    InboundIntakeReceipt receipt = intakeSink.acceptForReview(command);
    return new InboundIntegrationResult(
        InboundIntegrationStatus.ACCEPTED_FOR_REVIEW,
        receipt.sourceItemId(),
        receipt.informationPacketId(),
        false,
        "accepted_for_review");
  }
}
