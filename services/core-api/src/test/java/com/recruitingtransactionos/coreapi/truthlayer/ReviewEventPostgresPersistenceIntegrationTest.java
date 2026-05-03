package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;
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
class ReviewEventPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
  }

  @Test
  void appendReviewEventPersistsReviewGovernanceVocabulary() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000040001");
    UUID reviewerId = uuid("00000000-0000-0000-0000-000000040002");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000040003");
    insertOrganizationAndReviewer(organizationId, reviewerId);
    ClaimId claimId = appendClaim(organizationId, candidateId);

    ReviewEventAppendCommand command = new ReviewEventAppendCommand(
        organizationId,
        reviewerId,
        new EntityRef("candidate", candidateId),
        "headline",
        RiskTier.T2_MEDIUM_RISK,
        ReviewDecision.APPROVED,
        false,
        "reviewed candidate headline against CV source span",
        Duration.ofMillis(1234),
        claimId,
        new SourceSpanRef("source-item:30-36"));

    ReviewEventAppendResult result = service().append(command);

    PersistedReviewEvent persisted = findReview(result.reviewEventId());
    assertThat(persisted.organizationId()).isEqualTo(organizationId);
    assertThat(persisted.reviewerUserId()).isEqualTo(reviewerId);
    assertThat(persisted.targetEntityType()).isEqualTo("candidate");
    assertThat(persisted.targetEntityId()).isEqualTo(candidateId);
    assertThat(persisted.fieldPath()).isEqualTo("headline");
    assertThat(persisted.riskTier()).isEqualTo(RiskTier.T2_MEDIUM_RISK.wireValue());
    assertThat(persisted.decision()).isEqualTo(ReviewDecision.APPROVED.wireValue());
    assertThat(persisted.bulkFlag()).isFalse();
    assertThat(persisted.durationMs()).isEqualTo(1234);
    assertThat(persisted.claimLedgerItemId()).isEqualTo(claimId.value());
    assertThat(persisted.sourceSpanRef()).isEqualTo("source-item:30-36");
    assertThat(persisted.reason()).isEqualTo("reviewed candidate headline against CV source span");
    assertThat(persisted.status()).isEqualTo("completed");
    assertThat(persisted.createdAt()).isNotNull();
  }

  @Test
  void appendBulkReviewDoesNotCreateVerifiedFact() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000040101");
    UUID reviewerId = uuid("00000000-0000-0000-0000-000000040102");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000040103");
    insertOrganizationAndReviewer(organizationId, reviewerId);
    ClaimId claimId = appendClaim(organizationId, candidateId);

    ReviewEventAppendResult result = service().append(command(
        organizationId,
        reviewerId,
        candidateId,
        RiskTier.T1_LOW_RISK,
        ReviewDecision.APPROVED,
        true,
        "skills.normalized",
        "bulk accepted low-risk normalization",
        claimId));

    PersistedReviewEvent persisted = findReview(result.reviewEventId());
    assertThat(persisted.bulkFlag()).isTrue();
    assertThat(persisted.decision()).isEqualTo(ReviewDecision.APPROVED.wireValue());
    assertThat(findClaimVerificationStatus(claimId))
        .isEqualTo(VerificationStatus.AI_EXTRACTED.wireValue());
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(countVerifiedClaimStatuses(organizationId)).isZero();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
  }

  @Test
  void appendHighRiskReviewPreservesRiskTier() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000040201");
    UUID reviewerId = uuid("00000000-0000-0000-0000-000000040202");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000040203");
    insertOrganizationAndReviewer(organizationId, reviewerId);

    ReviewEventAppendResult t3 = service().append(command(
        organizationId,
        reviewerId,
        candidateId,
        RiskTier.T3_HIGH_RISK,
        ReviewDecision.ESCALATED,
        false,
        "compensation_expectation",
        "high-risk compensation evidence needs senior review",
        null));
    ReviewEventAppendResult t4 = service().append(command(
        organizationId,
        reviewerId,
        candidateId,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        ReviewDecision.NEEDS_CONFIRMATION,
        false,
        "fee_terms",
        "transaction legal field needs explicit confirmation",
        null));

    assertThat(findReview(t3.reviewEventId()).riskTier())
        .isEqualTo(RiskTier.T3_HIGH_RISK.wireValue());
    assertThat(findReview(t4.reviewEventId()).riskTier())
        .isEqualTo(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING.wireValue());
  }

  @Test
  void appendUsesExplicitWireValues() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000040301");
    UUID reviewerId = uuid("00000000-0000-0000-0000-000000040302");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000040303");
    insertOrganizationAndReviewer(organizationId, reviewerId);

    PersistedReviewEvent persisted = findReview(service().append(command(
        organizationId,
        reviewerId,
        candidateId,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        ReviewDecision.NEEDS_CONFIRMATION,
        false,
        "offer.start_date",
        "start date cannot be confirmed from weak source",
        null)).reviewEventId());

    assertThat(persisted.riskTier()).isEqualTo(RiskTier.T4_TRANSACTION_LEGAL_BLOCKING.wireValue());
    assertThat(persisted.decision()).isEqualTo(ReviewDecision.NEEDS_CONFIRMATION.wireValue());
    assertThat(persisted.decision()).isNotEqualTo(ReviewDecision.NEEDS_CONFIRMATION.name());
  }

  @Test
  void appendIsInsertOnlyByContract() {
    assertThat(publicDeclaredMethodNames(ReviewEventPort.class)).containsExactly("append");
    assertThat(publicDeclaredMethodNames(JdbcReviewEventPort.class)).containsExactly("append");
    assertThat(declaredMethodNames(ReviewEventPort.class))
        .noneMatch(this::looksLikeMutableWriteShortcut);
    assertThat(declaredMethodNames(JdbcReviewEventPort.class))
        .noneMatch(this::looksLikeMutableWriteShortcut);
  }

  @Test
  void nullOrInvalidRequiredReviewDataIsRejected() {
    ReviewEventService reviewEventService = service();

    assertThatThrownBy(() -> reviewEventService.append(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command must not be null");

    assertThatThrownBy(() -> new ReviewEventAppendCommand(
        uuid("00000000-0000-0000-0000-000000040401"),
        uuid("00000000-0000-0000-0000-000000040402"),
        new EntityRef("candidate", uuid("00000000-0000-0000-0000-000000040403")),
        "",
        RiskTier.T2_MEDIUM_RISK,
        ReviewDecision.APPROVED,
        false,
        "missing field path should be rejected",
        Duration.ofSeconds(1),
        null,
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("targetFieldPath must not be blank");

    assertThatThrownBy(() -> new ReviewEventAppendCommand(
        uuid("00000000-0000-0000-0000-000000040501"),
        uuid("00000000-0000-0000-0000-000000040502"),
        new EntityRef("candidate", uuid("00000000-0000-0000-0000-000000040503")),
        "headline",
        RiskTier.T2_MEDIUM_RISK,
        ReviewDecision.APPROVED,
        false,
        "negative duration should be rejected",
        Duration.ofMillis(-1),
        null,
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reviewDuration must not be negative");
  }

  @Test
  void fullFlywayMigrationStillAppliesBeforeReviewPersistenceTest() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(25);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "24", "25");
  }

  @Test
  void consentDisclosurePersistenceTablesNowExistWithoutChangingReviewEventScope()
      throws SQLException {
    assertThat(tableExists("privacy", "consent_record")).isTrue();
    assertThat(tableExists("privacy", "unlock_decision")).isTrue();
    assertThat(tableExists("privacy", "disclosure_record")).isTrue();
  }

  private static ReviewEventService service() {
    return new ReviewEventService(new JdbcReviewEventPort(dataSource));
  }

  private static ClaimId appendClaim(UUID organizationId, UUID candidateId) {
    return new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource)).append(
        new ClaimLedgerAppendCommand(
            organizationId,
            new EntityRef("candidate", candidateId),
            "headline",
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            new SourceSpanRef("source-item:30-36"),
            ActorRole.CANDIDATE,
            VerificationStatus.AI_EXTRACTED,
            ClientShareability.INTERNAL_ONLY,
            null,
            null))
        .claimId();
  }

  private static ReviewEventAppendCommand command(
      UUID organizationId,
      UUID reviewerId,
      UUID candidateId,
      RiskTier riskTier,
      ReviewDecision decision,
      boolean bulkApproval,
      String fieldPath,
      String reason,
      ClaimId claimId) {
    return new ReviewEventAppendCommand(
        organizationId,
        reviewerId,
        new EntityRef("candidate", candidateId),
        fieldPath,
        riskTier,
        decision,
        bulkApproval,
        reason,
        Duration.ofSeconds(42),
        claimId,
        new SourceSpanRef("source-item:30-36"));
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
      organization.setString(2, "Task 3B Org " + organizationId);
      organization.setString(3, "Task 3B Org");
      organization.executeUpdate();

      reviewer.setObject(1, reviewerId);
      reviewer.setObject(2, organizationId);
      reviewer.setString(3, "reviewer-" + reviewerId + "@example.test");
      reviewer.setString(4, "Task 3B Reviewer");
      reviewer.executeUpdate();
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
              duration_ms,
              claim_ledger_item_id,
              source_span_ref,
              reason,
              status,
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
            resultSet.getInt("duration_ms"),
            resultSet.getObject("claim_ledger_item_id", UUID.class),
            resultSet.getString("source_span_ref"),
            resultSet.getString("reason"),
            resultSet.getString("status"),
            resultSet.getObject("created_at", OffsetDateTime.class));
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

  private static int countVerifiedClaimStatuses(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM governance.claim_ledger_item
            WHERE organization_id = ?
              AND verification_status IN (
                'candidate_confirmed'::governance.verification_status,
                'external_verified'::governance.verification_status
              )
            """)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static String findClaimVerificationStatus(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT verification_status::text AS verification_status
            FROM governance.claim_ledger_item
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getString("verification_status");
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
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private boolean looksLikeMutableWriteShortcut(String methodName) {
    String normalized = normalized(methodName);
    return normalized.contains("update")
        || normalized.contains("delete")
        || normalized.contains("upsert")
        || normalized.contains("savecanonical")
        || normalized.contains("savecandidateprofile")
        || normalized.contains("confirmfact")
        || normalized.contains("writecanonical")
        || normalized.contains("canonicalfact");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> declaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
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
      int durationMs,
      UUID claimLedgerItemId,
      String sourceSpanRef,
      String reason,
      String status,
      OffsetDateTime createdAt) {
  }
}
