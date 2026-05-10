package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

enum InboundIntegrationPurpose {
  SOURCE_INTAKE,
  ATS_HRIS_IMPORT_BASELINE,
  CONFIRMED_FACT_WRITE
}

enum InboundIntegrationStatus {
  ACCEPTED_FOR_REVIEW,
  BLOCKED_CONFIRMED_FACT_WRITE,
  BLOCKED_CROSS_ORG
}

record InboundIntakeReceipt(UUID sourceItemId, UUID informationPacketId) {}

record InboundIntegrationResult(
    InboundIntegrationStatus status,
    UUID sourceItemId,
    UUID informationPacketId,
    boolean confirmedFactWritten,
    String safeStatusCode) {}

interface InboundIntegrationSink {
  InboundIntakeReceipt acceptForReview(InboundIntegrationCommand command);
}
