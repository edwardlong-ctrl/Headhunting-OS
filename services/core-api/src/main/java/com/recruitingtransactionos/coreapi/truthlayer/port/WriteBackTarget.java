package com.recruitingtransactionos.coreapi.truthlayer.port;

public record WriteBackTarget(String value) {

  public static WriteBackTarget of(AITaskWriteBackTarget target) {
    return new WriteBackTarget(target.wireValue());
  }

  public WriteBackTarget {
    value = PortContractGuards.requireNonBlank(value, "writeBackTarget");
  }
}
