package com.recruitingtransactionos.coreapi.interviewfeedback;

import java.util.Objects;
import java.util.UUID;

public record MatchCalibrationSignalId(UUID value) {
  public MatchCalibrationSignalId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
