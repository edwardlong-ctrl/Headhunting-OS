package com.recruitingtransactionos.coreapi.consultantmatching.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.consultantmatching.StoredMatchReport;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class JdbcMatchReportPersistencePortIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027d001");
  private static final UUID CONSULTANT_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027d002");
  private static final UUID COMPANY_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027d003");
  private static final UUID JOB_UUID =
      UUID.fromString("00000000-0000-0000-0000-00000027d004");
  private static final UUID CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027d005");
  private static final Instant GENERATED_AT = Instant.parse("2026-05-03T00:30:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;
  private static MigrateResult migrateResult;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    seedOrganizationAndUser();
    seedCompanyAndJob();
    seedCandidate();
  }

  @Test
  void roundTripsOpaqueRefsAndRealCapRiskSemantics() {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(25);
    JdbcMatchReportPersistencePort port = new JdbcMatchReportPersistencePort(dataSource);
    StoredMatchReport stored = sampleStoredMatchReport();

    port.create(stored);

    Optional<StoredMatchReport> reloaded =
        port.findLatestByCandidateIdAndJobId(ORGANIZATION_ID, new JobId(JOB_UUID), CANDIDATE_ID);

    assertThat(reloaded).isPresent();
    StoredMatchReport value = reloaded.orElseThrow();
    assertThat(value.matchReport().jobRef().value())
        .isEqualTo("job_ref_" + JOB_UUID.toString().replace("-", ""));
    assertThat(value.matchReport().matchReportId().value())
        .isEqualTo("match_report_00000000000000000000000027d00011");
    assertThat(value.matchReport().scoreCapDecision().proposedScore().value()).isEqualTo(5);
    assertThat(value.matchReport().scoreCapDecision().cappedScore().value()).isEqualTo(4);
    assertThat(value.matchReport().scoreCapDecision().humanReviewRequired()).isTrue();
    assertThat(value.matchReport().scoreCapDecision().additionalEvidenceRequired()).isTrue();
    assertThat(value.matchReport().scoreCapDecision().clientDeliveryBlocked()).isFalse();
    assertThat(value.matchReport().provenanceSummary().authenticityRisk()).isEqualTo(AuthenticityRiskLevel.MEDIUM);
    assertThat(value.reidentificationRiskSignal()).isEqualTo(ReidentificationRiskSignal.MEDIUM);
    assertThat(value.industryPackKey()).isEqualTo("semiconductor");
    assertThat(value.industryPackMaturity()).isEqualTo(IndustryPackMaturity.SEEDED);
    assertThat(value.ontologyStale()).isFalse();
    assertThat(value.matchReport().evidenceCoverage().coverageLevel()).isEqualTo(EvidenceCoverageLevel.HIGH);
    assertThat(value.selectionReason()).isEqualTo("job_industry_pack");
    assertThat(value.antiPatternWarnings()).containsExactly("software testing is not equal to DV verification");
    assertThat(value.explanations()).containsExactly("Persistence round-trip explanation");
    assertThat(port.findByJobIdAndOrganizationId(ORGANIZATION_ID, new JobId(JOB_UUID)))
        .anyMatch(report ->
            report.matchReport().matchReportId().value().equals("match_report_00000000000000000000000027d00011"));
  }

  @Test
  void readsLegacyRowsWithNullIndustryPackMetadataWithoutFabricatingValues() throws SQLException {
    JdbcMatchReportPersistencePort port = new JdbcMatchReportPersistencePort(dataSource);
    insertLegacyMatchReportWithNullIndustryPackMetadata();

    Optional<StoredMatchReport> reloaded =
        port.findLatestByCandidateIdAndJobId(ORGANIZATION_ID, new JobId(JOB_UUID), CANDIDATE_ID);

    assertThat(reloaded).isPresent();
    StoredMatchReport value = reloaded.orElseThrow();
    assertThat(value.matchReport().matchReportId().value())
        .isEqualTo("match_report_00000000000000000000000027d00012");
    assertThat(value.industryPackKey()).isNull();
    assertThat(value.industryPackMaturity()).isNull();
    assertThat(value.matchReport().evidenceCoverage().coverageLevel()).isEqualTo(EvidenceCoverageLevel.PARTIAL);
    assertThat(value.selectionReason()).isNull();
    assertThat(value.ontologyStale()).isNull();
    assertThat(value.antiPatternWarnings()).isEmpty();
  }

  private static StoredMatchReport sampleStoredMatchReport() {
    EnumMap<MatchDimension, MatchScore> dimensionScores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      dimensionScores.put(dimension, MatchScore.of(dimension == MatchDimension.TECHNICAL_FIT ? 4 : 3));
    }
    MatchReport report = new MatchReport(
        MatchReportId.of("match_report_00000000000000000000000027d00011"),
        MatchJobRef.of("job_ref_" + JOB_UUID.toString().replace("-", "")),
        MatchSubjectRef.of("match_subject_candidate_" + CANDIDATE_ID.toString().replace("-", "")),
        MatchScore.of(4),
        dimensionScores,
        ScoreConfidence.MEDIUM,
        new EvidenceCoverage(0.75d, EvidenceCoverageLevel.HIGH, 4, 2),
        new ProvenanceSummary(
            ProvenanceCategory.CONSULTANT_ATTESTED,
            ProvenanceSourceStrength.HIGH_TRUST,
            ProvenanceWeight.of(0.82d),
            EvidenceAssertionStrength.EXPLICIT,
            AuthenticityRiskLevel.MEDIUM),
        new ScoreCapDecision(
            MatchScore.of(5),
            MatchScore.of(4),
            true,
            ScoreCapReason.INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE,
            "Independent high-trust evidence is insufficient for a top score.",
            true,
            true,
            false),
        "ontology-v2.1",
        "industry-pack-v1",
        GENERATED_AT);
    return new StoredMatchReport(
        ORGANIZATION_ID,
        report,
        "candidate",
        CANDIDATE_ID,
        null,
        ReidentificationRiskSignal.MEDIUM,
        "semiconductor",
        IndustryPackMaturity.SEEDED,
        false,
        "job_industry_pack",
        List.of("software testing is not equal to DV verification"),
        List.of("Persistence round-trip explanation"),
        List.of("Round-trip interview question"));
  }

  private static void seedOrganizationAndUser() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement organization = connection.prepareStatement("""
             INSERT INTO identity.organization (
               organization_id, legal_name, display_name, status, default_timezone
             ) VALUES (?, ?, ?, 'active', 'UTC')
             ON CONFLICT (organization_id) DO NOTHING
             """);
         PreparedStatement user = connection.prepareStatement("""
             INSERT INTO identity.user_account (
               user_account_id, organization_id, email, display_name, status, password_hash
             ) VALUES (?, ?, ?, ?, 'active', NULL)
             ON CONFLICT (user_account_id) DO NOTHING
             """)) {
      organization.setObject(1, ORGANIZATION_ID);
      organization.setString(2, "consultant-matching-org");
      organization.setString(3, "Consultant Matching Org");
      organization.executeUpdate();

      user.setObject(1, CONSULTANT_ID);
      user.setObject(2, ORGANIZATION_ID);
      user.setString(3, "consultant-matching@example.test");
      user.setString(4, "Consultant Matching Tester");
      user.executeUpdate();
    }
  }

  private static void seedCompanyAndJob() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement company = connection.prepareStatement("""
             INSERT INTO recruiting.company (
               company_id, organization_id, name, status, owner_consultant_id
             ) VALUES (?, ?, ?, 'active', ?)
             ON CONFLICT (company_id) DO NOTHING
             """);
         PreparedStatement job = connection.prepareStatement("""
             INSERT INTO recruiting.job (
               job_id, organization_id, company_id, title, status, owner_consultant_id
             ) VALUES (?, ?, ?, ?, 'activated', ?)
             ON CONFLICT (job_id) DO NOTHING
             """)) {
      company.setObject(1, COMPANY_ID);
      company.setObject(2, ORGANIZATION_ID);
      company.setString(3, "Persistence Test Co");
      company.setObject(4, CONSULTANT_ID);
      company.executeUpdate();

      job.setObject(1, JOB_UUID);
      job.setObject(2, ORGANIZATION_ID);
      job.setObject(3, COMPANY_ID);
      job.setString(4, "Senior Java Consultant");
      job.setObject(5, CONSULTANT_ID);
      job.executeUpdate();
    }
  }

  private static void seedCandidate() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement candidate = connection.prepareStatement("""
             INSERT INTO recruiting.candidate (
               candidate_id, organization_id, status, privacy_status, owner_consultant_id
             ) VALUES (?, ?, 'available', 'internal_only', ?)
             ON CONFLICT (candidate_id) DO NOTHING
             """)) {
      candidate.setObject(1, CANDIDATE_ID);
      candidate.setObject(2, ORGANIZATION_ID);
      candidate.setObject(3, CONSULTANT_ID);
      candidate.executeUpdate();
    }
  }

  private static void insertLegacyMatchReportWithNullIndustryPackMetadata() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO recruiting.match_report (
               match_report_id, organization_id, job_id, candidate_id, shortlist_candidate_card_id,
               subject_type, match_subject_ref, proposed_score, overall_score, score_confidence,
               cap_applied, cap_reason, cap_safe_explanation, human_review_required,
               additional_evidence_required, client_delivery_blocked, authenticity_risk,
               reidentification_risk_signal, industry_pack_key, industry_pack_maturity, ontology_stale,
               selection_reason, ontology_version, industry_pack_version, dimension_scores,
               evidence_coverage, provenance_summary, explanations, interview_questions,
               anti_pattern_warnings, generated_at
             ) VALUES (
               ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL,
               NULL, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, NULL, ?
             )
             ON CONFLICT (match_report_id) DO NOTHING
             """)) {
      statement.setObject(1, UUID.fromString("00000000-0000-0000-0000-000027d00012"));
      statement.setObject(2, ORGANIZATION_ID);
      statement.setObject(3, JOB_UUID);
      statement.setObject(4, CANDIDATE_ID);
      statement.setString(5, "candidate");
      statement.setString(6, "match_subject_candidate_" + CANDIDATE_ID.toString().replace("-", ""));
      statement.setInt(7, 4);
      statement.setInt(8, 4);
      statement.setString(9, ScoreConfidence.MEDIUM.name());
      statement.setBoolean(10, false);
      statement.setString(11, ScoreCapReason.NONE.name());
      statement.setString(12, "Legacy report without industry pack metadata.");
      statement.setBoolean(13, false);
      statement.setBoolean(14, false);
      statement.setBoolean(15, false);
      statement.setString(16, AuthenticityRiskLevel.LOW.name());
      statement.setString(17, ReidentificationRiskSignal.MEDIUM.name());
      statement.setString(18, "ontology-v2.0");
      statement.setString(19, "industry-pack-v0");
      statement.setString(20, "{\"TECHNICAL_FIT\":4,\"INDUSTRY_FIT\":3,\"SENIORITY_FIT\":3,\"SALARY_FIT\":3,\"LOCATION_FIT\":3,\"MOTIVATION_FIT\":3,\"AVAILABILITY_FIT\":3,\"CULTURE_OR_MANAGER_FIT\":3,\"EVIDENCE_STRENGTH\":3}");
      statement.setString(21, "{\"coverageRatio\":0.5,\"coverageLevel\":\"PARTIAL\",\"independentEvidenceCount\":2,\"independentHighTrustEvidenceCount\":1}");
      statement.setString(22, "{\"strongestProvenanceCategory\":\"CONSULTANT_ATTESTED\",\"strongestSourceStrength\":\"HIGH_TRUST\",\"provenanceWeight\":0.7,\"assertionStrength\":\"IMPLIED\",\"authenticityRisk\":\"LOW\"}");
      statement.setString(23, "[\"Legacy explanation\"]");
      statement.setString(24, "[\"Legacy interview question\"]");
      statement.setObject(25, java.sql.Timestamp.from(GENERATED_AT.plusSeconds(1)));
      statement.executeUpdate();
    }
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("unwrap not supported");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }

      @Override
      public java.io.PrintWriter getLogWriter() {
        return new java.io.PrintWriter(System.out);
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getGlobal();
      }
    };
  }
}
