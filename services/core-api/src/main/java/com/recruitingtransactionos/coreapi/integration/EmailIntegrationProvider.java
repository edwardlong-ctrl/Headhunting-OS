package com.recruitingtransactionos.coreapi.integration;

public interface EmailIntegrationProvider {
  IntegrationProviderResult send(OutboundIntegrationCommand command);
}
