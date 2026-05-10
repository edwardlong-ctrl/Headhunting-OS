package com.recruitingtransactionos.coreapi.integration;

final class NoOpEmailIntegrationProvider implements EmailIntegrationProvider {
  @Override
  public IntegrationProviderResult send(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured("noop_email", "email_provider_not_configured");
  }
}

final class NoOpSmsIntegrationProvider implements SmsIntegrationProvider {
  @Override
  public IntegrationProviderResult send(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured("noop_sms", "sms_provider_not_configured");
  }
}

final class NoOpCalendarIntegrationProvider implements CalendarIntegrationProvider {
  @Override
  public IntegrationProviderResult createEvent(OutboundIntegrationCommand command) {
    return IntegrationProviderResult.unconfigured(
        "noop_calendar", "calendar_provider_not_configured");
  }
}

final class NoOpOcrSttIntegrationProvider implements OcrSttIntegrationProvider {
  @Override
  public IntegrationProviderResult requestProcessing(OcrSttProcessingRequest request) {
    return IntegrationProviderResult.placeholder(
        "noop_ocr_stt", "ocr_stt_worker_not_configured");
  }
}

final class NoOpAtsHrisIntegrationProvider implements AtsHrisIntegrationProvider {
  @Override
  public IntegrationProviderResult validateMapping(AtsHrisMappingCommand command) {
    return IntegrationProviderResult.placeholder(
        "noop_ats_hris", "ats_hris_mapping_boundary_only");
  }
}

final class NoOpWebhookEventProvider implements WebhookEventProvider {
  @Override
  public IntegrationProviderResult dispatch(WebhookDispatchCommand command) {
    return IntegrationProviderResult.unconfigured(
        "noop_webhook_event", "webhook_event_provider_not_configured");
  }
}
