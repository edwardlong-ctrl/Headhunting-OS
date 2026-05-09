package com.recruitingtransactionos.coreapi.industrypack.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackCalibrationProfile;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import com.recruitingtransactionos.coreapi.industrypack.port.IndustryPackReadPort;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcIndustryPackReadPort implements IndustryPackReadPort {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String FIND_PACK_BY_ID_SQL = """
      SELECT industry_pack_id, pack_key, display_name, maturity, is_active
      FROM recruiting.industry_pack
      WHERE industry_pack_id = ?
      """;

  private static final String FIND_PACK_BY_KEY_SQL = """
      SELECT industry_pack_id, pack_key, display_name, maturity, is_active
      FROM recruiting.industry_pack
      WHERE pack_key = ?
      """;

  private static final String FIND_ACTIVE_VERSION_SQL = """
      SELECT ontology_version_id, industry_pack_id, version_key, source, owner,
        effective_from, review_by, deprecated_at
      FROM recruiting.ontology_version
      WHERE industry_pack_id = ?
        AND effective_from <= ?
        AND (deprecated_at IS NULL OR deprecated_at > ?)
      ORDER BY effective_from DESC
      LIMIT 1
      """;

  private static final String FIND_TEMPLATE_SQL = """
      SELECT industry_role_family_template_id, industry_pack_id, ontology_version_id,
        role_family, display_name, scorecard_dimensions, scoring_guidance,
        interview_question_templates::text AS interview_question_templates,
        evidence_examples::text AS evidence_examples,
        anti_patterns::text AS anti_patterns,
        required_skill_keys::text AS required_skill_keys
      FROM recruiting.industry_role_family_template
      WHERE industry_pack_id = ? AND ontology_version_id = ? AND role_family = ?
      """;

  private static final String FIND_CALIBRATION_PROFILES_SQL = """
      SELECT
        pack.industry_pack_id,
        pack.pack_key,
        pack.display_name,
        pack.maturity,
        pack.is_active,
        pack.calibration_review_by,
        pack.gold_cases::text AS gold_cases,
        pack.negative_cases::text AS negative_cases,
        pack.pack_anti_patterns::text AS pack_anti_patterns,
        pack.score_caps::text AS score_caps,
        pack.drift_signals::text AS drift_signals,
        ontology.ontology_version_id,
        ontology.version_key,
        ontology.source,
        ontology.owner,
        ontology.effective_from,
        ontology.review_by,
        ontology.deprecated_at
      FROM recruiting.industry_pack pack
      JOIN LATERAL (
        SELECT ontology_version_id, industry_pack_id, version_key, source, owner,
          effective_from, review_by, deprecated_at
        FROM recruiting.ontology_version
        WHERE industry_pack_id = pack.industry_pack_id
          AND effective_from <= ?
          AND (deprecated_at IS NULL OR deprecated_at > ?)
        ORDER BY effective_from DESC
        LIMIT 1
      ) ontology ON true
      WHERE pack.is_active = true
      ORDER BY pack.pack_key ASC
      """;

  private final DataSource dataSource;

  public JdbcIndustryPackReadPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<IndustryPack> findById(IndustryPackId industryPackId) {
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    return findPack(FIND_PACK_BY_ID_SQL, statement -> statement.setObject(1, industryPackId.value()));
  }

  @Override
  public Optional<IndustryPack> findByKey(IndustryPackKey packKey) {
    Objects.requireNonNull(packKey, "packKey must not be null");
    return findPack(FIND_PACK_BY_KEY_SQL, statement -> statement.setString(1, packKey.value()));
  }

  @Override
  public Optional<OntologyVersion> findActiveOntologyVersion(IndustryPackId industryPackId, Instant asOf) {
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    Instant instant = asOf == null ? Instant.now() : asOf;
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_VERSION_SQL)) {
      statement.setObject(1, industryPackId.value());
      statement.setObject(2, OffsetDateTime.ofInstant(instant, java.time.ZoneOffset.UTC));
      statement.setObject(3, OffsetDateTime.ofInstant(instant, java.time.ZoneOffset.UTC));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(mapOntologyVersion(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find active ontology version", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<IndustryRoleFamilyTemplate> findRoleFamilyTemplate(
      IndustryPackId industryPackId,
      UUID ontologyVersionId,
      String roleFamily) {
    Objects.requireNonNull(industryPackId, "industryPackId must not be null");
    Objects.requireNonNull(ontologyVersionId, "ontologyVersionId must not be null");
    String normalizedRoleFamily = normalizeRoleFamily(roleFamily);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_TEMPLATE_SQL)) {
      statement.setObject(1, industryPackId.value());
      statement.setObject(2, ontologyVersionId);
      statement.setString(3, normalizedRoleFamily);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(mapRoleFamilyTemplate(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find role family template", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<IndustryPackCalibrationProfile> findCalibrationProfiles(Instant asOf) {
    Instant instant = asOf == null ? Instant.now() : asOf;
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_CALIBRATION_PROFILES_SQL)) {
      OffsetDateTime timestamp = OffsetDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
      statement.setObject(1, timestamp);
      statement.setObject(2, timestamp);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<IndustryPackCalibrationProfile> profiles = new java.util.ArrayList<>();
        while (resultSet.next()) {
          profiles.add(mapCalibrationProfile(resultSet));
        }
        return List.copyOf(profiles);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find industry pack calibration profiles", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private Optional<IndustryPack> findPack(String sql, SqlBinder binder) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      binder.bind(statement);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(mapIndustryPack(resultSet)) : Optional.empty();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find industry pack", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static IndustryPack mapIndustryPack(ResultSet resultSet) throws SQLException {
    return new IndustryPack(
        new IndustryPackId(resultSet.getObject("industry_pack_id", UUID.class)),
        new IndustryPackKey(resultSet.getString("pack_key")),
        resultSet.getString("display_name"),
        IndustryPackMaturity.valueOf(resultSet.getString("maturity").toUpperCase(Locale.ROOT)),
        resultSet.getBoolean("is_active"));
  }

  private static OntologyVersion mapOntologyVersion(ResultSet resultSet) throws SQLException {
    OffsetDateTime deprecatedAt = resultSet.getObject("deprecated_at", OffsetDateTime.class);
    return new OntologyVersion(
        resultSet.getObject("ontology_version_id", UUID.class),
        new IndustryPackId(resultSet.getObject("industry_pack_id", UUID.class)),
        resultSet.getString("version_key"),
        resultSet.getString("source"),
        resultSet.getString("owner"),
        resultSet.getObject("effective_from", OffsetDateTime.class).toInstant(),
        resultSet.getObject("review_by", OffsetDateTime.class).toInstant(),
        deprecatedAt == null ? null : deprecatedAt.toInstant());
  }

  private static IndustryPackCalibrationProfile mapCalibrationProfile(ResultSet resultSet) throws SQLException {
    try {
      return new IndustryPackCalibrationProfile(
          mapIndustryPack(resultSet),
          new OntologyVersion(
              resultSet.getObject("ontology_version_id", UUID.class),
              new IndustryPackId(resultSet.getObject("industry_pack_id", UUID.class)),
              resultSet.getString("version_key"),
              resultSet.getString("source"),
              resultSet.getString("owner"),
              resultSet.getObject("effective_from", OffsetDateTime.class).toInstant(),
              resultSet.getObject("review_by", OffsetDateTime.class).toInstant(),
              resultSet.getObject("deprecated_at", OffsetDateTime.class) == null
                  ? null
                  : resultSet.getObject("deprecated_at", OffsetDateTime.class).toInstant()),
          resultSet.getObject("calibration_review_by", OffsetDateTime.class).toInstant(),
          OBJECT_MAPPER.readValue(resultSet.getString("gold_cases"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("negative_cases"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("pack_anti_patterns"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("score_caps"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("drift_signals"), new TypeReference<List<String>>() {}));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to map industry pack calibration profile", exception);
    }
  }

  private static IndustryRoleFamilyTemplate mapRoleFamilyTemplate(ResultSet resultSet) throws SQLException {
    try {
      return new IndustryRoleFamilyTemplate(
          resultSet.getObject("industry_role_family_template_id", UUID.class),
          new IndustryPackId(resultSet.getObject("industry_pack_id", UUID.class)),
          resultSet.getObject("ontology_version_id", UUID.class),
          resultSet.getString("role_family"),
          resultSet.getString("display_name"),
          resultSet.getString("scorecard_dimensions"),
          resultSet.getString("scoring_guidance"),
          OBJECT_MAPPER.readValue(resultSet.getString("interview_question_templates"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("evidence_examples"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("anti_patterns"), new TypeReference<List<String>>() {}),
          OBJECT_MAPPER.readValue(resultSet.getString("required_skill_keys"), new TypeReference<List<String>>() {}));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to map role family template", exception);
    }
  }

  private static String normalizeRoleFamily(String roleFamily) {
    Objects.requireNonNull(roleFamily, "roleFamily must not be null");
    String normalized = roleFamily.strip().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace('/', '_');
    while (normalized.contains("__")) {
      normalized = normalized.replace("__", "_");
    }
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("roleFamily must not be blank");
    }
    return normalized;
  }

  @FunctionalInterface
  private interface SqlBinder {
    void bind(PreparedStatement statement) throws SQLException;
  }
}
