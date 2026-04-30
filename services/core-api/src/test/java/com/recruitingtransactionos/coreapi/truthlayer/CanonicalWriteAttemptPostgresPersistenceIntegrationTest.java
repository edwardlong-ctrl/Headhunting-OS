package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcCanonicalWriteAttemptPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcCanonicalWriteAttemptReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptId;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.CanonicalWriteAttemptRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CanonicalWriteAttemptPostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000180001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000180002");
  private static int blockedCommandCounter;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    insertOrganizationAndUser(ORGANIZATION_ID, USER_ID);
  }

  @Test
  void migrationCreatesTableAndIndexes() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(12);
    assertThat(appliedMigrationVersions()).contains("12");
    assertThat(tableExists("governance", "canonical_write_attempt")).isTrue();
    assertThat(indexExists("governance", "cwa_org_decision_occurred_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_entity_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_claim_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_review_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_workflow_event_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_actor_idx")).isTrue();
    assertThat(indexExists("governance", "cwa_org_idempotency_uidx")).isTrue();
  }

  @Test
  void appendBlockedAttemptPersistsRow() throws SQLException {
    int before = countAttempts(ORGANIZATION_ID);
    CanonicalWriteAttemptAppendResult result = port().append(blockedCommand());

    assertThat(result.attemptId()).isNotNull();
    assertThat(result.attemptId().value()).isNotNull();
    assertThat(countAttempts(ORGANIZATION_ID) - before).isEqualTo(1);

    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT decision, reason_codes, workflow_event_id "
                + "FROM governance.canonical_write_attempt WHERE organization_id = ?"
                + " AND entity_id = ?")) {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setObject(2, UUID.fromString("00000000-0000-0000-1000-000000000001"));
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("decision")).isEqualTo("block");
        assertThat((String[]) resultSet.getArray("reason_codes").getArray())
            .contains("system_inference_cannot_be_canonical_fact");
        resultSet.getObject("workflow_event_id", UUID.class);
        assertThat(resultSet.wasNull()).isTrue();
      }
    }
  }

  @Test
  void appendAllowedAttemptPersistsRowWithWorkflowEventId() throws SQLException {
    int before = countAttempts(ORGANIZATION_ID);
    UUID workflowEventUuid = UUID.fromString("00000000-0000-0000-0001-000000000001");
    CanonicalWriteAttemptAppendCommand command = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0002-000000000001")),
        null,
        "skills.java",
        "claim-value:skills:v2",
        null,
        new ClaimId(UUID.fromString("00000000-0000-0000-0003-000000000001")),
        new ReviewEventId(UUID.fromString("00000000-0000-0000-0004-000000000001")),
        "allow",
        List.of("canonical_write_gate_passed"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-allowed-idempotency"),
        null,
        null,
        new WorkflowEventId(workflowEventUuid),
        Instant.parse("2026-04-30T12:00:00Z"));

    CanonicalWriteAttemptAppendResult result = port().append(command);
    assertThat(result.attemptId()).isNotNull();
    assertThat(countAttempts(ORGANIZATION_ID) - before).isEqualTo(1);

    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT decision, workflow_event_id "
                + "FROM governance.canonical_write_attempt WHERE organization_id = ?"
                + " AND entity_id = ?")) {
      statement.setObject(1, ORGANIZATION_ID);
      statement.setObject(2, UUID.fromString("00000000-0000-0000-0002-000000000001"));
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("decision")).isEqualTo("allow");
        assertThat(resultSet.getObject("workflow_event_id", UUID.class))
            .isEqualTo(workflowEventUuid);
      }
    }
  }

  @Test
  void idempotencyKeyEnforcesUniqueness() throws SQLException {
    UUID entityId = UUID.fromString("00000000-0000-0000-0005-000000000001");
    CanonicalWriteAttemptAppendCommand command = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("IDEMPOTENCY_UNIQUE_TEST", entityId),
        null,
        "skills.java",
        "claim-value:skills:idemp-v1",
        null,
        null,
        null,
        "block",
        List.of("idempotency_uniqueness_test_reason"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-idempotency-unique-key"),
        null,
        null,
        null,
        Instant.parse("2026-04-30T13:00:00Z"));

    int before = countAttempts(ORGANIZATION_ID);
    CanonicalWriteAttemptAppendResult first = port().append(command);
    assertThat(countAttempts(ORGANIZATION_ID) - before).isEqualTo(1);

    assertThatThrownBy(() -> port().append(command))
        .isInstanceOf(IllegalStateException.class);
    assertThat(countAttempts(ORGANIZATION_ID) - before).isEqualTo(1);
  }

  @Test
  void findByIdempotencyKeyReturnsExistingAttempt() {
    CanonicalWriteAttemptAppendCommand command = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0006-000000000001")),
        null,
        "headline",
        "claim-value:headline:find-v1",
        null,
        null,
        null,
        "require_review",
        List.of("conflicting_claim_requires_explicit_review"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-find-idempotency-key"),
        null,
        null,
        null,
        Instant.parse("2026-04-30T14:00:00Z"));

    CanonicalWriteAttemptAppendResult appendResult = port().append(command);
    Optional<CanonicalWriteAttemptIdempotencyRecord> found =
        port().findByIdempotencyKey(ORGANIZATION_ID,
            new WorkflowIdempotencyKey("test-find-idempotency-key"));

    assertThat(found).isPresent();
    assertThat(found.get().attemptId()).isEqualTo(appendResult.attemptId());
  }

  @Test
  void findByIdempotencyKeyReturnsEmptyForUnknownKey() {
    Optional<CanonicalWriteAttemptIdempotencyRecord> found =
        port().findByIdempotencyKey(ORGANIZATION_ID,
            new WorkflowIdempotencyKey("non-existent-key"));

    assertThat(found).isEmpty();
  }

  @Test
  void searchFiltersByDecision() throws SQLException {
    port().append(blockedCommand());
    port().append(allowedCommand("test-search-allowed-1"));
    port().append(allowedCommand("test-search-allowed-2"));

    CanonicalWriteAttemptQuery blockedQuery = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).decision("block").build();
    List<CanonicalWriteAttemptRecord> blockedRecords = readPort().search(blockedQuery);
    assertThat(blockedRecords).isNotEmpty();
    assertThat(blockedRecords.getFirst().decision()).isEqualTo("block");

    CanonicalWriteAttemptQuery allowedQuery = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).decision("allow").build();
    List<CanonicalWriteAttemptRecord> allowedRecords = readPort().search(allowedQuery);
    assertThat(allowedRecords).isNotEmpty();
    assertThat(allowedRecords).allMatch(r -> r.decision().equals("allow"));
  }

  @Test
  void searchFiltersByReasonCode() throws SQLException {
    UUID reasonTestEntityId = UUID.randomUUID();
    CanonicalWriteAttemptAppendCommand cmd = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("REASON_FILTER_TEST", reasonTestEntityId),
        null,
        "headline",
        "claim-value:reason-filter-v1",
        null,
        null,
        null,
        "block",
        List.of("unique_reason_code_for_filter_test_a",
            "unique_reason_code_for_filter_test_b"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-reason-filter-key"),
        null,
        null,
        null,
        Instant.parse("2026-04-30T15:00:00Z"));
    port().append(cmd);

    CanonicalWriteAttemptQuery query = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID)
        .decision("block")
        .reasonCode("unique_reason_code_for_filter_test_a")
        .build();
    List<CanonicalWriteAttemptRecord> records = readPort().search(query);
    assertThat(records).isNotEmpty();
    assertThat(records.getFirst().entityId()).isEqualTo(reasonTestEntityId);

    CanonicalWriteAttemptQuery noMatchQuery = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID)
        .reasonCode("non_existent_reason")
        .build();
    List<CanonicalWriteAttemptRecord> noMatchRecords = readPort().search(noMatchQuery);
    assertThat(noMatchRecords).isEmpty();
  }

  @Test
  void searchFiltersByTimeRange() throws SQLException {
    CanonicalWriteAttemptAppendCommand early = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("TIME_RANGE_TEST", UUID.randomUUID()),
        null,
        "headline",
        "claim-value:time-range-early-v1",
        null,
        null,
        null,
        "block",
        List.of("time_range_test_reason"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-time-range-early"),
        null,
        null,
        null,
        Instant.parse("2026-04-30T10:00:00Z"));
    CanonicalWriteAttemptAppendCommand late = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("TIME_RANGE_TEST", UUID.randomUUID()),
        null,
        "headline",
        "claim-value:time-range-late-v1",
        null,
        null,
        null,
        "block",
        List.of("time_range_test_reason"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-time-range-late"),
        null,
        null,
        null,
        Instant.parse("2026-04-30T12:00:00Z"));
    port().append(early);
    port().append(late);

    CanonicalWriteAttemptQuery query = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID)
        .entityType("TIME_RANGE_TEST")
        .occurredFrom(Instant.parse("2026-04-30T11:00:00Z"))
        .build();
    List<CanonicalWriteAttemptRecord> records = readPort().search(query);
    assertThat(records).hasSize(1);
    assertThat(records.getFirst().idempotencyKey().value())
        .isEqualTo("test-time-range-late");
  }

  @Test
  void searchEnforcesLimitAndOffset() throws SQLException {
    for (int i = 0; i < 5; i++) {
      port().append(new CanonicalWriteAttemptAppendCommand(
          ORGANIZATION_ID,
          new EntityRef("LIMIT_OFFSET_TEST", UUID.randomUUID()),
          null,
          "headline",
          "claim-value:limit-offset-v" + i,
          null,
          null,
          null,
          "block",
          List.of("limit_offset_test_reason"),
          new ActorRef(USER_ID, ActorRole.CONSULTANT),
          null,
          new WorkflowIdempotencyKey("test-limit-offset-" + i),
          null,
          null,
          null,
          Instant.parse("2026-04-30T16:00:0" + i + "Z")));
    }

    CanonicalWriteAttemptQuery page1 = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).entityType("LIMIT_OFFSET_TEST").limit(2).offset(0).build();
    List<CanonicalWriteAttemptRecord> p1 = readPort().search(page1);
    assertThat(p1).hasSize(2);

    CanonicalWriteAttemptQuery page2 = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).entityType("LIMIT_OFFSET_TEST").limit(2).offset(2).build();
    List<CanonicalWriteAttemptRecord> p2 = readPort().search(page2);
    assertThat(p2).hasSize(2);

    CanonicalWriteAttemptQuery page3 = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).entityType("LIMIT_OFFSET_TEST").limit(2).offset(4).build();
    List<CanonicalWriteAttemptRecord> p3 = readPort().search(page3);
    assertThat(p3).hasSize(1);
  }

  @Test
  void searchFiltersByActorUserId() throws SQLException {
    port().append(blockedCommand());

    CanonicalWriteAttemptQuery query = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).actorUserId(USER_ID).build();
    List<CanonicalWriteAttemptRecord> records = readPort().search(query);
    assertThat(records).isNotEmpty();
    assertThat(records).allMatch(r -> r.actorUserId().equals(USER_ID));

    CanonicalWriteAttemptQuery noMatchQuery = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID)
        .actorUserId(UUID.randomUUID())
        .build();
    List<CanonicalWriteAttemptRecord> noMatchRecords = readPort().search(noMatchQuery);
    assertThat(noMatchRecords).isEmpty();
  }

  @Test
  void reasonCodesArrayRoundTripsCorrectly() throws SQLException {
    UUID roundTripEntityId = UUID.randomUUID();
    CanonicalWriteAttemptAppendCommand command = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("ROUND_TRIP_TEST", roundTripEntityId),
        7,
        "skills.java",
        "claim-value:skills:round-trip-v1",
        "intake.extraction_run:a|intake.information_packet:b",
        new ClaimId(UUID.randomUUID()),
        new ReviewEventId(UUID.randomUUID()),
        "block",
        List.of("system_inference_cannot_be_canonical_fact",
            "ai_extracted_claim_cannot_be_canonical_fact",
            "bulk_approve_cannot_create_candidate_confirmed"),
        new ActorRef(USER_ID, ActorRole.ADMIN),
        null,
        null,
        new WorkflowCorrelationId(UUID.randomUUID()),
        new WorkflowCausationId(UUID.randomUUID()),
        null,
        Instant.parse("2026-04-30T17:00:00Z"));
    port().append(command);

    CanonicalWriteAttemptQuery query = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).entityType("ROUND_TRIP_TEST").build();
    List<CanonicalWriteAttemptRecord> records = readPort().search(query);
    assertThat(records).hasSize(1);

    CanonicalWriteAttemptRecord record = records.getFirst();
    assertThat(record.decision()).isEqualTo("block");
    assertThat(record.reasonCodes())
        .containsExactly(
            "system_inference_cannot_be_canonical_fact",
            "ai_extracted_claim_cannot_be_canonical_fact",
            "bulk_approve_cannot_create_candidate_confirmed");
    assertThat(record.entityType()).isEqualTo("ROUND_TRIP_TEST");
    assertThat(record.targetFieldPath()).isEqualTo("skills.java");
    assertThat(record.sourceSpanRef()).isEqualTo(
        "intake.extraction_run:a|intake.information_packet:b");
    assertThat(record.actorUserId()).isEqualTo(USER_ID);
    assertThat(record.actorRole()).isEqualTo(ActorRole.ADMIN);
    assertThat(record.entityVersion()).isEqualTo(7);
  }

  @Test
  void roundTripQueryPreservesAllProvenanceFields() throws SQLException {
    UUID entityId = UUID.randomUUID();
    ClaimId claimId = new ClaimId(UUID.randomUUID());
    ReviewEventId reviewId = new ReviewEventId(UUID.randomUUID());
    WorkflowEventId wfId = new WorkflowEventId(UUID.randomUUID());
    WorkflowCorrelationId corrId = new WorkflowCorrelationId(UUID.randomUUID());
    WorkflowCausationId causId = new WorkflowCausationId(UUID.randomUUID());
    WorkflowIdempotencyKey key = new WorkflowIdempotencyKey("test-roundtrip-key");

    CanonicalWriteAttemptAppendCommand command = new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("PROVENANCE_TEST", entityId),
        3,
        "headline",
        "claim-value:full-roundtrip:v1",
        "intake.extraction_run:x|intake.information_packet:y",
        claimId,
        reviewId,
        "allow",
        List.of("canonical_write_gate_passed"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        key,
        corrId,
        causId,
        wfId,
        Instant.parse("2026-04-30T18:00:00Z"));
    port().append(command);

    CanonicalWriteAttemptQuery query = CanonicalWriteAttemptQuery
        .builder(ORGANIZATION_ID).entityType("PROVENANCE_TEST").build();
    List<CanonicalWriteAttemptRecord> records = readPort().search(query);
    assertThat(records).hasSize(1);

    CanonicalWriteAttemptRecord r = records.getFirst();
    assertThat(r.organizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(r.entityType()).isEqualTo("PROVENANCE_TEST");
    assertThat(r.entityId()).isEqualTo(entityId);
    assertThat(r.entityVersion()).isEqualTo(3);
    assertThat(r.targetFieldPath()).isEqualTo("headline");
    assertThat(r.proposedValueRef()).isEqualTo("claim-value:full-roundtrip:v1");
    assertThat(r.sourceSpanRef()).isEqualTo(
        "intake.extraction_run:x|intake.information_packet:y");
    assertThat(r.claimLedgerItemId()).isEqualTo(claimId);
    assertThat(r.reviewEventId()).isEqualTo(reviewId);
    assertThat(r.decision()).isEqualTo("allow");
    assertThat(r.reasonCodes()).containsExactly("canonical_write_gate_passed");
    assertThat(r.actorUserId()).isEqualTo(USER_ID);
    assertThat(r.actorRole()).isEqualTo(ActorRole.CONSULTANT);
    assertThat(r.idempotencyKey()).isEqualTo(key);
    assertThat(r.correlationId()).isEqualTo(corrId);
    assertThat(r.causationId()).isEqualTo(causId);
    assertThat(r.workflowEventId()).isEqualTo(wfId);
    assertThat(r.occurredAt()).isEqualTo(Instant.parse("2026-04-30T18:00:00Z"));
    assertThat(r.createdAt()).isNotNull();
  }

  private static CanonicalWriteAttemptAppendCommand blockedCommand() {
    return new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-1000-000000000001")),
        null,
        "headline",
        "claim-value:headline:block-v1",
        null,
        null,
        null,
        "block",
        List.of("system_inference_cannot_be_canonical_fact"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey("test-blocked-idempotency-" + (++blockedCommandCounter)),
        null,
        null,
        null,
        Instant.parse("2026-04-30T12:00:00Z"));
  }

  private static CanonicalWriteAttemptAppendCommand allowedCommand(String idempotencyKey) {
    return new CanonicalWriteAttemptAppendCommand(
        ORGANIZATION_ID,
        new EntityRef("CANDIDATE", UUID.randomUUID()),
        null,
        "headline",
        "claim-value:headline:allow-v1",
        null,
        null,
        null,
        "allow",
        List.of("canonical_write_gate_passed"),
        new ActorRef(USER_ID, ActorRole.CONSULTANT),
        null,
        new WorkflowIdempotencyKey(idempotencyKey),
        null,
        null,
        null,
        Instant.parse("2026-04-30T12:00:00Z"));
  }

  private static CanonicalWriteAttemptPort port() {
    return new JdbcCanonicalWriteAttemptPort(dataSource);
  }

  private static CanonicalWriteAttemptReadPort readPort() {
    return new JdbcCanonicalWriteAttemptReadPort(dataSource);
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
      public Connection getConnection(String username, String password) {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public void setLoginTimeout(int seconds) {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public int getLoginTimeout() {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public Logger getParentLogger() {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException("not used by test");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        throw new UnsupportedOperationException("not used by test");
      }
    };
  }

  private static void insertOrganizationAndUser(UUID orgId, UUID userId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement orgStmt = connection.prepareStatement(
            "INSERT INTO identity.organization (organization_id, legal_name, display_name,"
                + " status, default_timezone) VALUES (?, 'Test Org', 'test-org-180001',"
                + " 'active', 'UTC')");
        PreparedStatement userStmt = connection.prepareStatement(
            "INSERT INTO identity.user_account (user_account_id, organization_id, email,"
                + " display_name, status) VALUES (?, ?, ?, ?, 'active')")) {
      orgStmt.setObject(1, orgId);
      orgStmt.executeUpdate();
      userStmt.setObject(1, userId);
      userStmt.setObject(2, orgId);
      userStmt.setString(3, "test-attempt-" + userId.getLeastSignificantBits() + "@test.com");
      userStmt.setString(4, "Test User " + userId.getLeastSignificantBits());
      userStmt.executeUpdate();
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static boolean indexExists(String schema, String indexName) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT FROM pg_indexes "
                + "WHERE schemaname = ? AND indexname = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, indexName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static int countAttempts(UUID orgId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM governance.canonical_write_attempt "
                + "WHERE organization_id = ?")) {
      statement.setObject(1, orgId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    MigrationInfoService info = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .load()
        .info();
    return java.util.Arrays.stream(info.applied())
        .map(m -> m.getVersion().getVersion())
        .sorted()
        .toList();
  }
}
