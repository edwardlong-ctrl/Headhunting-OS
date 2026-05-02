package com.recruitingtransactionos.coreapi.consultantmatching.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.matching.AuthenticityRiskLevel;
import com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverage;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverageLevel;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.matching.MatchJobRef;
import com.recruitingtransactionos.coreapi.matching.MatchReport;
import com.recruitingtransactionos.coreapi.matching.MatchReportId;
import com.recruitingtransactionos.coreapi.matching.MatchScore;
import com.recruitingtransactionos.coreapi.matching.MatchSubjectRef;
import com.recruitingtransactionos.coreapi.matching.ProvenanceCategory;
import com.recruitingtransactionos.coreapi.matching.ProvenanceSourceStrength;
import com.recruitingtransactionos.coreapi.matching.ProvenanceSummary;
import com.recruitingtransactionos.coreapi.matching.ProvenanceWeight;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import com.recruitingtransactionos.coreapi.matching.ScoreCapDecision;
import com.recruitingtransactionos.coreapi.matching.ScoreCapReason;
import com.recruitingtransactionos.coreapi.matching.ScoreConfidence;
import com.recruitingtransactionos.coreapi.consultantmatching.StoredMatchReport;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcMatchReportPersistencePort implements MatchReportPersistencePort {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.match_report (
        match_report_id, organization_id, job_id, candidate_id, shortlist_candidate_card_id,
        subject_type, match_subject_ref, proposed_score, overall_score, score_confidence,
        cap_applied, cap_reason, cap_safe_explanation, human_review_required,
        additional_evidence_required, client_delivery_blocked, authenticity_risk,
        reidentification_risk_signal, industry_pack_key, industry_pack_maturity, ontology_stale,
        selection_reason, ontology_version, industry_pack_version,
        dimension_scores, evidence_coverage, provenance_summary, explanations,
        interview_questions, anti_pattern_warnings, generated_at
      ) VALUES (
        ?, ?, ?, ?, ?, ?,
        ?, ?, ?, ?, ?, ?,
        ?, ?, ?, ?, ?, ?,
        ?, ?, ?, ?, ?, ?,
        ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb,
        ?::jsonb, ?::jsonb, ?
      )
      """;

  private static final String BASE_SELECT = """
      SELECT match_report_id, organization_id, job_id, candidate_id, shortlist_candidate_card_id,
        subject_type, match_subject_ref, proposed_score, overall_score, score_confidence,
        cap_applied, cap_reason, cap_safe_explanation, human_review_required,
        additional_evidence_required, client_delivery_blocked, authenticity_risk,
        reidentification_risk_signal, industry_pack_key, industry_pack_maturity, ontology_stale,
        selection_reason, ontology_version, industry_pack_version,
        dimension_scores::text AS dimension_scores,
        evidence_coverage::text AS evidence_coverage,
        provenance_summary::text AS provenance_summary,
        explanations::text AS explanations,
        interview_questions::text AS interview_questions,
        anti_pattern_warnings::text AS anti_pattern_warnings,
        created_at, generated_at
      FROM recruiting.match_report
      """;

  private static final String FIND_BY_JOB_SQL =
      BASE_SELECT + " WHERE organization_id = ? AND job_id = ? ORDER BY created_at DESC";

  private static final String FIND_LATEST_BY_CANDIDATE_SQL =
      BASE_SELECT + " WHERE organization_id = ? AND job_id = ? AND candidate_id = ? ORDER BY created_at DESC LIMIT 1";

  private static final String FIND_LATEST_BY_SHORTLIST_CARD_SQL =
      BASE_SELECT + " WHERE organization_id = ? AND job_id = ? AND shortlist_candidate_card_id = ? ORDER BY created_at DESC LIMIT 1";

  private final DataSource dataSource;

  public JdbcMatchReportPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public StoredMatchReport create(StoredMatchReport storedMatchReport) {
    Objects.requireNonNull(storedMatchReport, "storedMatchReport must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      MatchReport matchReport = storedMatchReport.matchReport();
      statement.setObject(1, opaqueRefToUuid(matchReport.matchReportId().value(), "match_report_"));
      statement.setObject(2, storedMatchReport.organizationId());
      statement.setObject(3, opaqueRefToUuid(matchReport.jobRef().value(), "job_ref_"));
      statement.setObject(4, storedMatchReport.candidateId());
      statement.setObject(5, storedMatchReport.shortlistCandidateCardId());
      statement.setString(6, storedMatchReport.subjectType());
      statement.setString(7, matchReport.candidateCardRef().value());
      statement.setInt(8, matchReport.scoreCapDecision().proposedScore().value());
      statement.setInt(9, matchReport.overallScore().value());
      statement.setString(10, matchReport.scoreConfidence().name());
      statement.setBoolean(11, matchReport.scoreCapDecision().capApplied());
      statement.setString(12, matchReport.scoreCapDecision().reasonCode().name());
      statement.setString(13, matchReport.scoreCapDecision().safeExplanation());
      statement.setBoolean(14, matchReport.scoreCapDecision().humanReviewRequired());
      statement.setBoolean(15, matchReport.scoreCapDecision().additionalEvidenceRequired());
      statement.setBoolean(16, matchReport.scoreCapDecision().clientDeliveryBlocked());
      statement.setString(17, matchReport.provenanceSummary().authenticityRisk().name());
      statement.setString(18, storedMatchReport.reidentificationRiskSignal().name());
      statement.setString(19, storedMatchReport.industryPackKey());
      statement.setString(
          20,
          storedMatchReport.industryPackMaturity() != null
              ? storedMatchReport.industryPackMaturity().wireValue()
              : null);
      statement.setObject(21, storedMatchReport.ontologyStale());
      statement.setString(22, storedMatchReport.selectionReason());
      statement.setString(23, matchReport.ontologyVersion());
      statement.setString(24, matchReport.industryPackVersion());
      statement.setString(25, serializeDimensionScores(matchReport.dimensionScores()));
      statement.setString(26, serializeEvidenceCoverage(matchReport.evidenceCoverage()));
      statement.setString(27, serializeProvenanceSummary(matchReport.provenanceSummary()));
      statement.setString(28, serializeStringList(storedMatchReport.explanations()));
      statement.setString(29, serializeStringList(storedMatchReport.interviewQuestions()));
      statement.setString(30, serializeStringList(storedMatchReport.antiPatternWarnings()));
      statement.setTimestamp(31, Timestamp.from(matchReport.generatedAt()));
      statement.executeUpdate();
      if (storedMatchReport.shortlistCandidateCardId() != null) {
        updateShortlistCardMatchReport(
            connection,
            storedMatchReport.shortlistCandidateCardId(),
            storedMatchReport.organizationId(),
            opaqueRefToUuid(matchReport.matchReportId().value(), "match_report_"));
      }
      return storedMatchReport;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create match report", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<StoredMatchReport> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<StoredMatchReport> reports = new ArrayList<>();
        while (resultSet.next()) {
          reports.add(toStoredMatchReport(resultSet));
        }
        return List.copyOf(reports);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to read match reports by job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<StoredMatchReport> findLatestByCandidateIdAndJobId(
      UUID organizationId, JobId jobId, UUID candidateId) {
    return findOne(FIND_LATEST_BY_CANDIDATE_SQL, organizationId, jobId.value(), candidateId);
  }

  @Override
  public Optional<StoredMatchReport> findLatestByShortlistCandidateCardIdAndJobId(
      UUID organizationId, JobId jobId, UUID shortlistCandidateCardId) {
    return findOne(FIND_LATEST_BY_SHORTLIST_CARD_SQL, organizationId, jobId.value(), shortlistCandidateCardId);
  }

  private Optional<StoredMatchReport> findOne(String sql, UUID organizationId, UUID jobId, UUID subjectId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(subjectId, "subjectId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId);
      statement.setObject(3, subjectId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toStoredMatchReport(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to read match report", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static void updateShortlistCardMatchReport(
      Connection connection,
      UUID shortlistCandidateCardId,
      UUID organizationId,
      UUID matchReportId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE recruiting.shortlist_candidate_card SET match_report_id = ?, updated_at = NOW() WHERE shortlist_candidate_card_id = ? AND organization_id = ?")) {
      statement.setObject(1, matchReportId);
      statement.setObject(2, shortlistCandidateCardId);
      statement.setObject(3, organizationId);
      statement.executeUpdate();
    }
  }

  private static StoredMatchReport toStoredMatchReport(ResultSet rs) throws SQLException {
    try {
      MatchReport matchReport = new MatchReport(
          MatchReportId.of(uuidToOpaqueRef(rs.getObject("match_report_id", UUID.class), "match_report_")),
          MatchJobRef.of("job_ref_" + rs.getObject("job_id", UUID.class).toString().replace("-", "")),
          MatchSubjectRef.of(rs.getString("match_subject_ref")),
          MatchScore.of(rs.getInt("overall_score")),
          readDimensionScores(rs.getString("dimension_scores")),
          ScoreConfidence.valueOf(rs.getString("score_confidence")),
          readEvidenceCoverage(rs.getString("evidence_coverage")),
          readProvenanceSummary(rs.getString("provenance_summary")),
          readScoreCapDecision(
              rs.getInt("proposed_score"),
              rs.getInt("overall_score"),
              rs.getString("cap_reason"),
              rs.getString("cap_safe_explanation"),
              rs.getBoolean("cap_applied"),
              rs.getBoolean("human_review_required"),
              rs.getBoolean("additional_evidence_required"),
              rs.getBoolean("client_delivery_blocked")),
          rs.getString("ontology_version"),
          rs.getString("industry_pack_version"),
          rs.getObject("generated_at", OffsetDateTime.class).toInstant());
      return new StoredMatchReport(
          rs.getObject("organization_id", UUID.class),
          matchReport,
          rs.getString("subject_type"),
          rs.getObject("candidate_id", UUID.class),
          rs.getObject("shortlist_candidate_card_id", UUID.class),
          ReidentificationRiskSignal.valueOf(rs.getString("reidentification_risk_signal")),
          rs.getString("industry_pack_key"),
          parseIndustryPackMaturity(rs.getString("industry_pack_maturity")),
          readNullableBoolean(rs, "ontology_stale"),
          rs.getString("selection_reason"),
          readStringList(rs.getString("anti_pattern_warnings")),
          readStringList(rs.getString("explanations")),
          readStringList(rs.getString("interview_questions")));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to map stored match report", exception);
    }
  }

  private static IndustryPackMaturity parseIndustryPackMaturity(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return IndustryPackMaturity.valueOf(value.toUpperCase(Locale.ROOT));
  }

  private static Boolean readNullableBoolean(ResultSet rs, String columnName) throws SQLException {
    boolean value = rs.getBoolean(columnName);
    return rs.wasNull() ? null : value;
  }

  private static List<String> readStringList(String json) throws Exception {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
  }

  private static Map<MatchDimension, MatchScore> readDimensionScores(String json) throws Exception {
    Map<String, Integer> raw = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Integer>>() {});
    EnumMap<MatchDimension, MatchScore> scores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      scores.put(dimension, MatchScore.of(raw.getOrDefault(dimension.name(), 1)));
    }
    return scores;
  }

  private static EvidenceCoverage readEvidenceCoverage(String json) throws Exception {
    Map<String, Object> raw = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    return new EvidenceCoverage(
        Double.parseDouble(String.valueOf(raw.getOrDefault("coverageRatio", 0.0d))),
        parseEvidenceCoverageLevel(String.valueOf(raw.getOrDefault("coverageLevel", EvidenceCoverageLevel.NONE.name()))),
        Integer.parseInt(String.valueOf(raw.getOrDefault("independentEvidenceCount", 0))),
        Integer.parseInt(String.valueOf(raw.getOrDefault("independentHighTrustEvidenceCount", 0))));
  }

  private static EvidenceCoverageLevel parseEvidenceCoverageLevel(String value) {
    if (value == null || value.isBlank()) {
      return EvidenceCoverageLevel.NONE;
    }
    String normalized = value.strip().toUpperCase(Locale.ROOT);
    return EvidenceCoverageLevel.valueOf(normalized);
  }

  private static ProvenanceSummary readProvenanceSummary(String json) throws Exception {
    Map<String, Object> raw = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    return new ProvenanceSummary(
        ProvenanceCategory.valueOf(String.valueOf(raw.get("strongestProvenanceCategory"))),
        ProvenanceSourceStrength.valueOf(String.valueOf(raw.get("strongestSourceStrength"))),
        ProvenanceWeight.of(Double.parseDouble(String.valueOf(raw.get("provenanceWeight")))),
        EvidenceAssertionStrength.valueOf(String.valueOf(raw.get("assertionStrength"))),
        AuthenticityRiskLevel.valueOf(String.valueOf(raw.get("authenticityRisk"))));
  }

  private static ScoreCapDecision readScoreCapDecision(
      int proposedScore,
      int overallScore,
      String capReason,
      String capSafeExplanation,
      boolean capApplied,
      boolean humanReviewRequired,
      boolean additionalEvidenceRequired,
      boolean clientDeliveryBlocked) {
    return new ScoreCapDecision(
        MatchScore.of(proposedScore),
        MatchScore.of(overallScore),
        capApplied,
        ScoreCapReason.valueOf(capReason),
        capSafeExplanation,
        humanReviewRequired,
        additionalEvidenceRequired,
        clientDeliveryBlocked);
  }

  private static String serializeDimensionScores(Map<MatchDimension, MatchScore> scores) {
    try {
      EnumMap<MatchDimension, Integer> raw = new EnumMap<>(MatchDimension.class);
      for (Map.Entry<MatchDimension, MatchScore> entry : scores.entrySet()) {
        raw.put(entry.getKey(), entry.getValue().value());
      }
      return OBJECT_MAPPER.writeValueAsString(raw);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize dimension scores", exception);
    }
  }

  private static String serializeEvidenceCoverage(EvidenceCoverage coverage) {
    try {
      return OBJECT_MAPPER.writeValueAsString(Map.of(
          "coverageRatio", coverage.coverageRatio(),
          "coverageLevel", coverage.coverageLevel().name(),
          "independentEvidenceCount", coverage.independentEvidenceCount(),
          "independentHighTrustEvidenceCount", coverage.independentHighTrustEvidenceCount()));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize evidence coverage", exception);
    }
  }

  private static String serializeProvenanceSummary(ProvenanceSummary summary) {
    try {
      return OBJECT_MAPPER.writeValueAsString(Map.of(
          "strongestProvenanceCategory", summary.strongestProvenanceCategory().name(),
          "strongestSourceStrength", summary.strongestSourceStrength().name(),
          "provenanceWeight", summary.provenanceWeight().value(),
          "assertionStrength", summary.assertionStrength().name(),
          "authenticityRisk", summary.authenticityRisk().name()));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize provenance summary", exception);
    }
  }

  private static String serializeStringList(List<String> values) {
    try {
      return OBJECT_MAPPER.writeValueAsString(values);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to serialize string list", exception);
    }
  }

  private static UUID opaqueRefToUuid(String value, String prefix) {
    String compact = value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    if (compact.length() != 32) {
      throw new IllegalArgumentException("opaque ref must contain a 32-char UUID payload");
    }
    return UUID.fromString(
        compact.substring(0, 8) + "-"
            + compact.substring(8, 12) + "-"
            + compact.substring(12, 16) + "-"
            + compact.substring(16, 20) + "-"
            + compact.substring(20));
  }

  private static String uuidToOpaqueRef(UUID value, String prefix) {
    return prefix + value.toString().replace("-", "");
  }
}
