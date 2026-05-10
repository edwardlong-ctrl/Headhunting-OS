package com.recruitingtransactionos.coreapi.integration;

public final class NoOpCalendarIntegrationProvider implements CalendarIntegrationProvider {
  @Override
  public IntegrationProviderResult createEvent(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured(
        "noop_calendar", "calendar_provider_not_configured");
  }
}
