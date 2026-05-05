package com.recruitingtransactionos.coreapi.interviewfeedback;

import java.util.Objects;
import java.util.UUID;

public record InterviewFeedbackSuggestionId(UUID value) {
  public InterviewFeedbackSuggestionId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
