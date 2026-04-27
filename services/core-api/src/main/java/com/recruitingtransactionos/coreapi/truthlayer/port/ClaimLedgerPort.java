package com.recruitingtransactionos.coreapi.truthlayer.port;

public interface ClaimLedgerPort {

  ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command);
}
