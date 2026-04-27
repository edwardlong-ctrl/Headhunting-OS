package com.recruitingtransactionos.coreapi.truthlayer.port;

public record WriteBackTarget(String value) {

  public WriteBackTarget {
    value = PortContractGuards.requireNonBlank(value, "writeBackTarget");
  }
}
