package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import java.util.Objects;

public final class ClaimLedgerService {

  private final ClaimLedgerPort claimLedgerPort;

  public ClaimLedgerService(ClaimLedgerPort claimLedgerPort) {
    this.claimLedgerPort = Objects.requireNonNull(claimLedgerPort,
        "claimLedgerPort must not be null");
  }

  public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return claimLedgerPort.append(command);
  }
}
