package com.recruitingtransactionos.coreapi.supportops;

import java.util.Objects;

final class NoOpSupportOperationsTransactionBoundary
    implements SupportOperationsTransactionBoundary {

  @Override
  public <T> T run(Work<T> work) {
    Objects.requireNonNull(work, "work must not be null");
    try {
      return work.execute();
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "checked support operations transaction failure",
          exception);
    }
  }
}
