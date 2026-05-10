package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationProviderPlaceholderTest {

  @Test
  void providerPlaceholdersReportUnconfiguredOrFailClosedStatusHonestly() {
    OutboundIntegrationCommand outbound = IntegrationTestFixtures.safeSummaryEmailCommand();

    assertThat(new NoOpEmailIntegrationProvider().send(outbound).status())
        .isEqualTo(IntegrationProviderStatus.UNCONFIGURED);
    assertThat(new NoOpSmsIntegrationProvider().send(outbound).safeStatusCode())
        .isEqualTo("sms_provider_not_configured");
    assertThat(new NoOpCalendarIntegrationProvider().createEvent(outbound).status())
        .isEqualTo(IntegrationProviderStatus.UNCONFIGURED);
    assertThat(new NoOpWebhookEventProvider().dispatch(WebhookDispatchCommand.fromOutbound(outbound)).status())
        .isEqualTo(IntegrationProviderStatus.UNCONFIGURED);

    OcrSttProcessingRequest ocrRequest = new OcrSttProcessingRequest(
        IntegrationTestFixtures.ORG_A,
        IntegrationTestFixtures.ACTOR_ID,
        "ocr-source-1",
        "application/pdf",
        "s3://intake/ocr-source-1");
    assertThat(new NoOpOcrSttIntegrationProvider().requestProcessing(ocrRequest).status())
        .isEqualTo(IntegrationProviderStatus.PRODUCTION_PLACEHOLDER);
    assertThat(new NoOpOcrSttIntegrationProvider().requestProcessing(ocrRequest).safeStatusCode())
        .isEqualTo("ocr_stt_worker_not_configured");
  }

  @Test
  void atsHrisPlaceholderOnlyValidatesBoundaryMappingAndDoesNotPretendImportSucceeded() {
    AtsHrisMappingCommand command = new AtsHrisMappingCommand(
        IntegrationTestFixtures.ORG_A,
        IntegrationTestFixtures.ACTOR_ID,
        "legacy-ats",
        "candidate",
        Map.of("fullName", "candidate_name", "email", "source_email"),
        false);

    IntegrationProviderResult result = new NoOpAtsHrisIntegrationProvider().validateMapping(command);

    assertThat(result.status()).isEqualTo(IntegrationProviderStatus.PRODUCTION_PLACEHOLDER);
    assertThat(result.safeStatusCode()).isEqualTo("ats_hris_mapping_boundary_only");
  }

  private static final class IntegrationTestFixtures {
    private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000490001");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490002");

    private static OutboundIntegrationCommand safeSummaryEmailCommand() {
      return new OutboundIntegrationCommand(
          ORG_A,
          ACTOR_ID,
          ORG_A,
          "consultant_reviewed_shortlist_summary",
          IntegrationChannel.EMAIL,
          new OutboundIntegrationTarget("client@example.com", ORG_A),
          IntegrationPayloadKind.SAFE_SUMMARY_EXPORT,
          "Shortlist summary",
          "Anonymous shortlist summary for client review.",
          null,
          RedactionDecision.SAFE_SUMMARY_ONLY,
          DisclosureState.NOT_DISCLOSED,
          "task49-provider-placeholder");
    }
  }
}
