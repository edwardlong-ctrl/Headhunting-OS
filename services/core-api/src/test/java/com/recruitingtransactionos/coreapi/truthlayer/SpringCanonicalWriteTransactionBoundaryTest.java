package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteResult;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionException;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class SpringCanonicalWriteTransactionBoundaryTest {

  @Test
  void successfulCallbackCommitsAndPreservesResult() {
    RecordingPlatformTransactionManager transactionManager =
        new RecordingPlatformTransactionManager();
    SpringCanonicalWriteTransactionBoundary boundary =
        new SpringCanonicalWriteTransactionBoundary(transactionManager);
    CanonicalWriteResult expected = canonicalResult();

    CanonicalWriteResult actual = boundary.run(() -> expected);

    assertThat(actual).isSameAs(expected);
    assertThat(transactionManager.getTransactions).isEqualTo(1);
    assertThat(transactionManager.commits).isEqualTo(1);
    assertThat(transactionManager.rollbacks).isZero();
    assertThat(transactionManager.lastDefinition.getPropagationBehavior())
        .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Test
  void runtimeFailureRollsBackAndPropagatesOriginalFailure() {
    RecordingPlatformTransactionManager transactionManager =
        new RecordingPlatformTransactionManager();
    SpringCanonicalWriteTransactionBoundary boundary =
        new SpringCanonicalWriteTransactionBoundary(transactionManager);
    IllegalStateException failure = new IllegalStateException("runtime domain failure");

    assertThatThrownBy(() -> boundary.run(() -> {
      throw failure;
    })).isSameAs(failure);

    assertThat(transactionManager.commits).isZero();
    assertThat(transactionManager.rollbacks).isEqualTo(1);
  }

  @Test
  void checkedFailureRollsBackAndIsExplicitlyWrapped() {
    RecordingPlatformTransactionManager transactionManager =
        new RecordingPlatformTransactionManager();
    SpringCanonicalWriteTransactionBoundary boundary =
        new SpringCanonicalWriteTransactionBoundary(transactionManager);
    CheckedCanonicalWriteFailure failure =
        new CheckedCanonicalWriteFailure("checked write failure");

    assertThatThrownBy(() -> boundary.run(() -> {
      throw failure;
    }))
        .isInstanceOf(CanonicalWriteTransactionException.class)
        .hasCause(failure)
        .hasMessageContaining("checked canonical write transaction failure");

    assertThat(transactionManager.commits).isZero();
    assertThat(transactionManager.rollbacks).isEqualTo(1);
  }

  @Test
  void transactionBoundaryHasNoCanonicalBusinessLogic() throws IOException {
    String source = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/"
            + "SpringCanonicalWriteTransactionBoundary.java");

    assertThat(source)
        .contains("PlatformTransactionManager")
        .contains("TransactionTemplate")
        .doesNotContain("CanonicalWriteGate")
        .doesNotContain("WorkflowEventService")
        .doesNotContain("CandidateProfile")
        .doesNotContain("ClaimLedger")
        .doesNotContain("ReviewEvent")
        .doesNotContain("INSERT INTO")
        .doesNotContain("UPDATE ");
  }

  private static CanonicalWriteResult canonicalResult() {
    return new CanonicalWriteResult(
        new CanonicalWriteDecision(
            CanonicalWriteDecisionType.ALLOW,
            List.of("unit_transaction_boundary_result")),
        false,
        null,
        false,
        "unit-test-no-canonical-persistence",
        null);
  }

  private static String sourceFile(String relativePath) throws IOException {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return Files.readString(direct);
    }
    return Files.readString(userDir.resolve("services/core-api").resolve(relativePath));
  }

  private static final class RecordingPlatformTransactionManager
      implements PlatformTransactionManager {
    private int getTransactions;
    private int commits;
    private int rollbacks;
    private TransactionDefinition lastDefinition;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      getTransactions++;
      lastDefinition = definition;
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
      commits++;
    }

    @Override
    public void rollback(TransactionStatus status) {
      rollbacks++;
    }
  }

  private static final class CheckedCanonicalWriteFailure extends Exception {
    private CheckedCanonicalWriteFailure(String message) {
      super(message);
    }
  }
}
