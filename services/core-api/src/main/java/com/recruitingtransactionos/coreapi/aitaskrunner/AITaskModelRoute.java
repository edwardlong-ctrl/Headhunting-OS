package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.Objects;

public record AITaskModelRoute(String providerKey, String modelName) {

  public AITaskModelRoute {
    providerKey = requireNonBlank(providerKey, "providerKey");
    modelName = requireNonBlank(modelName, "modelName");
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
