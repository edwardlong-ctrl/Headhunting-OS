package com.recruitingtransactionos.coreapi.privacyredaction.persistence;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskDecision;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskFeature;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskLevel;
import com.recruitingtransactionos.coreapi.privacyredaction.PersistedReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.privacyredaction.port.ReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcReidentificationRiskAssessmentPort
    implements ReidentificationRiskAssessmentPort {

  private static final String INSERT_SQL = """
      INSERT INTO privacy.reidentification_risk_assessment (
        reidentification_risk_assessment_ref,
        organization_id,
        candidate_card_id,
        candidate_ref,
        job_ref,
        redaction_level,
        risk_level,
        decision,
        unsafe_features,
        risk_score,
        explanation,
        workflow_event_id,
        recorded_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::text[], ?, ?, ?, ?)
      """;

  private static final String COMMON_SELECT = """
      SELECT
        reidentification_risk_assessment_ref,
        organization_id,
        candidate_card_id,
        candidate_ref,
        job_ref,
        redaction_level,
        risk_level,
        decision,
        unsafe_features,
        risk_score,
        explanation,
        workflow_event_id,
        recorded_at
      FROM privacy.reidentification_risk_assessment
      """;

  private static final String FIND_BY_REF_SQL =
      COMMON_SELECT
          + "WHERE organization_id = ? AND reidentification_risk_assessment_ref = ?";

  private static final String FIND_RECENT_BY_CARD_SQL =
      COMMON_SELECT
          + "WHERE organization_id = ? AND candidate_card_id = ?"
          + " ORDER BY recorded_at DESC, reidentification_risk_assessment_ref DESC"
          + " LIMIT ?";

  private final DataSource dataSource;

  public JdbcReidentificationRiskAssessmentPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public PersistedReidentificationRiskAssessment append(
      PersistedReidentificationRiskAssessment assessment) {
    Objects.requireNonNull(assessment, "assessment must not be null");
    ReidentificationRiskAssessment value = assessment.assessment();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setString(1, assessment.reidentificationRiskAssessmentRef());
      statement.setObject(2, assessment.organizationId());
      statement.setString(3, value.cardId().value());
      if (assessment.candidateRef() == null) {
        statement.setNull(4, Types.VARCHAR);
      } else {
        statement.setString(4, assessment.candidateRef());
      }
      if (assessment.jobRef() == null) {
        statement.setNull(5, Types.VARCHAR);
      } else {
        statement.setString(5, assessment.jobRef());
      }
      statement.setString(6, value.redactionLevel().wireValue());
      statement.setString(7, value.riskLevel().wireValue());
      statement.setString(8, value.decision().wireValue());
      Array unsafeFeaturesArray = connection.createArrayOf(
          "text",
          value.unsafeFeatures().stream()
              .map(ReidentificationRiskFeature::wireValue)
              .toArray(String[]::new));
      statement.setArray(9, unsafeFeaturesArray);
      statement.setDouble(10, value.riskScore());
      statement.setString(11, value.explanation());
      Optional<WorkflowEventId> workflowEventId = assessment.workflowEventId();
      if (workflowEventId.isEmpty()) {
        statement.setNull(12, Types.OTHER);
      } else {
        statement.setObject(12, workflowEventId.get().value());
      }
      statement.setObject(13, OffsetDateTime.ofInstant(
          assessment.recordedAt(),
          ZoneOffset.UTC));
      statement.executeUpdate();
      return assessment;
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to append re-identification risk assessment", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<PersistedReidentificationRiskAssessment> findByRefAndOrganizationId(
      UUID organizationId,
      String reidentificationRiskAssessmentRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(
        reidentificationRiskAssessmentRef,
        "reidentificationRiskAssessmentRef must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_REF_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, reidentificationRiskAssessmentRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(map(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find re-identification risk assessment", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<PersistedReidentificationRiskAssessment> findRecentByCandidateCardId(
      UUID organizationId,
      String candidateCardId,
      int limit) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateCardId, "candidateCardId must not be null");
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be at least 1");
    }
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_RECENT_BY_CARD_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, candidateCardId);
      statement.setInt(3, limit);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<PersistedReidentificationRiskAssessment> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(map(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to list re-identification risk assessments", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static PersistedReidentificationRiskAssessment map(ResultSet resultSet)
      throws SQLException {
    String[] unsafeFeatureValues =
        (String[]) resultSet.getArray("unsafe_features").getArray();
    Set<ReidentificationRiskFeature> unsafeFeatures = decodeUnsafeFeatures(unsafeFeatureValues);
    ReidentificationRiskAssessment assessment = new ReidentificationRiskAssessment(
        AnonymousCandidateCardId.of(resultSet.getString("candidate_card_id")),
        RedactionLevel.fromWireValue(resultSet.getString("redaction_level")),
        ReidentificationRiskLevel.fromWireValue(resultSet.getString("risk_level")),
        unsafeFeatures,
        ReidentificationRiskDecision.fromWireValue(resultSet.getString("decision")),
        resultSet.getDouble("risk_score"),
        resultSet.getString("explanation"));
    UUID workflowEventUuid = resultSet.getObject("workflow_event_id", UUID.class);
    Optional<WorkflowEventId> workflowEventId = workflowEventUuid == null
        ? Optional.empty()
        : Optional.of(new WorkflowEventId(workflowEventUuid));
    return new PersistedReidentificationRiskAssessment(
        resultSet.getString("reidentification_risk_assessment_ref"),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getString("candidate_ref"),
        resultSet.getString("job_ref"),
        assessment,
        workflowEventId,
        resultSet.getObject("recorded_at", OffsetDateTime.class).toInstant());
  }

  private static Set<ReidentificationRiskFeature> decodeUnsafeFeatures(String[] values) {
    if (values == null || values.length == 0) {
      return Set.of();
    }
    EnumSet<ReidentificationRiskFeature> features =
        EnumSet.noneOf(ReidentificationRiskFeature.class);
    Arrays.stream(values)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .map(ReidentificationRiskFeature::fromWireValue)
        .forEach(features::add);
    return Set.copyOf(features);
  }
}
