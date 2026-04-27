package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowEventServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050002");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050003");
  private static final UUID SOURCE_REF_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050004");
  private static final UUID AI_TASK_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050005");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050006");
  private static final UUID CORRELATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000050007");

  @Test
  void appendDelegatesToPort() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);
    WorkflowEventAppendCommand command = validCommand();

    WorkflowEventAppendResult result = service.append(command);

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).containsExactly(command);
  }

  @Test
  void serviceRejectsNullCommandBeforePortAppend() {
    RecordingWorkflowEventPort port = new RecordingWorkflowEventPort();
    WorkflowEventService service = new WorkflowEventService(port);

    assertThatThrownBy(() -> service.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceDoesNotExposeCanonicalWriteBehavior() {
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(WorkflowEventService.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void serviceDoesNotExposeTransitionValidationOrMutateStateBehavior() {
    assertThat(publicDeclaredMethodNames(WorkflowEventService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(WorkflowEventService.class))
        .noneMatch(this::looksLikeWorkflowEngineBehavior);
  }

  private static WorkflowEventAppendCommand validCommand() {
    return new WorkflowEventAppendCommand(
        ORGANIZATION_ID,
        "recruiting",
        new EntityRef("candidate", CANDIDATE_ID),
        7,
        "candidate.shortlisted",
        new WorkflowStateSnapshot("{\"status\":\"consultant_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        "domain_service",
        SOURCE_REF_ID,
        new AITaskRunId(AI_TASK_RUN_ID),
        new ReviewEventId(REVIEW_EVENT_ID),
        "consultant published anonymous shortlist after review",
        "workflow-event-service-test",
        CORRELATION_ID,
        null,
        Instant.parse("2026-04-28T02:00:00Z"));
  }

  private boolean looksLikeCanonicalWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("updatecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact")
        || normalized.contains("candidateconfirmed")
        || normalized.contains("externalverified");
  }

  private boolean looksLikeWorkflowEngineBehavior(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("transition")
        || normalized.contains("statemachine")
        || normalized.contains("mutatestate")
        || normalized.contains("advancestate")
        || normalized.contains("applystate")
        || normalized.contains("approve")
        || normalized.contains("reject");
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

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();
    private final WorkflowEventAppendResult result = new WorkflowEventAppendResult(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000050101")));

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return result;
    }
  }
}
