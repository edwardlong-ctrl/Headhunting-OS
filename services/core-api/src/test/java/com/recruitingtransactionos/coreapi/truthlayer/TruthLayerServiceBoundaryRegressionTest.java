package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteResult;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteReviewEvidence;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TruthLayerServiceBoundaryRegressionTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090002");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090003");
  private static final UUID CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090004");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090005");
  private static final UUID CORRELATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090006");
  private static final UUID CAUSATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000090007");
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T04:00:00Z");

  @Test
  void appendServicesExposeOnlyAppendBoundaries() {
    assertThat(publicDeclaredMethodNames(ClaimLedgerService.class)).containsExactly("append");
    assertThat(publicDeclaredMethodNames(ReviewEventService.class)).containsExactly("append");
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class)).containsExactly("append");

    assertThat(allDeclaredMethodNames(ClaimLedgerService.class))
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeCanonicalPersistenceApi);
    assertThat(allDeclaredMethodNames(ReviewEventService.class))
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeReviewPromotionApi);
    assertThat(allDeclaredMethodNames(WorkflowEventService.class))
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeWorkflowEngineApi);
  }

  @Test
  void canonicalWriteServiceExposesOnlyAttemptBoundary() {
    assertThat(publicDeclaredMethodNames(CanonicalWriteService.class)).containsExactly("attempt");
    assertThat(publicDeclaredMethodNames(CanonicalWriteService.class))
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeCandidatePersistenceApi);
  }

  @Test
  void canonicalWriteServiceCallsGateBeforeAppendingAllowedAudit() throws IOException {
    String source = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/"
            + "CanonicalWriteService.java");

    int gateDecision = source.indexOf("gate.decide");
    int workflowAppend = source.indexOf("workflowEventService.append");
    int allowedResult = source.indexOf("new CanonicalWriteResult(\n"
        + "        decision,\n"
        + "        true");

    assertThat(gateDecision).isNotNegative();
    assertThat(workflowAppend).isGreaterThan(gateDecision);
    assertThat(allowedResult).isGreaterThan(workflowAppend);
  }

  @Test
  void allowedCanonicalWriteBoundaryAppendsAuditAndDefersCanonicalPersistence() {
    RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder().build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isEqualTo(workflowPort.result.workflowEventId());
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(result.canonicalPersistenceStatus())
        .isEqualTo(CanonicalWriteService.CANONICAL_PERSISTENCE_DEFERRED);
    assertThat(workflowPort.commands).hasSize(1);
    assertThat(workflowPort.commands.getFirst().action())
        .isEqualTo("CANONICAL_WRITE_ALLOWED");
    assertThat(workflowPort.commands.getFirst().idempotencyKey().value())
        .isEqualTo("truth-layer-boundary-regression");
    assertThat(workflowPort.commands.getFirst().correlationId().value())
        .isEqualTo(CORRELATION_ID);
    assertThat(workflowPort.commands.getFirst().causationId().value())
        .isEqualTo(CAUSATION_ID);
    assertThat(workflowPort.commands.getFirst().afterState().json())
        .contains(CanonicalWriteService.CANONICAL_PERSISTENCE_DEFERRED);
  }

  @Test
  void blockedAndRequireReviewCanonicalWriteBoundariesAppendNoSuccessAudit() {
    RecordingWorkflowEventPort blockedWorkflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult blocked = service(blockedWorkflowPort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(blocked.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(blocked.workflowEventAppended()).isFalse();
    assertThat(blockedWorkflowPort.commands).isEmpty();

    RecordingWorkflowEventPort reviewWorkflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult requireReview = service(reviewWorkflowPort).attempt(commandBuilder()
        .targetRiskTier(RiskTier.T3_HIGH_RISK)
        .targetVerificationStatus(VerificationStatus.CANDIDATE_CONFIRMED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.NEEDS_CONFIRMATION,
            false,
            false,
            "candidate confirmation remains pending"))
        .build());

    assertThat(requireReview.decision().type())
        .isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(requireReview.workflowEventAppended()).isFalse();
    assertThat(reviewWorkflowPort.commands).isEmpty();
  }

  @Test
  void bulkReviewEvidenceCannotProduceVerifiedCanonicalSemantics() {
    for (VerificationStatus targetStatus : List.of(
        VerificationStatus.CANDIDATE_CONFIRMED,
        VerificationStatus.EXTERNAL_VERIFIED)) {
      RecordingWorkflowEventPort workflowPort = new RecordingWorkflowEventPort();

      CanonicalWriteResult result = service(workflowPort).attempt(commandBuilder()
          .targetVerificationStatus(targetStatus)
          .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
          .reviewEvidence(new CanonicalWriteReviewEvidence(
              new ReviewEventId(REVIEW_EVENT_ID),
              ReviewDecision.APPROVED,
              true,
              false,
              "bulk review cannot create verified fact semantics"))
          .build());

      assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
      assertThat(result.decision().reasons())
          .contains("bulk_approve_cannot_create_" + targetStatus.wireValue());
      assertThat(result.workflowEventAppended()).isFalse();
      assertThat(workflowPort.commands).isEmpty();
    }
  }

  @Test
  void systemInferenceAndGenericT4ReviewCannotPassServiceGate() {
    RecordingWorkflowEventPort inferenceWorkflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult inferenceResult = service(inferenceWorkflowPort).attempt(commandBuilder()
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(inferenceResult.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(inferenceResult.decision().reasons())
        .contains("system_inference_cannot_be_canonical_fact");
    assertThat(inferenceWorkflowPort.commands).isEmpty();

    RecordingWorkflowEventPort t4WorkflowPort = new RecordingWorkflowEventPort();
    CanonicalWriteResult t4Result = service(t4WorkflowPort).attempt(commandBuilder()
        .targetRiskTier(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING)
        .targetVerificationStatus(VerificationStatus.EXTERNAL_VERIFIED)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.APPROVED,
            false,
            false,
            "generic review is not transaction/legal approval"))
        .build());

    assertThat(t4Result.decision().type())
        .isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
    assertThat(t4Result.decision().reasons())
        .contains("high_risk_write_requires_explicit_review_approval");
    assertThat(t4WorkflowPort.commands).isEmpty();
  }

  @Test
  void productionTruthLayerDoesNotExposeForbiddenBoundaryNames() throws IOException {
    List<String> forbiddenNames = List.of(
        "saveCandidate",
        "updateCandidate",
        "saveCandidateProfile",
        "updateCandidateProfile",
        "saveCanonical",
        "writeRawCandidate",
        "disclose",
        "unlock",
        "transitionTo",
        "validateTransition",
        "mutateState");

    List<String> matches = new ArrayList<>();
    for (Path file : productionJavaFiles()) {
      String source = Files.readString(file);
      for (String forbiddenName : forbiddenNames) {
        if (source.contains(forbiddenName)) {
          matches.add(relativeToProject(file) + " contains " + forbiddenName);
        }
      }
    }

    assertThat(matches).isEmpty();
  }

  @Test
  void productionTruthLayerHasNoJpaSpringDataWebOrRestAnnotations() throws IOException {
    Pattern forbidden = Pattern.compile(
        "(jakarta\\.persistence|javax\\.persistence|org\\.springframework\\.data"
            + "|org\\.springframework\\.web|@Entity\\b|@Table\\b|@Repository\\b"
            + "|@RestController\\b|@Controller\\b|@RequestMapping\\b|@GetMapping\\b"
            + "|@PostMapping\\b|@PutMapping\\b|@PatchMapping\\b|@DeleteMapping\\b)");
    List<String> matches = new ArrayList<>();
    for (Path file : productionJavaFiles()) {
      Matcher matcher = forbidden.matcher(Files.readString(file));
      while (matcher.find()) {
        matches.add(relativeToProject(file) + " contains " + matcher.group());
      }
    }

    assertThat(matches).isEmpty();
  }

  @Test
  void jdbcPersistenceAdaptersRemainLimitedToAppendOnlyTruthLayerTables() throws IOException {
    Set<String> insertedTables = new TreeSet<>();
    Pattern insertPattern = Pattern.compile(
        "INSERT\\s+INTO\\s+([a-z_]+\\.[a-z_]+)",
        Pattern.CASE_INSENSITIVE);
    for (Path file : persistenceJavaFiles()) {
      String source = Files.readString(file);
      Matcher matcher = insertPattern.matcher(source);
      while (matcher.find()) {
        insertedTables.add(matcher.group(1));
      }
      assertThat(source)
          .doesNotContain("UPDATE ")
          .doesNotContain("DELETE FROM ")
          .doesNotContain("recruiting.candidate")
          .doesNotContain("recruiting.candidate_profile");
    }

    assertThat(insertedTables)
        .containsExactly(
            "governance.ai_task_definition",
            "governance.ai_task_run",
            "governance.canonical_write_attempt",
            "governance.claim_ledger_item",
            "governance.review_event",
            "workflow.workflow_event");
  }

  @Test
  void productionTruthLayerDoesNotWriteCandidateOrCandidateProfile() throws IOException {
    Pattern canonicalTableWrite = Pattern.compile(
        "(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+recruiting\\."
            + "(candidate|candidate_profile)\\b",
        Pattern.CASE_INSENSITIVE);
    List<String> matches = new ArrayList<>();
    for (Path file : productionJavaFiles()) {
      Matcher matcher = canonicalTableWrite.matcher(Files.readString(file));
      while (matcher.find()) {
        matches.add(relativeToProject(file) + " contains " + matcher.group());
      }
    }

    assertThat(matches).isEmpty();
  }

  @Test
  void transactionBoundaryContractPointsToSpringRollbackCoordinator() throws IOException {
    String contractSource = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/"
            + "CanonicalWriteTransactionBoundary.java");
    String springSource = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/"
            + "SpringCanonicalWriteTransactionBoundary.java");

    assertThat(contractSource)
        .contains("Canonical write transaction boundary")
        .contains("future canonical writes");
    assertThat(springSource)
        .contains("PlatformTransactionManager")
        .contains("TransactionTemplate")
        .contains("PROPAGATION_REQUIRED")
        .doesNotContain("java.sql.Connection")
        .doesNotContain("javax.sql.DataSource")
        .doesNotContain("setAutoCommit")
        .doesNotContain("commit()")
        .doesNotContain("rollback()");
  }

  @Test
  void canonicalWriteBoundaryIntegrationTestAssertsCommitAndRollbackBehavior()
      throws IOException {
    String source = sourceFile(
        "src/test/java/com/recruitingtransactionos/coreapi/truthlayer/"
            + "CanonicalWriteTransactionBoundaryIntegrationTest.java")
        .toLowerCase(Locale.ROOT);

    assertThat(source)
        .contains("successfultransactioncommitsworkfloweventappend")
        .contains("failedtransactionrollsbackworkfloweventappend")
        .contains("datasourcetransactionmanager")
        .doesNotContain("setautocommit")
        .doesNotContain("savepoint");
  }

  @Test
  void realSpringTransactionBoundaryImplementationHasNoBusinessLogic() {
    assertThat(publicDeclaredMethodNames(SpringCanonicalWriteTransactionBoundary.class))
        .containsExactly("run");
    assertThat(allDeclaredMethodNames(SpringCanonicalWriteTransactionBoundary.class))
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeCandidatePersistenceApi)
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeWorkflowEngineApi)
        .noneMatch(TruthLayerServiceBoundaryRegressionTest::looksLikeReviewPromotionApi);
  }

  private static CanonicalWriteService service(WorkflowEventPort workflowPort) {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        new WorkflowEventService(workflowPort),
        CanonicalWriteTransactionBoundary.immediate());
  }

  private static CanonicalWriteCommand.Builder commandBuilder() {
    return CanonicalWriteCommand.builder()
        .organizationId(ORGANIZATION_ID)
        .targetEntity(new EntityRef("CANDIDATE", CANDIDATE_ID))
        .targetFieldPath("headline")
        .proposedValueRef("claim-value:headline:v1")
        .claimId(new ClaimId(CLAIM_ID))
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE,
            false))
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(REVIEW_EVENT_ID),
            ReviewDecision.APPROVED,
            false,
            false,
            "reviewed source span before canonical boundary"))
        .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .targetRiskTier(RiskTier.T1_LOW_RISK)
        .clientVisible(false)
        .conflictsWithCanonical(false)
        .actor(new ActorRef(ACTOR_ID, ActorRole.CONSULTANT))
        .reason("reviewed source span before canonical boundary")
        .correlationId(CORRELATION_ID)
        .causationId(CAUSATION_ID)
        .idempotencyKey("truth-layer-boundary-regression")
        .occurredAt(OCCURRED_AT);
  }

  private static boolean looksLikeCanonicalPersistenceApi(String methodName) {
    String normalized = normalize(methodName);
    return normalized.contains("savecanonical")
        || normalized.contains("savecandidate")
        || normalized.contains("updatecandidate")
        || normalized.contains("writerawcandidate")
        || normalized.contains("canonicalfact");
  }

  private static boolean looksLikeReviewPromotionApi(String methodName) {
    String normalized = normalize(methodName);
    return looksLikeCanonicalPersistenceApi(methodName)
        || normalized.contains("candidateconfirmed")
        || normalized.contains("externalverified")
        || normalized.contains("promote")
        || normalized.contains("verifyfact");
  }

  private static boolean looksLikeWorkflowEngineApi(String methodName) {
    String normalized = normalize(methodName);
    return normalized.contains("transitionto")
        || normalized.contains("validatetransition")
        || normalized.contains("mutatestate")
        || normalized.contains("statemachine")
        || normalized.contains("workflowengine");
  }

  private static boolean looksLikeCandidatePersistenceApi(String methodName) {
    String normalized = normalize(methodName);
    return looksLikeCanonicalPersistenceApi(methodName)
        || normalized.contains("unlock")
        || normalized.contains("disclose")
        || normalized.contains("candidateprofile");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> allDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static List<Path> productionJavaFiles() throws IOException {
    try (Stream<Path> stream = Files.walk(productionRoot())) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static List<Path> persistenceJavaFiles() throws IOException {
    try (Stream<Path> stream = Files.walk(projectPath(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/persistence"))) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static Path productionRoot() {
    return projectPath("src/main/java/com/recruitingtransactionos/coreapi/truthlayer");
  }

  private static String sourceFile(String relativePath) throws IOException {
    return Files.readString(projectPath(relativePath));
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
  }

  private static String relativeToProject(Path file) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    if (file.startsWith(userDir)) {
      return userDir.relativize(file).toString();
    }
    Path coreApi = userDir.resolve("services/core-api");
    if (file.startsWith(coreApi)) {
      return coreApi.relativize(file).toString();
    }
    return file.toString();
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000090101")));

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      return Optional.empty();
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return result;
    }
  }
}
