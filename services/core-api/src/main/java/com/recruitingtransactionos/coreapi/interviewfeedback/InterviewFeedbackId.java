package com.recruitingtransactionos.coreapi.interviewfeedback;

import java.util.Objects;
import java.util.UUID;

public record InterviewFeedbackId(UUID value) {

  public InterviewFeedbackId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
