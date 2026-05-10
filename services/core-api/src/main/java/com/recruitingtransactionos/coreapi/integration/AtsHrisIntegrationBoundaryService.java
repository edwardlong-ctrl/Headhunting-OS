package com.recruitingtransactionos.coreapi.integration;

import java.util.Locale;
import java.util.Objects;

public final class AtsHrisIntegrationBoundaryService {

  private final AtsHrisIntegrationProvider provider;

  public AtsHrisIntegrationBoundaryService(AtsHrisIntegrationProvider provider) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
  }

  AtsHrisMappingResult validateMapping(AtsHrisMappingCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    boolean canonicalTargetRequested = command.confirmedFactWriteRequested()
        || command.fieldMappings().values().stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .anyMatch(value -> value.startsWith("canonical.") || value.contains("confirmed_fact"));
    if (canonicalTargetRequested) {
      return new AtsHrisMappingResult(
          AtsHrisMappingStatus.BLOCKED_CONFIRMED_FACT_WRITE,
          null,
          "ats_hris_confirmed_fact_write_blocked");
    }
    IntegrationProviderResult result = provider.validateMapping(command);
    return new AtsHrisMappingResult(
        AtsHrisMappingStatus.ACCEPTED_FOR_REVIEW_MAPPING,
        result.status(),
        result.safeStatusCode());
  }
}
