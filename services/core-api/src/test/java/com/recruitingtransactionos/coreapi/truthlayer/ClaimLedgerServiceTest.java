package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClaimLedgerServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000010001");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000010002");
  private static final UUID SOURCE_ITEM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000010003");
  private static final UUID AI_TASK_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000010004");

  @Test
  void appendDelegatesToPort() {
    RecordingClaimLedgerPort port = new RecordingClaimLedgerPort();
    ClaimLedgerService service = new ClaimLedgerService(port);
    ClaimLedgerAppendCommand command = validCommand();

    ClaimLedgerAppendResult result = service.append(command);

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).containsExactly(command);
  }

  @Test
  void serviceDoesNotExposeCanonicalWriteBehavior() {
    assertThat(publicDeclaredMethodNames(ClaimLedgerService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(ClaimLedgerService.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void invalidCommandsFailFastBeforePortAppend() {
    RecordingClaimLedgerPort port = new RecordingClaimLedgerPort();
    ClaimLedgerService service = new ClaimLedgerService(port);

    assertThatThrownBy(() -> service.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands).isEmpty();

    assertThatThrownBy(() -> commandWithBlankTargetFieldPath())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("targetFieldPath must not be blank");
    assertThat(port.commands).isEmpty();
  }

  private static ClaimLedgerAppendCommand validCommand() {
    return new ClaimLedgerAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("candidate", CANDIDATE_ID),
        "motivation",
        ClaimType.INTENT,
        AssertionStrength.WEAK_SIGNAL,
        new SourceSpanRef("source-item:12-18"),
        ActorRole.CANDIDATE,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.CONSENT_REQUIRED,
        SOURCE_ITEM_ID,
        new AITaskRunId(AI_TASK_RUN_ID));
  }

  private static ClaimLedgerAppendCommand commandWithBlankTargetFieldPath() {
    return new ClaimLedgerAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("candidate", CANDIDATE_ID),
        " ",
        ClaimType.INTENT,
        AssertionStrength.WEAK_SIGNAL,
        new SourceSpanRef("source-item:12-18"),
        ActorRole.CANDIDATE,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.CONSENT_REQUIRED,
        null,
        null);
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

  private static final class RecordingClaimLedgerPort implements ClaimLedgerPort {
    private final List<ClaimLedgerAppendCommand> commands = new ArrayList<>();
    private final ClaimLedgerAppendResult result = new ClaimLedgerAppendResult(
        new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000010101")));

    @Override
    public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
      commands.add(command);
      return result;
    }
  }
}
