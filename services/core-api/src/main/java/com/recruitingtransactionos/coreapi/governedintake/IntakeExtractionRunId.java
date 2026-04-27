package com.recruitingtransactionos.coreapi.governedintake;

import java.util.Objects;
import java.util.UUID;

public record IntakeExtractionRunId(UUID value) {

  public IntakeExtractionRunId {
    Objects.requireNonNull(value, "extractionRunId must not be null");
  }
}
