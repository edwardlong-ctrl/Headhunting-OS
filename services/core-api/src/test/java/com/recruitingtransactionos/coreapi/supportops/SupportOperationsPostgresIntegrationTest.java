package com.recruitingtransactionos.coreapi.supportops;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class SupportOperationsPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000561001");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000561002");
  private static final UUID ADMIN_A = UUID.fromString("00000000-0000-0000-0000-000000561101");
  private static final UUID USER_A = UUID.fromString("00000000-0000-0000-0000-000000561201");
  private static final UUID USER_B = UUID.fromString("00000000-0000-0000-0000-000000561202");
  private static final UUID TARGET_CANDIDATE = UUID.fromString("00000000-0000-0000-0000-000000561301");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrateAndSeed() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUser(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    dataSource = source;

    try (Connection connection = connection()) {
      insertOrganization(connection, ORG_A, "Support Ops Org A");
      insertOrganization(connection, ORG_B, "Support Ops Org B");
      insertUser(connection, ORG_A, ADMIN_A, "support-admin@example.test", "Support Admin");
      insertUser(connection, ORG_A, USER_A, "support-user-a@example.test", "Support User A");
      insertUser(connection, ORG_B, USER_B, "support-user-b@example.test", "Support User B");
      insertRole(connection, ORG_A, ADMIN_A, "admin");
      insertRole(connection, ORG_A, USER_A, "candidate");
      insertRole(connection, ORG_B, USER_B, "candidate");
    }
  }

  @Test
  void supportLookupIsOrganizationScopedAndAuditedInPostgres() throws SQLException {
    SupportOperationsService service = service();

    SupportUserLookupResult allowed = service.lookupUser(new SupportUserLookupCommand(
        admin(),
        ORG_A,
        USER_A,
        "SUP-56-LOOKUP",
        "Investigate user's notification state."));
    SupportUserLookupResult crossOrg = service.lookupUser(new SupportUserLookupCommand(
        admin(),
        ORG_B,
        USER_B,
        "SUP-56-LOOKUP",
        "Investigate user's notification state."));

    assertThat(allowed.allowed()).isTrue();
    assertThat(allowed.user()).isPresent();
    assertThat(allowed.user().orElseThrow().organizationId()).isEqualTo(ORG_A);
    assertThat(allowed.user().orElseThrow().roles()).contains(PortalRole.CANDIDATE);
    assertThat(crossOrg.allowed()).isFalse();
    assertThat(crossOrg.user()).isEmpty();
    assertThat(supportAuditCount("support.lookup_user")).isGreaterThanOrEqualTo(2);
  }

  @Test
  void failedNotificationRetryReusesNotificationServiceAndIsDuplicateSafe() throws SQLException {
    SupportOperationsService service = service();
    NotificationService notificationService =
        new NotificationService(dataSource, new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)));
    UUID notificationId = notificationService.createNotification(new NotificationService.CreateNotificationCommand(
        ORG_A,
        USER_A,
        PortalRole.CANDIDATE,
        "candidate_follow_up_reminder",
        "Reminder",
        "You still have a pending support-visible action.",
        "/candidate/follow-up",
        WorkflowEntityType.NOTIFICATION.wireValue(),
        UUID.fromString("00000000-0000-0000-0000-000000561401"),
        "original-support-retry",
        "{}",
        Instant.parse("2026-05-10T00:00:00Z"))).notificationId();
    insertFailedDeliveryAttempt(notificationId);

    SupportActionResult first = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(),
        ORG_A,
        notificationId,
        "SUP-56-RETRY",
        "Retry failed provider delivery."));
    SupportActionResult second = service.retryFailedNotification(new FailedNotificationSupportRetryCommand(
        admin(),
        ORG_A,
        notificationId,
        "SUP-56-RETRY",
        "Retry failed provider delivery."));

    assertThat(first.allowed()).isTrue();
    assertThat(first.resultCode()).isEqualTo("notification_retry_created");
    assertThat(second.allowed()).isTrue();
    assertThat(second.resultCode()).isEqualTo("notification_retry_duplicate_skipped");
    assertThat(retryNotificationCount(notificationId, "SUP-56-RETRY")).isEqualTo(1);
    assertThat(supportAuditCount("support.retry_failed_notification")).isGreaterThanOrEqualTo(2);
  }

  @Test
  void dataCorrectionRequestPersistsReviewWorkflowAndSupportAuditWithoutFactMutation() throws SQLException {
    SupportOperationsService service = service();

    DataCorrectionRequestResult result = service.requestDataCorrection(new DataCorrectionSupportCommand(
        admin(),
        ORG_A,
        new EntityRef(WorkflowEntityType.CANDIDATE.wireValue(), TARGET_CANDIDATE),
        "profile.summary",
        "Support correction request for mixed employer summary.",
        "SUP-56-CORRECTION",
        "Create governed correction request."));

    assertThat(result.allowed()).isTrue();
    assertThat(result.canonicalFactsMutated()).isFalse();
    assertThat(reviewEventExists(result.reviewEventId().value())).isTrue();
    assertThat(workflowEventExists(result.workflowEventId().value())).isTrue();
    assertThat(supportAuditCount("support.request_data_correction")).isGreaterThanOrEqualTo(1);
  }

  private static SupportOperationsService service() {
    NotificationService notificationService =
        new NotificationService(dataSource, new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)));
    return new SupportOperationsService(
        new SupportOperationsPermissionPolicy(),
        new JdbcSupportUserLookupPort(dataSource),
        new NotificationServiceFailedNotificationRetryPort(notificationService),
        command -> new AITaskSupportReplayOutcome(new AITaskRunId(UUID.randomUUID()), false, "ai_replay_created"),
        new ReviewEventService(new JdbcReviewEventPort(dataSource)),
        new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
        new JdbcSupportActionAuditPort(dataSource));
  }

  private static SupportActor admin() {
    return new SupportActor(ORG_A, ADMIN_A, PortalRole.ADMIN);
  }

  private static int supportAuditCount(String action) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM audit.audit_log
            WHERE organization_id = ?
              AND action = ?
            """)) {
      statement.setObject(1, ORG_A);
      statement.setString(2, action);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static int retryNotificationCount(UUID originalNotificationId, String ticketRef) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM operations.notification
            WHERE organization_id = ?
              AND source_ref = ?
            """)) {
      statement.setObject(1, ORG_A);
      statement.setString(2, "support_retry:" + originalNotificationId + ":" + ticketRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static boolean reviewEventExists(UUID reviewEventId) throws SQLException {
    return exists("SELECT 1 FROM governance.review_event WHERE review_event_id = ?", reviewEventId);
  }

  private static boolean workflowEventExists(UUID workflowEventId) throws SQLException {
    return exists("SELECT 1 FROM workflow.workflow_event WHERE workflow_event_id = ?", workflowEventId);
  }

  private static boolean exists(String sql, UUID id) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static void insertFailedDeliveryAttempt(UUID notificationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO operations.notification_delivery_attempt (
              notification_delivery_attempt_id,
              organization_id,
              notification_id,
              channel,
              provider_key,
              status,
              safe_error_code,
              attempted_at,
              created_at
            )
            VALUES (?, ?, ?, 'email', 'provider_x', 'failed', 'provider_timeout', now(), now())
            """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, ORG_A);
      statement.setObject(3, notificationId);
      statement.executeUpdate();
    }
  }

  private static void insertOrganization(
      Connection connection,
      UUID organizationId,
      String name) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO identity.organization (
          organization_id, legal_name, display_name, status, default_timezone
        )
        VALUES (?, ?, ?, 'active', 'UTC')
        """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, name);
      statement.setString(3, name);
      statement.executeUpdate();
    }
  }

  private static void insertUser(
      Connection connection,
      UUID organizationId,
      UUID userId,
      String email,
      String displayName) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO identity.user_account (
          user_account_id, organization_id, email, display_name, status
        )
        VALUES (?, ?, ?, ?, 'active')
        """)) {
      statement.setObject(1, userId);
      statement.setObject(2, organizationId);
      statement.setString(3, email);
      statement.setString(4, displayName);
      statement.executeUpdate();
    }
  }

  private static void insertRole(
      Connection connection,
      UUID organizationId,
      UUID userId,
      String role) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO identity.role_assignment (
          role_assignment_id, organization_id, user_account_id, role, scope_type, status, granted_by
        )
        VALUES (?, ?, ?, ?::governance.actor_role, 'organization', 'active', ?)
        """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, organizationId);
      statement.setObject(3, userId);
      statement.setString(4, role);
      statement.setObject(5, userId);
      statement.executeUpdate();
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }
}
