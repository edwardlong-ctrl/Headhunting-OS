package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Objects;

public record ClaimLedgerAppendResult(ClaimId claimId) {

  public ClaimLedgerAppendResult {
    Objects.requireNonNull(claimId, "claimId must not be null");
  }
}
