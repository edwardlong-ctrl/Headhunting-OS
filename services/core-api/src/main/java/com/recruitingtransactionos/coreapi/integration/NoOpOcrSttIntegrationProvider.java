package com.recruitingtransactionos.coreapi.integration;

public final class NoOpOcrSttIntegrationProvider implements OcrSttIntegrationProvider {
  @Override
  public IntegrationProviderResult requestProcessing(OcrSttProcessingRequest request) {
    return IntegrationProviderResult.placeholder(
        "noop_ocr_stt", "ocr_stt_worker_not_configured");
  }
}
