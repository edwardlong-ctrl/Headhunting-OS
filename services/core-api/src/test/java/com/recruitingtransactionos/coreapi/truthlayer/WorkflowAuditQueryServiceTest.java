package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowAuditQueryServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000a0001");
  private static final UUID WORKFLOW_EVENT_UUID =
      UUID.fromString("00000000-0000-0000-0000-0000000a0002");

  @Test
  void queryRequiresOrganizationId() {
    WorkflowAuditQueryService service =
        new WorkflowAuditQueryService(new RecordingWorkflowAuditReadPort());

    assertThatThrownBy(() -> service.search(WorkflowAuditQuery.builder(null).build()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void invalidLimitIsRejected() {
    WorkflowAuditQueryService service =
        new WorkflowAuditQueryService(new RecordingWorkflowAuditReadPort());

    assertThatThrownBy(() -> service.search(WorkflowAuditQuery.builder(ORGANIZATION_ID)
        .limit(0)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be greater than zero");

    assertThatThrownBy(() -> service.search(WorkflowAuditQuery.builder(ORGANIZATION_ID)
        .limit(-1)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be greater than zero");
  }

  @Test
  void tooLargeLimitIsRejected() {
    WorkflowAuditQueryService service =
        new WorkflowAuditQueryService(new RecordingWorkflowAuditReadPort());

    assertThatThrownBy(() -> service.search(WorkflowAuditQuery.builder(ORGANIZATION_ID)
        .limit(WorkflowAuditQuery.MAX_LIMIT + 1)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be 100 or fewer");
  }

  @Test
  void invalidTimeRangeIsRejected() {
    WorkflowAuditQueryService service =
        new WorkflowAuditQueryService(new RecordingWorkflowAuditReadPort());

    assertThatThrownBy(() -> service.search(WorkflowAuditQuery.builder(ORGANIZATION_ID)
        .occurredFrom(Instant.parse("2026-04-28T04:00:00Z"))
        .occurredTo(Instant.parse("2026-04-28T03:00:00Z"))
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("occurredFrom must be before or equal to occurredTo");
  }

  @Test
  void searchDelegatesValidatedQueryToReadPort() {
    RecordingWorkflowAuditReadPort port = new RecordingWorkflowAuditReadPort();
    WorkflowAuditQueryService service = new WorkflowAuditQueryService(port);
    WorkflowAuditQuery query = WorkflowAuditQuery.builder(ORGANIZATION_ID)
        .entityType("CANDIDATE")
        .actionCode("CANDIDATE_SHORTLISTED")
        .actorType(ActorRole.CONSULTANT)
        .limit(25)
        .offset(5)
        .build();

    List<WorkflowAuditRecord> result = service.search(query);

    assertThat(result).isEqualTo(port.searchResult);
    assertThat(port.queries).containsExactly(query);
    assertThat(port.findByIdCalls).isEmpty();
  }

  @Test
  void findByIdRequiresOrganizationAndEventId() {
    WorkflowAuditQueryService service =
        new WorkflowAuditQueryService(new RecordingWorkflowAuditReadPort());

    assertThatThrownBy(() -> service.findById(null, new WorkflowEventId(WORKFLOW_EVENT_UUID)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
    assertThatThrownBy(() -> service.findById(ORGANIZATION_ID, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("workflowEventId must not be null");
  }

  @Test
  void readServiceAndPortExposeReadOnlyAuditBoundary() {
    assertThat(publicDeclaredMethodNames(WorkflowAuditQueryService.class))
        .containsExactly("findById", "search");
    assertThat(publicDeclaredMethodNames(WorkflowAuditReadPort.class))
        .containsExactly("findById", "search");
    assertThat(allDeclaredMethodNames(WorkflowAuditQueryService.class))
        .noneMatch(this::looksLikeWriteOrWorkflowEngineBehavior);
    assertThat(allDeclaredMethodNames(WorkflowAuditReadPort.class))
        .noneMatch(this::looksLikeWriteOrWorkflowEngineBehavior);
  }

  @Test
  void workflowEventServiceRemainsAppendBoundaryOnly() {
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class))
        .containsExactly("append");
    assertThat(allDeclaredMethodNames(WorkflowEventService.class))
        .noneMatch(this::looksLikeReadModelOrWorkflowEngineBehavior);
  }

  @Test
  void jdbcAuditReadAdapterDoesNotJoinTargetBusinessTables() throws IOException {
    String source = Files.readString(projectPath(
        "src/main/java/com/recruitingtransactionos/coreapi/truthlayer/persistence/"
            + "JdbcWorkflowAuditReadPort.java"));

    assertThat(source)
        .contains("FROM workflow.workflow_event we")
        .doesNotContain(" JOIN ")
        .doesNotContain("recruiting.candidate")
        .doesNotContain("recruiting.candidate_profile")
        .doesNotContain("privacy.consent")
        .doesNotContain("privacy.disclosure");
  }

  private boolean looksLikeWriteOrWorkflowEngineBehavior(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("append")
        || normalized.contains("insert")
        || normalized.contains("update")
        || normalized.contains("delete")
        || normalized.contains("transition")
        || normalized.contains("statemachine")
        || normalized.contains("mutatestate")
        || normalized.contains("workflowengine");
  }

  private boolean looksLikeReadModelOrWorkflowEngineBehavior(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("search")
        || normalized.contains("findbyentity")
        || normalized.contains("findbycorrelation")
        || normalized.contains("findbycausation")
        || normalized.contains("transition")
        || normalized.contains("statemachine")
        || normalized.contains("mutatestate")
        || normalized.contains("workflowengine");
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

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
  }

  private static final class RecordingWorkflowAuditReadPort implements WorkflowAuditReadPort {
    private final List<WorkflowAuditQuery> queries = new ArrayList<>();
    private final List<FindByIdCall> findByIdCalls = new ArrayList<>();
    private final List<WorkflowAuditRecord> searchResult = List.of();

    @Override
    public Optional<WorkflowAuditRecord> findById(
        UUID organizationId,
        WorkflowEventId workflowEventId) {
      findByIdCalls.add(new FindByIdCall(organizationId, workflowEventId));
      return Optional.empty();
    }

    @Override
    public List<WorkflowAuditRecord> search(WorkflowAuditQuery query) {
      queries.add(query);
      return searchResult;
    }
  }

  private record FindByIdCall(
      UUID organizationId,
      WorkflowEventId workflowEventId) {
  }
}
