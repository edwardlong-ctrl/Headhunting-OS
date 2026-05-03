package com.recruitingtransactionos.coreapi.privacyredaction;

import java.util.Objects;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring-backed transaction boundary for privacy redaction audit writes.
 */
public final class SpringRedactionAuditTransactionBoundary
    implements RedactionAuditTransactionBoundary {

  private final TransactionTemplate transactionTemplate;

  public SpringRedactionAuditTransactionBoundary(
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
            "checked privacy redaction transaction failure",
            exception);
      }
    });
  }
}
