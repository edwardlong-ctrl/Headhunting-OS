package com.recruitingtransactionos.coreapi.governedintake.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class JdbcIntakeExtractionRunPort implements IntakeExtractionRunPort {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private static final String INSERT_SQL = """
      INSERT INTO intake.extraction_run (
        extraction_run_id,
        organization_id,
        information_packet_id,
        mode,
        status,
        input_schema_version,
        output_schema_version,
        extractor_version,
        source_snapshot_hash,
        output_json,
        failure_reason,
        created_at,
        completed_at
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT
        extraction_run_id,
        organization_id,
        information_packet_id,
        mode,
        status,
        input_schema_version,
        output_schema_version,
        extractor_version,
        source_snapshot_hash,
        output_json::text AS output_json,
        failure_reason,
        created_at,
        completed_at
      FROM intake.extraction_run
      WHERE organization_id = ?
        AND extraction_run_id = ?
      """;

  private static final String LIST_BY_PACKET_SQL = """
      SELECT
        extraction_run_id,
        organization_id,
        information_packet_id,
        mode,
        status,
        input_schema_version,
        output_schema_version,
        extractor_version,
        source_snapshot_hash,
        output_json::text AS output_json,
        failure_reason,
        created_at,
        completed_at
      FROM intake.extraction_run
      WHERE organization_id = ?
        AND information_packet_id = ?
      ORDER BY created_at ASC, extraction_run_id ASC
      """;

  private final DataSource dataSource;

  public JdbcIntakeExtractionRunPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public IntakeExtractionRun save(IntakeExtractionRun run) {
    Objects.requireNonNull(run, "run must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      bindSave(statement, run);
      statement.executeUpdate();
      return findById(run.organizationId(), run.extractionRunId())
          .orElseThrow(() -> new IllegalStateException(
              "extraction run was not readable after save"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to save intake extraction run", exception);
    }
  }

  @Override
  public Optional<IntakeExtractionRun> findById(
      UUID organizationId,
      IntakeExtractionRunId extractionRunId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, extractionRunId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toRun(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find intake extraction run by id", exception);
    }
  }

  @Override
  public List<IntakeExtractionRun> listByInformationPacket(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(LIST_BY_PACKET_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, informationPacketId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<IntakeExtractionRun> runs = new ArrayList<>();
        while (resultSet.next()) {
          runs.add(toRun(resultSet));
        }
        return List.copyOf(runs);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to list intake extraction runs for information packet",
          exception);
    }
  }

  private static void bindSave(
      PreparedStatement statement,
      IntakeExtractionRun run) throws SQLException {
    statement.setObject(1, run.extractionRunId().value());
    statement.setObject(2, run.organizationId());
    statement.setObject(3, run.informationPacketId().value());
    statement.setString(4, run.mode().wireValue());
    statement.setString(5, run.status().wireValue());
    statement.setString(6, run.inputSchemaVersion());
    statement.setString(7, run.outputSchemaVersion());
    statement.setString(8, run.extractorVersion());
    statement.setString(9, run.sourceSnapshotHash());
    setNullableJson(statement, 10, outputJson(run));
    setNullableString(statement, 11, run.failureReason().orElse(null));
    statement.setObject(12, OffsetDateTime.ofInstant(run.createdAt(), ZoneOffset.UTC));
    if (run.completedAt().isPresent()) {
      statement.setObject(13, OffsetDateTime.ofInstant(run.completedAt().orElseThrow(), ZoneOffset.UTC));
    } else {
      statement.setNull(13, Types.TIMESTAMP_WITH_TIMEZONE);
    }
  }

  private static IntakeExtractionRun toRun(ResultSet resultSet) throws SQLException {
    return new IntakeExtractionRun(
        new IntakeExtractionRunId(resultSet.getObject("extraction_run_id", UUID.class)),
        resultSet.getObject("organization_id", UUID.class),
        new InformationPacketId(resultSet.getObject("information_packet_id", UUID.class)),
        IntakeExtractionMode.fromWireValue(resultSet.getString("mode")),
        IntakeExtractionStatus.fromWireValue(resultSet.getString("status")),
        resultSet.getString("input_schema_version"),
        resultSet.getString("output_schema_version"),
        resultSet.getString("extractor_version"),
        resultSet.getString("source_snapshot_hash"),
        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
        optionalInstant(resultSet, "completed_at"),
        Optional.ofNullable(resultSet.getString("failure_reason")),
        outputEnvelope(resultSet.getString("output_json")));
  }

  private static Optional<Instant> optionalInstant(ResultSet resultSet, String column)
      throws SQLException {
    OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(value.toInstant());
  }

  private static Optional<IntakeExtractionOutputEnvelope> outputEnvelope(String outputJson) {
    if (outputJson == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(OBJECT_MAPPER.readValue(
          outputJson,
          IntakeExtractionOutputEnvelope.class));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to decode intake extraction output JSON", exception);
    }
  }

  private static String outputJson(IntakeExtractionRun run) {
    if (run.outputEnvelope().isEmpty()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(run.outputEnvelope().orElseThrow());
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to encode intake extraction output JSON", exception);
    }
  }

  private static void setNullableString(
      PreparedStatement statement,
      int index,
      String value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
      return;
    }
    statement.setString(index, value);
  }

  private static void setNullableJson(
      PreparedStatement statement,
      int index,
      String value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.OTHER);
      return;
    }
    statement.setString(index, value);
  }
}
