package com.recruitingtransactionos.coreapi.privacyredaction;

import java.util.Objects;

/**
 * Transaction boundary for privacy redaction audit orchestration.
 */
@FunctionalInterface
public interface RedactionAuditTransactionBoundary {

  <T> T run(Work<T> work);

  static RedactionAuditTransactionBoundary immediate() {
    return new RedactionAuditTransactionBoundary() {
      @Override
      public <T> T run(Work<T> work) {
        Objects.requireNonNull(work, "work must not be null");
        try {
          return work.execute();
        } catch (RuntimeException | Error exception) {
          throw exception;
        } catch (Exception exception) {
          throw new IllegalStateException(
              "checked privacy redaction transaction failure",
              exception);
        }
      }
    };
  }

  @FunctionalInterface
  interface Work<T> {
    T execute() throws Exception;
  }
}
