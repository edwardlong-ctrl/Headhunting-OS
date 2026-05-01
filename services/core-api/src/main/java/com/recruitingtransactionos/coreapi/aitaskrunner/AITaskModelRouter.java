package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.Map;
import java.util.Objects;

public final class AITaskModelRouter {

  private final Map<String, AITaskRunnerProperties.Route> routes;

  public AITaskModelRouter(AITaskRunnerProperties properties) {
    Objects.requireNonNull(properties, "properties must not be null");
    this.routes = Map.copyOf(properties.getRoutes());
  }

  public AITaskModelRoute routeFor(String taskKey) {
    AITaskRunnerProperties.Route route = routes.get(taskKey);
    if (route == null) {
      throw new IllegalArgumentException("unknown_ai_task_route");
    }
    return new AITaskModelRoute(route.getProvider(), route.getModel());
  }
}
