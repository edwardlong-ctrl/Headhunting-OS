package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AITaskModelRouter {

  private final Map<String, AITaskRunnerProperties.Route> routes;
  private final GovernanceConfigService governanceConfigService;
  private final Set<String> providerAllowlist;
  private final Set<String> configuredProviderModelPairs;

  public AITaskModelRouter(AITaskRunnerProperties properties) {
    this(properties, null);
  }

  public AITaskModelRouter(
      AITaskRunnerProperties properties,
      GovernanceConfigService governanceConfigService) {
    this(properties, governanceConfigService, Set.of());
  }

  public AITaskModelRouter(
      AITaskRunnerProperties properties,
      GovernanceConfigService governanceConfigService,
      Set<String> providerAllowlist) {
    Objects.requireNonNull(properties, "properties must not be null");
    this.routes = Map.copyOf(properties.getRoutes());
    this.governanceConfigService = governanceConfigService;
    this.providerAllowlist = Set.copyOf(
        Objects.requireNonNull(providerAllowlist, "providerAllowlist must not be null"));
    this.configuredProviderModelPairs = this.routes.values().stream()
        .map(route -> providerModelKey(route.getProvider(), route.getModel()))
        .collect(Collectors.toUnmodifiableSet());
  }

  public AITaskModelRoute routeFor(String taskKey) {
    return routeFor(null, taskKey);
  }

  public AITaskModelRoute routeFor(UUID organizationId, String taskKey) {
    if (governanceConfigService != null && organizationId != null) {
      JsonNode override = governanceConfigService.loadPayload(organizationId, "model-routing", "default")
          .path(taskKey);
      if (override.isObject()) {
        String provider = override.path("provider").asText(null);
        String model = override.path("model").asText(null);
        if (provider != null && !provider.isBlank()
            && model != null && !model.isBlank()
            && isAllowedRoute(provider, model)) {
          return new AITaskModelRoute(provider, model);
        }
      }
    }
    AITaskRunnerProperties.Route route = routes.get(taskKey);
    if (route == null) {
      throw new IllegalArgumentException("unknown_ai_task_route");
    }
    return new AITaskModelRoute(route.getProvider(), route.getModel());
  }

  private boolean isAllowedRoute(String provider, String model) {
    return providerAllowlist.isEmpty()
        || (providerAllowlist.contains(provider)
            && configuredProviderModelPairs.contains(providerModelKey(provider, model)));
  }

  private static String providerModelKey(String provider, String model) {
    return provider + "\u0000" + model;
  }
}
