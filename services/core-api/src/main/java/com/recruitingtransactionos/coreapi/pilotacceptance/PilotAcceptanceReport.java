package com.recruitingtransactionos.coreapi.pilotacceptance;

import java.util.List;
import java.util.Objects;

public record PilotAcceptanceReport(
    String reportId,
    String title,
    PilotAcceptanceOverallStatus overallStatus,
    List<PilotAcceptanceRequirement> requirements) {

  public PilotAcceptanceReport {
    reportId = PilotAcceptanceRequirement.requireNonBlank(reportId, "reportId");
    title = PilotAcceptanceRequirement.requireNonBlank(title, "title");
    overallStatus = Objects.requireNonNull(overallStatus, "overallStatus");
    requirements = List.copyOf(requirements);
    if (requirements.isEmpty()) {
      throw new IllegalArgumentException("pilot_acceptance_report_requires_requirements");
    }
    PilotAcceptanceOverallStatus computedStatus = requirements.stream()
        .allMatch(PilotAcceptanceRequirement::passedWithEvidence)
            ? PilotAcceptanceOverallStatus.CONTROLLED_PILOT_READY
            : PilotAcceptanceOverallStatus.NOT_READY;
    if (overallStatus != computedStatus) {
      throw new IllegalArgumentException("pilot_acceptance_report_status_mismatch");
    }
  }

  public static PilotAcceptanceReport fromRequirements(
      String reportId,
      String title,
      List<PilotAcceptanceRequirement> requirements) {
    List<PilotAcceptanceRequirement> copied = List.copyOf(requirements);
    PilotAcceptanceOverallStatus status = copied.stream().allMatch(PilotAcceptanceRequirement::passedWithEvidence)
        ? PilotAcceptanceOverallStatus.CONTROLLED_PILOT_READY
        : PilotAcceptanceOverallStatus.NOT_READY;
    return new PilotAcceptanceReport(reportId, title, status, copied);
  }

  public boolean readyForControlledPilot() {
    return overallStatus == PilotAcceptanceOverallStatus.CONTROLLED_PILOT_READY;
  }

  public List<PilotAcceptanceRequirement> blockingRequirements() {
    return requirements.stream()
        .filter(requirement -> !requirement.passedWithEvidence())
        .toList();
  }
}
