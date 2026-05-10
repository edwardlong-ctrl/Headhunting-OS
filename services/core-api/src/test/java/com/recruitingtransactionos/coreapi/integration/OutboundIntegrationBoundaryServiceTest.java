package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboundIntegrationBoundaryServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000490201");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000490202");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490203");

  @Test
  void outboundPayloadRequiresOrgActorAndReason() {
    assertThatThrownBy(() -> safeSummaryCommand(null, ACTOR_ID, ORG_A, ORG_A, "reason"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
    assertThatThrownBy(() -> safeSummaryCommand(ORG_A, null, ORG_A, ORG_A, "reason"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("actorId must not be null");
    assertThatThrownBy(() -> safeSummaryCommand(ORG_A, ACTOR_ID, ORG_A, ORG_A, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reason must not be blank");
  }

  @Test
  void safeSummaryEmailExportIsAuditedAndDoesNotSendRawSensitiveData() {
    RecordingAuditRecorder auditRecorder = new RecordingAuditRecorder();
    RecordingEmailProvider emailProvider = new RecordingEmailProvider();
    AuditedOutboundIntegrationService service = AuditedOutboundIntegrationService.withProviders(
        auditRecorder,
        emailProvider,
        new NoOpSmsIntegrationProvider(),
        new NoOpCalendarIntegrationProvider(),
        new NoOpWebhookEventProvider());

    OutboundIntegrationResult result = service.send(safeSummaryCommand(
        ORG_A,
        ACTOR_ID,
        ORG_A,
        ORG_A,
        "consultant_reviewed_shortlist_summary"));

    assertThat(result.status()).isEqualTo(OutboundIntegrationStatus.AUDITED_PROVIDER_ACCEPTED);
    assertThat(result.auditId()).isNotNull();
    assertThat(result.sensitiveDataSent()).isFalse();
    assertThat(emailProvider.commands).hasSize(1);
    assertThat(auditRecorder.records).singleElement().satisfies(record -> {
      assertThat(record.command().payloadKind()).isEqualTo(IntegrationPayloadKind.SAFE_SUMMARY_EXPORT);
      assertThat(record.providerResult().status()).isEqualTo(IntegrationProviderStatus.ACCEPTED);
      assertThat(record.command().rawSensitivePayload()).isNull();
    });
  }

  @Test
  void crossOrgOutboundAttemptFailsClosedAndDoesNotCallProvider() {
    RecordingAuditRecorder auditRecorder = new RecordingAuditRecorder();
    RecordingEmailProvider emailProvider = new RecordingEmailProvider();
    AuditedOutboundIntegrationService service = AuditedOutboundIntegrationService.withProviders(
        auditRecorder,
        emailProvider,
        new NoOpSmsIntegrationProvider(),
        new NoOpCalendarIntegrationProvider(),
        new NoOpWebhookEventProvider());

    OutboundIntegrationResult result = service.send(safeSummaryCommand(
        ORG_A,
        ACTOR_ID,
        ORG_B,
        ORG_A,
        "cross_org_attempt"));

    assertThat(result.status()).isEqualTo(OutboundIntegrationStatus.BLOCKED_CROSS_ORG);
    assertThat(emailProvider.commands).isEmpty();
    assertThat(auditRecorder.records).singleElement()
        .extracting(record -> record.providerResult().safeStatusCode())
        .isEqualTo("cross_org_outbound_blocked");
  }

  @Test
  void rawCandidateFieldsCannotBeSentBeforeDisclosureUnlock() {
    RecordingAuditRecorder auditRecorder = new RecordingAuditRecorder();
    RecordingEmailProvider emailProvider = new RecordingEmailProvider();
    AuditedOutboundIntegrationService service = AuditedOutboundIntegrationService.withProviders(
        auditRecorder,
        emailProvider,
        new NoOpSmsIntegrationProvider(),
        new NoOpCalendarIntegrationProvider(),
        new NoOpWebhookEventProvider());

    OutboundIntegrationCommand unsafe = new OutboundIntegrationCommand(
        ORG_A,
        ACTOR_ID,
        ORG_A,
        "candidate_identity_not_unlocked",
        IntegrationChannel.EMAIL,
        new OutboundIntegrationTarget("client@example.com", ORG_A),
        IntegrationPayloadKind.FOLLOW_UP_MESSAGE,
        "Candidate details",
        "Candidate detail payload",
        "{\"candidateName\":\"Jane Zhang\",\"email\":\"jane@example.com\",\"phone\":\"+86-10-5555\"}",
        RedactionDecision.SAFE_SUMMARY_ONLY,
        DisclosureState.NOT_DISCLOSED,
        "task49-unsafe-identity");

    OutboundIntegrationResult result = service.send(unsafe);

    assertThat(result.status()).isEqualTo(OutboundIntegrationStatus.BLOCKED_SENSITIVE_DATA);
    assertThat(result.sensitiveDataSent()).isFalse();
    assertThat(emailProvider.commands).isEmpty();
    assertThat(auditRecorder.records).singleElement()
        .extracting(record -> record.providerResult().safeStatusCode())
        .isEqualTo("raw_candidate_fields_blocked_before_disclosure");
  }

  private static OutboundIntegrationCommand safeSummaryCommand(
      UUID organizationId,
      UUID actorId,
      UUID targetOrganizationId,
      UUID actorOrganizationId,
      String reason) {
    return new OutboundIntegrationCommand(
        organizationId,
        actorId,
        actorOrganizationId,
        reason,
        IntegrationChannel.EMAIL,
        new OutboundIntegrationTarget("client@example.com", targetOrganizationId),
        IntegrationPayloadKind.SAFE_SUMMARY_EXPORT,
        "Shortlist summary",
        "Anonymous shortlist summary for client review.",
        null,
        RedactionDecision.SAFE_SUMMARY_ONLY,
        DisclosureState.NOT_DISCLOSED,
        "task49-safe-summary");
  }

  private static final class RecordingEmailProvider implements EmailIntegrationProvider {
    private final List<OutboundIntegrationCommand> commands = new ArrayList<>();

    @Override
    public IntegrationProviderResult send(OutboundIntegrationCommand command) {
      commands.add(command);
      return IntegrationProviderResult.accepted("test_email", "queued");
    }
  }

  private static final class RecordingAuditRecorder implements IntegrationAuditRecorder {
    private final List<OutboundIntegrationAuditRecord> records = new ArrayList<>();

    @Override
    public UUID recordOutbound(OutboundIntegrationCommand command, IntegrationProviderResult providerResult) {
      records.add(new OutboundIntegrationAuditRecord(command, providerResult));
      return UUID.fromString("00000000-0000-0000-0000-000000490299");
    }
  }

  private record OutboundIntegrationAuditRecord(
      OutboundIntegrationCommand command,
      IntegrationProviderResult providerResult) {}
}
