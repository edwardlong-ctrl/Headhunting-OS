package com.recruitingtransactionos.coreapi.integration;

public final class NoOpAtsHrisIntegrationProvider implements AtsHrisIntegrationProvider {
  @Override
  public IntegrationProviderResult validateMapping(AtsHrisMappingCommand command) {
    return IntegrationProviderResult.placeholder(
        "noop_ats_hris", "ats_hris_mapping_boundary_only");
  }
}
