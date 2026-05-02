package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcAITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AITaskRunGovernanceContractTest {

  private static final UUID ORGANIZATION_ID = uuid("00000000-0000-0000-0000-000000100001");
  private static final UUID REQUESTED_BY_ID = uuid("00000000-0000-0000-0000-000000100002");
  private static final UUID CANDIDATE_ID = uuid("00000000-0000-0000-0000-000000100003");
  private static final Instant STARTED_AT = Instant.parse("2026-04-28T06:00:00Z");
  private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");
  private static final Path APPS_ROOT = Path.of("../../apps");

  @Test
  void statusVocabularyIsSmallAndExplicitForGovernanceSkeleton() {
    assertThat(AITaskRunStatus.values())
        .containsExactly(
            AITaskRunStatus.CREATED,
            AITaskRunStatus.RUNNING,
            AITaskRunStatus.SUCCEEDED,
            AITaskRunStatus.FAILED,
            AITaskRunStatus.CANCELLED);
    assertThat(Stream.of(AITaskRunStatus.values()).map(AITaskRunStatus::wireValue))
        .containsExactly("created", "running", "succeeded", "failed", "cancelled");
  }

  @Test
  void validRunCanCarryTaskModelPromptAndSchemaVersionMetadata() {
    AITaskRunAppendCommand command = succeededCommand();

    assertThat(command.taskName()).isEqualTo("candidate-profile-extraction");
    assertThat(command.taskVersion()).isEqualTo("candidate-profile-extraction.v1");
    assertThat(command.inputSchemaVersion()).isEqualTo("candidate-profile-input.v1");
    assertThat(command.outputSchemaVersion()).isEqualTo("candidate-profile-output.v1");
    assertThat(command.promptVersion()).isEqualTo("prompt.candidate-profile-extraction.v1");
    assertThat(command.model()).isEqualTo(new ModelRef("metadata-only", "no-model-call", "v0"));
    assertThat(command.status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
    assertThat(command.requestedBy()).isEqualTo(new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT));
    assertThat(command.correlationId())
        .isEqualTo(new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000100004")));
    assertThat(command.causationId())
        .isEqualTo(new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000100005")));
    assertThat(command.targetEntity()).isEqualTo(new EntityRef("CANDIDATE", CANDIDATE_ID));
  }

  @Test
  void validRunCanCarryExplicitWriteBackAndHumanReviewMetadataWithoutExecutingWriteBack() {
    AITaskRunAppendCommand command = commandWithGovernanceMetadata(
        AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
        AITaskHumanReviewStatus.REQUIRED,
        ActorRole.AI);

    assertThat(command.humanReviewStatus()).isEqualTo("required");
    assertThat(command.writeBackTarget())
        .isEqualTo(new WriteBackTarget("claim_ledger_proposal"));
    assertThat(command.requestedBy()).isEqualTo(new ActorRef(REQUESTED_BY_ID, ActorRole.AI));
  }

  @Test
  void requiredVersionAndModelFieldsCannotBeBlank() {
    assertThatThrownBy(() -> new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.CREATED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000100004")),
        new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000100005")),
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(),
        STARTED_AT,
        null,
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("taskVersion must not be blank");

    assertThatThrownBy(() -> new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        " ",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.CREATED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        null,
        null,
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(),
        STARTED_AT,
        null,
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("inputSchemaVersion must not be blank");

    assertThatThrownBy(() -> new ModelRef("", "no-model-call", "v0"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("modelProvider must not be blank");
  }

  @Test
  void completedAtCannotBeBeforeStartedAt() {
    assertThatThrownBy(() -> new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.SUCCEEDED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        null,
        null,
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(),
        STARTED_AT,
        STARTED_AT.minusSeconds(1),
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("completedAt must not be before startedAt");
  }

  @Test
  void failedRunsRequireSafeSingleLineFailureReason() {
    assertThatThrownBy(() -> new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.FAILED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        null,
        null,
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(),
        STARTED_AT,
        STARTED_AT.plusSeconds(5),
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("failureReason must be present for failed AI task runs");

    assertThatThrownBy(() -> new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.FAILED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        null,
        null,
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(),
        STARTED_AT,
        STARTED_AT.plusSeconds(5),
        "java.lang.IllegalStateException: provider secret leaked\n\tat example.Stack.trace"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("failureReason must be a safe single-line reason");
  }

  @Test
  void aiTaskRunPortsRemainGovernanceScopedAndDoNotExposeWriteBackExecutionOrCanonicalMutation() {
    assertThat(publicDeclaredMethodNames(AITaskRunPort.class))
        .containsExactly("append", "findById", "update");
    assertThat(publicDeclaredMethodNames(JdbcAITaskRunPort.class))
        .containsExactly("append", "findById", "update");
    assertThat(publicDeclaredMethodNames(AITaskRunService.class))
        .containsExactly("append", "findById", "update");

    assertThat(declaredMethodNames(AITaskRunPort.class))
        .noneMatch(this::looksLikeUnsafeAiTaskRunBehavior);
    assertThat(declaredMethodNames(JdbcAITaskRunPort.class))
        .noneMatch(this::looksLikeUnsafeAiTaskRunBehavior);
    assertThat(declaredMethodNames(AITaskRunService.class))
        .noneMatch(this::looksLikeUnsafeAiTaskRunBehavior);
  }

  @Test
  void serviceRejectsDeniedGovernanceMetadataBeforePersistenceAppend() {
    RecordingAITaskRunPort port = new RecordingAITaskRunPort();
    AITaskRunService service = new AITaskRunService(port);
    AITaskRunAppendCommand command = commandWithGovernanceMetadata(
        AITaskWriteBackTarget.CANONICAL_CANDIDATE_PROFILE,
        AITaskHumanReviewStatus.APPROVED,
        ActorRole.AI);

    assertThatThrownBy(() -> service.append(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("human_review_approval_actor_required");
    assertThat(port.appendCalls).isZero();
  }

  @Test
  void task10BDoesNotAddAiModelCallsWriteBackExecutionApiControllerOrUi() throws IOException {
    assertThat(sourceTree()).doesNotContain(
        "ChatClient",
        "OpenAI",
        "Anthropic",
        "WebClient",
        "RestTemplate");

    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
      assertThat(paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .filter(path -> normalized(path.getFileName().toString()).contains("aitask")
              || normalized(path.getFileName().toString()).contains("aigovernance"))
          .map(path -> path.getFileName().toString())
          .filter(name -> normalized(name).contains("controller")
              || normalized(name).contains("api"))
          .toList())
          .as("Task 10B must not add AI governance API/controller classes")
          .isEmpty();
    }

    if (Files.exists(APPS_ROOT)) {
      try (Stream<Path> paths = Files.walk(APPS_ROOT)) {
        assertThat(paths.filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .filter(name -> normalized(name).contains("aitaskrun")
                || normalized(name).contains("aigovernance"))
            .toList())
            .as("Task 10B must not add frontend/UI files")
            .isEmpty();
      }
    }

    assertThat(aiTaskGovernanceSource()).doesNotContain(
        "CanonicalWriteService",
        "CandidateProfileService",
        "ClaimLedgerService",
        "ReviewEventService",
        "WorkflowEventService",
        "upsertCandidateProfileField",
        "new ClaimLedgerAppendCommand",
        "new ReviewEventAppendCommand",
        "new WorkflowEventAppendCommand");
  }

  private static AITaskRunAppendCommand succeededCommand() {
    return new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.SUCCEEDED,
        null,
        null,
        new ActorRef(REQUESTED_BY_ID, ActorRole.CONSULTANT),
        new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000100004")),
        new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000100005")),
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(uuid("00000000-0000-0000-0000-000000100006")),
        STARTED_AT,
        STARTED_AT.plusSeconds(8),
        null);
  }

  private static AITaskRunAppendCommand commandWithGovernanceMetadata(
      AITaskWriteBackTarget target,
      AITaskHumanReviewStatus reviewStatus,
      ActorRole requestedByRole) {
    return new AITaskRunAppendCommand(
        ORGANIZATION_ID,
        "candidate-profile-extraction",
        "candidate-profile-extraction.v1",
        "candidate-profile-input.v1",
        "candidate-profile-output.v1",
        "prompt.candidate-profile-extraction.v1",
        new ModelRef("metadata-only", "no-model-call", "v0"),
        AITaskRunStatus.SUCCEEDED,
        reviewStatus.wireValue(),
        new WriteBackTarget(target.wireValue()),
        new ActorRef(REQUESTED_BY_ID, requestedByRole),
        new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000100004")),
        new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000100005")),
        new EntityRef("CANDIDATE", CANDIDATE_ID),
        List.of(uuid("00000000-0000-0000-0000-000000100006")),
        STARTED_AT,
        STARTED_AT.plusSeconds(8),
        null);
  }

  private static String sourceTree() throws IOException {
    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT.resolve("com/recruitingtransactionos/coreapi/truthlayer"))) {
      StringBuilder source = new StringBuilder();
      for (Path path : paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .toList()) {
        source.append(Files.readString(path)).append('\n');
      }
      return source.toString();
    }
  }

  private static String aiTaskGovernanceSource() throws IOException {
    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT.resolve("com/recruitingtransactionos/coreapi/truthlayer"))) {
      StringBuilder source = new StringBuilder();
      for (Path path : paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .filter(path -> normalized(path.getFileName().toString()).contains("aitask")
              || normalized(path.getFileName().toString()).contains("aigovernance"))
          .toList()) {
        source.append(Files.readString(path)).append('\n');
      }
      return source.toString();
    }
  }

  private boolean looksLikeUnsafeAiTaskRunBehavior(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("execute")
        || normalized.contains("route")
        || normalized.contains("retry")
        || normalized.contains("queue")
        || normalized.contains("worker")
        || normalized.contains("enforcewriteback")
        || normalized.contains("canonical")
        || normalized.contains("claimledger")
        || normalized.contains("reviewevent")
        || normalized.contains("candidateprofile");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> declaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class RecordingAITaskRunPort implements AITaskRunPort {

    private int appendCalls;

    @Override
    public AITaskRunAppendResult append(AITaskRunAppendCommand command) {
      appendCalls++;
      return new AITaskRunAppendResult(new AITaskRunId(
          uuid("00000000-0000-0000-0000-000000100099")));
    }

    @Override
    public Optional<AITaskRunRecord> findById(UUID organizationId, AITaskRunId aiTaskRunId) {
      return Optional.empty();
    }
  }
}
