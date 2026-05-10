package com.recruitingtransactionos.coreapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtsHrisIntegrationBoundaryTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000490401");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000490402");

  @Test
  void atsHrisMappingContractRejectsConfirmedFactWriteTargets() {
    AtsHrisIntegrationBoundaryService service =
        new AtsHrisIntegrationBoundaryService(new NoOpAtsHrisIntegrationProvider());

    AtsHrisMappingResult result = service.validateMapping(new AtsHrisMappingCommand(
        ORG_A,
        ACTOR_ID,
        "legacy-ats",
        "candidate",
        Map.of("email", "canonical.candidate.email"),
        true));

    assertThat(result.status()).isEqualTo(AtsHrisMappingStatus.BLOCKED_CONFIRMED_FACT_WRITE);
    assertThat(result.providerStatus()).isNull();
    assertThat(result.safeStatusCode()).isEqualTo("ats_hris_confirmed_fact_write_blocked");
  }

  @Test
  void atsHrisMappingContractAllowsOnlyReviewFirstMappingBaseline() {
    AtsHrisIntegrationBoundaryService service =
        new AtsHrisIntegrationBoundaryService(new NoOpAtsHrisIntegrationProvider());

    AtsHrisMappingResult result = service.validateMapping(new AtsHrisMappingCommand(
        ORG_A,
        ACTOR_ID,
        "legacy-ats",
        "candidate",
        Map.of("email", "source.email", "note", "packet.note"),
        false));

    assertThat(result.status()).isEqualTo(AtsHrisMappingStatus.ACCEPTED_FOR_REVIEW_MAPPING);
    assertThat(result.providerStatus()).isEqualTo(IntegrationProviderStatus.PRODUCTION_PLACEHOLDER);
    assertThat(result.safeStatusCode()).isEqualTo("ats_hris_mapping_boundary_only");
  }
}
