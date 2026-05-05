package com.recruitingtransactionos.coreapi.followup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

@Service
public final class FollowUpSubmissionService {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.follow_up_submission (
        follow_up_submission_id,
        organization_id,
        candidate_id,
        candidate_profile_id,
        form_id,
        field_path,
        answer_json,
        status,
        submitted_by_user_id,
        reviewed_by_user_id,
        workflow_event_id,
        submitted_at,
        reviewed_at,
        notes,
        created_at,
        updated_at,
        version
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_LATEST_FOR_FIELD_SQL = """
      SELECT follow_up_submission_id, organization_id, candidate_id, candidate_profile_id,
             form_id, field_path, answer_json, status, submitted_by_user_id,
             reviewed_by_user_id, workflow_event_id, submitted_at, reviewed_at,
             notes, created_at, updated_at, version
      FROM recruiting.follow_up_submission
      WHERE organization_id = ?
        AND candidate_profile_id = ?
        AND field_path = ?
      ORDER BY submitted_at DESC
      LIMIT 1
      """;

  private static final String FIND_PENDING_BY_ORG_SQL = """
      SELECT follow_up_submission_id, organization_id, candidate_id, candidate_profile_id,
             form_id, field_path, answer_json, status, submitted_by_user_id,
             reviewed_by_user_id, workflow_event_id, submitted_at, reviewed_at,
             notes, created_at, updated_at, version
      FROM recruiting.follow_up_submission
      WHERE organization_id = ?
        AND status IN ('submitted', 'under_review')
      ORDER BY submitted_at DESC
      LIMIT ?
      """;

  private final DataSource dataSource;

  public FollowUpSubmissionService(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  public FollowUpSubmission create(CreateFollowUpSubmissionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Instant now = command.submittedAt() != null ? command.submittedAt() : Instant.now();
    UUID submissionId = command.followUpSubmissionId() != null
        ? command.followUpSubmissionId()
        : UUID.randomUUID();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, submissionId);
      statement.setObject(2, command.organizationId());
      statement.setObject(3, command.candidateId());
      statement.setObject(4, command.candidateProfileId());
      statement.setString(5, command.formId());
      statement.setString(6, command.fieldPath());
      statement.setString(7, command.answerJson());
      statement.setString(8, command.status());
      statement.setObject(9, command.submittedByUserId());
      statement.setObject(10, null);
      statement.setObject(11, command.workflowEventId());
      statement.setTimestamp(12, Timestamp.from(now));
      statement.setObject(13, null);
      statement.setString(14, command.notes());
      statement.setTimestamp(15, Timestamp.from(now));
      statement.setTimestamp(16, Timestamp.from(now));
      statement.setInt(17, 1);
      statement.executeUpdate();
      return new FollowUpSubmission(
          submissionId,
          command.organizationId(),
          command.candidateId(),
          command.candidateProfileId(),
          command.formId(),
          command.fieldPath(),
          command.answerJson(),
          command.status(),
          command.submittedByUserId(),
          null,
          command.workflowEventId(),
          now,
          null,
          command.notes(),
          now,
          now,
          1);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create follow-up submission", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public Optional<FollowUpSubmission> findLatestForField(
      UUID organizationId,
      UUID candidateProfileId,
      String fieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_FOR_FIELD_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateProfileId);
      statement.setString(3, fieldPath);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSubmission(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load follow-up submission", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public List<FollowUpSubmission> listPendingByOrganization(UUID organizationId, int limit) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    int normalizedLimit = Math.max(1, limit);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_PENDING_BY_ORG_SQL)) {
      statement.setObject(1, organizationId);
      statement.setInt(2, normalizedLimit);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<FollowUpSubmission> items = new ArrayList<>();
        while (resultSet.next()) {
          items.add(toSubmission(resultSet));
        }
        return List.copyOf(items);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list pending follow-up submissions", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static FollowUpSubmission toSubmission(ResultSet resultSet) throws SQLException {
    return new FollowUpSubmission(
        resultSet.getObject("follow_up_submission_id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getObject("candidate_id", UUID.class),
        resultSet.getObject("candidate_profile_id", UUID.class),
        resultSet.getString("form_id"),
        resultSet.getString("field_path"),
        resultSet.getString("answer_json"),
        resultSet.getString("status"),
        resultSet.getObject("submitted_by_user_id", UUID.class),
        resultSet.getObject("reviewed_by_user_id", UUID.class),
        resultSet.getObject("workflow_event_id", UUID.class),
        toInstant(resultSet.getTimestamp("submitted_at")),
        toInstant(resultSet.getTimestamp("reviewed_at")),
        resultSet.getString("notes"),
        toInstant(resultSet.getTimestamp("created_at")),
        toInstant(resultSet.getTimestamp("updated_at")),
        resultSet.getInt("version"));
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  public record CreateFollowUpSubmissionCommand(
      UUID followUpSubmissionId,
      UUID organizationId,
      UUID candidateId,
      UUID candidateProfileId,
      String formId,
      String fieldPath,
      String answerJson,
      String status,
      UUID submittedByUserId,
      UUID workflowEventId,
      String notes,
      Instant submittedAt) {}

  public record FollowUpSubmission(
      UUID followUpSubmissionId,
      UUID organizationId,
      UUID candidateId,
      UUID candidateProfileId,
      String formId,
      String fieldPath,
      String answerJson,
      String status,
      UUID submittedByUserId,
      UUID reviewedByUserId,
      UUID workflowEventId,
      Instant submittedAt,
      Instant reviewedAt,
      String notes,
      Instant createdAt,
      Instant updatedAt,
      int version) {}
}
