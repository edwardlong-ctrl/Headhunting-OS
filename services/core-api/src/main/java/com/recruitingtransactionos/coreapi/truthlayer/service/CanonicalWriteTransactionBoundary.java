package com.recruitingtransactionos.coreapi.truthlayer.service;

import java.util.Objects;

/**
 * Canonical write transaction boundary for the service-owned canonical write seam.
 *
 * <p>The boundary is intentionally scoped to canonical-write service orchestration. It coordinates
 * future multi-step canonical writes and future canonical writes from a governed boundary without
 * putting gate, profile, workflow, or persistence business logic in the boundary itself.
 */
@FunctionalInterface
public interface CanonicalWriteTransactionBoundary {

  <T> T run(Work<T> work);

  static CanonicalWriteTransactionBoundary immediate() {
    return new CanonicalWriteTransactionBoundary() {
      @Override
      public <T> T run(Work<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        try {
          return work.execute();
        } catch (RuntimeException | Error exception) {
          throw exception;
        } catch (Exception exception) {
          throw new CanonicalWriteTransactionException(
              "checked canonical write transaction failure", exception);
        }
      }
    };
  }

  @FunctionalInterface
  interface Work<T> {
    T execute() throws Exception;
  }
}
