package com.recruitingtransactionos.coreapi.integration;

interface EmailIntegrationProvider {
  IntegrationProviderResult send(OutboundIntegrationCommand command);
}

interface SmsIntegrationProvider {
  IntegrationProviderResult send(OutboundIntegrationCommand command);
}

interface CalendarIntegrationProvider {
  IntegrationProviderResult createEvent(OutboundIntegrationCommand command);
}

interface OcrSttIntegrationProvider {
  IntegrationProviderResult requestProcessing(OcrSttProcessingRequest request);
}

interface AtsHrisIntegrationProvider {
  IntegrationProviderResult validateMapping(AtsHrisMappingCommand command);
}

interface WebhookEventProvider {
  IntegrationProviderResult dispatch(WebhookDispatchCommand command);
}
