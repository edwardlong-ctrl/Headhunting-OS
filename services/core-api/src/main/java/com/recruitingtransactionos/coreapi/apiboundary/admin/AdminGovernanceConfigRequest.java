package com.recruitingtransactionos.coreapi.apiboundary.admin;

public record AdminGovernanceConfigRequest(
    String payloadJson,
    Boolean enabled) {}
