package com.recruitingtransactionos.coreapi.integration;

public final class NoOpWebhookEventProvider implements WebhookEventProvider {
  @Override
  public IntegrationProviderResult dispatch(WebhookDispatchCommand command) {
    return IntegrationProviderResult.unconfigured(
        "noop_webhook_event", "webhook_event_provider_not_configured");
  }
}
