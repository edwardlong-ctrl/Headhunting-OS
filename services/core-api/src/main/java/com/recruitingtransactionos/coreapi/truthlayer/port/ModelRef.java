package com.recruitingtransactionos.coreapi.truthlayer.port;

public record ModelRef(
    String provider,
    String name,
    String version) {

  public ModelRef {
    provider = PortContractGuards.requireNonBlank(provider, "modelProvider");
    name = PortContractGuards.requireNonBlank(name, "modelName");
    if (version != null) {
      version = PortContractGuards.requireNonBlank(version, "modelVersion");
    }
  }
}
