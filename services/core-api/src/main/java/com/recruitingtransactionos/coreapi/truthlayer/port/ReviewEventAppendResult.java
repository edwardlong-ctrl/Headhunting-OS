package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record ReviewEventAppendResult(ReviewEventId reviewEventId) {

  public ReviewEventAppendResult {
    Objects.requireNonNull(reviewEventId, "reviewEventId must not be null");
  }
}
