package com.recruitingtransactionos.coreapi.supportops;

import java.util.Objects;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class SpringSupportOperationsTransactionBoundary
    implements SupportOperationsTransactionBoundary {

  private final TransactionTemplate transactionTemplate;

  public SpringSupportOperationsTransactionBoundary(
      PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "transactionManager must not be null");
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public <T> T run(Work<T> work) {
    Objects.requireNonNull(work, "work must not be null");
    return transactionTemplate.execute(status -> {
      try {
        return work.execute();
      } catch (RuntimeException | Error exception) {
        throw exception;
      } catch (Exception exception) {
        status.setRollbackOnly();
        throw new IllegalStateException(
            "checked support operations transaction failure",
            exception);
      }
    });
  }
}
