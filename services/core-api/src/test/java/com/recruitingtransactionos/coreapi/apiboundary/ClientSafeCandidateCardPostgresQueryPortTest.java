package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ClientSafeCandidateCardPostgresQueryPortTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-00000013b001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-00000013b002");
  private static final UUID CANDIDATE_A = uuid("00000000-0000-0000-0000-00000013b003");
  private static final UUID PROFILE_A = uuid("00000000-0000-0000-0000-00000013b004");
  private static final UUID CANDIDATE_B = uuid("00000000-0000-0000-0000-00000013b005");
  private static final UUID PROFILE_B = uuid("00000000-0000-0000-0000-00000013b006");

  private static final String RAW_FULL_NAME = "Task 13B Raw Candidate";
  private static final String RAW_EMAIL = "task13b.raw@example.com";
  private static final String RAW_PHONE = "+86 138 0013 0001";
  private static final String RAW_LINKEDIN = "https://linkedin.example/task13b";
  private static final String RAW_EMPLOYER = "ExactCorp Task13B";
  private static final String RAW_PROJECT = "Exact Task13B NPU";
  private static final String RAW_SOURCE_TEXT =
      "Task 13B Raw Candidate at ExactCorp Task13B on Exact Task13B NPU";
  private static final String RAW_CONSULTANT_NOTES =
      "Do not show this Task 13B consultant note to the client.";

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    insertOrganization(ORG_A);
    insertOrganization(ORG_B);
    insertCandidate(ORG_A, CANDIDATE_A);
    insertCandidate(ORG_B, CANDIDATE_B);
  }

  @Test
  void findsRealClientSafeCardProjectionByAnonymousCardRef() throws SQLException {
    seedSuccessCardProjection();

    Optional<ClientSafeCandidateCard> card = queryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_success_0001"));

    assertThat(card).isPresent();
    assertThat(card.orElseThrow().anonymousCandidateRef().value())
        .isEqualTo("anon_candidate_task13b_success_0001");
    assertThat(card.orElseThrow().redactionLevel()).isEqualTo(RedactionLevel.L2_CLIENT_SAFE);
    String cardText = card.orElseThrow().toString();
    assertThat(cardText)
        .doesNotContain(
            CANDIDATE_A.toString(),
            PROFILE_A.toString(),
            RAW_FULL_NAME,
            RAW_EMAIL,
            RAW_PHONE,
            RAW_LINKEDIN,
            RAW_EMPLOYER,
            RAW_PROJECT,
            RAW_SOURCE_TEXT,
            RAW_CONSULTANT_NOTES,
            "candidateProfileId",
            "fullName",
            "email",
            "phone",
            "linkedInUrl",
            "identityDisclosed");
  }

  @Test
  void queryScopeCannotBeCreatedWithoutOrganizationId() {
    assertThatThrownBy(() -> ClientSafeCandidateCardQueryScope.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void productionBeanPathRequiresScopedQueryForGloballyUniqueAnonymousCardRef()
      throws SQLException {
    seedSuccessCardProjection();

    Optional<ClientSafeCandidateCard> card = productionBeanPathQueryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_success_0001"));

    assertThat(card).isPresent();
    assertThat(card.orElseThrow().anonymousCandidateRef().value())
        .isEqualTo("anon_candidate_task13b_success_0001");
    assertThat(card.orElseThrow().toString())
        .doesNotContain(
            CANDIDATE_A.toString(),
            PROFILE_A.toString(),
            RAW_FULL_NAME,
            RAW_EMAIL,
            RAW_PHONE,
            RAW_LINKEDIN,
            RAW_EMPLOYER,
            RAW_PROJECT,
            RAW_SOURCE_TEXT,
            RAW_CONSULTANT_NOTES);
  }

  @Test
  void existingApiRouteReturnsRealSafeCardFromBackendData() throws SQLException {
    seedSuccessCardProjection();
    ClientSafeCandidateCardController controller = new ClientSafeCandidateCardController(
        new ClientSafeCandidateCardApiQueryService(queryPort()));

    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> response =
        controller.readClientSafeCandidateCard(
            "card_task13b_success_0001",
            new RtoAuthenticatedPrincipal(UUID.randomUUID(), ORG_A, PortalRole.CLIENT, "Test", UUID.randomUUID()));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).isInstanceOf(ClientSafeCandidateCardResponse.class);
    ClientSafeCandidateCardResponse data =
        (ClientSafeCandidateCardResponse) response.getBody().data();
    assertThat(data.anonymousCardRef()).isEqualTo("card_task13b_success_0001");
    assertThat(data.clientAlias()).startsWith("alias-");
    assertThat(data.redactionLevel()).isEqualTo("l2_client_safe");
    assertThat(data.toString())
        .doesNotContain(
            CANDIDATE_A.toString(),
            PROFILE_A.toString(),
            RAW_FULL_NAME,
            RAW_EMAIL,
            RAW_PHONE,
            RAW_LINKEDIN,
            RAW_EMPLOYER,
            RAW_PROJECT,
            RAW_SOURCE_TEXT,
            RAW_CONSULTANT_NOTES,
            "candidateProfileId",
            "fullName",
            "email",
            "phone",
            "linkedInUrl",
            "identityDisclosed");
  }

  @Test
  void productionBeanPathDeniesCrossOrganizationCardRefs() throws SQLException {
    String otherOrgOnlyCardRef = "card_task13b_other_org_only_0001";
    insertCandidateProfile(
        ORG_B,
        CANDIDATE_B,
        uuid("00000000-0000-0000-0000-00000013b304"),
        otherOrgOnlyCardRef,
        """
            {
              "anonymousCandidateRef": "anon_candidate_task13b_other_org_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Other organization candidate",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "This safe text belongs to another organization.",
              "safeSkillSummary": "Safe skill summary.",
              "safeEvidenceSummaries": ["Other organization evidence."],
              "safeMatchNarratives": ["Other organization match narrative."]
            }
            """);
    ClientSafeCandidateCardController controller = new ClientSafeCandidateCardController(
        new ClientSafeCandidateCardApiQueryService(productionBeanPathQueryPort()));

    ResponseEntity<ApiResponseEnvelope<ApiSafeResponseBody>> response =
        controller.readClientSafeCandidateCard(
            otherOrgOnlyCardRef,
            new RtoAuthenticatedPrincipal(UUID.randomUUID(), ORG_A, PortalRole.CLIENT, "Test", UUID.randomUUID()));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().error()).isNotNull();
    assertThat(response.getBody().error().safeReason())
        .isEqualTo("client_safe_candidate_card_unavailable");
  }

  @Test
  void scopedRouteHandlesDuplicateAnonymousCardRefsWithinOrganizationBoundary()
      throws SQLException {
    String reusedCardRef = "card_task13b_cross_org_duplicate_0001";
    insertCandidateProfile(
        ORG_A,
        CANDIDATE_A,
        uuid("00000000-0000-0000-0000-00000013b504"),
        reusedCardRef,
        """
            {
              "anonymousCandidateRef": "anon_candidate_task13b_duplicate_a_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Organization A candidate",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "Organization A safe text.",
              "safeSkillSummary": "Safe skill summary.",
              "safeEvidenceSummaries": ["Organization A evidence."],
              "safeMatchNarratives": ["Organization A match narrative."]
            }
            """);
    insertCandidateProfile(
        ORG_B,
        CANDIDATE_B,
        uuid("00000000-0000-0000-0000-00000013b604"),
        reusedCardRef,
        """
            {
              "anonymousCandidateRef": "anon_candidate_task13b_duplicate_b_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Organization B candidate",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "Organization B safe text.",
              "safeSkillSummary": "Safe skill summary.",
              "safeEvidenceSummaries": ["Organization B evidence."],
              "safeMatchNarratives": ["Organization B match narrative."]
            }
            """);

    assertThat(productionBeanPathQueryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of(reusedCardRef))).isPresent();
    assertThat(productionBeanPathQueryPort().findByAnonymousCardId(
        scope(ORG_B),
        AnonymousCandidateCardId.of(reusedCardRef))).isPresent();
  }

  private static void seedSuccessCardProjection() throws SQLException {
    if (countProjectionRows("card_task13b_success_0001") > 0) {
      return;
    }
    insertCandidateProfile(
        ORG_A,
        CANDIDATE_A,
        PROFILE_A,
        "card_task13b_success_0001",
        """
            {
              "rawCandidateId": "00000000-0000-0000-0000-00000013b003",
              "rawCandidateProfileId": "00000000-0000-0000-0000-00000013b004",
              "fullName": "Task 13B Raw Candidate",
              "rawSourceText": "Task 13B Raw Candidate at ExactCorp Task13B on Exact Task13B NPU",
              "consultantInternalNotes": "Do not show this Task 13B consultant note to the client.",
              "anonymousCandidateRef": "anon_candidate_task13b_success_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Senior verification leader in advanced-chip programs",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "Has led complex verification programs without disclosing employer or code names.",
              "safeSkillSummary": "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
              "safeEvidenceSummaries": ["Evidence generalized from approved profile signals."],
              "safeMatchNarratives": ["Strong fit based on generalized capability evidence."]
            }
            """);
  }

  @Test
  void returnsUnavailableWhenNoEligibleSafeProjectionExists() throws SQLException {
    insertCandidateProfile(
        ORG_A,
        CANDIDATE_A,
        uuid("00000000-0000-0000-0000-00000013b104"),
        "card_task13b_unavailable_0001",
        """
            {
              "anonymousCandidateRef": "anon_candidate_task13b_unavailable_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l4_identity_disclosed",
              "generalizedHeadline": "Identity-disclosed output must not be used",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "This row is not eligible for anonymous client output.",
              "safeSkillSummary": "This row is not eligible for anonymous client output.",
              "safeEvidenceSummaries": ["Unsafe L4 projection."],
              "safeMatchNarratives": ["Unsafe L4 projection."]
            }
            """);

    assertThat(queryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_missing_0001"))).isEmpty();
    assertThat(queryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_unavailable_0001"))).isEmpty();
  }

  @Test
  void doesNotReturnProjectionFromAnotherOrganizationOrUnsafeCarryover()
      throws SQLException {
    String reusedCardRef = "card_task13b_reused_scope_0001";
    insertCandidateProfile(
        ORG_B,
        CANDIDATE_B,
        PROFILE_B,
        reusedCardRef,
        """
            {
              "anonymousCandidateRef": "anon_candidate_task13b_scope_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Other organization candidate",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "This projection belongs to another organization.",
              "safeSkillSummary": "Safe skill summary.",
              "safeEvidenceSummaries": ["Other organization evidence."],
              "safeMatchNarratives": ["Other organization match narrative."]
            }
            """);
    insertCandidateProfile(
        ORG_A,
        CANDIDATE_A,
        uuid("00000000-0000-0000-0000-00000013b204"),
        "card_task13b_unsafe_carryover_0001",
        """
            {
              "fullName": "Task 13B Raw Candidate",
              "anonymousCandidateRef": "anon_candidate_task13b_unsafe_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Senior verification leader",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "Task 13B Raw Candidate is unsafe raw identity carryover.",
              "safeSkillSummary": "Safe skill summary.",
              "safeEvidenceSummaries": ["Evidence generalized from approved profile signals."],
              "safeMatchNarratives": ["Strong fit based on generalized capability evidence."]
            }
            """);

    ClientSafeCandidateCardQueryPort scopedOrgAQuery = queryPort(ORG_A);

    assertThat(scopedOrgAQuery.findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of(reusedCardRef))).isEmpty();
    assertThat(scopedOrgAQuery.findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_unsafe_carryover_0001"))).isEmpty();
    assertThat(queryPort(ORG_B).findByAnonymousCardId(
        scope(ORG_B),
        AnonymousCandidateCardId.of(reusedCardRef))).isPresent();
  }

  @Test
  void snapshotLevelReidentificationRisksFailClosedEvenWhenOutputTextIsGeneralized()
      throws SQLException {
    insertCandidateProfile(
        ORG_A,
        CANDIDATE_A,
        uuid("00000000-0000-0000-0000-00000013b404"),
        "card_task13b_snapshot_risk_0001",
        """
            {
              "exactCurrentEmployer": "ExactCorp Task13B",
              "exactProjectProductOrChipNames": ["Exact Task13B NPU"],
              "anonymousCandidateRef": "anon_candidate_task13b_snapshot_risk_0001",
              "projectionVersion": "projection-v13b",
              "redactionLevel": "l2_client_safe",
              "generalizedHeadline": "Senior verification leader",
              "generalizedRoleFamily": "semiconductor_verification",
              "generalizedSeniorityBand": "senior_ic",
              "generalizedLocationRegion": "greater_china",
              "safeSummary": "Has led complex verification programs without disclosing employer or code names.",
              "safeSkillSummary": "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
              "safeEvidenceSummaries": ["Evidence generalized from approved profile signals."],
              "safeMatchNarratives": ["Strong fit based on generalized capability evidence."]
            }
            """);

    assertThat(queryPort().findByAnonymousCardId(
        scope(ORG_A),
        AnonymousCandidateCardId.of("card_task13b_snapshot_risk_0001"))).isEmpty();
  }

  private static ClientSafeCandidateCardQueryPort queryPort() {
    return queryPort(ORG_A);
  }

  private static ClientSafeCandidateCardQueryPort productionBeanPathQueryPort() {
    return new PostgresClientSafeCandidateCardQueryPort(dataSource);
  }

  private static ClientSafeCandidateCardQueryPort queryPort(UUID organizationId) {
    return new PostgresClientSafeCandidateCardQueryPort(dataSource);
  }

  private static ClientSafeCandidateCardQueryScope scope(UUID organizationId) {
    return ClientSafeCandidateCardQueryScope.of(organizationId);
  }

  private static void insertCandidateProfile(
      UUID organizationId,
      UUID candidateId,
      UUID profileId,
      String anonymousCardRef,
      String projectionJson) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate_profile (
              candidate_profile_id,
              organization_id,
              candidate_id,
              profile_version,
              status,
              field_status_map,
              metadata
            )
            VALUES (?, ?, ?, ?, 'canonical', '{}'::jsonb, jsonb_build_object(
              'client_safe_candidate_card_projection_by_ref',
              jsonb_build_object(?::text, ?::jsonb)
            ))
            """)) {
      statement.setObject(1, profileId);
      statement.setObject(2, organizationId);
      statement.setObject(3, candidateId);
      statement.setInt(4, Math.abs(profileId.hashCode() % 100000) + 1);
      statement.setString(5, anonymousCardRef);
      statement.setString(6, projectionJson);
      statement.executeUpdate();
    }
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, "Task 13B Org " + organizationId);
      statement.setString(3, "Task 13B Org");
      statement.executeUpdate();
    }
  }

  private static void insertCandidate(UUID organizationId, UUID candidateId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate (
              candidate_id,
              organization_id,
              status,
              privacy_status
            )
            VALUES (?, ?, 'available', 'internal_only')
            """)) {
      statement.setObject(1, candidateId);
      statement.setObject(2, organizationId);
      statement.executeUpdate();
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static int countProjectionRows(String anonymousCardRef) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM recruiting.candidate_profile
            WHERE jsonb_extract_path(
                metadata,
                'client_safe_candidate_card_projection_by_ref',
                ?::text
              ) IS NOT NULL
            """)) {
      statement.setString(1, anonymousCardRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return connection();
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
      }

      @Override
      public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
      }

      @Override
      public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("DriverManager parent logger is not supported");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
      }
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }
}
