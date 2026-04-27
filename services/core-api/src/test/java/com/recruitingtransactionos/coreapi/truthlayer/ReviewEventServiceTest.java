package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReviewEventServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000030001");
  private static final UUID REVIEWER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000030002");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000030003");
  private static final UUID CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000030004");

  @Test
  void appendDelegatesToPort() {
    RecordingReviewEventPort port = new RecordingReviewEventPort();
    ReviewEventService service = new ReviewEventService(port);
    ReviewEventAppendCommand command = validCommand(false);

    ReviewEventAppendResult result = service.append(command);

    assertThat(result).isEqualTo(port.result);
    assertThat(port.commands).containsExactly(command);
  }

  @Test
  void serviceRejectsNullCommandBeforePortAppend() {
    RecordingReviewEventPort port = new RecordingReviewEventPort();
    ReviewEventService service = new ReviewEventService(port);

    assertThatThrownBy(() -> service.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");
    assertThat(port.commands).isEmpty();
  }

  @Test
  void serviceDoesNotExposeCanonicalWriteBehavior() {
    assertThat(publicDeclaredMethodNames(ReviewEventService.class))
        .containsExactly("append");
    assertThat(declaredMethodNames(ReviewEventService.class))
        .noneMatch(this::looksLikeCanonicalWriteShortcut);
  }

  @Test
  void bulkReviewIsRecordedAsReviewOnly() {
    RecordingReviewEventPort port = new RecordingReviewEventPort();
    ReviewEventService service = new ReviewEventService(port);
    ReviewEventAppendCommand command = validCommand(true);

    service.append(command);

    assertThat(port.commands).containsExactly(command);
    assertThat(port.commands.getFirst().bulkApproval()).isTrue();
    assertThat(publicDeclaredMethodNames(ReviewEventService.class))
        .doesNotContain("confirmFact", "writeCanonicalFact", "saveCandidateProfile");
  }

  private static ReviewEventAppendCommand validCommand(boolean bulkApproval) {
    return new ReviewEventAppendCommand(
        ORGANIZATION_ID,
        REVIEWER_ID,
        new EntityRef("candidate", CANDIDATE_ID),
        "headline",
        RiskTier.T2_MEDIUM_RISK,
        ReviewDecision.APPROVED,
        bulkApproval,
        "reviewed against source span before accepting",
        Duration.ofSeconds(42),
        new ClaimId(CLAIM_ID),
        new SourceSpanRef("source-item:30-36"));
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

  private static final class RecordingReviewEventPort implements ReviewEventPort {
    private final List<ReviewEventAppendCommand> commands = new ArrayList<>();
    private final ReviewEventAppendResult result = new ReviewEventAppendResult(
        new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000030101")));

    @Override
    public ReviewEventAppendResult append(ReviewEventAppendCommand command) {
      commands.add(command);
      return result;
    }
  }
}
