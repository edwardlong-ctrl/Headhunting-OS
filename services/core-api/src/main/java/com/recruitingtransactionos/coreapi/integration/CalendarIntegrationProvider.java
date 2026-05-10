package com.recruitingtransactionos.coreapi.integration;

public interface CalendarIntegrationProvider {
  IntegrationProviderResult createEvent(OutboundIntegrationCommand command);
}
