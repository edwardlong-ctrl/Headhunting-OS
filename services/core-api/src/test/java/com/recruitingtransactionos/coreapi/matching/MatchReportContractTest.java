package com.recruitingtransactionos.coreapi.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MatchReportContractTest {

  @Test
  void matchReportCanBeCreatedWithOpaqueRefsAndNoRawCandidateProfile() {
    MatchReport report = validReport();

    assertThat(report.matchReportId().value()).startsWith("match_report_");
    assertThat(report.jobRef().value()).startsWith("job_ref_");
    assertThat(report.candidateCardRef().value()).startsWith("match_subject_");
    assertThat(report.overallScore()).isEqualTo(MatchScore.of(4));
    assertThat(report.dimensionScores()).containsKeys(MatchDimension.values());
    assertThat(report.scoreConfidence()).isEqualTo(ScoreConfidence.MEDIUM);
    assertThat(report.evidenceCoverage().coverageRatio()).isEqualTo(0.72);
    assertThat(report.provenanceSummary().provenanceWeight())
        .isEqualTo(ProvenanceWeight.of(0.65));
    assertThat(report.scoreCapDecision().reasonCode()).isEqualTo(ScoreCapReason.NONE);
    assertThat(report.isCanonicalFact()).isFalse();
    assertThat(report.isClientSafeApiOutput()).isFalse();
  }

  @Test
  void invalidOverallScoresAreRejected() {
    assertThatThrownBy(() -> MatchScore.of(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");

    assertThatThrownBy(() -> MatchScore.of(6))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");
  }

  @Test
  void invalidDimensionScoresAreRejected() {
    assertThatThrownBy(() -> MatchScore.of(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");

    assertThatThrownBy(() -> MatchScore.of(8))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("match score must be between 1 and 5");
  }

  @Test
  void allRequiredDimensionsCanBeRepresented() {
    assertThat(MatchDimension.values())
        .containsExactly(
            MatchDimension.TECHNICAL_FIT,
            MatchDimension.INDUSTRY_FIT,
            MatchDimension.SENIORITY_FIT,
            MatchDimension.SALARY_FIT,
            MatchDimension.LOCATION_FIT,
            MatchDimension.MOTIVATION_FIT,
            MatchDimension.AVAILABILITY_FIT,
            MatchDimension.EVIDENCE_STRENGTH,
            MatchDimension.CULTURE_OR_MANAGER_FIT);

    assertThat(validReport().dimensionScores()).containsKeys(MatchDimension.values());
  }

  @Test
  void scoreConfidenceVocabularyIsExplicit() {
    assertThat(ScoreConfidence.values())
        .containsExactly(
            ScoreConfidence.LOW,
            ScoreConfidence.MEDIUM,
            ScoreConfidence.HIGH);

    assertThat(ScoreConfidence.LOW.wireValue()).isEqualTo("low");
    assertThat(ScoreConfidence.MEDIUM.wireValue()).isEqualTo("medium");
    assertThat(ScoreConfidence.HIGH.wireValue()).isEqualTo("high");
  }

  @Test
  void evidenceCoverageIsBounded() {
    EvidenceCoverage coverage = new EvidenceCoverage(
        0.5,
        EvidenceCoverageLevel.MEDIUM,
        3,
        2);

    assertThat(coverage.coverageRatio()).isEqualTo(0.5);
    assertThat(coverage.independentEvidenceCount()).isEqualTo(3);
    assertThat(coverage.independentHighTrustEvidenceCount()).isEqualTo(2);

    assertThatThrownBy(() -> new EvidenceCoverage(-0.01, EvidenceCoverageLevel.LOW, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evidence coverage ratio must be between 0.0 and 1.0");
    assertThatThrownBy(() -> new EvidenceCoverage(1.01, EvidenceCoverageLevel.HIGH, 1, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evidence coverage ratio must be between 0.0 and 1.0");
    assertThatThrownBy(() -> new EvidenceCoverage(0.5, EvidenceCoverageLevel.MEDIUM, 1, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("independentHighTrustEvidenceCount must not exceed independentEvidenceCount");
  }

  @Test
  void opaqueRefsRejectRawUuidCandidateOrJobIdentifiers() {
    String rawUuid = "00000000-0000-0000-0000-00000011a001";

    assertThatThrownBy(() -> MatchReportId.of(rawUuid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("matchReportId must not be a raw UUID");
    assertThatThrownBy(() -> MatchJobRef.of(rawUuid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("jobRef must not be a raw UUID");
    assertThatThrownBy(() -> MatchSubjectRef.of(rawUuid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidateCardRef must not be a raw UUID");
  }

  @Test
  void publicMatchReportContractSurfaceDoesNotExposeRawInternalTypes() {
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
        AITaskRunRecord.class);
    Set<Class<?>> publicOutputTypes = Set.of(
        MatchReport.class,
        MatchReportId.class,
        MatchJobRef.class,
        MatchSubjectRef.class,
        MatchScore.class,
        EvidenceCoverage.class,
        ProvenanceSummary.class,
        ProvenanceWeight.class,
        ScoreCapDecision.class,
        ScoreCapPolicyRequest.class);

    for (Class<?> outputType : publicOutputTypes) {
      assertThat(outputType.getRecordComponents())
          .as(outputType.getSimpleName())
          .extracting(RecordComponent::getType)
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
              "AITaskRun"));
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
              "consultantNote"));
    }
  }

  private static MatchReport validReport() {
    return new MatchReport(
        MatchReportId.of("match_report_task11a_0001"),
        MatchJobRef.of("job_ref_task11a_0001"),
        MatchSubjectRef.of("match_subject_task11a_0001"),
        MatchScore.of(4),
        allDimensionScores(4),
        ScoreConfidence.MEDIUM,
        new EvidenceCoverage(0.72, EvidenceCoverageLevel.HIGH, 4, 2),
        new ProvenanceSummary(
            ProvenanceCategory.CONSULTANT_DEEP_DIVE,
            ProvenanceSourceStrength.HIGH_TRUST,
            ProvenanceWeight.of(0.65),
            EvidenceAssertionStrength.EXPLICIT,
            AuthenticityRiskLevel.LOW),
        new ScoreCapDecision(
            MatchScore.of(4),
            MatchScore.of(4),
            false,
            ScoreCapReason.NONE,
            "No score cap was applied by the current metadata policy.",
            false,
            false,
            false),
        "ontology-semiconductor-v0-placeholder",
        "industry-pack-semiconductor-v0-placeholder",
        Instant.parse("2026-04-29T00:00:00Z"));
  }

  private static Map<MatchDimension, MatchScore> allDimensionScores(int score) {
    EnumMap<MatchDimension, MatchScore> scores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      scores.put(dimension, MatchScore.of(score));
    }
    return scores;
  }

  private static List<String> componentNames(Class<?> type) {
    return List.of(type.getRecordComponents()).stream()
        .map(RecordComponent::getName)
        .toList();
  }

  private static List<String> componentTypeNames(Class<?> type) {
    return List.of(type.getRecordComponents()).stream()
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
}
