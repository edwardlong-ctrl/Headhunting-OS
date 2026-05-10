package com.recruitingtransactionos.coreapi.integration;

public interface WebhookEventProvider {
  IntegrationProviderResult dispatch(WebhookDispatchCommand command);
}
