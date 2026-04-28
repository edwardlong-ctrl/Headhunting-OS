package com.recruitingtransactionos.coreapi.candidateprofile.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflict;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStaleness;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCandidateProfilePersistencePort implements CandidateProfilePersistencePort {

  private static final String FIELD_DOCUMENTS_METADATA_KEY = "candidate_profile_fields";
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
      .findAndAddModules()
      .build();
  private static final TypeReference<Map<String, StoredFieldDocument>> STORED_FIELD_MAP_TYPE =
      new TypeReference<>() {
      };

  private static final String INSERT_PROFILE_SQL = """
      INSERT INTO recruiting.candidate_profile (
        candidate_profile_id,
        organization_id,
        candidate_id,
        profile_version,
        status,
        field_status_map,
        source_claim_ids,
        metadata
      )
      SELECT ?, ?, ?, ?, 'draft', ?::jsonb, ?, ?::jsonb
      WHERE EXISTS (
        SELECT 1
        FROM recruiting.candidate
        WHERE organization_id = ?
          AND candidate_id = ?
      )
      """;

  private static final String FIND_PROFILE_BY_ID_SQL = """
      SELECT
        candidate_profile_id,
        organization_id,
        candidate_id,
        profile_version,
        field_status_map::text AS field_status_map,
        metadata::text AS metadata,
        created_at,
        updated_at
      FROM recruiting.candidate_profile
      WHERE organization_id = ?
        AND candidate_profile_id = ?
      """;

  private static final String FIND_PROFILE_BY_CANDIDATE_ID_SQL = """
      SELECT
        candidate_profile_id,
        organization_id,
        candidate_id,
        profile_version,
        field_status_map::text AS field_status_map,
        metadata::text AS metadata,
        created_at,
        updated_at
      FROM recruiting.candidate_profile
      WHERE organization_id = ?
        AND candidate_id = ?
      ORDER BY profile_version DESC, created_at DESC
      LIMIT 1
      """;

  private static final String FIND_FIELD_DOCUMENTS_FOR_UPDATE_SQL = """
      SELECT
        field_status_map::text AS field_status_map,
        metadata::text AS metadata
      FROM recruiting.candidate_profile
      WHERE organization_id = ?
        AND candidate_profile_id = ?
      """;

  private static final String UPDATE_FIELD_SQL = """
      UPDATE recruiting.candidate_profile
      SET field_status_map = ?::jsonb,
          source_claim_ids = ?,
          metadata = ?::jsonb,
          updated_at = now(),
          version = version + 1
      WHERE organization_id = ?
        AND candidate_profile_id = ?
      """;

  private final DataSource dataSource;

  public JdbcCandidateProfilePersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public CandidateProfile createCandidateProfile(CandidateProfile candidateProfile) {
    Objects.requireNonNull(candidateProfile, "candidateProfile must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_PROFILE_SQL)) {
      bindCreate(connection, statement, candidateProfile);
      int inserted = statement.executeUpdate();
      if (inserted != 1) {
        throw new IllegalArgumentException("candidate not found in organization");
      }
      return findCandidateProfileByIdAndOrganizationId(
          candidateProfile.organizationId(),
          candidateProfile.candidateProfileId())
          .orElseThrow(() -> new IllegalStateException(
          "candidate profile was not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create candidate profile", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_PROFILE_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateProfileId.value());
      return findOne(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidate profile by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
      UUID organizationId,
      CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        FIND_PROFILE_BY_CANDIDATE_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId.value());
      return findOne(statement);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find candidate profile by candidate id",
          exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public CandidateProfileField upsertCandidateProfileField(
      UUID organizationId,
      CandidateProfileId candidateProfileId,
      CandidateProfileField field) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    Objects.requireNonNull(field, "field must not be null");

    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      StoredProfileJson storedProfileJson = findStoredProfileJson(
          connection,
          organizationId,
          candidateProfileId);
      Map<String, String> statusMap = readStatusMap(storedProfileJson.fieldStatusMapJson());
      ObjectNode metadata = readMetadata(storedProfileJson.metadataJson());
      Map<String, StoredFieldDocument> fieldDocuments = readFieldDocuments(metadata);

      statusMap.put(field.fieldPath().value(), field.fieldStatus().wireValue());
      fieldDocuments.put(field.fieldPath().value(), StoredFieldDocument.from(field));
      writeFieldDocuments(metadata, fieldDocuments);

      try (PreparedStatement statement = connection.prepareStatement(UPDATE_FIELD_SQL)) {
        statement.setString(1, writeJson(statusMap));
        Array sourceClaimIds = connection.createArrayOf(
            "uuid",
            sourceClaimIds(fieldDocuments).toArray(UUID[]::new));
        statement.setArray(2, sourceClaimIds);
        statement.setString(3, writeJson(metadata));
        statement.setObject(4, organizationId);
        statement.setObject(5, candidateProfileId.value());
        int updated = statement.executeUpdate();
        sourceClaimIds.free();
        if (updated != 1) {
          throw new IllegalArgumentException("candidate profile not found in organization");
        }
      }
      return field;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to upsert candidate profile field", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CandidateProfileField> listCandidateProfileFields(
      UUID organizationId,
      CandidateProfileId candidateProfileId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
    return findCandidateProfileByIdAndOrganizationId(organizationId, candidateProfileId)
        .map(CandidateProfile::fields)
        .orElseGet(List::of);
  }

  private static void bindCreate(
      Connection connection,
      PreparedStatement statement,
      CandidateProfile candidateProfile) throws SQLException {
    Map<String, String> statusMap = statusMap(candidateProfile.fields());
    ObjectNode metadata = metadataWithFields(candidateProfile.fields());
    Array sourceClaimIds = connection.createArrayOf(
        "uuid",
        sourceClaimIds(candidateProfile.fields()).toArray(UUID[]::new));

    statement.setObject(1, candidateProfile.candidateProfileId().value());
    statement.setObject(2, candidateProfile.organizationId());
    statement.setObject(3, candidateProfile.candidateId().value());
    statement.setInt(4, candidateProfile.profileVersion().value());
    statement.setString(5, writeJson(statusMap));
    statement.setArray(6, sourceClaimIds);
    statement.setString(7, writeJson(metadata));
    statement.setObject(8, candidateProfile.organizationId());
    statement.setObject(9, candidateProfile.candidateId().value());
  }

  private static Optional<CandidateProfile> findOne(PreparedStatement statement)
      throws SQLException {
    try (ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        return Optional.empty();
      }
      return Optional.of(toCandidateProfile(resultSet));
    }
  }

  private static CandidateProfile toCandidateProfile(ResultSet resultSet) throws SQLException {
    String metadataJson = resultSet.getString("metadata");
    return CandidateProfile.builder()
        .candidateProfileId(new CandidateProfileId(resultSet.getObject(
            "candidate_profile_id",
            UUID.class)))
        .organizationId(resultSet.getObject("organization_id", UUID.class))
        .candidateId(new CandidateId(resultSet.getObject("candidate_id", UUID.class)))
        .profileVersion(new CandidateProfileVersion(resultSet.getInt("profile_version")))
        .fields(fieldsFromMetadata(metadataJson))
        .createdAt(resultSet.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(resultSet.getObject("updated_at", OffsetDateTime.class).toInstant())
        .build();
  }

  private static StoredProfileJson findStoredProfileJson(
      Connection connection,
      UUID organizationId,
      CandidateProfileId candidateProfileId) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(FIND_FIELD_DOCUMENTS_FOR_UPDATE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateProfileId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new IllegalArgumentException("candidate profile not found in organization");
        }
        return new StoredProfileJson(
            resultSet.getString("field_status_map"),
            resultSet.getString("metadata"));
      }
    }
  }

  private static Map<String, String> statusMap(List<CandidateProfileField> fields) {
    Map<String, String> statusMap = new LinkedHashMap<>();
    for (CandidateProfileField field : fields) {
      statusMap.put(field.fieldPath().value(), field.fieldStatus().wireValue());
    }
    return statusMap;
  }

  private static ObjectNode metadataWithFields(List<CandidateProfileField> fields) {
    ObjectNode metadata = OBJECT_MAPPER.createObjectNode();
    Map<String, StoredFieldDocument> fieldDocuments = new LinkedHashMap<>();
    for (CandidateProfileField field : fields) {
      fieldDocuments.put(field.fieldPath().value(), StoredFieldDocument.from(field));
    }
    writeFieldDocuments(metadata, fieldDocuments);
    return metadata;
  }

  private static List<CandidateProfileField> fieldsFromMetadata(String metadataJson) {
    Map<String, StoredFieldDocument> fieldDocuments = readFieldDocuments(readMetadata(metadataJson));
    List<CandidateProfileField> fields = new ArrayList<>();
    for (Map.Entry<String, StoredFieldDocument> entry : fieldDocuments.entrySet()) {
      fields.add(entry.getValue().toCandidateProfileField(entry.getKey()));
    }
    return List.copyOf(fields);
  }

  private static Map<String, String> readStatusMap(String statusMapJson) {
    try {
      return new LinkedHashMap<>(OBJECT_MAPPER.readValue(
          statusMapJson,
          new TypeReference<Map<String, String>>() {
          }));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to read candidate profile field_status_map",
          exception);
    }
  }

  private static ObjectNode readMetadata(String metadataJson) {
    try {
      JsonNode node = OBJECT_MAPPER.readTree(metadataJson);
      if (node == null || !node.isObject()) {
        return OBJECT_MAPPER.createObjectNode();
      }
      return (ObjectNode) node.deepCopy();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to read candidate profile metadata", exception);
    }
  }

  private static Map<String, StoredFieldDocument> readFieldDocuments(ObjectNode metadata) {
    JsonNode fieldNode = metadata.path(FIELD_DOCUMENTS_METADATA_KEY);
    if (!fieldNode.isObject()) {
      return new LinkedHashMap<>();
    }
    try {
      return new LinkedHashMap<>(OBJECT_MAPPER.convertValue(fieldNode, STORED_FIELD_MAP_TYPE));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Failed to read candidate profile field documents",
          exception);
    }
  }

  private static void writeFieldDocuments(
      ObjectNode metadata,
      Map<String, StoredFieldDocument> fieldDocuments) {
    metadata.set(FIELD_DOCUMENTS_METADATA_KEY, OBJECT_MAPPER.valueToTree(fieldDocuments));
  }

  private static Set<UUID> sourceClaimIds(List<CandidateProfileField> fields) {
    Set<UUID> sourceClaimIds = new LinkedHashSet<>();
    for (CandidateProfileField field : fields) {
      if (field.sourceClaimId() != null) {
        sourceClaimIds.add(field.sourceClaimId().value());
      }
    }
    return sourceClaimIds;
  }

  private static Set<UUID> sourceClaimIds(Map<String, StoredFieldDocument> fieldDocuments) {
    Set<UUID> sourceClaimIds = new LinkedHashSet<>();
    for (StoredFieldDocument document : fieldDocuments.values()) {
      if (document.sourceClaimId() != null) {
        sourceClaimIds.add(document.sourceClaimId());
      }
    }
    return sourceClaimIds;
  }

  private static String writeJson(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("value must be JSON serializable", exception);
    }
  }

  private static CandidateProfileFieldStatus statusFromWireValue(String wireValue) {
    for (CandidateProfileFieldStatus status : CandidateProfileFieldStatus.values()) {
      if (status.wireValue().equals(wireValue)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown candidate profile field status: " + wireValue);
  }

  private record StoredProfileJson(String fieldStatusMapJson, String metadataJson) {
  }

  private record StoredFieldDocument(
      String valueJson,
      String fieldStatus,
      CandidateProfileFieldLineage lineage,
      CandidateProfileFieldConflict conflict,
      CandidateProfileFieldStaleness staleness,
      Instant lastReviewedAt,
      UUID confirmedByActorId,
      CandidateProfileVersion confirmedAgainstProfileVersion,
      UUID sourceClaimId,
      UUID sourceReviewEventId,
      UUID sourceWorkflowEventId,
      String notes) {

    private static StoredFieldDocument from(CandidateProfileField field) {
      return new StoredFieldDocument(
          field.value().jsonValue(),
          field.fieldStatus().wireValue(),
          field.lineage(),
          field.conflict(),
          field.staleness(),
          field.lastReviewedAt(),
          field.confirmedByActorId(),
          field.confirmedAgainstProfileVersion(),
          field.sourceClaimId() == null ? null : field.sourceClaimId().value(),
          field.sourceReviewEventId() == null ? null : field.sourceReviewEventId().value(),
          field.sourceWorkflowEventId() == null ? null : field.sourceWorkflowEventId().value(),
          field.notes());
    }

    private CandidateProfileField toCandidateProfileField(String fieldPath) {
      return CandidateProfileField.builder()
          .fieldPath(CandidateProfileFieldPath.of(fieldPath))
          .value(CandidateProfileFieldValue.ofJson(valueJson))
          .fieldStatus(statusFromWireValue(fieldStatus))
          .lineage(lineage)
          .conflict(conflict)
          .staleness(staleness)
          .lastReviewedAt(lastReviewedAt)
          .confirmedByActorId(confirmedByActorId)
          .confirmedAgainstProfileVersion(confirmedAgainstProfileVersion)
          .sourceClaimId(sourceClaimId == null ? null : new ClaimId(sourceClaimId))
          .sourceReviewEventId(
              sourceReviewEventId == null ? null : new ReviewEventId(sourceReviewEventId))
          .sourceWorkflowEventId(
              sourceWorkflowEventId == null ? null : new WorkflowEventId(sourceWorkflowEventId))
          .notes(notes)
          .build();
    }
  }
}
