package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.List;
import java.util.Map;

public record PilotDataReport(
    String command,
    boolean valid,
    Map<String, Integer> counts,
    List<PilotDataValidationResult.Issue> issues) {

  public PilotDataReport {
    counts = Map.copyOf(counts == null ? Map.of() : counts);
    issues = List.copyOf(issues == null ? List.of() : issues);
  }

  public PilotDataReport withCommand(String replacementCommand) {
    return new PilotDataReport(replacementCommand, valid, counts, issues);
  }
}
