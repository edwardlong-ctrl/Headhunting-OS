package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import java.util.Objects;

public final class ReviewEventService {

  private final ReviewEventPort reviewEventPort;

  public ReviewEventService(ReviewEventPort reviewEventPort) {
    this.reviewEventPort = Objects.requireNonNull(reviewEventPort,
        "reviewEventPort must not be null");
  }

  public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
    validate(command);
    return reviewEventPort.append(command);
  }

  private static void validate(ReviewEventAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.organizationId(), "organizationId must not be null");
    Objects.requireNonNull(command.reviewerId(), "reviewerId must not be null");
    Objects.requireNonNull(command.targetEntity(), "targetEntity must not be null");
    Objects.requireNonNull(command.riskTier(), "riskTier must not be null");
    Objects.requireNonNull(command.decision(), "decision must not be null");
    Objects.requireNonNull(command.reviewDuration(), "reviewDuration must not be null");
  }
}
