package com.recruitingtransactionos.coreapi.interviewfeedback.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackDecision;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcomeLabel;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignal;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignalId;
import com.recruitingtransactionos.coreapi.interviewfeedback.RejectReasonTaxonomy;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.MatchCalibrationSignalPort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcMatchCalibrationSignalPort implements MatchCalibrationSignalPort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.match_calibration_signal (
        match_calibration_signal_id, organization_id, interview_feedback_id,
        candidate_company_interaction_id, job_id, candidate_id, industry_pack_key,
        decision, outcome_label, reject_reason_taxonomy, confidence, metadata,
        created_at, version
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
      """;

  private static final String SELECT_BY_FEEDBACK_SQL = """
      SELECT match_calibration_signal_id, organization_id, interview_feedback_id,
        candidate_company_interaction_id, job_id, candidate_id, industry_pack_key,
        decision, outcome_label, reject_reason_taxonomy, confidence,
        metadata::text AS metadata, created_at, version
      FROM recruiting.match_calibration_signal
      WHERE organization_id = ? AND interview_feedback_id = ?
      ORDER BY created_at
      """;

  private final DataSource dataSource;

  public JdbcMatchCalibrationSignalPort(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public MatchCalibrationSignal create(MatchCalibrationSignal signal) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, signal.matchCalibrationSignalId().value());
      statement.setObject(2, signal.organizationId());
      statement.setObject(3, signal.interviewFeedbackId().value());
      statement.setObject(4, signal.candidateCompanyInteractionId().value());
      statement.setObject(5, signal.jobId().value());
      statement.setObject(6, signal.candidateId() != null ? signal.candidateId().value() : null);
      statement.setString(7, signal.industryPackKey());
      statement.setString(8, signal.decision() != null ? signal.decision().wireValue() : null);
      statement.setString(9, signal.outcomeLabel() != null ? signal.outcomeLabel().wireValue() : null);
      statement.setString(10, signal.rejectReasonTaxonomy() != null ? signal.rejectReasonTaxonomy().wireValue() : null);
      statement.setString(11, signal.confidence());
      statement.setString(12, signal.metadata());
      statement.setTimestamp(13, java.sql.Timestamp.from(signal.createdAt()));
      statement.setInt(14, signal.version());
      statement.executeUpdate();
      return signal;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create match calibration signal", exception);
    }
  }

  @Override
  public List<MatchCalibrationSignal> findByInterviewFeedbackIdAndOrganizationId(
      UUID organizationId,
      InterviewFeedbackId interviewFeedbackId) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(SELECT_BY_FEEDBACK_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, interviewFeedbackId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        ArrayList<MatchCalibrationSignal> signals = new ArrayList<>();
        while (resultSet.next()) {
          signals.add(new MatchCalibrationSignal(
              new MatchCalibrationSignalId(resultSet.getObject("match_calibration_signal_id", UUID.class)),
              resultSet.getObject("organization_id", UUID.class),
              new InterviewFeedbackId(resultSet.getObject("interview_feedback_id", UUID.class)),
              new CandidateCompanyInteractionId(resultSet.getObject("candidate_company_interaction_id", UUID.class)),
              new JobId(resultSet.getObject("job_id", UUID.class)),
              resultSet.getObject("candidate_id", UUID.class) != null
                  ? new CandidateId(resultSet.getObject("candidate_id", UUID.class))
                  : null,
              resultSet.getString("industry_pack_key"),
              resultSet.getString("decision") != null
                  ? InterviewFeedbackDecision.fromWireValue(resultSet.getString("decision"))
                  : null,
              resultSet.getString("outcome_label") != null
                  ? InterviewOutcomeLabel.fromWireValue(resultSet.getString("outcome_label"))
                  : null,
              resultSet.getString("reject_reason_taxonomy") != null
                  ? RejectReasonTaxonomy.fromWireValue(resultSet.getString("reject_reason_taxonomy"))
                  : null,
              resultSet.getString("confidence"),
              resultSet.getString("metadata"),
              resultSet.getObject("created_at", java.time.OffsetDateTime.class).toInstant(),
              resultSet.getInt("version")));
        }
        return List.copyOf(signals);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load match calibration signals", exception);
    }
  }
}
