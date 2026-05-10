package com.recruitingtransactionos.coreapi.integration;

import java.util.Objects;
import java.util.UUID;

public final class AuditedOutboundIntegrationService {

  private final IntegrationAuditRecorder auditRecorder;
  private final EmailIntegrationProvider emailProvider;
  private final SmsIntegrationProvider smsProvider;
  private final CalendarIntegrationProvider calendarProvider;
  private final WebhookEventProvider webhookProvider;

  public AuditedOutboundIntegrationService(IntegrationAuditRecorder auditRecorder) {
    this(
        auditRecorder,
        new NoOpEmailIntegrationProvider(),
        new NoOpSmsIntegrationProvider(),
        new NoOpCalendarIntegrationProvider(),
        new NoOpWebhookEventProvider());
  }

  private AuditedOutboundIntegrationService(
      IntegrationAuditRecorder auditRecorder,
      EmailIntegrationProvider emailProvider,
      SmsIntegrationProvider smsProvider,
      CalendarIntegrationProvider calendarProvider,
      WebhookEventProvider webhookProvider) {
    this.auditRecorder = Objects.requireNonNull(auditRecorder, "auditRecorder must not be null");
    this.emailProvider = Objects.requireNonNull(emailProvider, "emailProvider must not be null");
    this.smsProvider = Objects.requireNonNull(smsProvider, "smsProvider must not be null");
    this.calendarProvider = Objects.requireNonNull(
        calendarProvider, "calendarProvider must not be null");
    this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider must not be null");
  }

  static AuditedOutboundIntegrationService withProviders(
      IntegrationAuditRecorder auditRecorder,
      EmailIntegrationProvider emailProvider,
      SmsIntegrationProvider smsProvider,
      CalendarIntegrationProvider calendarProvider,
      WebhookEventProvider webhookProvider) {
    return new AuditedOutboundIntegrationService(
        auditRecorder,
        emailProvider,
        smsProvider,
        calendarProvider,
        webhookProvider);
  }

  public OutboundIntegrationResult send(OutboundIntegrationCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    if (!command.organizationId().equals(command.actorOrganizationId())
        || !command.organizationId().equals(command.target().organizationId())) {
      return auditedBlocked(
          command,
          OutboundIntegrationStatus.BLOCKED_CROSS_ORG,
          "cross_org_outbound_blocked");
    }
    if (command.hasRawSensitivePayload()
        && (command.redactionDecision() != RedactionDecision.DISCLOSURE_UNLOCK_CONFIRMED
            || command.disclosureState() != DisclosureState.DISCLOSED)) {
      return auditedBlocked(
          command,
          OutboundIntegrationStatus.BLOCKED_SENSITIVE_DATA,
          "raw_candidate_fields_blocked_before_disclosure");
    }

    IntegrationProviderResult providerResult = providerResult(command);
    UUID auditId = auditRecorder.recordOutbound(command, providerResult);
    return new OutboundIntegrationResult(
        outboundStatus(providerResult),
        providerResult.status(),
        providerResult.providerKey(),
        providerResult.safeStatusCode(),
        auditId,
        command.hasRawSensitivePayload());
  }

  private OutboundIntegrationResult auditedBlocked(
      OutboundIntegrationCommand command,
      OutboundIntegrationStatus status,
      String safeStatusCode) {
    IntegrationProviderResult providerResult =
        IntegrationProviderResult.failedClosed("integration_boundary", safeStatusCode);
    UUID auditId = auditRecorder.recordOutbound(command, providerResult);
    return new OutboundIntegrationResult(
        status,
        providerResult.status(),
        providerResult.providerKey(),
        providerResult.safeStatusCode(),
        auditId,
        false);
  }

  private IntegrationProviderResult providerResult(OutboundIntegrationCommand command) {
    return switch (command.channel()) {
      case EMAIL -> emailProvider.send(command);
      case SMS -> smsProvider.send(command);
      case CALENDAR -> calendarProvider.createEvent(command);
      case WEBHOOK_EVENT -> webhookProvider.dispatch(WebhookDispatchCommand.fromOutbound(command));
      case WECHAT -> IntegrationProviderResult.unconfigured(
          "wechat_safe_summary_placeholder", "wechat_provider_not_configured");
      case OCR_STT, ATS_HRIS -> IntegrationProviderResult.failedClosed(
          "integration_boundary", "outbound_channel_not_supported");
    };
  }

  private static OutboundIntegrationStatus outboundStatus(IntegrationProviderResult result) {
    return switch (result.status()) {
      case ACCEPTED, DELIVERED -> OutboundIntegrationStatus.AUDITED_PROVIDER_ACCEPTED;
      case UNCONFIGURED -> OutboundIntegrationStatus.AUDITED_PROVIDER_UNCONFIGURED;
      case PRODUCTION_PLACEHOLDER -> OutboundIntegrationStatus.AUDITED_PROVIDER_PLACEHOLDER;
      case FAILED_CLOSED, REJECTED -> OutboundIntegrationStatus.BLOCKED_SENSITIVE_DATA;
    };
  }
}
