package com.recruitingtransactionos.coreapi.aitaskrunner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigPort;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AITaskModelRouterTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000370001");
  private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000370002");

  @Test
  void routeForUsesSameOrganizationGovernanceOverrideBeforeStaticProperties() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "candidate-profile-parser",
        "static-provider",
        "static-model");
    InMemoryGovernanceConfigPort configPort = new InMemoryGovernanceConfigPort();
    configPort.record = record(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "admin-provider",
                "model": "admin-model"
              }
            }
            """);
    AITaskModelRouter router = new AITaskModelRouter(
        properties,
        new GovernanceConfigService(configPort, new ObjectMapper()));

    AITaskModelRoute route = router.routeFor(ORGANIZATION_ID, "candidate-profile-parser");

    assertThat(route.providerKey()).isEqualTo("admin-provider");
    assertThat(route.modelName()).isEqualTo("admin-model");
    assertThat(configPort.lastOrganizationId).isEqualTo(ORGANIZATION_ID);
  }

  @Test
  void routeForDoesNotApplyAnotherOrganizationsGovernanceOverride() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "candidate-profile-parser",
        "static-provider",
        "static-model");
    InMemoryGovernanceConfigPort configPort = new InMemoryGovernanceConfigPort();
    configPort.record = record(
        OTHER_ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "other-provider",
                "model": "other-model"
              }
            }
            """);
    AITaskModelRouter router = new AITaskModelRouter(
        properties,
        new GovernanceConfigService(configPort, new ObjectMapper()));

    AITaskModelRoute route = router.routeFor(ORGANIZATION_ID, "candidate-profile-parser");

    assertThat(route.providerKey()).isEqualTo("static-provider");
    assertThat(route.modelName()).isEqualTo("static-model");
  }

  @Test
  void routeForIgnoresDisabledGovernanceOverride() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "candidate-profile-parser",
        "static-provider",
        "static-model");
    InMemoryGovernanceConfigPort configPort = new InMemoryGovernanceConfigPort();
    configPort.record = record(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "disabled-provider",
                "model": "disabled-model"
              }
            }
            """,
        false);
    AITaskModelRouter router = new AITaskModelRouter(
        properties,
        new GovernanceConfigService(configPort, new ObjectMapper()));

    AITaskModelRoute route = router.routeFor(ORGANIZATION_ID, "candidate-profile-parser");

    assertThat(route.providerKey()).isEqualTo("static-provider");
    assertThat(route.modelName()).isEqualTo("static-model");
  }

  @Test
  void routeForIgnoresGovernanceOverrideWithUnknownProvider() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "candidate-profile-parser",
        "deepseek",
        "deepseek-v4-pro");
    InMemoryGovernanceConfigPort configPort = new InMemoryGovernanceConfigPort();
    configPort.record = record(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "unknown-provider",
                "model": "unknown-model"
              }
            }
            """);
    AITaskModelRouter router = new AITaskModelRouter(
        properties,
        new GovernanceConfigService(configPort, new ObjectMapper()),
        Set.of("deepseek"));

    AITaskModelRoute route = router.routeFor(ORGANIZATION_ID, "candidate-profile-parser");

    assertThat(route.providerKey()).isEqualTo("deepseek");
    assertThat(route.modelName()).isEqualTo("deepseek-v4-pro");
  }

  @Test
  void routeForIgnoresGovernanceOverrideWithUnconfiguredModelForRegisteredProvider() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "candidate-profile-parser",
        "deepseek",
        "deepseek-v4-pro");
    InMemoryGovernanceConfigPort configPort = new InMemoryGovernanceConfigPort();
    configPort.record = record(
        ORGANIZATION_ID,
        "model-routing",
        "default",
        """
            {
              "candidate-profile-parser": {
                "provider": "deepseek",
                "model": "nonexistent-model"
              }
            }
            """);
    AITaskModelRouter router = new AITaskModelRouter(
        properties,
        new GovernanceConfigService(configPort, new ObjectMapper()),
        Set.of("deepseek"));

    AITaskModelRoute route = router.routeFor(ORGANIZATION_ID, "candidate-profile-parser");

    assertThat(route.providerKey()).isEqualTo("deepseek");
    assertThat(route.modelName()).isEqualTo("deepseek-v4-pro");
  }

  @Test
  void routeForUsesDefaultProductionRouteWhenTaskSpecificRouteIsAbsent() {
    AITaskRunnerProperties properties = propertiesWithRoute(
        "default",
        "deepseek",
        "deepseek-v4-pro");
    AITaskModelRouter router = new AITaskModelRouter(properties);

    AITaskModelRoute route = router.routeFor("source-classifier");

    assertThat(route.providerKey()).isEqualTo("deepseek");
    assertThat(route.modelName()).isEqualTo("deepseek-v4-pro");
  }

  private static AITaskRunnerProperties propertiesWithRoute(String taskKey, String provider, String model) {
    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route route = new AITaskRunnerProperties.Route();
    route.setProvider(provider);
    route.setModel(model);
    properties.getRoutes().put(taskKey, route);
    return properties;
  }

  private static GovernanceConfigRecord record(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson) {
    return record(organizationId, configType, configKey, payloadJson, true);
  }

  private static GovernanceConfigRecord record(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson,
      boolean enabled) {
    Instant now = Instant.parse("2026-05-07T00:00:00Z");
    return new GovernanceConfigRecord(
        UUID.fromString("00000000-0000-0000-0000-000000370099"),
        organizationId,
        configType,
        configKey,
        payloadJson,
        enabled,
        null,
        null,
        now,
        now,
        1);
  }

  private static final class InMemoryGovernanceConfigPort implements GovernanceConfigPort {
    private GovernanceConfigRecord record;
    private UUID lastOrganizationId;

    @Override
    public Optional<GovernanceConfigRecord> findByTypeAndKey(
        UUID organizationId,
        String configType,
        String configKey) {
      lastOrganizationId = organizationId;
      if (record == null
          || !record.organizationId().equals(organizationId)
          || !record.configType().equals(configType)
          || !record.configKey().equals(configKey)) {
        return Optional.empty();
      }
      return Optional.of(record);
    }

    @Override
    public List<GovernanceConfigRecord> listByType(UUID organizationId, String configType) {
      return List.of();
    }

    @Override
    public GovernanceConfigRecord upsert(
        UUID organizationId,
        String configType,
        String configKey,
        String payloadJson,
        boolean enabled,
        UUID actorUserId,
        PortalRole actorRole) {
      throw new UnsupportedOperationException("not needed");
    }
  }
}
