package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.List;

public record PilotDataValidationResult(boolean valid, List<Issue> issues) {

  public PilotDataValidationResult {
    issues = List.copyOf(issues == null ? List.of() : issues);
  }

  public record Issue(String code, String message, String ref) {
  }
}
