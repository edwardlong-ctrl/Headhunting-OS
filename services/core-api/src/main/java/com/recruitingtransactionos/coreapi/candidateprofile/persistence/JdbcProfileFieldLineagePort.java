package com.recruitingtransactionos.coreapi.candidateprofile.persistence;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineageId;
import com.recruitingtransactionos.coreapi.candidateprofile.port.ProfileFieldLineagePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcProfileFieldLineagePort implements ProfileFieldLineagePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.profile_field_lineage (
        profile_field_lineage_id,
        organization_id,
        candidate_profile_id,
        candidate_id,
        field_path,
        source_type,
        source_id,
        source_trust,
        provenance_label,
        recorded_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_BY_PROFILE_AND_FIELD_PATH_SQL = """
      SELECT
        profile_field_lineage_id,
        organization_id,
        candidate_profile_id,
        candidate_id,
        field_path,
        source_type,
        source_id,
        source_trust,
        provenance_label,
        recorded_at,
        created_at
      FROM recruiting.profile_field_lineage
      WHERE organization_id = ?
        AND candidate_profile_id = ?
        AND field_path = ?
      ORDER BY recorded_at
      """;

  private static final String FIND_BY_CANDIDATE_AND_FIELD_PATH_SQL = """
      SELECT
        profile_field_lineage_id,
        organization_id,
        candidate_profile_id,
        candidate_id,
        field_path,
        source_type,
        source_id,
        source_trust,
        provenance_label,
        recorded_at,
        created_at
      FROM recruiting.profile_field_lineage
      WHERE organization_id = ?
        AND candidate_id = ?
        AND field_path = ?
      ORDER BY recorded_at
      """;

  private static final String FIND_BY_SOURCE_TYPE_AND_SOURCE_ID_SQL = """
      SELECT
        profile_field_lineage_id,
        organization_id,
        candidate_profile_id,
        candidate_id,
        field_path,
        source_type,
        source_id,
        source_trust,
        provenance_label,
        recorded_at,
        created_at
      FROM recruiting.profile_field_lineage
      WHERE organization_id = ?
        AND source_type = ?
        AND source_id = ?
      ORDER BY recorded_at
      """;

  private final DataSource dataSource;

  public JdbcProfileFieldLineagePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public ProfileFieldLineage append(ProfileFieldLineage lineage) {
    Objects.requireNonNull(lineage, "lineage must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, lineage.profileFieldLineageId().value());
      statement.setObject(2, lineage.organizationId());
      statement.setObject(3, lineage.candidateProfileId().value());
      statement.setObject(4, lineage.candidateId().value());
      statement.setString(5, lineage.fieldPath().value());
      statement.setString(6, lineage.sourceType().wireValue());
      statement.setString(7, lineage.sourceId());
      statement.setString(8, lineage.sourceTrust());
      statement.setString(9, lineage.provenanceLabel());
      statement.setObject(10, OffsetDateTime.ofInstant(lineage.recordedAt(), ZoneOffset.UTC));
      statement.executeUpdate();
      return lineage;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to append profile field lineage", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ProfileFieldLineage> findByProfileAndFieldPath(
      UUID organizationId,
      CandidateProfileId candidateProfileId,
      CandidateProfileFieldPath fieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        FIND_BY_PROFILE_AND_FIELD_PATH_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateProfileId.value());
      statement.setString(3, fieldPath.value());
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find profile field lineage by profile and field path", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ProfileFieldLineage> findByCandidateAndFieldPath(
      UUID organizationId,
      CandidateId candidateId,
      CandidateProfileFieldPath fieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(fieldPath, "fieldPath must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        FIND_BY_CANDIDATE_AND_FIELD_PATH_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      statement.setString(3, fieldPath.value());
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find profile field lineage by candidate and field path", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<ProfileFieldLineage> findBySourceTypeAndSourceId(
      UUID organizationId,
      CandidateProfileFieldSourceType sourceType,
      String sourceId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(sourceId, "sourceId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        FIND_BY_SOURCE_TYPE_AND_SOURCE_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, sourceType.wireValue());
      statement.setString(3, sourceId);
      return findAll(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException(
          "Failed to find profile field lineage by source type and id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static List<ProfileFieldLineage> findAll(PreparedStatement statement)
      throws SQLException {
    try (ResultSet resultSet = statement.executeQuery()) {
      List<ProfileFieldLineage> results = new ArrayList<>();
      while (resultSet.next()) {
        results.add(toProfileFieldLineage(resultSet));
      }
      return List.copyOf(results);
    }
  }

  private static ProfileFieldLineage toProfileFieldLineage(ResultSet resultSet)
      throws SQLException {
    return ProfileFieldLineage.builder()
        .profileFieldLineageId(new ProfileFieldLineageId(
            resultSet.getObject("profile_field_lineage_id", UUID.class)))
        .organizationId(resultSet.getObject("organization_id", UUID.class))
        .candidateProfileId(new CandidateProfileId(
            resultSet.getObject("candidate_profile_id", UUID.class)))
        .candidateId(new CandidateId(resultSet.getObject("candidate_id", UUID.class)))
        .fieldPath(CandidateProfileFieldPath.of(resultSet.getString("field_path")))
        .sourceType(sourceTypeFromWireValue(resultSet.getString("source_type")))
        .sourceId(resultSet.getString("source_id"))
        .sourceTrust(resultSet.getString("source_trust"))
        .provenanceLabel(resultSet.getString("provenance_label"))
        .recordedAt(resultSet.getObject("recorded_at", OffsetDateTime.class).toInstant())
        .createdAt(resultSet.getObject("created_at", OffsetDateTime.class).toInstant())
        .build();
  }

  private static CandidateProfileFieldSourceType sourceTypeFromWireValue(String wireValue) {
    for (CandidateProfileFieldSourceType type : CandidateProfileFieldSourceType.values()) {
      if (type.wireValue().equals(wireValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown source type: " + wireValue);
  }
}
