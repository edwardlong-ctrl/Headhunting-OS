package com.recruitingtransactionos.coreapi.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ScoreCapPolicyTest {

  private final ScoreCapPolicy policy = new ScoreCapPolicy();

  @Test
  void scoreCapReasonVocabularyIsExplicit() {
    assertThat(ScoreCapReason.values())
        .containsExactly(
            ScoreCapReason.NONE,
            ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE,
            ScoreCapReason.COLD_INDUSTRY_PACK,
            ScoreCapReason.KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE,
            ScoreCapReason.WEAK_SIGNAL_INTENT_ONLY,
            ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK,
            ScoreCapReason.HIGH_AUTHENTICITY_RISK,
            ScoreCapReason.HIGH_REIDENTIFICATION_RISK,
            ScoreCapReason.POLICY_INPUT_REQUIRED);

    assertThat(ScoreCapReason.COLD_INDUSTRY_PACK.wireValue())
        .isEqualTo("cold_industry_pack");
  }

  @Test
  void capsColdIndustryPackScoreToMaxThree() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .industryPackMaturity(IndustryPackMaturity.COLD)
        .build());

    assertThat(decision.cappedScore()).isEqualTo(MatchScore.of(3));
    assertThat(decision.capApplied()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo(ScoreCapReason.COLD_INDUSTRY_PACK);
    assertThat(decision.additionalEvidenceRequired()).isTrue();
  }

  @Test
  void capsKeywordOnlyEvidenceWithoutProjectEvidenceToMaxThree() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .keywordOnlyEvidence(true)
        .projectEvidencePresent(false)
        .build());

    assertThat(decision.cappedScore()).isEqualTo(MatchScore.of(3));
    assertThat(decision.capApplied()).isTrue();
    assertThat(decision.reasonCode())
        .isEqualTo(ScoreCapReason.KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE);
    assertThat(decision.additionalEvidenceRequired()).isTrue();
  }

  @Test
  void capsWeakSignalCandidateIntentToMaxThree() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .candidateIntentSignalStrength(EvidenceAssertionStrength.WEAK_SIGNAL)
        .build());

    assertThat(decision.cappedScore()).isEqualTo(MatchScore.of(3));
    assertThat(decision.capApplied()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo(ScoreCapReason.WEAK_SIGNAL_INTENT_ONLY);
    assertThat(decision.additionalEvidenceRequired()).isTrue();
  }

  @Test
  void capsStaleOntologyOrIndustryPackToMaxFour() {
    ScoreCapDecision ontologyDecision = policy.decide(baseRequest(MatchScore.of(5))
        .ontologyStale(true)
        .build());
    ScoreCapDecision industryPackDecision = policy.decide(baseRequest(MatchScore.of(5))
        .industryPackVersionStale(true)
        .build());

    assertThat(ontologyDecision.cappedScore()).isEqualTo(MatchScore.of(4));
    assertThat(ontologyDecision.reasonCode())
        .isEqualTo(ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK);
    assertThat(industryPackDecision.cappedScore()).isEqualTo(MatchScore.of(4));
    assertThat(industryPackDecision.reasonCode())
        .isEqualTo(ScoreCapReason.STALE_ONTOLOGY_OR_INDUSTRY_PACK);
  }

  @Test
  void capsInsufficientIndependentHighTrustEvidenceToMaxFour() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .evidenceCoverage(new EvidenceCoverage(0.8, EvidenceCoverageLevel.HIGH, 3, 1))
        .build());

    assertThat(decision.cappedScore()).isEqualTo(MatchScore.of(4));
    assertThat(decision.capApplied()).isTrue();
    assertThat(decision.reasonCode())
        .isEqualTo(ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE);
    assertThat(decision.additionalEvidenceRequired()).isTrue();
  }

  @Test
  void highAuthenticityRiskRequiresReviewOrCapsTopScore() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .authenticityRisk(AuthenticityRiskLevel.HIGH)
        .build());

    assertThat(decision.cappedScore()).isEqualTo(MatchScore.of(4));
    assertThat(decision.capApplied()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo(ScoreCapReason.HIGH_AUTHENTICITY_RISK);
    assertThat(decision.humanReviewRequired()).isTrue();
    assertThat(decision.additionalEvidenceRequired()).isTrue();
  }

  @Test
  void highReidentificationRiskIsNotTreatedAsSafeClientDelivery() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(4))
        .reidentificationRiskSignal(ReidentificationRiskSignal.HIGH)
        .build());

    assertThat(decision.clientDeliveryBlocked()).isTrue();
    assertThat(decision.humanReviewRequired()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo(ScoreCapReason.HIGH_REIDENTIFICATION_RISK);
    assertThat(decision.safeExplanation()).contains("privacy review");
  }

  @Test
  void policyExplanationIsSafeAndDoesNotLeakRawSourcePiiOrInternalDetails() {
    ScoreCapDecision decision = policy.decide(baseRequest(MatchScore.of(5))
        .keywordOnlyEvidence(true)
        .projectEvidencePresent(false)
        .build());

    assertThat(decision.safeExplanation())
        .doesNotContain("Jane Alpha Candidate")
        .doesNotContain("jane.alpha@example.com")
        .doesNotContain("NebulaChip Systems")
        .doesNotContain("Orion-X7")
        .doesNotContain("raw source")
        .doesNotContain("consultant note")
        .doesNotContain("java.lang")
        .doesNotContain("Exception")
        .doesNotContain("\n")
        .doesNotContain("\tat ");
  }

  @Test
  void matchingPackageAddsNoAiModelCallCanonicalWritePersistenceApiControllerOrUi()
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
          .doesNotContain("javax.sql.DataSource")
          .doesNotContain("JdbcTemplate")
          .doesNotContain("Flyway")
          .doesNotContain("ChatClient")
          .doesNotContain("OpenAI")
          .doesNotContain("Anthropic")
          .doesNotContain("DeepSeek")
          .doesNotContain("PromptTemplate")
          .doesNotContain("ModelClient")
          .doesNotContain("AiClient")
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

  private static ScoreCapPolicyRequest.Builder baseRequest(MatchScore proposedScore) {
    return ScoreCapPolicyRequest.builder(proposedScore)
        .evidenceCoverage(new EvidenceCoverage(0.9, EvidenceCoverageLevel.HIGH, 4, 3))
        .industryPackMaturity(IndustryPackMaturity.CALIBRATED)
        .keywordOnlyEvidence(false)
        .projectEvidencePresent(true)
        .candidateIntentSignalStrength(EvidenceAssertionStrength.EXPLICIT)
        .ontologyStale(false)
        .industryPackVersionStale(false)
        .authenticityRisk(AuthenticityRiskLevel.LOW)
        .reidentificationRiskSignal(ReidentificationRiskSignal.LOW);
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

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }
}
