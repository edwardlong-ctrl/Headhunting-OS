package com.recruitingtransactionos.coreapi.truthlayer.service;

import java.util.Objects;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring-backed canonical write transaction boundary.
 *
 * <p>TransactionTemplate keeps Spring's default PROPAGATION_REQUIRED behavior: join an existing
 * transaction when one is present, otherwise open one for the canonical write callback.
 */
public final class SpringCanonicalWriteTransactionBoundary
    implements CanonicalWriteTransactionBoundary {

  private final TransactionTemplate transactionTemplate;

  public SpringCanonicalWriteTransactionBoundary(
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
        throw new CanonicalWriteTransactionException(
            "checked canonical write transaction failure", exception);
      }
    });
  }
}
