package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcAITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskGovernancePolicy;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AITaskGovernanceRegressionClosureTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Path CONTRACT_SCHEMA_DIR = Path.of("../../packages/contracts/schemas");
  private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");
  private static final Path APPS_ROOT = Path.of("../../apps");

  @Test
  void aiTaskRunSchemaContainsRequiredMetadataVocabulariesOnly() throws IOException {
    JsonNode schema = readSchema("ai-task-run.schema.json");
    JsonNode properties = schema.path("properties");
    Set<String> required = stringSet(schema.path("required"));

    assertThat(required).contains(
        "task_version",
        "input_schema_version",
        "output_schema_version",
        "prompt_version",
        "model_provider",
        "model_name",
        "status",
        "human_review_status");
    assertThat(stringList(properties.path("status").path("enum")))
        .containsExactlyElementsOf(wireValues(AITaskRunStatus.values()));
    assertThat(stringList(properties.path("write_back_target").path("enum")))
        .containsExactlyElementsOf(wireValues(AITaskWriteBackTarget.values()));
    assertThat(stringList(properties.path("human_review_status").path("enum")))
        .containsExactlyElementsOf(wireValues(AITaskHumanReviewStatus.values()));

    assertThat(properties.path("failure_reason").path("maxLength").asInt())
        .isLessThanOrEqualTo(512);
    assertThat(properties.path("failure_reason").path("$comment").asText())
        .contains("Safe single-line")
        .doesNotContain("stack trace payload");

    assertThat(fieldNames(properties)).doesNotContain(
        "execute_prompt",
        "prompt_text",
        "raw_prompt",
        "model_routing_policy",
        "worker_queue",
        "retry_count",
        "async_job_id",
        "write_back_executed",
        "canonical_write_result",
        "candidate_profile_patch",
        "approval_execution_result");
  }

  @Test
  void aiTaskRunAppendSurfaceRemainsMetadataAppendReadOnly() {
    assertThat(publicDeclaredMethodNames(AITaskRunPort.class)).containsExactly("append", "findById");
    assertThat(publicDeclaredMethodNames(JdbcAITaskRunPort.class)).containsExactly("append", "findById");
    assertThat(publicDeclaredMethodNames(AITaskRunService.class)).containsExactly("append", "findById");

    assertThat(allDeclaredMethodNames(AITaskRunPort.class)).noneMatch(this::looksExecutable);
    assertThat(allDeclaredMethodNames(JdbcAITaskRunPort.class)).noneMatch(this::looksExecutable);
    assertThat(allDeclaredMethodNames(AITaskRunService.class)).noneMatch(this::looksExecutable);
  }

  @Test
  void aiGovernanceSourceDoesNotWireAiExecutionWriteBackOrCanonicalMutation()
      throws IOException {
    String source = aiGovernanceSource();

    assertThat(source).doesNotContain(
        "ChatClient",
        "OpenAI",
        "Anthropic",
        "DeepSeek",
        "RestTemplate",
        "WebClient",
        "PromptTemplate",
        "ExecutorService",
        "CompletableFuture",
        "TaskScheduler",
        "RetryTemplate",
        "@Async",
        "BlockingQueue",
        "Kafka",
        "Rabbit",
        "CanonicalWriteService",
        "CandidateProfileService",
        "ClaimLedgerService",
        "ReviewEventService",
        "WorkflowEventService",
        "new ClaimLedgerAppendCommand",
        "new ReviewEventAppendCommand",
        "new WorkflowEventAppendCommand",
        "upsertCandidateProfileField",
        "candidateProfileWriteTarget");
  }

  @Test
  void aiGovernanceTask10SurfaceAddsNoApiSecuritySessionConsentCommercialOrUiBehavior()
      throws IOException {
    String source = aiGovernanceSource();

    assertThat(source).doesNotContain(
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@GetMapping",
        "@PostMapping",
        "org.springframework.security",
        "SecurityFilterChain",
        "HttpSession",
        "ConsentRecord",
        "DisclosureRecord",
        "identity_disclosed",
        "unlock",
        "PlacementService",
        "CommissionService",
        "OfferService",
        "InvoiceService");

    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
      assertThat(paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .filter(path -> aiGovernanceFileName(path.getFileName().toString()))
          .map(path -> path.getFileName().toString())
          .filter(name -> normalized(name).contains("controller")
              || normalized(name).contains("api")
              || normalized(name).contains("security")
              || normalized(name).contains("session"))
          .toList())
          .as("Task 10 AI governance files must not add API/security/session classes")
          .isEmpty();
    }

    if (Files.exists(APPS_ROOT)) {
      try (Stream<Path> paths = Files.walk(APPS_ROOT)) {
        assertThat(paths
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .filter(AITaskGovernanceRegressionClosureTest::aiGovernanceFileName)
            .toList())
            .as("Task 10 must not add frontend/UI AI governance files")
            .isEmpty();
      }
    }
  }

  @Test
  void governancePolicyStillHasNoConstructorDependenciesOnExecutionServices() {
    assertThat(AITaskGovernancePolicy.class.getDeclaredConstructors())
        .hasSize(1)
        .allSatisfy(constructor -> assertThat(constructor.getParameterCount()).isZero());
  }

  private boolean looksExecutable(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("execute")
        || normalized.contains("prompt")
        || normalized.contains("route")
        || normalized.contains("retry")
        || normalized.contains("async")
        || normalized.contains("queue")
        || normalized.contains("worker")
        || normalized.contains("executewriteback")
        || normalized.contains("performwriteback")
        || normalized.contains("applywriteback")
        || normalized.contains("canonical")
        || normalized.contains("candidateprofile")
        || normalized.contains("claimledger")
        || normalized.contains("reviewevent")
        || normalized.contains("workflowevent");
  }

  private static JsonNode readSchema(String fileName) throws IOException {
    return OBJECT_MAPPER.readTree(CONTRACT_SCHEMA_DIR.resolve(fileName).toFile());
  }

  private static Set<String> stringSet(JsonNode arrayNode) {
    assertThat(arrayNode.isArray()).as("expected JSON array node but got %s", arrayNode).isTrue();
    return new LinkedHashSet<>(stringList(arrayNode));
  }

  private static List<String> stringList(JsonNode arrayNode) {
    assertThat(arrayNode.isArray()).as("expected JSON array node but got %s", arrayNode).isTrue();
    List<String> values = new ArrayList<>();
    arrayNode.forEach(value -> values.add(value.asText()));
    return values;
  }

  private static Set<String> fieldNames(JsonNode properties) {
    Set<String> names = new LinkedHashSet<>();
    properties.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private static <E extends Enum<E>> List<String> wireValues(E[] values) {
    return Arrays.stream(values)
        .map(value -> {
          if (value instanceof AITaskRunStatus status) {
            return status.wireValue();
          }
          if (value instanceof AITaskWriteBackTarget target) {
            return target.wireValue();
          }
          if (value instanceof AITaskHumanReviewStatus status) {
            return status.wireValue();
          }
          throw new IllegalArgumentException("unsupported enum type: " + value.getClass());
        })
        .toList();
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

  private static String aiGovernanceSource() throws IOException {
    StringBuilder source = new StringBuilder();
    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
      for (Path path : paths
          .filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(".java"))
          .filter(file -> aiGovernanceFileName(file.getFileName().toString()))
          .sorted()
          .toList()) {
        source.append(Files.readString(path)).append('\n');
      }
    }
    return source.toString();
  }

  private static boolean aiGovernanceFileName(String fileName) {
    String normalized = normalized(fileName);
    return normalized.contains("aitask")
        || normalized.contains("aigovernance");
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }
}
