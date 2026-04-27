package com.recruitingtransactionos.coreapi.truthlayer.service;

import java.util.Objects;

/**
 * Canonical write transaction boundary skeleton for the Task 3D/3E service seam.
 *
 * <p>This abstraction only marks where a future transaction-owned canonical write may run. The
 * default implementation executes work immediately and does not provide JDBC rollback coordination.
 */
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
