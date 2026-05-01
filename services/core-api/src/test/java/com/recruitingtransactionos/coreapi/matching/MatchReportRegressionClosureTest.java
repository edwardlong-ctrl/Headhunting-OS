package com.recruitingtransactionos.coreapi.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ClientSafeCandidateCardResponse;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForReview;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteGate;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MatchReportRegressionClosureTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-04-29T10:30:00Z");
  private static final String RAW_UUID = "00000000-0000-0000-0000-0000001100c1";
  private static final String FORBIDDEN_FULL_NAME = "Jane Task11C Candidate";
  private static final String FORBIDDEN_EMAIL = "jane.task11c@example.com";
  private static final String FORBIDDEN_PHONE = "+86 138 0000 11C";
  private static final String FORBIDDEN_EXACT_EMPLOYER = "NebulaChip Systems";
  private static final String FORBIDDEN_PROJECT = "Orion-X7";
  private static final String FORBIDDEN_RAW_SOURCE =
      "Raw source text: Jane Task11C Candidate led Orion-X7 at NebulaChip Systems.";
  private static final String FORBIDDEN_CONSULTANT_NOTE =
      "Consultant-private note: do not disclose active negotiation.";

  private final MatchReportGenerationService service = new MatchReportGenerationService();
  private final ScoreCapPolicy scoreCapPolicy = new ScoreCapPolicy();

  @Test
  void matchReportAndGenerationContractsStayOpaqueNonCanonicalNonClientSafeAndLeakFree() {
    MatchReportGenerationResult result =
        service.generate(validRequest(MatchScore.of(5), completeHighTrustEvidence()));
    MatchReport report = result.matchReport();

    assertThat(report.matchReportId().value()).startsWith("match_report_");
    assertThat(report.jobRef().value()).startsWith("job_ref_");
    assertThat(report.candidateCardRef().value()).startsWith("match_subject_");
    assertThat(report.isCanonicalFact()).isFalse();
    assertThat(report.isClientSafeApiOutput()).isFalse();
    assertThat(report.overallScore().value()).isBetween(1, 5);
    assertThat(report.dimensionScores()).containsOnlyKeys(MatchDimension.values());
    assertThat(report.dimensionScores().values())
        .allSatisfy(score -> assertThat(score.value()).isBetween(1, 5));

    assertSurfaceDoesNotExposeForbiddenTypes(
        Set.of(
            MatchReport.class,
            MatchReportGenerationRequest.class,
            MatchReportGenerationResult.class,
            MatchEvidenceSummary.class,
            EvidenceCoverageInput.class,
            MatchEvidenceSignal.class,
            ScoreCapDecision.class,
            ScoreCapPolicyRequest.class,
            ProvenanceSummary.class));
    assertSanitized(report.toString());
    assertSanitized(result.evidenceSummary().toString());
    assertSanitized(report.scoreCapDecision().safeExplanation());
  }

  @Test
  void generationAcceptsOnlyOpaqueRefsAndSafeScoreEvidenceProvenanceMetadata() {
    Method generate = publicGenerateMethod();

    assertThat(generate.getParameterTypes())
        .containsExactly(MatchReportGenerationRequest.class);
    assertThat(generate.getReturnType()).isEqualTo(MatchReportGenerationResult.class);
    assertThat(recordComponentTypeNames(MatchReportGenerationRequest.class))
        .contains(
            MatchReportId.class.getName(),
            MatchJobRef.class.getName(),
            MatchSubjectRef.class.getName(),
            MatchScore.class.getName(),
            EvidenceCoverageInput.class.getName(),
            IndustryPackMaturity.class.getName(),
            EvidenceAssertionStrength.class.getName(),
            AuthenticityRiskLevel.class.getName(),
            ReidentificationRiskSignal.class.getName())
        .noneMatch(name -> containsAny(
            name,
            "CandidateProfile",
            "SourceItem",
            "InformationPacket",
            "ClaimLedger",
            "ReviewEvent",
            "WorkflowEvent",
            "CanonicalWrite",
            "ApiSafeResponseBody",
            "ClientSafeCandidateCardResponse"));

    assertThatThrownBy(() -> MatchReportId.of(RAW_UUID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("matchReportId must not be a raw UUID");
    assertThatThrownBy(() -> MatchJobRef.of(RAW_UUID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("jobRef must not be a raw UUID");
    assertThatThrownBy(() -> MatchSubjectRef.of(RAW_UUID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidateCardRef must not be a raw UUID");
    assertThatThrownBy(() -> MatchSubjectRef.of("card_task11c_0001"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidateCardRef must use the match_subject_ prefix");

    assertThatThrownBy(() -> service.generate(validRequest(
            MatchScore.of(4),
            evidenceWithUnknownProvenance())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provenanceCategory UNKNOWN is not allowed for generation");
    assertThatThrownBy(() -> service.generate(validRequest(
            MatchScore.of(4),
            evidenceWithUnknownAssertion())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("assertionStrength UNKNOWN is not allowed for generation");
    assertThatThrownBy(() -> service.generate(validRequest(
            MatchScore.of(4),
            completeHighTrustEvidence(),
            IndustryPackMaturity.CALIBRATED,
            false,
            true,
            EvidenceAssertionStrength.EXPLICIT,
            false,
            false,
            AuthenticityRiskLevel.UNKNOWN,
            ReidentificationRiskSignal.LOW)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("authenticityRisk UNKNOWN is not allowed for generation");
    assertThatThrownBy(() -> service.generate(validRequest(
            MatchScore.of(4),
            completeHighTrustEvidence(),
            IndustryPackMaturity.CALIBRATED,
            false,
            true,
            EvidenceAssertionStrength.EXPLICIT,
            false,
            false,
            AuthenticityRiskLevel.LOW,
            ReidentificationRiskSignal.UNKNOWN)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reidentificationRiskSignal UNKNOWN is not allowed for generation");
  }

  @Test
  void scoreEvidenceCoverageAndProvenanceRemainBoundedDeterministicAndConservative() {
    MatchReportGenerationResult complete =
        service.generate(validRequest(MatchScore.of(5), completeHighTrustEvidence()));
    MatchReportGenerationResult repeated =
        service.generate(validRequest(MatchScore.of(5), completeHighTrustEvidence()));

    assertThat(complete.matchReport()).isEqualTo(repeated.matchReport());
    assertThat(complete.evidenceSummary()).isEqualTo(repeated.evidenceSummary());
    assertThat(complete.matchReport().overallScore()).isEqualTo(MatchScore.of(5));
    assertThat(complete.matchReport().scoreConfidence()).isEqualTo(ScoreConfidence.HIGH);
    assertThat(complete.evidenceSummary().requiredDimensions())
        .containsExactly(MatchDimension.values());
    assertThat(complete.evidenceSummary().coveredDimensions())
        .containsExactly(MatchDimension.values());
    assertThat(complete.evidenceSummary().missingEvidenceDimensions()).isEmpty();
    assertThat(complete.evidenceSummary().evidenceCoverage().coverageRatio()).isEqualTo(1.0);
    assertThat(complete.evidenceSummary().evidenceCoverage().coverageLevel())
        .isEqualTo(EvidenceCoverageLevel.COMPLETE);

    MatchReportGenerationResult partial =
        service.generate(validRequest(MatchScore.of(4), partialEvidence()));
    assertThat(partial.evidenceSummary().evidenceCoverage().coverageRatio())
        .isCloseTo(3.0 / MatchDimension.values().length, offset(0.000001));
    assertThat(partial.evidenceSummary().evidenceCoverage().coverageRatio())
        .isLessThan(complete.evidenceSummary().evidenceCoverage().coverageRatio());
    assertThat(partial.evidenceSummary().missingEvidenceDimensions())
        .contains(
            MatchDimension.SENIORITY_FIT,
            MatchDimension.SALARY_FIT,
            MatchDimension.LOCATION_FIT,
            MatchDimension.AVAILABILITY_FIT,
            MatchDimension.EVIDENCE_STRENGTH,
            MatchDimension.CULTURE_OR_MANAGER_FIT);
    assertThat(partial.matchReport().scoreConfidence()).isEqualTo(ScoreConfidence.LOW);

    MatchReport weakOnly = service.generate(validRequest(
        MatchScore.of(5),
        completeWeakSignalEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.WEAK_SIGNAL,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW)).matchReport();
    assertThat(weakOnly.overallScore()).isEqualTo(MatchScore.of(3));
    assertThat(weakOnly.scoreConfidence()).isEqualTo(ScoreConfidence.LOW);
    assertThat(weakOnly.provenanceSummary().strongestSourceStrength())
        .isEqualTo(ProvenanceSourceStrength.LOW_TRUST);

    MatchReport aiSystemWeakOnly = service.generate(validRequest(
        MatchScore.of(5),
        aiSystemAndWeakSignalEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW)).matchReport();
    assertThat(aiSystemWeakOnly.overallScore()).isEqualTo(MatchScore.of(4));
    assertThat(aiSystemWeakOnly.scoreConfidence()).isEqualTo(ScoreConfidence.LOW);
    assertThat(aiSystemWeakOnly.provenanceSummary().strongestSourceStrength())
        .isEqualTo(ProvenanceSourceStrength.LOW_TRUST);
    assertThat(aiSystemWeakOnly.isCanonicalFact()).isFalse();
  }

  @Test
  void scoreCapPolicyAppliesRequiredCapsAndKeepsReasonsSafe() {
    List<CapCase> cases = List.of(
        new CapCase(
            "cold industry pack",
            basePolicyRequest(MatchScore.of(5))
                .industryPackMaturity(IndustryPackMaturity.COLD)
                .build(),
            MatchScore.of(3),
            ScoreCapReason.COLD_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "keyword only without project evidence",
            basePolicyRequest(MatchScore.of(5))
                .keywordOnlyEvidence(true)
                .projectEvidencePresent(false)
                .build(),
            MatchScore.of(3),
            ScoreCapReason.KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE,
            false,
            true,
            false),
        new CapCase(
            "weak intent signal",
            basePolicyRequest(MatchScore.of(5))
                .candidateIntentSignalStrength(EvidenceAssertionStrength.WEAK_SIGNAL)
                .build(),
            MatchScore.of(3),
            ScoreCapReason.WEAK_SIGNAL_INTENT_ONLY,
            false,
            true,
            false),
        new CapCase(
            "stale ontology",
            basePolicyRequest(MatchScore.of(5))
                .ontologyStale(true)
                .build(),
            MatchScore.of(4),
            ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "stale industry pack",
            basePolicyRequest(MatchScore.of(5))
                .industryPackVersionStale(true)
                .build(),
            MatchScore.of(4),
            ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "insufficient independent high-trust evidence",
            basePolicyRequest(MatchScore.of(5))
                .evidenceCoverage(new EvidenceCoverage(1.0, EvidenceCoverageLevel.COMPLETE, 8, 1))
                .build(),
            MatchScore.of(4),
            ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE,
            false,
            true,
            false),
        new CapCase(
            "high authenticity risk",
            basePolicyRequest(MatchScore.of(5))
                .authenticityRisk(AuthenticityRiskLevel.HIGH)
                .build(),
            MatchScore.of(4),
            ScoreCapReason.HIGH_AUTHENTICITY_RISK,
            true,
            true,
            false),
        new CapCase(
            "high re-identification risk",
            basePolicyRequest(MatchScore.of(5))
                .reidentificationRiskSignal(ReidentificationRiskSignal.HIGH)
                .build(),
            MatchScore.of(5),
            ScoreCapReason.HIGH_REIDENTIFICATION_RISK,
            true,
            false,
            true));

    for (CapCase capCase : cases) {
      ScoreCapDecision decision = scoreCapPolicy.decide(capCase.request());

      assertThat(decision.cappedScore()).as(capCase.name()).isEqualTo(capCase.expectedScore());
      assertThat(decision.reasonCode()).as(capCase.name()).isEqualTo(capCase.expectedReason());
      assertThat(decision.humanReviewRequired())
          .as(capCase.name())
          .isEqualTo(capCase.humanReviewRequired());
      assertThat(decision.additionalEvidenceRequired())
          .as(capCase.name())
          .isEqualTo(capCase.additionalEvidenceRequired());
      assertThat(decision.clientDeliveryBlocked())
          .as(capCase.name())
          .isEqualTo(capCase.clientDeliveryBlocked());
      assertSanitized(decision.safeExplanation());
    }
  }

  @Test
  void generationAppliesScoreCapBeforeReturnAndFinalDimensionsCannotExceedFinalScore() {
    MatchReport report = service.generate(validRequest(
        MatchScore.of(5),
        completeHighTrustEvidence(),
        IndustryPackMaturity.COLD,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW)).matchReport();

    assertThat(report.overallScore()).isEqualTo(MatchScore.of(3));
    assertThat(report.scoreCapDecision().cappedScore()).isEqualTo(MatchScore.of(3));
    assertThat(report.scoreCapDecision().reasonCode()).isEqualTo(ScoreCapReason.COLD_INDUSTRY_PACK);
    assertThat(report.dimensionScores().values())
        .allSatisfy(score -> assertThat(score.value()).isLessThanOrEqualTo(
            report.overallScore().value()));
  }

  @Test
  void task11MatchingPackageAddsNoAiPersistenceCanonicalMutationGovernanceWriteApiOrUi()
      throws IOException {
    List<Path> productionFiles = matchingProductionFiles();

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
              "model_router",
              "modelRouting",
              "AITaskRunService",
              "ExecutorService",
              "CompletableFuture",
              "TaskScheduler",
              "BlockingQueue",
              "Kafka",
              "Rabbit",
              "CanonicalWriteService",
              "CanonicalWriteGate",
              "CanonicalWriteCommand",
              "CandidateProfileService",
              "CandidateProfilePersistencePort",
              "upsertCandidateProfileField",
              "candidate_profile",
              "ClaimLedgerService",
              "ClaimLedgerAppendCommand",
              "ReviewEventService",
              "ReviewEventAppendCommand",
              "WorkflowEventService",
              "WorkflowEventAppendCommand");
    }

    assertThat(matchingMigrationFiles()).isEmpty();
    assertThat(findMatchingUiFiles()).isEmpty();
  }

  private static void assertSurfaceDoesNotExposeForbiddenTypes(Set<Class<?>> surfaceTypes) {
    Set<Class<?>> forbiddenTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimId.class,
        ClaimLedgerAppendCommand.class,
        ClaimLedgerItemForCanonicalWrite.class,
        ClaimLedgerItemForReview.class,
        ReviewEventAppendCommand.class,
        ReviewEventForCanonicalWrite.class,
        ReviewEventId.class,
        WorkflowEventAppendCommand.class,
        WorkflowEventId.class,
        AITaskRunAppendCommand.class,
        AITaskRunRecord.class,
        CanonicalWriteGate.class,
        CanonicalWriteService.class,
        ApiSafeResponseBody.class,
        ClientSafeCandidateCardResponse.class);

    for (Class<?> surfaceType : surfaceTypes) {
      assertThat(recordComponentTypes(surfaceType))
          .as(surfaceType.getSimpleName())
          .doesNotContainAnyElementsOf(forbiddenTypes);
      assertThat(recordComponentTypeNames(surfaceType))
          .as(surfaceType.getSimpleName())
          .noneMatch(name -> containsAny(
              name,
              "CandidateProfile",
              "SourceItem",
              "InformationPacket",
              "ClaimLedger",
              "ReviewEvent",
              "WorkflowEvent",
              "AITaskRun",
              "CanonicalWrite",
              "ApiSafeResponseBody",
              "ClientSafeCandidateCardResponse"));
      assertThat(recordComponentNames(surfaceType))
          .as(surfaceType.getSimpleName())
          .noneMatch(name -> containsAny(
              name,
              "rawCandidate",
              "candidateProfile",
              "sourceItem",
              "informationPacket",
              "claimLedger",
              "reviewEvent",
              "workflowEvent",
              "rawSource",
              "sourceText",
              "pii",
              "consultantNote",
              "governanceRecord",
              "internalAudit"));
    }
  }

  private static Method publicGenerateMethod() {
    List<Method> methods = Stream.of(MatchReportGenerationService.class.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .filter(method -> method.getName().equals("generate"))
        .toList();
    assertThat(methods).hasSize(1);
    return methods.get(0);
  }

  private static MatchReportGenerationRequest validRequest(
      MatchScore requestedScore,
      EvidenceCoverageInput evidenceCoverageInput) {
    return validRequest(
        requestedScore,
        evidenceCoverageInput,
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW);
  }

  private static MatchReportGenerationRequest validRequest(
      MatchScore requestedScore,
      EvidenceCoverageInput evidenceCoverageInput,
      IndustryPackMaturity industryPackMaturity,
      boolean keywordOnlyEvidence,
      boolean projectEvidencePresent,
      EvidenceAssertionStrength candidateIntentSignalStrength,
      boolean ontologyStale,
      boolean industryPackVersionStale,
      AuthenticityRiskLevel authenticityRisk,
      ReidentificationRiskSignal reidentificationRiskSignal) {
    return new MatchReportGenerationRequest(
        MatchReportId.of("match_report_task11c_0001"),
        MatchJobRef.of("job_ref_task11c_0001"),
        MatchSubjectRef.of("match_subject_task11c_0001"),
        requestedScore,
        allDimensionScores(requestedScore.value()),
        evidenceCoverageInput,
        industryPackMaturity,
        keywordOnlyEvidence,
        projectEvidencePresent,
        candidateIntentSignalStrength,
        ontologyStale,
        industryPackVersionStale,
        authenticityRisk,
        reidentificationRiskSignal,
        "ontology-semiconductor-v0-placeholder",
        "industry-pack-semiconductor-v0-placeholder",
        GENERATED_AT);
  }

  private static EvidenceCoverageInput completeHighTrustEvidence() {
    List<MatchEvidenceSignal> signals = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      signals.add(new MatchEvidenceSignal(
          dimension,
          dimension == MatchDimension.MOTIVATION_FIT
              ? ProvenanceCategory.CANDIDATE_CONFIRMED
              : ProvenanceCategory.EXTERNAL_VERIFIED,
          EvidenceAssertionStrength.EXPLICIT,
          true));
    }
    return evidenceInput(signals);
  }

  private static EvidenceCoverageInput partialEvidence() {
    return evidenceInput(List.of(
        new MatchEvidenceSignal(
            MatchDimension.TECHNICAL_FIT,
            ProvenanceCategory.EXTERNAL_VERIFIED,
            EvidenceAssertionStrength.EXPLICIT,
            true),
        new MatchEvidenceSignal(
            MatchDimension.INDUSTRY_FIT,
            ProvenanceCategory.EXTERNAL_VERIFIED,
            EvidenceAssertionStrength.EXPLICIT,
            true),
        new MatchEvidenceSignal(
            MatchDimension.MOTIVATION_FIT,
            ProvenanceCategory.CONSULTANT_ATTESTED,
            EvidenceAssertionStrength.EXPLICIT,
            true)));
  }

  private static EvidenceCoverageInput completeWeakSignalEvidence() {
    List<MatchEvidenceSignal> signals = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      signals.add(new MatchEvidenceSignal(
          dimension,
          ProvenanceCategory.WEAK_SIGNAL,
          EvidenceAssertionStrength.WEAK_SIGNAL,
          true));
    }
    return evidenceInput(signals);
  }

  private static EvidenceCoverageInput aiSystemAndWeakSignalEvidence() {
    List<MatchEvidenceSignal> signals = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      ProvenanceCategory category = switch (dimension.ordinal() % 3) {
        case 0 -> ProvenanceCategory.AI_EXTRACTED;
        case 1 -> ProvenanceCategory.SYSTEM_INFERENCE;
        default -> ProvenanceCategory.WEAK_SIGNAL;
      };
      signals.add(new MatchEvidenceSignal(
          dimension,
          category,
          EvidenceAssertionStrength.IMPLIED,
          true));
    }
    return evidenceInput(signals);
  }

  private static EvidenceCoverageInput evidenceWithUnknownProvenance() {
    return evidenceInput(List.of(new MatchEvidenceSignal(
        MatchDimension.TECHNICAL_FIT,
        ProvenanceCategory.UNKNOWN,
        EvidenceAssertionStrength.EXPLICIT,
        true)));
  }

  private static EvidenceCoverageInput evidenceWithUnknownAssertion() {
    return evidenceInput(List.of(new MatchEvidenceSignal(
        MatchDimension.TECHNICAL_FIT,
        ProvenanceCategory.EXTERNAL_VERIFIED,
        EvidenceAssertionStrength.UNKNOWN,
        true)));
  }

  private static EvidenceCoverageInput evidenceInput(List<MatchEvidenceSignal> signals) {
    return new EvidenceCoverageInput(EnumSet.allOf(MatchDimension.class), signals);
  }

  private static Map<MatchDimension, MatchScore> allDimensionScores(int score) {
    EnumMap<MatchDimension, MatchScore> scores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      scores.put(dimension, MatchScore.of(score));
    }
    return scores;
  }

  private static ScoreCapPolicyRequest.Builder basePolicyRequest(MatchScore proposedScore) {
    return ScoreCapPolicyRequest.builder(proposedScore)
        .evidenceCoverage(new EvidenceCoverage(1.0, EvidenceCoverageLevel.COMPLETE, 9, 3))
        .industryPackMaturity(IndustryPackMaturity.CALIBRATED)
        .keywordOnlyEvidence(false)
        .projectEvidencePresent(true)
        .candidateIntentSignalStrength(EvidenceAssertionStrength.EXPLICIT)
        .ontologyStale(false)
        .industryPackVersionStale(false)
        .authenticityRisk(AuthenticityRiskLevel.LOW)
        .reidentificationRiskSignal(ReidentificationRiskSignal.LOW);
  }

  private static List<Class<?>> recordComponentTypes(Class<?> type) {
    RecordComponent[] components = type.getRecordComponents();
    if (components == null) {
      return List.of();
    }
    return List.of(components).stream()
        .map(RecordComponent::getType)
        .toList();
  }

  private static List<String> recordComponentTypeNames(Class<?> type) {
    RecordComponent[] components = type.getRecordComponents();
    if (components == null) {
      return List.of();
    }
    return List.of(components).stream()
        .map(component -> component.getGenericType().getTypeName())
        .toList();
  }

  private static List<String> recordComponentNames(Class<?> type) {
    RecordComponent[] components = type.getRecordComponents();
    if (components == null) {
      return List.of();
    }
    return List.of(components).stream()
        .map(RecordComponent::getName)
        .toList();
  }

  private static List<Path> matchingProductionFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi/matching");
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static List<Path> matchingMigrationFiles() throws IOException {
    Path migrationRoot = projectPath("src/main/resources/db/migration");
    if (!Files.exists(migrationRoot)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(migrationRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".sql"))
          .filter(path -> {
              String content = readUnchecked(path).toLowerCase(Locale.ROOT);
              return content.contains("score_cap")
                  || content.contains("create schema matching")
                  || content.contains("create table matching.")
                  || content.contains("alter table matching.");
          })
          .toList();
    }
  }

  private static List<Path> apiBoundaryFilesMentioningMatchReport() throws IOException {
    Path apiBoundaryRoot =
        projectPath("src/main/java/com/recruitingtransactionos/coreapi/apiboundary");
    if (!Files.exists(apiBoundaryRoot)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(apiBoundaryRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> containsAny(readUnchecked(path), "MatchReport", "ScoreCapDecision"))
          .toList();
    }
  }

  private static List<Path> findMatchingUiFiles() throws IOException {
    Path appsRoot = projectPath("../../apps").normalize();
    if (!Files.exists(appsRoot)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(appsRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> containsAny(
              normalized(path.getFileName().toString()),
              "matchreport",
              "matchreportgeneration",
              "scorecap"))
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

  private static String readUnchecked(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("failed to read " + path, exception);
    }
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

  private static void assertSanitized(String value) {
    assertThat(value)
        .doesNotContain(FORBIDDEN_FULL_NAME)
        .doesNotContain(FORBIDDEN_EMAIL)
        .doesNotContain(FORBIDDEN_PHONE)
        .doesNotContain(FORBIDDEN_EXACT_EMPLOYER)
        .doesNotContain(FORBIDDEN_PROJECT)
        .doesNotContain(FORBIDDEN_RAW_SOURCE)
        .doesNotContain(FORBIDDEN_CONSULTANT_NOTE)
        .doesNotContain("raw source text")
        .doesNotContain("consultant-private note")
        .doesNotContain("consultant note")
        .doesNotContain("internal governance")
        .doesNotContain("internal audit")
        .doesNotContain("stack trace")
        .doesNotContain("java.lang")
        .doesNotContain("\tat ")
        .doesNotContain("candidate_profile");
  }

  private record CapCase(
      String name,
      ScoreCapPolicyRequest request,
      MatchScore expectedScore,
      ScoreCapReason expectedReason,
      boolean humanReviewRequired,
      boolean additionalEvidenceRequired,
      boolean clientDeliveryBlocked) {
  }
}
