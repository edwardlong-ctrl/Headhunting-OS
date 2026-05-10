package com.recruitingtransactionos.coreapi.integration;

public interface SmsIntegrationProvider {
  IntegrationProviderResult send(OutboundIntegrationCommand command);
}
