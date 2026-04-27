package com.recruitingtransactionos.coreapi.truthlayer;

import java.util.Objects;
import java.util.Set;

public record WorkflowActionPolicy(
    WorkflowActionCode actionCode,
    Set<WorkflowEntityType> allowedEntityTypes,
    RiskTier riskTier,
    boolean beforeStateRequired,
    boolean afterStateRequired,
    boolean reasonRequired,
    boolean humanFinalActorRequired,
    boolean aiOnlyFinalizationForbidden,
    boolean stateTransition,
    String description) {

  public WorkflowActionPolicy {
    Objects.requireNonNull(actionCode, "actionCode must not be null");
    allowedEntityTypes = Set.copyOf(Objects.requireNonNull(allowedEntityTypes,
        "allowedEntityTypes must not be null"));
    if (allowedEntityTypes.isEmpty()) {
      throw new IllegalArgumentException("allowedEntityTypes must not be empty");
    }
    Objects.requireNonNull(riskTier, "riskTier must not be null");
    description = requireNonBlank(description, "description");
    if (stateTransition && (!beforeStateRequired || !afterStateRequired)) {
      throw new IllegalArgumentException(
          "state-transition actions must require before_state and after_state");
    }
    if (riskTier.requiresHumanFinalActor()
        && (!reasonRequired || !humanFinalActorRequired || !aiOnlyFinalizationForbidden)) {
      throw new IllegalArgumentException(
          "T3/T4 actions must require reason, human actor, and no AI-only finalization");
    }
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
