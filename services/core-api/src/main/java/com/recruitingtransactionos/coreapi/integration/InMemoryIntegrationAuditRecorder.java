package com.recruitingtransactionos.coreapi.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class InMemoryIntegrationAuditRecorder implements IntegrationAuditRecorder {
  private final List<AuditRecord> records = new ArrayList<>();

  @Override
  public UUID recordOutbound(
      OutboundIntegrationCommand command,
      IntegrationProviderResult providerResult) {
    UUID auditId = UUID.randomUUID();
    records.add(new AuditRecord(auditId, command, providerResult));
    return auditId;
  }

  List<AuditRecord> records() {
    return List.copyOf(records);
  }

  record AuditRecord(
      UUID auditId,
      OutboundIntegrationCommand command,
      IntegrationProviderResult providerResult) {}
}
