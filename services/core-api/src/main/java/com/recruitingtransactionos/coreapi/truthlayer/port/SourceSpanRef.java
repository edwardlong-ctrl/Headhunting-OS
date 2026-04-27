package com.recruitingtransactionos.coreapi.truthlayer.port;

public record SourceSpanRef(String value) {

  public SourceSpanRef {
    value = PortContractGuards.requireNonBlank(value, "sourceSpanRef");
  }
}
