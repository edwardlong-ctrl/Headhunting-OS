package com.recruitingtransactionos.coreapi.integration;

public interface InboundIntegrationSink {
  InboundIntakeReceipt acceptForReview(InboundIntegrationCommand command);
}
