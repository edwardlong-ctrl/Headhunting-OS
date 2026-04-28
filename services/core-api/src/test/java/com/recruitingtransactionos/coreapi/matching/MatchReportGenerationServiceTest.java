package com.recruitingtransactionos.coreapi.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemForReview;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import java.io.IOException;
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

class MatchReportGenerationServiceTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-04-29T08:00:00Z");
  private static final String FORBIDDEN_FULL_NAME = "Jane Task11B Candidate";
  private static final String FORBIDDEN_EMAIL = "jane.task11b@example.com";
  private static final String FORBIDDEN_PHONE = "+86 138 0000 11B";
  private static final String FORBIDDEN_EXACT_EMPLOYER = "NebulaChip Systems";
  private static final String FORBIDDEN_PROJECT = "Orion-X7";
  private static final String FORBIDDEN_RAW_SOURCE =
      "Raw source text says Jane Task11B Candidate built Orion-X7 at NebulaChip Systems.";
  private static final String FORBIDDEN_CONSULTANT_NOTE =
      "Consultant note: do not disclose active negotiation.";

  private final MatchReportGenerationService service = new MatchReportGenerationService();

  @Test
  void validGenerationRequestCreatesMatchReportWithOpaqueRefsOnly() {
    MatchReportGenerationResult result =
        service.generate(validRequest(MatchScore.of(5), completeHighTrustEvidence()));

    MatchReport report = result.matchReport();
    assertThat(report.matchReportId().value()).startsWith("match_report_");
    assertThat(report.jobRef().value()).startsWith("job_ref_");
    assertThat(report.candidateCardRef().value()).startsWith("match_subject_");
    assertThat(report.overallScore()).isEqualTo(MatchScore.of(5));
    assertThat(report.dimensionScores()).containsOnlyKeys(MatchDimension.values());
    assertThat(report.scoreConfidence()).isEqualTo(ScoreConfidence.HIGH);
    assertThat(report.evidenceCoverage().coverageLevel()).isEqualTo(EvidenceCoverageLevel.COMPLETE);
    assertThat(report.provenanceSummary().strongestProvenanceCategory())
        .isEqualTo(ProvenanceCategory.EXTERNAL_VERIFIED);
    assertThat(report.scoreCapDecision().reasonCode()).isEqualTo(ScoreCapReason.NONE);
    assertThat(report.scoreCapDecision().safeExplanation()).doesNotContain("\n");
    assertThat(report.isCanonicalFact()).isFalse();
    assertThat(report.isClientSafeApiOutput()).isFalse();

    assertThat(result.evidenceSummary().missingEvidenceDimensions()).isEmpty();
    assertThat(result.evidenceSummary().weakSignalOnlyDimensions()).isEmpty();
  }

  @Test
  void generationRejectsMissingJobRef() {
    assertThatThrownBy(() -> service.generate(new MatchReportGenerationRequest(
            MatchReportId.of("match_report_task11b_missing_job"),
            null,
            MatchSubjectRef.of("match_subject_task11b_0001"),
            MatchScore.of(4),
            allDimensionScores(4),
            completeHighTrustEvidence(),
            IndustryPackMaturity.CALIBRATED,
            false,
            true,
            EvidenceAssertionStrength.EXPLICIT,
            false,
            false,
            AuthenticityRiskLevel.LOW,
            ReidentificationRiskSignal.LOW,
            "ontology-semiconductor-v0-placeholder",
            "industry-pack-semiconductor-v0-placeholder",
            GENERATED_AT)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("jobRef must not be null");
  }

  @Test
  void generationRejectsMissingCandidateCardOrSubjectRef() {
    assertThatThrownBy(() -> service.generate(new MatchReportGenerationRequest(
            MatchReportId.of("match_report_task11b_missing_subject"),
            MatchJobRef.of("job_ref_task11b_0001"),
            null,
            MatchScore.of(4),
            allDimensionScores(4),
            completeHighTrustEvidence(),
            IndustryPackMaturity.CALIBRATED,
            false,
            true,
            EvidenceAssertionStrength.EXPLICIT,
            false,
            false,
            AuthenticityRiskLevel.LOW,
            ReidentificationRiskSignal.LOW,
            "ontology-semiconductor-v0-placeholder",
            "industry-pack-semiconductor-v0-placeholder",
            GENERATED_AT)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("candidateCardRef must not be null");
  }

  @Test
  void generationRejectsInvalidRequestedScores() {
    assertThatThrownBy(() -> MatchScore.of(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");
    assertThatThrownBy(() -> MatchScore.of(6))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");
  }

  @Test
  void generationAppliesScoreCapPolicyBeforeReturningReport() {
    MatchReportGenerationResult result = service.generate(validRequest(
        MatchScore.of(5),
        completeLowTrustEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW));

    MatchReport report = result.matchReport();
    assertThat(report.overallScore()).isEqualTo(MatchScore.of(4));
    assertThat(report.scoreCapDecision().proposedScore()).isEqualTo(MatchScore.of(5));
    assertThat(report.scoreCapDecision().cappedScore()).isEqualTo(MatchScore.of(4));
    assertThat(report.scoreCapDecision().reasonCode())
        .isEqualTo(ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE);
    assertThat(report.scoreCapDecision().safeExplanation())
        .contains("Independent high-trust evidence");
    assertThat(report.dimensionScores().values())
        .allSatisfy(score -> assertThat(score.value()).isLessThanOrEqualTo(4));
  }

  @Test
  void generationIntegratesAllRequiredScoreCapScenarios() {
    List<CapCase> cases = List.of(
        new CapCase(
            "cold pack",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.COLD,
                false,
                true,
                EvidenceAssertionStrength.EXPLICIT,
                false,
                false,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(3),
            ScoreCapReason.COLD_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "keyword only",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                true,
                false,
                EvidenceAssertionStrength.EXPLICIT,
                false,
                false,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(3),
            ScoreCapReason.KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE,
            false,
            true,
            false),
        new CapCase(
            "weak intent",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                false,
                true,
                EvidenceAssertionStrength.WEAK_SIGNAL,
                false,
                false,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(3),
            ScoreCapReason.WEAK_SIGNAL_INTENT_ONLY,
            false,
            true,
            false),
        new CapCase(
            "stale ontology",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                false,
                true,
                EvidenceAssertionStrength.EXPLICIT,
                true,
                false,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(4),
            ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "stale industry pack",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                false,
                true,
                EvidenceAssertionStrength.EXPLICIT,
                false,
                true,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(4),
            ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK,
            false,
            true,
            false),
        new CapCase(
            "high authenticity",
            validRequest(
                MatchScore.of(5),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                false,
                true,
                EvidenceAssertionStrength.EXPLICIT,
                false,
                false,
                AuthenticityRiskLevel.HIGH,
                ReidentificationRiskSignal.LOW),
            MatchScore.of(4),
            ScoreCapReason.HIGH_AUTHENTICITY_RISK,
            true,
            true,
            false),
        new CapCase(
            "high reidentification",
            validRequest(
                MatchScore.of(4),
                completeHighTrustEvidence(),
                IndustryPackMaturity.CALIBRATED,
                false,
                true,
                EvidenceAssertionStrength.EXPLICIT,
                false,
                false,
                AuthenticityRiskLevel.LOW,
                ReidentificationRiskSignal.HIGH),
            MatchScore.of(4),
            ScoreCapReason.HIGH_REIDENTIFICATION_RISK,
            true,
            false,
            true));

    for (CapCase capCase : cases) {
      MatchReport report = service.generate(capCase.request()).matchReport();

      assertThat(report.overallScore()).as(capCase.name()).isEqualTo(capCase.expectedScore());
      assertThat(report.scoreCapDecision().reasonCode())
          .as(capCase.name())
          .isEqualTo(capCase.expectedReason());
      assertThat(report.scoreCapDecision().safeExplanation()).as(capCase.name()).isNotBlank();
      assertThat(report.scoreCapDecision().humanReviewRequired())
          .as(capCase.name())
          .isEqualTo(capCase.humanReviewRequired());
      assertThat(report.scoreCapDecision().additionalEvidenceRequired())
          .as(capCase.name())
          .isEqualTo(capCase.additionalEvidenceRequired());
      assertThat(report.scoreCapDecision().clientDeliveryBlocked())
          .as(capCase.name())
          .isEqualTo(capCase.clientDeliveryBlocked());
    }
  }

  @Test
  void evidenceCoverageIsBoundedAndDeterministic() {
    MatchReportGenerationResult result =
        service.generate(validRequest(MatchScore.of(4), partialEvidence()));

    MatchEvidenceSummary summary = result.evidenceSummary();
    assertThat(summary.evidenceCoverage().coverageRatio())
        .isCloseTo(3.0 / MatchDimension.values().length, offset(0.000001));
    assertThat(summary.evidenceCoverage().coverageLevel()).isEqualTo(EvidenceCoverageLevel.MEDIUM);
    assertThat(summary.evidenceCoverage().independentEvidenceCount()).isEqualTo(3);
    assertThat(summary.evidenceCoverage().independentHighTrustEvidenceCount()).isEqualTo(2);
    assertThat(summary.coveredDimensions())
        .containsExactly(
            MatchDimension.TECHNICAL_FIT,
            MatchDimension.INDUSTRY_FIT,
            MatchDimension.MOTIVATION_FIT);
  }

  @Test
  void missingEvidenceReducesCoverageAndConfidence() {
    MatchReportGenerationResult result =
        service.generate(validRequest(MatchScore.of(4), partialEvidence()));

    assertThat(result.evidenceSummary().missingEvidenceDimensions())
        .contains(
            MatchDimension.SENIORITY_FIT,
            MatchDimension.SALARY_FIT,
            MatchDimension.LOCATION_FIT,
            MatchDimension.AVAILABILITY_FIT,
            MatchDimension.EVIDENCE_STRENGTH,
            MatchDimension.CULTURE_OR_MANAGER_FIT);
    assertThat(result.matchReport().scoreConfidence()).isEqualTo(ScoreConfidence.LOW);
  }

  @Test
  void weakSignalOnlyEvidenceCannotSupportHighConfidenceFiveScore() {
    MatchReportGenerationResult result = service.generate(validRequest(
        MatchScore.of(5),
        completeWeakSignalEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.WEAK_SIGNAL,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW));

    MatchReport report = result.matchReport();
    assertThat(report.overallScore()).isEqualTo(MatchScore.of(3));
    assertThat(report.scoreConfidence()).isEqualTo(ScoreConfidence.LOW);
    assertThat(result.evidenceSummary().weakSignalOnlyDimensions())
        .containsExactly(MatchDimension.values());
    assertThat(report.provenanceSummary().strongestSourceStrength())
        .isEqualTo(ProvenanceSourceStrength.LOW_TRUST);
  }

  @Test
  void provenanceWeightingIsDeterministicAndDoesNotTurnAiOrSystemInferenceIntoFact() {
    MatchReportGenerationResult first = service.generate(validRequest(
        MatchScore.of(5),
        aiAndSystemInferenceEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW));
    MatchReportGenerationResult second = service.generate(validRequest(
        MatchScore.of(5),
        aiAndSystemInferenceEvidence(),
        IndustryPackMaturity.CALIBRATED,
        false,
        true,
        EvidenceAssertionStrength.EXPLICIT,
        false,
        false,
        AuthenticityRiskLevel.LOW,
        ReidentificationRiskSignal.LOW));

    assertThat(first.matchReport().provenanceSummary())
        .isEqualTo(second.matchReport().provenanceSummary());
    assertThat(first.matchReport().overallScore()).isEqualTo(MatchScore.of(4));
    assertThat(first.matchReport().scoreConfidence()).isEqualTo(ScoreConfidence.LOW);
    assertThat(first.matchReport().provenanceSummary().strongestProvenanceCategory())
        .isEqualTo(ProvenanceCategory.AI_EXTRACTED);
    assertThat(first.matchReport().provenanceSummary().strongestSourceStrength())
        .isEqualTo(ProvenanceSourceStrength.LOW_TRUST);
    assertThat(first.matchReport().isCanonicalFact()).isFalse();
  }

  @Test
  void unknownProvenanceFailsClosed() {
    assertThatThrownBy(() -> service.generate(validRequest(
            MatchScore.of(4),
            evidenceWithUnknownProvenance())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provenanceCategory UNKNOWN is not allowed for generation");
  }

  @Test
  void generationPublicSurfacesDoNotExposeRawCandidateProfileOrGovernanceTypes() {
    Set<Class<?>> forbiddenTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimLedgerAppendCommand.class,
        ClaimLedgerItemForCanonicalWrite.class,
        ClaimLedgerItemForReview.class,
        ReviewEventAppendCommand.class,
        ReviewEventForCanonicalWrite.class,
        WorkflowEventAppendCommand.class,
        AITaskRunAppendCommand.class,
        AITaskRunRecord.class,
        ApiSafeResponseBody.class);
    Set<Class<?>> publicSurfaceTypes = Set.of(
        MatchReportGenerationRequest.class,
        MatchReportGenerationResult.class,
        MatchEvidenceSummary.class,
        EvidenceCoverageInput.class,
        MatchEvidenceSignal.class,
        ProvenanceWeightingPolicy.class,
        MatchReportGenerationService.class);

    for (Class<?> outputType : publicSurfaceTypes) {
      assertThat(recordComponentTypes(outputType))
          .as(outputType.getSimpleName())
          .doesNotContainAnyElementsOf(forbiddenTypes);
      assertThat(componentTypeNames(outputType))
          .as(outputType.getSimpleName())
          .noneMatch(name -> containsAny(
              name,
              "CandidateProfile",
              "SourceItem",
              "InformationPacket",
              "ClaimLedger",
              "ReviewEvent",
              "WorkflowEvent",
              "AITaskRun",
              "ApiSafeResponseBody"));
      assertThat(componentNames(outputType))
          .as(outputType.getSimpleName())
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
              "auditData"));
    }
  }

  @Test
  void generatedMatchReportDoesNotExposeRawSourcePiiConsultantNotesOrInternalAuditData() {
    MatchReportGenerationResult result =
        service.generate(validRequest(MatchScore.of(5), completeHighTrustEvidence()));

    assertSanitized(result.matchReport().toString());
    assertSanitized(result.evidenceSummary().toString());
    assertSanitized(result.matchReport().scoreCapDecision().safeExplanation());
  }

  @Test
  void generatedMatchReportIsNotClientSafeApiOutput() {
    MatchReport report =
        service.generate(validRequest(MatchScore.of(4), completeHighTrustEvidence())).matchReport();

    assertThat(report.isClientSafeApiOutput()).isFalse();
  }

  @Test
  void generationAddsNoAiModelPersistenceCanonicalWriteProfileMutationGovernanceEventApiOrUi()
      throws IOException {
    List<Path> productionFiles = matchingProductionFiles();

    assertThat(productionFiles)
        .extracting(path -> path.getFileName().toString())
        .noneMatch(fileName -> fileName.endsWith("Controller.java"))
        .noneMatch(fileName -> fileName.endsWith("Repository.java"))
        .noneMatch(fileName -> fileName.endsWith("Port.java"))
        .noneMatch(fileName -> fileName.contains("Persistence"));

    for (Path file : productionFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@Service")
          .doesNotContain("@Repository")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework")
          .doesNotContain("ChatClient")
          .doesNotContain("OpenAI")
          .doesNotContain("Anthropic")
          .doesNotContain("DeepSeek")
          .doesNotContain("PromptTemplate")
          .doesNotContain("ModelClient")
          .doesNotContain("AiClient")
          .doesNotContain("RestTemplate")
          .doesNotContain("WebClient")
          .doesNotContain("JdbcTemplate")
          .doesNotContain("DataSource")
          .doesNotContain("Flyway")
          .doesNotContain("CanonicalWriteService")
          .doesNotContain("CanonicalWriteGate")
          .doesNotContain("CanonicalWriteCommand")
          .doesNotContain("CandidateProfileService")
          .doesNotContain("CandidateProfilePersistencePort")
          .doesNotContain("upsertCandidateProfileField")
          .doesNotContain("candidate_profile")
          .doesNotContain("ClaimLedgerService")
          .doesNotContain("ClaimLedgerAppendCommand")
          .doesNotContain("ReviewEventService")
          .doesNotContain("ReviewEventAppendCommand")
          .doesNotContain("WorkflowEventService")
          .doesNotContain("WorkflowEventAppendCommand");
    }

    assertThat(findMatchingUiFiles()).isEmpty();
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
        MatchReportId.of("match_report_task11b_0001"),
        MatchJobRef.of("job_ref_task11b_0001"),
        MatchSubjectRef.of("match_subject_task11b_0001"),
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

  private static EvidenceCoverageInput completeLowTrustEvidence() {
    List<MatchEvidenceSignal> signals = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      signals.add(new MatchEvidenceSignal(
          dimension,
          ProvenanceCategory.AI_EXTRACTED,
          EvidenceAssertionStrength.EXPLICIT,
          true));
    }
    return evidenceInput(signals);
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

  private static EvidenceCoverageInput aiAndSystemInferenceEvidence() {
    List<MatchEvidenceSignal> signals = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      signals.add(new MatchEvidenceSignal(
          dimension,
          dimension.ordinal() % 2 == 0
              ? ProvenanceCategory.AI_EXTRACTED
              : ProvenanceCategory.SYSTEM_INFERENCE,
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

  private static List<Path> findMatchingUiFiles() throws IOException {
    Path appsRoot = projectPath("../../apps").normalize();
    if (!Files.exists(appsRoot)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(appsRoot)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> normalized(path.getFileName().toString()).contains("matchreport")
              || normalized(path.getFileName().toString()).contains("scorecap"))
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

  private static List<String> componentNames(Class<?> type) {
    RecordComponent[] components = type.getRecordComponents();
    if (components == null) {
      return List.of();
    }
    return List.of(components).stream()
        .map(RecordComponent::getName)
        .toList();
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

  private static List<String> componentTypeNames(Class<?> type) {
    RecordComponent[] components = type.getRecordComponents();
    if (components == null) {
      return List.of();
    }
    return List.of(components).stream()
        .map(component -> component.getGenericType().getTypeName())
        .toList();
  }

  private static boolean containsAny(String value, String... needles) {
    for (String needle : needles) {
      if (value.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
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
        .doesNotContain("consultant note")
        .doesNotContain("internal audit")
        .doesNotContain("candidate_profile");
  }

  private record CapCase(
      String name,
      MatchReportGenerationRequest request,
      MatchScore expectedScore,
      ScoreCapReason expectedReason,
      boolean humanReviewRequired,
      boolean additionalEvidenceRequired,
      boolean clientDeliveryBlocked) {
  }
}
