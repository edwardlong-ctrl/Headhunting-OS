package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

public interface IntegrationAuditRecorder {
  UUID recordOutbound(OutboundIntegrationCommand command, IntegrationProviderResult providerResult);
}
