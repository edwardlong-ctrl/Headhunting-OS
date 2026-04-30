package com.recruitingtransactionos.coreapi.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CandidateProfilePostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000290001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000290002");
  private static final CandidateId CANDIDATE_A = new CandidateId(
      uuid("00000000-0000-0000-0000-000000290003"));
  private static final CandidateId CANDIDATE_B = new CandidateId(
      uuid("00000000-0000-0000-0000-000000290004"));
  private static final UUID ACTOR_A = uuid("00000000-0000-0000-0000-000000290005");
  private static final Instant NOW = Instant.parse("2026-04-28T22:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
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
  void flywayMigrationAppliesAndReusesExistingCandidateProfileTable() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(12);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
    assertThat(tableExists("recruiting", "candidate_profile")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "field_status_map")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "metadata")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "source_claim_ids")).isTrue();
    assertThat(indexExists("recruiting", "candidate_profile_version_uidx")).isTrue();
  }

  @Test
  void createCandidateProfilePersistsAndReadsBackByProfileIdAndCandidateId()
      throws SQLException {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290201"));
    insertCandidate(ORG_A, candidateId);

    CandidateProfile profile = service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(1),
        List.of()));

    Optional<CandidateProfile> byProfileId = service()
        .findCandidateProfileByIdAndOrganizationId(ORG_A, profile.candidateProfileId());
    Optional<CandidateProfile> byCandidateId = service()
        .findCandidateProfileByCandidateIdAndOrganizationId(ORG_A, candidateId);

    assertThat(byProfileId).contains(profile);
    assertThat(byCandidateId).contains(profile);
    assertThat(countCandidateProfileRows(profile.candidateProfileId())).isEqualTo(1);
    assertThat(countRows("governance.claim_ledger_item", ORG_A)).isZero();
    assertThat(countRows("governance.review_event", ORG_A)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
  }

  @Test
  void profileLookupIsOrganizationScoped() {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290202"));
    try {
      insertCandidate(ORG_A, candidateId);
    } catch (SQLException exception) {
      throw new IllegalStateException("failed to insert profile-scope test candidate", exception);
    }

    CandidateProfile profile = service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(2),
        List.of()));

    assertThat(service().findCandidateProfileByIdAndOrganizationId(
        ORG_B,
        profile.candidateProfileId())).isEmpty();
  }

  @Test
  void candidateIdLookupIsOrganizationScoped() {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290203"));
    try {
      insertCandidate(ORG_A, candidateId);
    } catch (SQLException exception) {
      throw new IllegalStateException("failed to insert candidate-scope test candidate", exception);
    }

    service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(3),
        List.of()));

    assertThat(service().findCandidateProfileByCandidateIdAndOrganizationId(ORG_B, candidateId))
        .isEmpty();
  }

  @Test
  void upsertFieldPersistsPathValueStatusLineageConflictAndStaleness() throws SQLException {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290204"));
    insertCandidate(ORG_A, candidateId);

    CandidateProfile profile = service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(4),
        List.of()));
    CandidateProfileField field = service().upsertCandidateProfileField(fieldRequest(
        profile.candidateProfileId(),
        CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY,
        CandidateProfileFieldValue.ofString("55000 RMB monthly"),
        CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED));

    CandidateProfile reloaded = service()
        .findCandidateProfileByIdAndOrganizationId(ORG_A, profile.candidateProfileId())
        .orElseThrow();

    assertThat(reloaded.fields()).containsExactly(field);
    assertThat(service().listCandidateProfileFields(ORG_A, profile.candidateProfileId()))
        .containsExactly(field);
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED);
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(field.fieldStatus()))
        .isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isClientFactEligible(field.fieldStatus()))
        .isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isTransactionReadyEligible(field.fieldStatus()))
        .isFalse();
    assertThat(field.lineage().sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .containsExactly(CandidateProfileFieldSourceType.SOURCE_SPAN);
    assertThat(field.conflict()).isNotNull();
    assertThat(field.conflict().hasMultipleSourceBackedValues()).isTrue();
    assertThat(field.staleness()).isNotNull();
    assertThat(field.staleness().stale()).isTrue();
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED);
    assertThat(field.sourceClaimId()).isNotNull();
    assertThat(field.sourceReviewEventId()).isNotNull();
    assertThat(rawFieldStatusMap(profile.candidateProfileId()))
        .contains("compensation.expected_salary")
        .contains("human_acknowledged");
    assertThat(rawMetadata(profile.candidateProfileId()))
        .contains("candidate_profile_fields")
        .contains("55000 RMB monthly")
        .contains("sourceClaimId")
        .contains("sourceReviewEventId");
    assertThat(rawSourceClaimIds(profile.candidateProfileId()))
        .contains(field.sourceClaimId().value());
  }

  @Test
  void upsertFieldPersistsAndReadsBackFullLineageStaleAndConflictMetadata()
      throws SQLException {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290206"));
    insertCandidate(ORG_A, candidateId);
    ClaimId claimId = new ClaimId(uuid("00000000-0000-0000-0000-000000290111"));
    ReviewEventId reviewEventId = new ReviewEventId(
        uuid("00000000-0000-0000-0000-000000290112"));
    WorkflowEventId workflowEventId = new WorkflowEventId(
        uuid("00000000-0000-0000-0000-000000290113"));
    UUID sourceItemId = uuid("00000000-0000-0000-0000-000000290114");
    UUID packetId = uuid("00000000-0000-0000-0000-000000290115");
    UUID extractionRunId = uuid("00000000-0000-0000-0000-000000290116");

    CandidateProfile profile = service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(6),
        List.of()));
    CandidateProfileField field = service().upsertCandidateProfileField(
        fullMetadataFieldRequest(
            profile.candidateProfileId(),
            claimId,
            reviewEventId,
            workflowEventId,
            sourceItemId,
            packetId,
            extractionRunId));

    CandidateProfileField reloaded = service()
        .findCandidateProfileByIdAndOrganizationId(ORG_A, profile.candidateProfileId())
        .orElseThrow()
        .fields()
        .getFirst();

    assertThat(reloaded).isEqualTo(field);
    assertThat(reloaded.value()).isEqualTo(CandidateProfileFieldValue.ofString(
        "55000 RMB monthly"));
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(reloaded.fieldStatus()))
        .isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.blocksClientVisibleFactStatement(
        reloaded.fieldStatus())).isTrue();
    assertThat(reloaded.lineage().sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .containsExactly(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_ITEM,
            CandidateProfileFieldSourceType.INFORMATION_PACKET,
            CandidateProfileFieldSourceType.INTAKE_EXTRACTION_RUN,
            CandidateProfileFieldSourceType.SOURCE_SPAN,
            CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE);
    assertThat(reloaded.lineage().sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceId)
        .contains(
            claimId.value().toString(),
            reviewEventId.value().toString(),
            workflowEventId.value().toString(),
            sourceItemId.toString(),
            packetId.toString(),
            extractionRunId.toString(),
            "span:postgres-test:full-lineage:compensation.expected_salary",
            "external-reference:salary-benchmark:290117");
    assertThat(reloaded.staleness().stale()).isTrue();
    assertThat(reloaded.staleness().staleReason())
        .isEqualTo("salary data must be refreshed before client-visible use");
    assertThat(reloaded.conflict().severity()).isEqualTo(CandidateProfileFieldConflictSeverity.BLOCKING);
    assertThat(reloaded.conflict().resolutionStatus())
        .isEqualTo(CandidateProfileFieldConflictResolutionStatus.NEEDS_REVIEW);
    assertThat(reloaded.conflict().conflictingValues())
        .extracting(CandidateProfileFieldConflictValue::value)
        .containsExactly(
            CandidateProfileFieldValue.ofString("45000 RMB monthly"),
            CandidateProfileFieldValue.ofString("55000 RMB monthly"));
    assertThat(service().listCandidateProfileFields(ORG_B, profile.candidateProfileId()))
        .isEmpty();

    String metadata = rawMetadata(profile.candidateProfileId());
    assertThat(metadata)
        .contains("candidate_profile_fields")
        .contains(claimId.value().toString())
        .contains(reviewEventId.value().toString())
        .contains(workflowEventId.value().toString())
        .contains(sourceItemId.toString())
        .contains(packetId.toString())
        .contains(extractionRunId.toString())
        .contains("span:postgres-test:full-lineage:compensation.expected_salary")
        .contains("external-reference:salary-benchmark:290117")
        .contains("salary data must be refreshed before client-visible use");
    assertThat(rawSourceClaimIds(profile.candidateProfileId()))
        .containsExactly(claimId.value());
  }

  @Test
  void systemInferencePersistsOnlyAsNonVerifiedInternalFieldStatus() {
    CandidateId candidateId = new CandidateId(uuid("00000000-0000-0000-0000-000000290205"));
    try {
      insertCandidate(ORG_A, candidateId);
    } catch (SQLException exception) {
      throw new IllegalStateException("failed to insert system-inference test candidate", exception);
    }

    CandidateProfile profile = service().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        candidateId,
        new CandidateProfileVersion(5),
        List.of()));

    CandidateProfileField field = service().upsertCandidateProfileField(fieldRequest(
        profile.candidateProfileId(),
        CandidateProfileFieldPath.INTENT_INTEREST_LEVEL,
        CandidateProfileFieldValue.ofString("possible passive interest"),
        CandidateProfileFieldStatus.SYSTEM_INFERENCE));

    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.SYSTEM_INFERENCE);
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(field.fieldStatus()))
        .isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.blocksClientVisibleFactStatement(
        field.fieldStatus())).isTrue();
  }

  @Test
  void candidateProfilePersistenceIsOnlyReachedThroughServiceBoundary()
      throws IOException {
    assertThat(sourceFile("src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/"
        + "CanonicalWriteService.java"))
        .doesNotContain("CandidateProfilePersistencePort")
        .doesNotContain("JdbcCandidateProfilePersistencePort")
        .doesNotContain("INSERT INTO recruiting.candidate_profile")
        .doesNotContain("UPDATE recruiting.candidate_profile");

    assertThat(sourceFile("src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/"
        + "IntakeCanonicalWriteBridgeService.java"))
        .doesNotContain("CandidateProfilePersistencePort")
        .doesNotContain("JdbcCandidateProfilePersistencePort")
        .doesNotContain("INSERT INTO recruiting.candidate_profile")
        .doesNotContain("UPDATE recruiting.candidate_profile");

    for (Path file : candidateProfileProductionFiles()) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework.web")
          .doesNotContain("ClientSafeCandidateCard");
    }
  }

  private static CandidateProfileService service() {
    return new CandidateProfileService(new JdbcCandidateProfilePersistencePort(dataSource));
  }

  private static UpsertCandidateProfileFieldRequest fieldRequest(
      CandidateProfileId profileId,
      CandidateProfileFieldPath fieldPath,
      CandidateProfileFieldValue value,
      CandidateProfileFieldStatus status) {
    return UpsertCandidateProfileFieldRequest.builder()
        .organizationId(ORG_A)
        .candidateProfileId(profileId)
        .fieldPath(fieldPath)
        .value(value)
        .fieldStatus(status)
        .lineage(new CandidateProfileFieldLineage(
            List.of(CandidateProfileFieldSourceReference.sourceSpan(
                "span:postgres-test:" + fieldPath.value(),
                "postgres_test",
                NOW)),
            "postgres-test-lineage",
            NOW))
        .conflict(conflict(fieldPath))
        .staleness(new CandidateProfileFieldStaleness(
            true,
            "field requires a fresh candidate check before client use",
            NOW.minusSeconds(86400),
            NOW.minusSeconds(3600),
            NOW.plusSeconds(86400),
            NOW))
        .lastReviewedAt(NOW)
        .confirmedByActorId(ACTOR_A)
        .sourceClaimId(new ClaimId(uuid("00000000-0000-0000-0000-000000290101")))
        .sourceReviewEventId(new ReviewEventId(uuid("00000000-0000-0000-0000-000000290102")))
        .notes("persisted by CandidateProfile persistence skeleton")
        .build();
  }

  private static UpsertCandidateProfileFieldRequest fullMetadataFieldRequest(
      CandidateProfileId profileId,
      ClaimId claimId,
      ReviewEventId reviewEventId,
      WorkflowEventId workflowEventId,
      UUID sourceItemId,
      UUID packetId,
      UUID extractionRunId) {
    CandidateProfileFieldPath fieldPath = CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY;
    return UpsertCandidateProfileFieldRequest.builder()
        .organizationId(ORG_A)
        .candidateProfileId(profileId)
        .fieldPath(fieldPath)
        .value(CandidateProfileFieldValue.ofString("55000 RMB monthly"))
        .fieldStatus(CandidateProfileFieldStatus.CONFLICTING)
        .lineage(new CandidateProfileFieldLineage(
            List.of(
                CandidateProfileFieldSourceReference.claimLedgerItem(claimId, NOW),
                CandidateProfileFieldSourceReference.reviewEvent(reviewEventId, NOW),
                CandidateProfileFieldSourceReference.workflowEvent(workflowEventId, NOW),
                CandidateProfileFieldSourceReference.sourceItem(new SourceItemId(sourceItemId), NOW),
                CandidateProfileFieldSourceReference.informationPacket(
                    new InformationPacketId(packetId),
                    NOW),
                CandidateProfileFieldSourceReference.intakeExtractionRun(
                    new IntakeExtractionRunId(extractionRunId),
                    NOW),
                CandidateProfileFieldSourceReference.sourceSpan(
                    "span:postgres-test:full-lineage:compensation.expected_salary",
                    "consultant_call",
                    NOW),
                CandidateProfileFieldSourceReference.externalEvidence(
                    "external-reference:salary-benchmark:290117",
                    "external_evidence",
                    NOW)),
            "postgres-test-full-lineage",
            NOW))
        .conflict(new CandidateProfileFieldConflict(
            fieldPath,
            List.of(
                new CandidateProfileFieldConflictValue(
                    CandidateProfileFieldValue.ofString("45000 RMB monthly"),
                    List.of(CandidateProfileFieldSourceReference.sourceSpan(
                        "span:postgres-test:full-conflict-a",
                        "resume",
                        NOW))),
                new CandidateProfileFieldConflictValue(
                    CandidateProfileFieldValue.ofString("55000 RMB monthly"),
                    List.of(CandidateProfileFieldSourceReference.sourceSpan(
                        "span:postgres-test:full-conflict-b",
                        "consultant_call",
                        NOW)))),
            CandidateProfileFieldConflictSeverity.BLOCKING,
            CandidateProfileFieldConflictResolutionStatus.NEEDS_REVIEW,
            NOW,
            "salary conflict must remain unresolved"))
        .staleness(new CandidateProfileFieldStaleness(
            true,
            "salary data must be refreshed before client-visible use",
            NOW.minusSeconds(86400 * 90L),
            NOW.minusSeconds(86400),
            NOW.plusSeconds(86400 * 7L),
            NOW))
        .lastReviewedAt(NOW)
        .confirmedByActorId(ACTOR_A)
        .sourceClaimId(claimId)
        .sourceReviewEventId(reviewEventId)
        .sourceWorkflowEventId(workflowEventId)
        .notes("full metadata persistence test")
        .build();
  }

  private static CandidateProfileFieldConflict conflict(CandidateProfileFieldPath fieldPath) {
    return new CandidateProfileFieldConflict(
        fieldPath,
        List.of(
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("45000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:postgres-test:conflict-a",
                    "resume",
                    NOW))),
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("55000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:postgres-test:conflict-b",
                    "consultant_call",
                    NOW)))),
        CandidateProfileFieldConflictSeverity.HIGH,
        CandidateProfileFieldConflictResolutionStatus.UNRESOLVED,
        NOW,
        "test conflict metadata");
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
      statement.setString(2, "Task 6B Org " + organizationId);
      statement.setString(3, "Task 6B Org");
      statement.executeUpdate();
    }
  }

  private static void insertCandidate(UUID organizationId, CandidateId candidateId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate (
              candidate_id,
              organization_id,
              status,
              privacy_status
            )
            VALUES (?, ?, 'new', 'internal_only')
            """)) {
      statement.setObject(1, candidateId.value());
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

  private static int countCandidateProfileRows(CandidateProfileId profileId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM recruiting.candidate_profile WHERE candidate_profile_id = ?")) {
      statement.setObject(1, profileId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static String rawFieldStatusMap(CandidateProfileId profileId) throws SQLException {
    return rawColumn(profileId, "field_status_map::text");
  }

  private static String rawMetadata(CandidateProfileId profileId) throws SQLException {
    return rawColumn(profileId, "metadata::text");
  }

  private static List<UUID> rawSourceClaimIds(CandidateProfileId profileId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT unnest(source_claim_ids) AS source_claim_id
            FROM recruiting.candidate_profile
            WHERE candidate_profile_id = ?
            ORDER BY source_claim_id
            """)) {
      statement.setObject(1, profileId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<UUID> claimIds = new java.util.ArrayList<>();
        while (resultSet.next()) {
          claimIds.add(resultSet.getObject("source_claim_id", UUID.class));
        }
        return claimIds;
      }
    }
  }

  private static String rawColumn(CandidateProfileId profileId, String expression)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT " + expression + " AS value FROM recruiting.candidate_profile "
                + "WHERE candidate_profile_id = ?")) {
      statement.setObject(1, profileId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getString("value");
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    return exists(
        "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')",
        schema,
        table);
  }

  private static boolean columnExists(String schema, String table, String column)
      throws SQLException {
    return exists(
        "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
            + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)",
        schema,
        table,
        column);
  }

  private static boolean indexExists(String schema, String indexName) throws SQLException {
    return exists(
        "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = ? AND indexname = ?)",
        schema,
        indexName);
  }

  private static boolean exists(String sql, String... parameters) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static List<Path> candidateProfileProductionFiles() throws IOException {
    try (java.util.stream.Stream<Path> stream = Files.walk(projectPath(
        "src/main/java/com/recruitingtransactionos/coreapi/candidateprofile"))) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static String sourceFile(String relativePath) throws IOException {
    return Files.readString(projectPath(relativePath));
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
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
