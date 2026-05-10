package com.recruitingtransactionos.coreapi.integration;

public final class NoOpSmsIntegrationProvider implements SmsIntegrationProvider {
  @Override
  public IntegrationProviderResult send(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured("noop_sms", "sms_provider_not_configured");
  }
}
