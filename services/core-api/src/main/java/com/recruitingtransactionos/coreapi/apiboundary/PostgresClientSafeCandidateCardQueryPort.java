package com.recruitingtransactionos.coreapi.apiboundary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientVisibleCandidateFieldPolicy;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

@Deprecated(forRemoval = true)
final class PostgresClientSafeCandidateCardQueryPort
    implements ClientSafeCandidateCardQueryPort {

  private static final String PROJECTION_METADATA_KEY =
      "client_safe_candidate_card_projection_by_ref";

  private static final String FIND_BY_CARD_ID_AND_ORGANIZATION_SQL = """
      SELECT
        candidate_id,
        candidate_profile_id,
        jsonb_extract_path(metadata, ?, ?)::text AS projection_json
      FROM recruiting.candidate_profile
      WHERE organization_id = ?
        AND status IN ('canonical', 'locked')
        AND jsonb_extract_path(metadata, ?, ?) IS NOT NULL
      ORDER BY updated_at DESC, created_at DESC
      LIMIT 2
      """;

  private static final AccessRequest CLIENT_SAFE_READ_ACCESS = new AccessRequest(
      PortalRole.CLIENT,
      ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
      AccessAction.READ,
      FieldClassification.CLIENT_SAFE,
      Set.of(),
      false);

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
      .findAndAddModules()
      .build();

  private final DataSource dataSource;
  private final ClientSafeCandidateProjectionService projectionService;
  private final ReidentificationRiskAssessmentService reidentificationRiskAssessmentService;

  public PostgresClientSafeCandidateCardQueryPort(DataSource dataSource) {
    this(
        dataSource,
        new ClientSafeCandidateProjectionService(),
        new ReidentificationRiskAssessmentService());
  }

  PostgresClientSafeCandidateCardQueryPort(
      DataSource dataSource,
      ClientSafeCandidateProjectionService projectionService,
      ReidentificationRiskAssessmentService reidentificationRiskAssessmentService) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.projectionService =
        Objects.requireNonNull(projectionService, "projectionService must not be null");
    this.reidentificationRiskAssessmentService = Objects.requireNonNull(
        reidentificationRiskAssessmentService,
        "reidentificationRiskAssessmentService must not be null");
  }

  @Override
  public Optional<ClientSafeCandidateCard> findByAnonymousCardId(
      ClientSafeCandidateCardQueryScope scope,
      AnonymousCandidateCardId cardId) {
    Objects.requireNonNull(scope, "scope must not be null");
    Objects.requireNonNull(cardId, "cardId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement =
        connection.prepareStatement(FIND_BY_CARD_ID_AND_ORGANIZATION_SQL)) {
      bind(statement, scope, cardId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        ProjectionRow row = projectionRow(resultSet);
        if (resultSet.next()) {
          return Optional.empty();
        }
        return project(cardId, row);
      }
    } catch (IllegalArgumentException | IllegalStateException | JsonProcessingException
        | SQLException exception) {
      return Optional.empty();
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void bind(
      PreparedStatement statement,
      ClientSafeCandidateCardQueryScope scope,
      AnonymousCandidateCardId cardId)
      throws SQLException {
    statement.setString(1, PROJECTION_METADATA_KEY);
    statement.setString(2, cardId.value());
    statement.setObject(3, scope.organizationId());
    statement.setString(4, PROJECTION_METADATA_KEY);
    statement.setString(5, cardId.value());
  }

  private Optional<ClientSafeCandidateCard> project(
      AnonymousCandidateCardId cardId,
      ProjectionRow row) throws JsonProcessingException {
    ProjectionDocument document = OBJECT_MAPPER.readValue(
        row.projectionJson(),
        ProjectionDocument.class);
    InternalCandidateProjectionSnapshot snapshot = document.toSnapshot(cardId, row);
    return Optional.of(projectionService.project(
        CLIENT_SAFE_READ_ACCESS,
        snapshot,
        reidentificationRiskAssessmentService.assess(snapshot)));
  }

  private static ProjectionRow projectionRow(ResultSet resultSet) throws SQLException {
    return new ProjectionRow(
        resultSet.getObject("candidate_id", UUID.class),
        resultSet.getObject("candidate_profile_id", UUID.class),
        resultSet.getString("projection_json"));
  }

  private record ProjectionRow(UUID candidateId, UUID candidateProfileId, String projectionJson) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ProjectionDocument(
      String rawCandidateId,
      String rawCandidateProfileId,
      String fullName,
      String email,
      String phone,
      String linkedInUrl,
      String exactCurrentEmployer,
      List<String> exactProjectProductOrChipNames,
      String rawSourceText,
      String consultantInternalNotes,
      String anonymousCandidateRef,
      String projectionVersion,
      String redactionLevel,
      String generalizedHeadline,
      String generalizedRoleFamily,
      String generalizedSeniorityBand,
      String generalizedLocationRegion,
      String safeSummary,
      String safeSkillSummary,
      List<String> safeEvidenceSummaries,
      List<String> safeMatchNarratives,
      Set<String> selectedClientVisibleFieldPaths) {

    private InternalCandidateProjectionSnapshot toSnapshot(
        AnonymousCandidateCardId cardId,
        ProjectionRow row) {
      return new InternalCandidateProjectionSnapshot(
          firstNonBlank(rawCandidateId, row.candidateId().toString()),
          firstNonBlank(rawCandidateProfileId, row.candidateProfileId().toString()),
          fullName,
          email,
          phone,
          linkedInUrl,
          exactCurrentEmployer,
          exactProjectProductOrChipNames == null ? List.of() : exactProjectProductOrChipNames,
          rawSourceText,
          consultantInternalNotes,
          cardId,
          AnonymousCandidateRef.of(anonymousCandidateRef),
          projectionVersion,
          RedactionLevel.fromWireValue(redactionLevel),
          generalizedHeadline,
          generalizedRoleFamily,
          generalizedSeniorityBand,
          generalizedLocationRegion,
          safeSummary,
          safeSkillSummary,
          safeEvidenceSummaries,
          safeMatchNarratives,
          selectedClientVisibleFieldPaths == null || selectedClientVisibleFieldPaths.isEmpty()
              ? ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths()
              : selectedClientVisibleFieldPaths);
    }

    private static String firstNonBlank(String preferred, String fallback) {
      if (preferred != null && !preferred.isBlank()) {
        return preferred;
      }
      return fallback;
    }
  }
}
