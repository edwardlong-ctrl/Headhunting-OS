package com.recruitingtransactionos.coreapi.integration;

public final class NoOpEmailIntegrationProvider implements EmailIntegrationProvider {
  @Override
  public IntegrationProviderResult send(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured("noop_email", "email_provider_not_configured");
  }
}
