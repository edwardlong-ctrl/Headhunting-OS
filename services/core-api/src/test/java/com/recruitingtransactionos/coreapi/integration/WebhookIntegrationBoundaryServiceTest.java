package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookIntegrationBoundaryServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000490301");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000490302");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490303");

  @Test
  void inboundWebhookRequiresOrgScopedRouteAndSchemaCheckedJsonObject() {
    RecordingInboundSink sink = new RecordingInboundSink();
    WebhookIntegrationBoundaryService service = new WebhookIntegrationBoundaryService(
        new InboundIntegrationBoundaryService(sink));

    WebhookInboundResult crossOrg = service.acceptInbound(webhookCommand(
        ORG_A,
        ORG_B,
        "candidate.email.received",
        "v1",
        "{\"body\":\"hello\"}",
        "idem-1"));
    WebhookInboundResult invalidSchema = service.acceptInbound(webhookCommand(
        ORG_A,
        ORG_A,
        "candidate.email.received",
        "v9",
        "{\"body\":\"hello\"}",
        "idem-2"));
    WebhookInboundResult invalidPayload = service.acceptInbound(webhookCommand(
        ORG_A,
        ORG_A,
        "candidate.email.received",
        "v1",
        "[\"not-object\"]",
        "idem-3"));

    assertThat(crossOrg.status()).isEqualTo(WebhookInboundStatus.BLOCKED_CROSS_ORG);
    assertThat(invalidSchema.status()).isEqualTo(WebhookInboundStatus.REJECTED_SCHEMA_INVALID);
    assertThat(invalidPayload.status()).isEqualTo(WebhookInboundStatus.REJECTED_SCHEMA_INVALID);
    assertThat(sink.commands).isEmpty();
  }

  @Test
  void inboundWebhookUsesProvidedIdempotencyKeyOrDeterministicEventKeyAndRoutesToReview() {
    RecordingInboundSink sink = new RecordingInboundSink();
    WebhookIntegrationBoundaryService service = new WebhookIntegrationBoundaryService(
        new InboundIntegrationBoundaryService(sink));

    WebhookInboundResult withProvidedKey = service.acceptInbound(webhookCommand(
        ORG_A,
        ORG_A,
        "candidate.email.received",
        "v1",
        "{\"body\":\"hello\"}",
        "provided-key"));
    WebhookInboundResult withDeterministicKey = service.acceptInbound(webhookCommand(
        ORG_A,
        ORG_A,
        "candidate.email.received",
        "v1",
        "{\"body\":\"hello\"}",
        null));

    assertThat(withProvidedKey.status()).isEqualTo(WebhookInboundStatus.ACCEPTED_FOR_REVIEW);
    assertThat(withProvidedKey.eventKey()).isEqualTo("provided-key");
    assertThat(withDeterministicKey.status()).isEqualTo(WebhookInboundStatus.ACCEPTED_FOR_REVIEW);
    assertThat(withDeterministicKey.eventKey()).startsWith("deterministic:");
    assertThat(sink.commands).hasSize(2);
    assertThat(sink.commands).allSatisfy(command ->
        assertThat(command.purpose()).isEqualTo(InboundIntegrationPurpose.SOURCE_INTAKE));
  }

  private static WebhookInboundCommand webhookCommand(
      UUID routeOrganizationId,
      UUID eventOrganizationId,
      String eventType,
      String schemaVersion,
      String payloadJson,
      String idempotencyKey) {
    return new WebhookInboundCommand(
        routeOrganizationId,
        eventOrganizationId,
        ACTOR_ID,
        "partner-webhook",
        eventType,
        schemaVersion,
        payloadJson,
        idempotencyKey);
  }

  private static final class RecordingInboundSink implements InboundIntegrationSink {
    private final List<InboundIntegrationCommand> commands = new ArrayList<>();

    @Override
    public InboundIntakeReceipt acceptForReview(InboundIntegrationCommand command) {
      commands.add(command);
      return new InboundIntakeReceipt(UUID.randomUUID(), UUID.randomUUID());
    }
  }
}
