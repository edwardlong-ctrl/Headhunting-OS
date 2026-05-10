package com.recruitingtransactionos.coreapi.integration;

import java.util.UUID;

interface IntegrationAuditRecorder {
  UUID recordOutbound(OutboundIntegrationCommand command, IntegrationProviderResult providerResult);
}
