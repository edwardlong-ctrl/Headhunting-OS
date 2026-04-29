package com.recruitingtransactionos.coreapi.interviewfeedback.persistence;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcome;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackPersistencePort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcInterviewFeedbackPersistencePort
    implements InterviewFeedbackPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.interview_feedback (
        interview_feedback_id, organization_id, candidate_company_interaction_id,
        job_id, interviewer_name, interviewer_role, interview_round,
        interview_date, outcome, ratings, strengths, concerns, notes,
        submitted_by_role, submitted_by_user_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT interview_feedback_id, organization_id, candidate_company_interaction_id,
        job_id, interviewer_name, interviewer_role, interview_round,
        interview_date, outcome, ratings::text AS ratings,
        strengths, concerns, notes, submitted_by_role, submitted_by_user_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.interview_feedback
      WHERE organization_id = ? AND interview_feedback_id = ?
      """;

  private static final String FIND_BY_INTERACTION_SQL = """
      SELECT interview_feedback_id, organization_id, candidate_company_interaction_id,
        job_id, interviewer_name, interviewer_role, interview_round,
        interview_date, outcome, ratings::text AS ratings,
        strengths, concerns, notes, submitted_by_role, submitted_by_user_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.interview_feedback
      WHERE organization_id = ? AND candidate_company_interaction_id = ?
      ORDER BY interview_round NULLS LAST, created_at
      """;

  private static final String FIND_BY_JOB_SQL = """
      SELECT interview_feedback_id, organization_id, candidate_company_interaction_id,
        job_id, interviewer_name, interviewer_role, interview_round,
        interview_date, outcome, ratings::text AS ratings,
        strengths, concerns, notes, submitted_by_role, submitted_by_user_id,
        metadata::text AS metadata, created_at, updated_at, version
      FROM recruiting.interview_feedback
      WHERE organization_id = ? AND job_id = ?
      ORDER BY interview_round NULLS LAST, created_at
      """;

  private final DataSource dataSource;

  public JdbcInterviewFeedbackPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public InterviewFeedback create(InterviewFeedback feedback) {
    Objects.requireNonNull(feedback, "feedback must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, feedback.interviewFeedbackId().value());
      statement.setObject(2, feedback.organizationId());
      statement.setObject(3, feedback.candidateCompanyInteractionId().value());
      statement.setObject(4, feedback.jobId().value());
      statement.setString(5, feedback.interviewerName());
      statement.setString(6, feedback.interviewerRole());
      if (feedback.interviewRound() != null) {
        statement.setInt(7, feedback.interviewRound());
      } else {
        statement.setNull(7, Types.INTEGER);
      }
      statement.setTimestamp(8,
          feedback.interviewDate() != null ? Timestamp.from(feedback.interviewDate()) : null);
      statement.setString(9, feedback.outcome().wireValue());
      statement.setString(10, feedback.ratings());
      statement.setString(11, feedback.strengths());
      statement.setString(12, feedback.concerns());
      statement.setString(13, feedback.notes());
      statement.setString(14, feedback.submittedByRole());
      statement.setObject(15, feedback.submittedByUserId());
      statement.setString(16, feedback.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(
          feedback.organizationId(), feedback.interviewFeedbackId())
          .orElseThrow(() -> new IllegalStateException(
              "interview_feedback not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create interview_feedback", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<InterviewFeedback> findByIdAndOrganizationId(
      UUID organizationId, InterviewFeedbackId feedbackId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(feedbackId, "feedbackId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, feedbackId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toFeedback(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find interview_feedback by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<InterviewFeedback> findByInteractionIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_INTERACTION_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, interactionId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<InterviewFeedback> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toFeedback(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find interview_feedback by interaction", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<InterviewFeedback> findByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_JOB_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, jobId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<InterviewFeedback> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toFeedback(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find interview_feedback by job", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static InterviewFeedback toFeedback(ResultSet rs) throws SQLException {
    OffsetDateTime interviewDateOdt = rs.getObject("interview_date", OffsetDateTime.class);
    int round = rs.getInt("interview_round");
    Integer interviewRound = rs.wasNull() ? null : round;
    return InterviewFeedback.builder()
        .interviewFeedbackId(new InterviewFeedbackId(
            rs.getObject("interview_feedback_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .candidateCompanyInteractionId(new CandidateCompanyInteractionId(
            rs.getObject("candidate_company_interaction_id", UUID.class)))
        .jobId(new JobId(rs.getObject("job_id", UUID.class)))
        .interviewerName(rs.getString("interviewer_name"))
        .interviewerRole(rs.getString("interviewer_role"))
        .interviewRound(interviewRound)
        .interviewDate(interviewDateOdt != null ? interviewDateOdt.toInstant() : null)
        .outcome(InterviewOutcome.fromWireValue(rs.getString("outcome")))
        .ratings(rs.getString("ratings"))
        .strengths(rs.getString("strengths"))
        .concerns(rs.getString("concerns"))
        .notes(rs.getString("notes"))
        .submittedByRole(rs.getString("submitted_by_role"))
        .submittedByUserId(rs.getObject("submitted_by_user_id", UUID.class))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
