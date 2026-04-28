package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public final class PermissionEvaluator {

  private final FieldAccessPolicy fieldAccessPolicy;

  public PermissionEvaluator() {
    this(new FieldAccessPolicy());
  }

  PermissionEvaluator(FieldAccessPolicy fieldAccessPolicy) {
    this.fieldAccessPolicy =
        Objects.requireNonNull(fieldAccessPolicy, "fieldAccessPolicy must not be null");
  }

  public AccessDecision evaluate(AccessRequest request) {
    return fieldAccessPolicy.decide(request);
  }
}
