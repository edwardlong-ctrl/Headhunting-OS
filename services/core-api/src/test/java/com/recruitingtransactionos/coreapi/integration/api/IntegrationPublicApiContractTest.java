package com.recruitingtransactionos.coreapi.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.integration.AtsHrisIntegrationBoundaryService;
import com.recruitingtransactionos.coreapi.integration.AtsHrisIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.AtsHrisMappingCommand;
import com.recruitingtransactionos.coreapi.integration.AtsHrisMappingResult;
import com.recruitingtransactionos.coreapi.integration.AtsHrisMappingStatus;
import com.recruitingtransactionos.coreapi.integration.AuditedOutboundIntegrationService;
import com.recruitingtransactionos.coreapi.integration.CalendarIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.DisclosureState;
import com.recruitingtransactionos.coreapi.integration.EmailIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationBoundaryService;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationCommand;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationPurpose;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationResult;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationSink;
import com.recruitingtransactionos.coreapi.integration.InboundIntegrationStatus;
import com.recruitingtransactionos.coreapi.integration.InboundIntakeReceipt;
import com.recruitingtransactionos.coreapi.integration.IntegrationAuditRecorder;
import com.recruitingtransactionos.coreapi.integration.IntegrationChannel;
import com.recruitingtransactionos.coreapi.integration.IntegrationPayloadKind;
import com.recruitingtransactionos.coreapi.integration.IntegrationProviderResult;
import com.recruitingtransactionos.coreapi.integration.IntegrationProviderStatus;
import com.recruitingtransactionos.coreapi.integration.NoOpAtsHrisIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.NoOpCalendarIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.NoOpEmailIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.NoOpOcrSttIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.NoOpSmsIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.NoOpWebhookEventProvider;
import com.recruitingtransactionos.coreapi.integration.OcrSttIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.OcrSttProcessingRequest;
import com.recruitingtransactionos.coreapi.integration.OutboundIntegrationCommand;
import com.recruitingtransactionos.coreapi.integration.OutboundIntegrationResult;
import com.recruitingtransactionos.coreapi.integration.OutboundIntegrationStatus;
import com.recruitingtransactionos.coreapi.integration.OutboundIntegrationTarget;
import com.recruitingtransactionos.coreapi.integration.RedactionDecision;
import com.recruitingtransactionos.coreapi.integration.SmsIntegrationProvider;
import com.recruitingtransactionos.coreapi.integration.WebhookDispatchCommand;
import com.recruitingtransactionos.coreapi.integration.WebhookEventProvider;
import com.recruitingtransactionos.coreapi.integration.WebhookInboundCommand;
import com.recruitingtransactionos.coreapi.integration.WebhookInboundResult;
import com.recruitingtransactionos.coreapi.integration.WebhookInboundStatus;
import com.recruitingtransactionos.coreapi.integration.WebhookIntegrationBoundaryService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationPublicApiContractTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000490601");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490602");

  @Test
  void providerPortsCommandsResultsAndBoundaryMethodsAreUsableAcrossPackageBoundaries() {
    EmailIntegrationProvider emailProvider = command ->
        IntegrationProviderResult.accepted("contract_email", "queued");
    SmsIntegrationProvider smsProvider = new NoOpSmsIntegrationProvider();
    CalendarIntegrationProvider calendarProvider = new NoOpCalendarIntegrationProvider();
    WebhookEventProvider webhookProvider = new NoOpWebhookEventProvider();
    IntegrationAuditRecorder auditRecorder = (command, providerResult) ->
        UUID.fromString("00000000-0000-0000-0000-000000490699");
    AuditedOutboundIntegrationService outboundService = AuditedOutboundIntegrationService.withProviders(
        auditRecorder,
        emailProvider,
        smsProvider,
        calendarProvider,
        webhookProvider);

    OutboundIntegrationResult outboundResult = outboundService.send(new OutboundIntegrationCommand(
        ORG_ID,
        ACTOR_ID,
        ORG_ID,
        "contract_safe_summary",
        IntegrationChannel.EMAIL,
        new OutboundIntegrationTarget("client@example.test", ORG_ID),
        IntegrationPayloadKind.SAFE_SUMMARY_EXPORT,
        "Shortlist summary",
        "Anonymous shortlist summary.",
        null,
        RedactionDecision.SAFE_SUMMARY_ONLY,
        DisclosureState.NOT_DISCLOSED,
        "contract-idempotency-key"));

    assertThat(outboundResult.status()).isEqualTo(OutboundIntegrationStatus.AUDITED_PROVIDER_ACCEPTED);
    assertThat(outboundResult.providerStatus()).isEqualTo(IntegrationProviderStatus.ACCEPTED);

    InboundIntegrationSink sink = command -> new InboundIntakeReceipt(
        UUID.fromString("00000000-0000-0000-0000-000000490603"),
        UUID.fromString("00000000-0000-0000-0000-000000490604"));
    InboundIntegrationBoundaryService inboundService = new InboundIntegrationBoundaryService(sink);
    InboundIntegrationResult inboundResult = inboundService.acceptInbound(new InboundIntegrationCommand(
        ORG_ID,
        ACTOR_ID,
        ORG_ID,
        "contract-email",
        IntegrationChannel.EMAIL,
        "message-1",
        SourceItemType.EMAIL,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        "{\"body\":\"candidate supplied context\"}",
        "{\"provider\":\"contract-email\"}",
        InboundIntegrationPurpose.SOURCE_INTAKE));

    assertThat(inboundResult.status()).isEqualTo(InboundIntegrationStatus.ACCEPTED_FOR_REVIEW);

    WebhookIntegrationBoundaryService webhookService = new WebhookIntegrationBoundaryService(inboundService);
    WebhookInboundResult webhookResult = webhookService.acceptInbound(new WebhookInboundCommand(
        ORG_ID,
        ORG_ID,
        ACTOR_ID,
        "contract-webhook",
        "candidate.email.received",
        "v1",
        "{\"body\":\"hello\"}",
        "webhook-contract-key"));

    assertThat(webhookResult.status()).isEqualTo(WebhookInboundStatus.ACCEPTED_FOR_REVIEW);

    AtsHrisIntegrationProvider atsProvider = new NoOpAtsHrisIntegrationProvider();
    AtsHrisIntegrationBoundaryService atsService = new AtsHrisIntegrationBoundaryService(atsProvider);
    AtsHrisMappingResult atsResult = atsService.validateMapping(new AtsHrisMappingCommand(
        ORG_ID,
        ACTOR_ID,
        "legacy-ats",
        "candidate",
        Map.of("email", "source.email"),
        false));

    assertThat(atsResult.status()).isEqualTo(AtsHrisMappingStatus.ACCEPTED_FOR_REVIEW_MAPPING);

    OcrSttIntegrationProvider ocrSttProvider = new NoOpOcrSttIntegrationProvider();
    IntegrationProviderResult ocrResult = ocrSttProvider.requestProcessing(new OcrSttProcessingRequest(
        ORG_ID,
        ACTOR_ID,
        "source-1",
        "application/pdf",
        "s3://bucket/source-1"));
    assertThat(ocrResult.status()).isEqualTo(IntegrationProviderStatus.PRODUCTION_PLACEHOLDER);

    assertThat(new NoOpEmailIntegrationProvider().send(new OutboundIntegrationCommand(
        ORG_ID,
        ACTOR_ID,
        ORG_ID,
        "contract_email_placeholder",
        IntegrationChannel.EMAIL,
        new OutboundIntegrationTarget("client@example.test", ORG_ID),
        IntegrationPayloadKind.SAFE_SUMMARY_EXPORT,
        "Shortlist summary",
        "Anonymous shortlist summary.",
        null,
        RedactionDecision.SAFE_SUMMARY_ONLY,
        DisclosureState.NOT_DISCLOSED,
        "contract-placeholder-key")).status()).isEqualTo(IntegrationProviderStatus.UNCONFIGURED);
  }
}
