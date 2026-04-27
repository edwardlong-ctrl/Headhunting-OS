package com.recruitingtransactionos.coreapi.truthlayer.service;

import java.util.Objects;

@FunctionalInterface
public interface CanonicalWriteTransactionBoundary {

  CanonicalWriteResult run(Work work);

  static CanonicalWriteTransactionBoundary immediate() {
    return work -> {
      Objects.requireNonNull(work, "work must not be null");
      return work.execute();
    };
  }

  @FunctionalInterface
  interface Work {
    CanonicalWriteResult execute();
  }
}
