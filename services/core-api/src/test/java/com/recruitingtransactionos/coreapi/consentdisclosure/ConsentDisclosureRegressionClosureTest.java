package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteGate;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ConsentDisclosureRegressionClosureTest {

  @Test
  void permissionAndProjectionStillDenyRawClientCandidateProfileAndL4Access() {
    PermissionEvaluator evaluator = new PermissionEvaluator();

    assertThat(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE,
        AccessAction.READ,
        FieldClassification.PII,
        Set.of(),
        false)).reasonCode()).isEqualTo("client_raw_candidate_denied");
    assertThat(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.PII,
        Set.of(),
        false)).reasonCode()).isEqualTo("client_raw_candidate_profile_denied");
    assertThat(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        true)).reasonCode()).isEqualTo("identity_disclosure_not_implemented");

    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    assertThatThrownBy(() -> projectionService.project(
        clientSafeReadRequest(),
        snapshot(RedactionLevel.L4_IDENTITY_DISCLOSED)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("client-safe projection cannot use L4 identity disclosure");
    assertThatThrownBy(() -> projectionService.project(
        new AccessRequest(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.UNLOCK,
            FieldClassification.PII,
            Set.of(),
            true),
        snapshot(RedactionLevel.L2_CLIENT_SAFE)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void consentDisclosurePackageDoesNotExposeRawCandidateProfileOrGovernanceInternals() {
    Set<Class<?>> forbiddenTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimLedgerAppendCommand.class,
        ClaimLedgerItemForCanonicalWrite.class,
        ReviewEventForCanonicalWrite.class,
        WorkflowEventAppendCommand.class,
        AITaskRunAppendCommand.class,
        AITaskRunRecord.class,
        CanonicalWriteGate.class,
        CanonicalWriteService.class);

    for (Class<?> surfaceType : Set.of(
        ConsentRecord.class,
        DisclosureRecord.class,
        UnlockDecision.class,
        UnlockDisclosureRequest.class,
        UnlockDisclosureDecision.class,
        DisclosureAuditBoundary.class,
        DisclosureAuditCommand.class)) {
      assertThat(recordComponentTypes(surfaceType))
          .as(surfaceType.getSimpleName())
          .doesNotContainAnyElementsOf(forbiddenTypes);
      assertThat(recordComponentTypeNames(surfaceType))
          .as(surfaceType.getSimpleName())
          .noneMatch(name -> containsAny(
              name,
              "CandidateProfileService",
              "CandidateProfilePersistence",
              "ClaimLedgerService",
              "ReviewEventService",
              "WorkflowEventService",
              "CanonicalWriteService",
              "AITaskRunService"));
    }
  }

  @Test
  void consentDisclosureSliceAddsNoApiPersistenceAiCanonicalWriteOrUiSurface()
      throws IOException {
    List<Path> productionFiles = consentDisclosureProductionFiles();

    assertThat(productionFiles)
        .extracting(path -> path.getFileName().toString())
        .noneMatch(fileName -> fileName.endsWith("Controller.java"))
        .noneMatch(fileName -> fileName.endsWith("Repository.java"))
        .noneMatch(fileName -> fileName.endsWith("Entity.java"))
        .noneMatch(fileName -> fileName.endsWith("Port.java"))
        .noneMatch(fileName -> fileName.contains("Persistence"))
        .noneMatch(fileName -> fileName.contains("Migration"));

    for (Path file : productionFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .as(file.toString())
          .doesNotContain(
              "@RestController",
              "@Controller",
              "@Service",
              "@Repository",
              "@RequestMapping",
              "@GetMapping",
              "@PostMapping",
              "org.springframework",
              "javax.sql.DataSource",
              "JdbcTemplate",
              "EntityManager",
              "@Entity",
              "@Table",
              "Flyway",
              "ChatClient",
              "OpenAI",
              "Anthropic",
              "DeepSeek",
              "PromptTemplate",
              "PromptClient",
              "ModelClient",
              "CanonicalWriteService",
              "CanonicalWriteGate",
              "CanonicalWriteCommand",
              "CandidateProfileService",
              "CandidateProfilePersistencePort",
              "upsertCandidateProfileField",
              "ClaimLedgerService",
              "ClaimLedgerAppendCommand",
              "ReviewEventService",
              "ReviewEventAppendCommand",
              "WorkflowEventService",
              "WorkflowEventAppendCommand",
              "AITaskRunService",
              "CompletableFuture",
              "ExecutorService",
              "BlockingQueue",
              "Kafka",
              "Rabbit");
    }

    assertThat(findConsentDisclosureUiFiles()).isEmpty();
  }

  private static AccessRequest clientSafeReadRequest() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }

  private static InternalCandidateProjectionSnapshot snapshot(RedactionLevel redactionLevel) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000001200c1",
        "00000000-0000-0000-0000-0000001200c2",
        "Task Twelve Candidate",
        "task12@example.com",
        "+86 138 0000 0012",
        "https://www.linkedin.com/in/task-twelve",
        "NebulaChip Systems",
        List.of("Orion-X7"),
        "Task Twelve Candidate led Orion-X7 at NebulaChip Systems.",
        "consultant-private note",
        AnonymousCandidateCardId.of("card_task12a_regression"),
        AnonymousCandidateRef.of("anon_candidate_task12a_regression"),
        "projection-v1",
        redactionLevel,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit based on generalized capability evidence."),
        Set.of(
            "anonymous.card_id",
            "anonymous.candidate_ref",
            "projection.version",
            "redaction.level",
            "profile.generalized_headline",
            "profile.generalized_role_family",
            "profile.generalized_seniority_band",
            "profile.generalized_location_region",
            "summary.safe_summary",
            "summary.safe_skill_summary",
            "summary.safe_evidence_placeholders",
            "summary.safe_match_narrative_placeholders"));
  }

  private static Set<Class<?>> recordComponentTypes(Class<?> type) {
    return Stream.of(type.getRecordComponents())
        .map(RecordComponent::getType)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static Set<String> recordComponentTypeNames(Class<?> type) {
    return Stream.of(type.getRecordComponents())
        .map(component -> component.getGenericType().getTypeName())
        .collect(java.util.stream.Collectors.toSet());
  }

  private static List<Path> consentDisclosureProductionFiles() throws IOException {
    Path packageRoot = projectPath(
        "src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure");
    try (Stream<Path> stream = Files.walk(packageRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .toList();
    }
  }

  private static List<Path> findConsentDisclosureUiFiles() throws IOException {
    Path appsRoot = projectPath("../../apps").normalize();
    if (!Files.exists(appsRoot)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(appsRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> containsAny(
              normalized(path.getFileName().toString()),
              "consentdisclosure",
              "disclosureunlock",
              "unlockdecision"))
          .toList();
    }
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
  }

  private static boolean containsAny(String value, String... needles) {
    for (String needle : needles) {
      if (value.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
