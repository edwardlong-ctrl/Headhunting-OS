package com.recruitingtransactionos.coreapi.interviewfeedback.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionScope;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionStatus;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionType;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcomeLabel;
import com.recruitingtransactionos.coreapi.interviewfeedback.RejectReasonTaxonomy;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackSuggestionPersistencePort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcInterviewFeedbackSuggestionPersistencePort
    implements InterviewFeedbackSuggestionPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.interview_feedback_suggestion (
        interview_feedback_suggestion_id, organization_id, interview_feedback_id,
        candidate_company_interaction_id, job_id, candidate_id, ai_task_run_id,
        scope, suggestion_type, status, outcome_label, reject_reason_taxonomy,
        title, rationale, payload, reviewed_by_user_id, reviewed_at, created_at,
        updated_at, version
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
      """;

  private static final String UPDATE_SQL = """
      UPDATE recruiting.interview_feedback_suggestion
      SET status = ?, outcome_label = ?, reject_reason_taxonomy = ?, title = ?,
          rationale = ?, payload = ?::jsonb, reviewed_by_user_id = ?, reviewed_at = ?,
          updated_at = ?, version = ?
      WHERE organization_id = ? AND interview_feedback_suggestion_id = ?
      """;

  private static final String BASE_SELECT = """
      SELECT interview_feedback_suggestion_id, organization_id, interview_feedback_id,
        candidate_company_interaction_id, job_id, candidate_id, ai_task_run_id,
        scope, suggestion_type, status, outcome_label, reject_reason_taxonomy,
        title, rationale, payload::text AS payload, reviewed_by_user_id, reviewed_at,
        created_at, updated_at, version
      FROM recruiting.interview_feedback_suggestion
      """;

  private final DataSource dataSource;

  public JdbcInterviewFeedbackSuggestionPersistencePort(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public InterviewFeedbackSuggestion create(InterviewFeedbackSuggestion suggestion) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bind(statement, suggestion, false);
      statement.executeUpdate();
      return suggestion;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create interview feedback suggestion", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public InterviewFeedbackSuggestion update(InterviewFeedbackSuggestion suggestion) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
      statement.setString(1, suggestion.status().wireValue());
      statement.setString(2, suggestion.outcomeLabel() != null ? suggestion.outcomeLabel().wireValue() : null);
      statement.setString(3, suggestion.rejectReasonTaxonomy() != null ? suggestion.rejectReasonTaxonomy().wireValue() : null);
      statement.setString(4, suggestion.title());
      statement.setString(5, suggestion.rationale());
      statement.setString(6, suggestion.payload());
      statement.setObject(7, suggestion.reviewedByUserId());
      statement.setTimestamp(8, suggestion.reviewedAt() != null ? Timestamp.from(suggestion.reviewedAt()) : null);
      statement.setTimestamp(9, Timestamp.from(suggestion.updatedAt()));
      statement.setInt(10, suggestion.version());
      statement.setObject(11, suggestion.organizationId());
      statement.setObject(12, suggestion.interviewFeedbackSuggestionId().value());
      statement.executeUpdate();
      return suggestion;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to update interview feedback suggestion", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<InterviewFeedbackSuggestion> findByIdAndOrganizationId(
      UUID organizationId,
      InterviewFeedbackSuggestionId suggestionId) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        BASE_SELECT + " WHERE organization_id = ? AND interview_feedback_suggestion_id = ?")) {
      statement.setObject(1, organizationId);
      statement.setObject(2, suggestionId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(map(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load interview feedback suggestion", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<InterviewFeedbackSuggestion> findByInteractionIdAndOrganizationId(
      UUID organizationId,
      CandidateCompanyInteractionId interactionId) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        BASE_SELECT + " WHERE organization_id = ? AND candidate_company_interaction_id = ? ORDER BY created_at DESC")) {
      statement.setObject(1, organizationId);
      statement.setObject(2, interactionId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list interview feedback suggestions", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<InterviewFeedbackSuggestion> listPendingByOrganization(UUID organizationId, int limit) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        BASE_SELECT + " WHERE organization_id = ? AND status = 'pending_review' ORDER BY created_at DESC LIMIT ?")) {
      statement.setObject(1, organizationId);
      statement.setInt(2, limit);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list pending interview feedback suggestions", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void bind(PreparedStatement statement, InterviewFeedbackSuggestion suggestion, boolean update)
      throws SQLException {
    statement.setObject(1, suggestion.interviewFeedbackSuggestionId().value());
    statement.setObject(2, suggestion.organizationId());
    statement.setObject(3, suggestion.interviewFeedbackId().value());
    statement.setObject(4, suggestion.candidateCompanyInteractionId().value());
    statement.setObject(5, suggestion.jobId().value());
    statement.setObject(6, suggestion.candidateId() != null ? suggestion.candidateId().value() : null);
    statement.setObject(7, suggestion.aiTaskRunId());
    statement.setString(8, suggestion.scope().wireValue());
    statement.setString(9, suggestion.suggestionType().wireValue());
    statement.setString(10, suggestion.status().wireValue());
    statement.setString(11, suggestion.outcomeLabel() != null ? suggestion.outcomeLabel().wireValue() : null);
    statement.setString(12, suggestion.rejectReasonTaxonomy() != null ? suggestion.rejectReasonTaxonomy().wireValue() : null);
    statement.setString(13, suggestion.title());
    statement.setString(14, suggestion.rationale());
    statement.setString(15, suggestion.payload());
    statement.setObject(16, suggestion.reviewedByUserId());
    statement.setTimestamp(17, suggestion.reviewedAt() != null ? Timestamp.from(suggestion.reviewedAt()) : null);
    statement.setTimestamp(18, Timestamp.from(suggestion.createdAt()));
    statement.setTimestamp(19, Timestamp.from(suggestion.updatedAt()));
    statement.setInt(20, suggestion.version());
  }

  private List<InterviewFeedbackSuggestion> mapAll(ResultSet resultSet) throws SQLException {
    java.util.ArrayList<InterviewFeedbackSuggestion> suggestions = new java.util.ArrayList<>();
    while (resultSet.next()) {
      suggestions.add(map(resultSet));
    }
    return List.copyOf(suggestions);
  }

  private InterviewFeedbackSuggestion map(ResultSet rs) throws SQLException {
    OffsetDateTime reviewedAt = rs.getObject("reviewed_at", OffsetDateTime.class);
    return new InterviewFeedbackSuggestion(
        new InterviewFeedbackSuggestionId(rs.getObject("interview_feedback_suggestion_id", UUID.class)),
        rs.getObject("organization_id", UUID.class),
        new InterviewFeedbackId(rs.getObject("interview_feedback_id", UUID.class)),
        new CandidateCompanyInteractionId(rs.getObject("candidate_company_interaction_id", UUID.class)),
        new JobId(rs.getObject("job_id", UUID.class)),
        rs.getObject("candidate_id", UUID.class) != null
            ? new CandidateId(rs.getObject("candidate_id", UUID.class))
            : null,
        rs.getObject("ai_task_run_id", UUID.class),
        InterviewFeedbackSuggestionScope.fromWireValue(rs.getString("scope")),
        InterviewFeedbackSuggestionType.fromWireValue(rs.getString("suggestion_type")),
        InterviewFeedbackSuggestionStatus.fromWireValue(rs.getString("status")),
        rs.getString("outcome_label") != null
            ? InterviewOutcomeLabel.fromWireValue(rs.getString("outcome_label"))
            : null,
        rs.getString("reject_reason_taxonomy") != null
            ? RejectReasonTaxonomy.fromWireValue(rs.getString("reject_reason_taxonomy"))
            : null,
        rs.getString("title"),
        rs.getString("rationale"),
        rs.getString("payload"),
        rs.getObject("reviewed_by_user_id", UUID.class),
        reviewedAt != null ? reviewedAt.toInstant() : null,
        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
        rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
        rs.getInt("version"));
  }
}
