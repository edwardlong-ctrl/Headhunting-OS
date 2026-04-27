package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TruthLayerPersistencePortContractTest {

  private static final Path TRUTH_LAYER_SOURCE_ROOT =
      Path.of("src/main/java/com/recruitingtransactionos/coreapi/truthlayer");
  private static final Path V2_MIGRATION =
      Path.of("src/main/resources/db/migration/V2__create_truth_layer_core_tables.sql");
  private static final UUID ORGANIZATION_ID = uuid("00000000-0000-0000-0000-000000000001");
  private static final UUID CANDIDATE_ID = uuid("00000000-0000-0000-0000-000000000002");
  private static final UUID REVIEWER_ID = uuid("00000000-0000-0000-0000-000000000003");
  private static final UUID SOURCE_ITEM_ID = uuid("00000000-0000-0000-0000-000000000004");
  private static final UUID AI_TASK_RUN_UUID = uuid("00000000-0000-0000-0000-000000000005");
  private static final UUID WORKFLOW_ACTOR_ID = uuid("00000000-0000-0000-0000-000000000006");

  @Test
  void claimLedgerAppendCommandCarriesClaimGovernanceVocabulary() {
    ClaimLedgerAppendCommand command = claimCommand();

    assertThat(recordComponentNames(ClaimLedgerAppendCommand.class))
        .contains("claimType", "assertionStrength", "sourceSpanReference", "speaker",
            "verificationStatus", "clientShareability", "targetEntity", "targetFieldPath");
    assertThat(command.claimType()).isEqualTo(ClaimType.INTENT);
    assertThat(command.assertionStrength()).isEqualTo(AssertionStrength.WEAK_SIGNAL);
    assertThat(command.sourceSpanReference()).isEqualTo(new SourceSpanRef("source-item:12-18"));
    assertThat(command.speaker()).isEqualTo(ActorRole.CANDIDATE);
    assertThat(command.verificationStatus()).isEqualTo(VerificationStatus.AI_EXTRACTED);
    assertThat(command.clientShareability()).isEqualTo(ClientShareability.CONSENT_REQUIRED);
    assertThat(command.targetEntity()).isEqualTo(targetCandidate());
    assertThat(command.targetFieldPath()).isEqualTo("motivation");
  }

  @Test
  void claimLedgerPortDoesNotAcceptCanonicalFactWrite() {
    assertThat(methodNames(ClaimLedgerPort.class))
        .containsExactly("append");
    assertThat(methodNames(ClaimLedgerPort.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void reviewEventCommandCarriesBulkApprovalAndRiskTier() {
    ReviewEventAppendCommand command = reviewCommand(true);

    assertThat(recordComponentNames(ReviewEventAppendCommand.class))
        .contains("reviewerId", "riskTier", "decision", "bulkApproval", "reason");
    assertThat(command.reviewerId()).isEqualTo(REVIEWER_ID);
    assertThat(command.riskTier()).isEqualTo(RiskTier.T2_MEDIUM_RISK);
    assertThat(command.decision()).isEqualTo(ReviewDecision.APPROVED);
    assertThat(command.bulkApproval()).isTrue();
    assertThat(command.reason()).isEqualTo("bulk normalized low-risk fields after source-span review");
  }

  @Test
  void reviewEventPortDoesNotTurnBulkIntoVerified() {
    assertThat(methodNames(ReviewEventPort.class))
        .containsExactly("append");
    assertThat(recordComponentNames(ReviewEventAppendCommand.class))
        .doesNotContain("verificationStatus", "targetVerificationStatus",
            "candidateConfirmed", "externalVerified");
    assertThat(methodNames(ReviewEventPort.class))
        .noneMatch(name -> normalized(name).contains("candidateconfirmed")
            || normalized(name).contains("externalverified")
            || normalized(name).contains("verify"));
  }

  @Test
  void workflowEventPortIsAppendOnlyByContract() {
    assertThat(methodNames(WorkflowEventPort.class))
        .containsExactly("append");
    assertThat(methodNames(WorkflowEventPort.class))
        .noneMatch(name -> normalized(name).contains("delete")
            || normalized(name).contains("update")
            || normalized(name).contains("mutate")
            || normalized(name).contains("save"));
  }

  @Test
  void aiTaskRunPortDoesNotWriteCanonicalFacts() {
    assertThat(methodNames(AITaskRunPort.class))
        .containsExactly("append");
    assertThat(methodNames(AITaskRunPort.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void portsDoNotImportPersistenceOrWebFrameworks() throws IOException {
    List<String> forbidden = List.of(
        "javax.persistence",
        "jakarta.persistence",
        "org.springframework.data",
        "org.springframework.web",
        "org.springframework.jdbc",
        "@Entity",
        "@Table",
        "@Repository",
        "@RestController",
        "@Service",
        "@Component");

    try (Stream<Path> paths = Files.walk(TRUTH_LAYER_SOURCE_ROOT)) {
      List<Path> sourceFiles = paths
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .filter(path -> path.toString().contains("/port/"))
          .toList();

      assertThat(sourceFiles).as("truth-layer port source files").isNotEmpty();
      for (Path sourceFile : sourceFiles) {
        String source = Files.readString(sourceFile);
        assertThat(source)
            .as("%s must remain pure Java with no persistence/web annotations", sourceFile)
            .doesNotContain(forbidden);
      }
    }
  }

  @Test
  void testOnlyInMemoryFakesCanAppendAndReadBackWithoutDatabase() {
    InMemoryClaimLedgerPort claimLedger = new InMemoryClaimLedgerPort();
    InMemoryReviewEventPort reviewEvents = new InMemoryReviewEventPort();
    InMemoryWorkflowEventPort workflowEvents = new InMemoryWorkflowEventPort();
    InMemoryAITaskRunPort aiTaskRuns = new InMemoryAITaskRunPort();

    ClaimLedgerAppendResult claimResult = claimLedger.append(claimCommand());
    ReviewEventAppendResult reviewResult = reviewEvents.append(reviewCommand(true));
    AITaskRunAppendResult aiTaskResult = aiTaskRuns.append(aiTaskRunCommand());
    WorkflowEventAppendResult workflowResult = workflowEvents.append(
        workflowCommand(aiTaskResult.aiTaskRunId(), reviewResult.reviewEventId()));

    assertThat(claimLedger.readBack(claimResult.claimId())).isEqualTo(claimCommand());
    assertThat(reviewEvents.readBack(reviewResult.reviewEventId())).isEqualTo(reviewCommand(true));
    assertThat(aiTaskRuns.readBack(aiTaskResult.aiTaskRunId())).isEqualTo(aiTaskRunCommand());
    assertThat(workflowEvents.readBack(workflowResult.workflowEventId()))
        .isEqualTo(workflowCommand(aiTaskResult.aiTaskRunId(), reviewResult.reviewEventId()));
  }

  @Test
  void v2MigrationVocabularyStillCoversThePorts() throws IOException {
    String sql = migrationSql();

    assertThat(sql)
        .contains("claim_type")
        .contains("assertion_strength")
        .contains("source_span_ref")
        .contains("speaker")
        .contains("verification_status")
        .contains("client_shareability");

    assertThat(sql)
        .contains("create table workflow.workflow_event")
        .contains("actor_user_id")
        .contains("actor_role")
        .contains("entity_type")
        .contains("action text")
        .contains("before_state")
        .contains("after_state")
        .contains("reason text")
        .contains("ai_task_run_id");

    assertThat(sql)
        .contains("create table governance.ai_task_run")
        .contains("task_version")
        .contains("input_schema_version")
        .contains("output_schema_version")
        .contains("prompt_version")
        .contains("model_version")
        .contains("human_review_status")
        .contains("write_back_target");
  }

  @Test
  void knownConsentDisclosureGapRemainsOutOfScopeForTask2G() throws IOException {
    assertThat(migrationSql())
        .doesNotContain("create table privacy.consent_record")
        .doesNotContain("create table privacy.disclosure_record");

    try (Stream<Path> paths = Files.walk(TRUTH_LAYER_SOURCE_ROOT)) {
      List<String> fileNames = paths
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .toList();

      assertThat(fileNames)
          .doesNotContain("ConsentRecordPort.java", "DisclosureRecordPort.java");
    }
  }

  private static ClaimLedgerAppendCommand claimCommand() {
    return new ClaimLedgerAppendCommand(
        ORGANIZATION_ID,
        targetCandidate(),
        "motivation",
        ClaimType.INTENT,
        AssertionStrength.WEAK_SIGNAL,
        new SourceSpanRef("source-item:12-18"),
        ActorRole.CANDIDATE,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.CONSENT_REQUIRED,
        SOURCE_ITEM_ID,
        new AITaskRunId(AI_TASK_RUN_UUID));
  }

  private static ReviewEventAppendCommand reviewCommand(boolean bulkApproval) {
    return new ReviewEventAppendCommand(
        ORGANIZATION_ID,
        REVIEWER_ID,
        targetCandidate(),
        "headline",
        RiskTier.T2_MEDIUM_RISK,
        ReviewDecision.APPROVED,
        bulkApproval,
        "bulk normalized low-risk fields after source-span review",
        Duration.ofSeconds(42),
        new ClaimId(uuid("00000000-0000-0000-0000-000000000101")),
        new SourceSpanRef("source-item:30-36"));
  }

  private static AITaskRunAppendCommand aiTaskRunCommand() {
    return new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "Claim Ledger Builder",
        "14.0",
        "claim-ledger-input.v1",
        "claim-ledger-output.v1",
        "prompt.claim-ledger-builder.v1",
        new ModelRef("placeholder-provider", "placeholder-model", "placeholder-model-version"),
        AITaskRunStatus.SUCCEEDED,
        "review_required",
        new WriteBackTarget("claim_ledger"),
        targetCandidate(),
        List.of(SOURCE_ITEM_ID),
        Instant.parse("2026-04-28T01:00:00Z"),
        Instant.parse("2026-04-28T01:00:05Z"));
  }

  private static WorkflowEventAppendCommand workflowCommand(
      AITaskRunId aiTaskRunId,
      ReviewEventId reviewEventId) {
    return new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        targetCandidate(),
        7,
        "CANDIDATE_CONSULTANT_REVIEW_STARTED",
        new WorkflowStateSnapshot("{\"status\":\"consultant_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"available\"}"),
        new ActorRef(WORKFLOW_ACTOR_ID, ActorRole.CONSULTANT),
        "domain_service",
        SOURCE_ITEM_ID,
        aiTaskRunId,
        reviewEventId,
        "field-level review completed",
        "candidate-reviewed-2g-contract",
        uuid("00000000-0000-0000-0000-000000000201"),
        null,
        Instant.parse("2026-04-28T01:01:00Z"));
  }

  private static EntityRef targetCandidate() {
    return new EntityRef("CANDIDATE", CANDIDATE_ID);
  }

  private static String migrationSql() throws IOException {
    return Files.readString(V2_MIGRATION).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static List<String> methodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> recordComponentNames(Class<? extends Record> recordType) {
    return Stream.of(recordType.getRecordComponents())
        .map(RecordComponent::getName)
        .toList();
  }

  private boolean looksLikeCanonicalWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("updatecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact");
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class InMemoryClaimLedgerPort implements ClaimLedgerPort {
    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<ClaimId, ClaimLedgerAppendCommand> commands = new LinkedHashMap<>();

    @Override
    public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
      ClaimId id = new ClaimId(uuid("00000000-0000-0000-0000-0000000003"
          + String.format("%02d", sequence.incrementAndGet())));
      commands.put(id, command);
      return new ClaimLedgerAppendResult(id);
    }

    private ClaimLedgerAppendCommand readBack(ClaimId claimId) {
      return commands.get(claimId);
    }
  }

  private static final class InMemoryReviewEventPort implements ReviewEventPort {
    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<ReviewEventId, ReviewEventAppendCommand> commands = new LinkedHashMap<>();

    @Override
    public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
      ReviewEventId id = new ReviewEventId(uuid("00000000-0000-0000-0000-0000000004"
          + String.format("%02d", sequence.incrementAndGet())));
      commands.put(id, command);
      return new ReviewEventAppendResult(id);
    }

    private ReviewEventAppendCommand readBack(ReviewEventId reviewEventId) {
      return commands.get(reviewEventId);
    }
  }

  private static final class InMemoryWorkflowEventPort implements WorkflowEventPort {
    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<WorkflowEventId, WorkflowEventAppendCommand> commands = new LinkedHashMap<>();

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      WorkflowEventId id = new WorkflowEventId(uuid("00000000-0000-0000-0000-0000000005"
          + String.format("%02d", sequence.incrementAndGet())));
      commands.put(id, command);
      return new WorkflowEventAppendResult(id);
    }

    private WorkflowEventAppendCommand readBack(WorkflowEventId workflowEventId) {
      return commands.get(workflowEventId);
    }
  }

  private static final class InMemoryAITaskRunPort implements AITaskRunPort {
    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<AITaskRunId, AITaskRunAppendCommand> commands = new LinkedHashMap<>();

    @Override
    public AITaskRunAppendResult append(AITaskRunAppendCommand command) {
      AITaskRunId id = new AITaskRunId(uuid("00000000-0000-0000-0000-0000000006"
          + String.format("%02d", sequence.incrementAndGet())));
      commands.put(id, command);
      return new AITaskRunAppendResult(id);
    }

    private AITaskRunAppendCommand readBack(AITaskRunId aiTaskRunId) {
      return commands.get(aiTaskRunId);
    }
  }
}
