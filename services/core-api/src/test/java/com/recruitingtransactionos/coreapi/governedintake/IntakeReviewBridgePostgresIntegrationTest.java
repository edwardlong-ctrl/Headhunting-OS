package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
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
class IntakeReviewBridgePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000200001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000200002");
  private static final UUID REVIEWER_A = uuid("00000000-0000-0000-0000-000000200003");
  private static final UUID REVIEWER_B = uuid("00000000-0000-0000-0000-000000200004");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T14:00:00Z");

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
    insertOrganizationAndReviewer(ORG_A, REVIEWER_A);
    insertOrganizationAndReviewer(ORG_B, REVIEWER_B);
  }

  @Test
  void flywaySchemaAlreadySupportsReviewBridgeLineageWithoutNewMigration() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(24);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "24");
    assertThat(columnExists("governance", "review_event", "claim_ledger_item_id")).isTrue();
    assertThat(columnExists("governance", "review_event", "source_span_ref")).isTrue();
  }

  @Test
  void governedIntakeClaimCanReceiveReviewEvent() throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("review-append");
    PersistedClaim before = findClaim(claimId);

    IntakeReviewBridgeResult result = reviewBridgeService().bridge(request(
        ORG_A,
        claimId,
        REVIEWER_A,
        "reviewed governed-intake claim before canonical boundary"));

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    PersistedReviewEvent review = findReview(result.reviewEventId());
    assertThat(review.organizationId()).isEqualTo(ORG_A);
    assertThat(review.reviewerUserId()).isEqualTo(REVIEWER_A);
    assertThat(review.targetEntityType()).isEqualTo("information_packet");
    assertThat(review.targetEntityId()).isEqualTo(before.entityId());
    assertThat(review.fieldPath()).isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(review.riskTier()).isEqualTo(RiskTier.T2_MEDIUM_RISK.wireValue());
    assertThat(review.decision()).isEqualTo(ReviewDecision.APPROVED.wireValue());
    assertThat(review.bulkFlag()).isFalse();
    assertThat(review.claimLedgerItemId()).isEqualTo(claimId.value());
    assertThat(review.sourceSpanRef())
        .contains("intake.review_bridge")
        .contains("claim_ledger_item:" + claimId.value());
    assertThat(review.reason()).isEqualTo("reviewed governed-intake claim before canonical boundary");

    PersistedClaim after = findClaim(claimId);
    assertThat(after.verificationStatus()).isEqualTo(before.verificationStatus());
    assertThat(after.reviewEventId()).isNull();
    assertThat(after.canonicalWriteAllowed()).isFalse();
    assertThat(countRows("governance.review_event", ORG_A)).isGreaterThanOrEqualTo(1);
    assertThat(countRows("workflow.workflow_event", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_A)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isZero();
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
  }

  @Test
  void wrongOrganizationCannotReviewGovernedIntakeClaim() throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("wrong-org");

    IntakeReviewBridgeResult result = reviewBridgeService().bridge(request(
        ORG_B,
        claimId,
        REVIEWER_B,
        "wrong organization must not review this claim"));

    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(result.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(countReviewEventsForClaim(claimId)).isZero();
    assertThat(countRows("governance.review_event", ORG_B)).isZero();
  }

  @Test
  void repeatedIdenticalReviewBridgeCallDoesNotAppendDuplicateReviewEvent() throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("duplicate-review");

    IntakeReviewBridgeResult first = reviewBridgeService().bridge(request(
        ORG_A,
        claimId,
        REVIEWER_A,
        "same review request should be idempotent"));
    IntakeReviewBridgeResult second = reviewBridgeService().bridge(request(
        ORG_A,
        claimId,
        REVIEWER_A,
        "same review request should be idempotent"));

    assertThat(first.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_ALREADY_EXISTS);
    assertThat(second.existingReviewEventId()).isEqualTo(first.reviewEventId());
    assertThat(countReviewEventsForClaim(claimId)).isEqualTo(1);
  }

  @Test
  void materiallyDifferentRepeatedReviewBridgeCallIsNewReviewEvidence() throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("different-review");

    IntakeReviewBridgeResult first = reviewBridgeService().bridge(request(
        ORG_A,
        claimId,
        REVIEWER_A,
        "first review evidence"));
    IntakeReviewBridgeResult second = reviewBridgeService().bridge(request(
        ORG_A,
        claimId,
        REVIEWER_A,
        "second review evidence materially differs"));

    assertThat(first.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(second.reviewEventId()).isNotEqualTo(first.reviewEventId());
    assertThat(countReviewEventsForClaim(claimId)).isEqualTo(2);
  }

  private static ClaimId appendGovernedIntakeClaim(String suffix) {
    IntakeExtractionRun run = saveBridgeEligibleRun(suffix);
    IntakeClaimLedgerBridgeResult result =
        claimBridgeService().bridge(claimBridgeRequest(ORG_A, run.extractionRunId()));
    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.SUCCEEDED);
    assertThat(result.appendedClaimIds()).hasSize(1);
    return result.appendedClaimIds().getFirst();
  }

  private static IntakeExtractionRun saveBridgeEligibleRun(String suffix) {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(sourceCommand(ORG_A, suffix).build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));
    IntakeExtractionRunId runId = new IntakeExtractionRunId(uuid(
        "00000000-0000-0000-0000-" + deterministicTail(suffix)));
    Instant now = RECEIVED_AT.plusSeconds(Math.abs(suffix.hashCode() % 1000));
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        runId,
        ORG_A,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        "intake-extraction-envelope.v1",
        List.of(sourceItem.sourceItemId()),
        List.of(),
        List.of(new IntakeExtractionSourceSnapshot(
            sourceItem.sourceItemId(),
            sourceItem.sourceType(),
            sourceItem.title(),
            sourceItem.contentHash(),
            sourceItem.externalRef())),
        List.of(new IntakeExtractedField(
            "intake.bridge_eligible.quality_note",
            "explicitly marked operational bridge fixture",
            sourceItem.sourceItemId(),
            0.5d,
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            "Operational fixture only; not a canonical candidate/company/job fact.",
            "fixture:quality-note")),
        List.of(),
        List.of(),
        List.of(),
        now);
    return extractionPort().save(new IntakeExtractionRun(
        runId,
        ORG_A,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.SUCCEEDED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:" + suffix,
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope)));
  }

  private static IntakeReviewBridgeService reviewBridgeService() {
    return new IntakeReviewBridgeService(
        new JdbcClaimLedgerItemReviewLookupPort(dataSource),
        new ReviewEventService(new JdbcReviewEventPort(dataSource)),
        new JdbcReviewEventSourceReferenceLookupPort(dataSource));
  }

  private static IntakeClaimLedgerBridgeService claimBridgeService() {
    return new IntakeClaimLedgerBridgeService(
        extractionPort(),
        new JdbcInformationPacketPersistencePort(dataSource),
        new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource)),
        new JdbcClaimLedgerSourceReferenceLookupPort(dataSource));
  }

  private static GovernedIntakeService intakeService() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static JdbcIntakeExtractionRunPort extractionPort() {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  private static IntakeClaimLedgerBridgeRequest claimBridgeRequest(
      UUID organizationId,
      IntakeExtractionRunId extractionRunId) {
    return new IntakeClaimLedgerBridgeRequest(
        organizationId,
        extractionRunId,
        ActorRole.CONSULTANT,
        null,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null);
  }

  private static IntakeReviewBridgeRequest request(
      UUID organizationId,
      ClaimId claimId,
      UUID reviewerId,
      String reason) {
    return IntakeReviewBridgeRequest.builder()
        .organizationId(organizationId)
        .claimLedgerItemId(claimId)
        .reviewerActorType(ActorRole.CONSULTANT)
        .reviewerActorId(reviewerId)
        .reviewDecision(ReviewDecision.APPROVED)
        .riskTier(RiskTier.T2_MEDIUM_RISK)
        .bulkFlag(false)
        .reason(reason)
        .reviewPolicy(IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY)
        .build();
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(
      UUID organizationId,
      String suffix) {
    String value = organizationId.toString().substring(30) + "-" + suffix;
    return SourceItemRegistrationCommand.builder()
        .organizationId(organizationId)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Senior DV engineer CV " + suffix)
        .contentHash("sha256:" + value)
        .externalRef("ats:" + value)
        .storageRef("s3://internal-intake/" + value)
        .rawRef("vault://source-" + value + "/raw")
        .language("en")
        .uploadedByActorType(ActorRole.CONSULTANT)
        .receivedAt(RECEIVED_AT)
        .metadataJson("{\"case\":\"" + value + "\"}")
        .status(SourceItemStatus.REGISTERED);
  }

  private static InformationPacketCreateCommand.Builder packetCommand(UUID organizationId) {
    return InformationPacketCreateCommand.builder()
        .organizationId(organizationId)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("candidate packet 200001")
        .metadataJson("{\"packet\":\"200001\"}");
  }

  private static PersistedClaim findClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              organization_id,
              entity_type,
              entity_id,
              source_span_ref,
              verification_status::text AS verification_status,
              canonical_write_allowed,
              client_shareability::text AS client_shareability,
              target_field_path,
              review_event_id
            FROM governance.claim_ledger_item
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedClaim(
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class),
            resultSet.getString("source_span_ref"),
            resultSet.getString("verification_status"),
            resultSet.getBoolean("canonical_write_allowed"),
            resultSet.getString("client_shareability"),
            resultSet.getString("target_field_path"),
            resultSet.getObject("review_event_id", UUID.class));
      }
    }
  }

  private static PersistedReviewEvent findReview(ReviewEventId reviewEventId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              review_event_id,
              organization_id,
              reviewer_user_id,
              target_entity_type,
              target_entity_id,
              field_path,
              risk_tier::text AS risk_tier,
              decision,
              bulk_flag,
              claim_ledger_item_id,
              source_span_ref,
              reason,
              created_at
            FROM governance.review_event
            WHERE review_event_id = ?
            """)) {
      statement.setObject(1, reviewEventId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedReviewEvent(
            resultSet.getObject("review_event_id", UUID.class),
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getObject("reviewer_user_id", UUID.class),
            resultSet.getString("target_entity_type"),
            resultSet.getObject("target_entity_id", UUID.class),
            resultSet.getString("field_path"),
            resultSet.getString("risk_tier"),
            resultSet.getString("decision"),
            resultSet.getBoolean("bulk_flag"),
            resultSet.getObject("claim_ledger_item_id", UUID.class),
            resultSet.getString("source_span_ref"),
            resultSet.getString("reason"),
            resultSet.getObject("created_at", OffsetDateTime.class));
      }
    }
  }

  private static int countReviewEventsForClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM governance.review_event
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
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

  private static boolean columnExists(String schema, String table, String column)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, column);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static void insertOrganizationAndReviewer(UUID organizationId, UUID reviewerId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement organization = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """);
        PreparedStatement reviewer = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, ?, ?, 'active')
            """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Task 5D Org " + organizationId);
      organization.setString(3, "Task 5D Org");
      organization.executeUpdate();

      reviewer.setObject(1, reviewerId);
      reviewer.setObject(2, organizationId);
      reviewer.setString(3, "reviewer-" + reviewerId + "@example.test");
      reviewer.setString(4, "Task 5D Reviewer");
      reviewer.executeUpdate();
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

  private static String deterministicTail(String suffix) {
    return switch (suffix) {
      case "review-append" -> "000000200101";
      case "wrong-org" -> "000000200102";
      case "duplicate-review" -> "000000200103";
      case "different-review" -> "000000200104";
      default -> "000000200199";
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private record PersistedClaim(
      UUID organizationId,
      String entityType,
      UUID entityId,
      String sourceSpanRef,
      String verificationStatus,
      boolean canonicalWriteAllowed,
      String clientShareability,
      String targetFieldPath,
      UUID reviewEventId) {
  }

  private record PersistedReviewEvent(
      UUID reviewEventId,
      UUID organizationId,
      UUID reviewerUserId,
      String targetEntityType,
      UUID targetEntityId,
      String fieldPath,
      String riskTier,
      String decision,
      boolean bulkFlag,
      UUID claimLedgerItemId,
      String sourceSpanRef,
      String reason,
      OffsetDateTime createdAt) {
  }
}
