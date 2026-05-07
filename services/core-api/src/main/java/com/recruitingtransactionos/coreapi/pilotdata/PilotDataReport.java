package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.List;
import java.util.Map;

public record PilotDataReport(
    String command,
    boolean valid,
    Map<String, Integer> counts,
    Map<String, Boolean> privacyChecks,
    Map<String, Boolean> workflowAuditChecks,
    Map<String, Boolean> seededAccountChecks,
    List<String> failedGateReasons,
    List<PilotDataValidationResult.Issue> issues) {

  public PilotDataReport {
    counts = Map.copyOf(counts == null ? Map.of() : counts);
    privacyChecks = Map.copyOf(privacyChecks == null ? Map.of() : privacyChecks);
    workflowAuditChecks = Map.copyOf(workflowAuditChecks == null ? Map.of() : workflowAuditChecks);
    seededAccountChecks = Map.copyOf(seededAccountChecks == null ? Map.of() : seededAccountChecks);
    failedGateReasons = List.copyOf(failedGateReasons == null ? List.of() : failedGateReasons);
    issues = List.copyOf(issues == null ? List.of() : issues);
  }

  public PilotDataReport(
      String command,
      boolean valid,
      Map<String, Integer> counts,
      List<PilotDataValidationResult.Issue> issues) {
    this(command, valid, counts, Map.of(), Map.of(), Map.of(), List.of(), issues);
  }

  public PilotDataReport withCommand(String replacementCommand) {
    return new PilotDataReport(
        replacementCommand,
        valid,
        counts,
        privacyChecks,
        workflowAuditChecks,
        seededAccountChecks,
        failedGateReasons,
        issues);
  }
}
