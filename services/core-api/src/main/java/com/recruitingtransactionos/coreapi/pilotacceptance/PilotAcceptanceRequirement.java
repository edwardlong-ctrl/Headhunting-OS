package com.recruitingtransactionos.coreapi.pilotacceptance;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record PilotAcceptanceRequirement(
    String id,
    PilotAcceptanceCategory category,
    String title,
    PilotAcceptanceRequirementStatus status,
    Set<String> evidence,
    Set<String> blockers) {

  public PilotAcceptanceRequirement {
    id = requireNonBlank(id, "id");
    category = Objects.requireNonNull(category, "category");
    title = requireNonBlank(title, "title");
    status = Objects.requireNonNull(status, "status");
    evidence = requireNonBlankEntries(evidence, "evidence");
    blockers = requireNonBlankEntries(blockers, "blockers");
    switch (status) {
      case PASSED -> {
        if (evidence.isEmpty()) {
          throw new IllegalArgumentException("passed_pilot_acceptance_requirement_requires_evidence");
        }
        if (!blockers.isEmpty()) {
          throw new IllegalArgumentException("passed_pilot_acceptance_requirement_cannot_have_blockers");
        }
      }
      case PARTIAL -> {
        if (evidence.isEmpty()) {
          throw new IllegalArgumentException("partial_pilot_acceptance_requirement_requires_evidence");
        }
        if (blockers.isEmpty()) {
          throw new IllegalArgumentException("partial_pilot_acceptance_requirement_requires_blockers");
        }
      }
      case BLOCKED -> {
        if (!evidence.isEmpty()) {
          throw new IllegalArgumentException("blocked_pilot_acceptance_requirement_cannot_have_evidence");
        }
        if (blockers.isEmpty()) {
          throw new IllegalArgumentException("blocked_pilot_acceptance_requirement_requires_blockers");
        }
      }
    }
  }

  public static PilotAcceptanceRequirement passed(
      String id,
      PilotAcceptanceCategory category,
      String title,
      Set<String> evidence) {
    return new PilotAcceptanceRequirement(
        id,
        category,
        title,
        PilotAcceptanceRequirementStatus.PASSED,
        evidence,
        Set.of());
  }

  public static PilotAcceptanceRequirement partial(
      String id,
      PilotAcceptanceCategory category,
      String title,
      Set<String> evidence,
      Set<String> blockers) {
    return new PilotAcceptanceRequirement(
        id,
        category,
        title,
        PilotAcceptanceRequirementStatus.PARTIAL,
        evidence,
        blockers);
  }

  public static PilotAcceptanceRequirement blocked(
      String id,
      PilotAcceptanceCategory category,
      String title,
      Set<String> blockers) {
    return new PilotAcceptanceRequirement(
        id,
        category,
        title,
        PilotAcceptanceRequirementStatus.BLOCKED,
        Set.of(),
        blockers);
  }

  public boolean passedWithEvidence() {
    return status == PilotAcceptanceRequirementStatus.PASSED && !evidence.isEmpty();
  }

  static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "_required");
    }
    return value.strip();
  }

  private static Set<String> requireNonBlankEntries(Set<String> values, String fieldName) {
    return Objects.requireNonNull(values, fieldName).stream()
        .map(value -> requireNonBlank(value, fieldName + "_entry"))
        .collect(Collectors.toUnmodifiableSet());
  }
}
