package com.recruitingtransactionos.coreapi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class NotificationServicePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;
  private static WorkflowEventService workflowEventService;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    workflowEventService = mock(WorkflowEventService.class);
  }

  @BeforeEach
  void resetMocks() {
    reset(workflowEventService);
  }

  @Test
  void preferencesAndNotifications_areScopedByPortalRole() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000340001");
    UUID userId = uuid("00000000-0000-0000-0000-000000340002");
    insertOrganizationAndUser(organizationId, userId);
    NotificationService service = service();

    service.upsertPreference(
        organizationId,
        userId,
        PortalRole.CANDIDATE,
        true,
        false,
        false,
        false,
        false,
        userId);
    service.upsertPreference(
        organizationId,
        userId,
        PortalRole.CLIENT,
        true,
        true,
        false,
        true,
        false,
        userId);

    NotificationService.NotificationPreferenceRecord candidatePreference =
        service.loadPreference(organizationId, userId, PortalRole.CANDIDATE);
    NotificationService.NotificationPreferenceRecord clientPreference =
        service.loadPreference(organizationId, userId, PortalRole.CLIENT);

    assertThat(candidatePreference.reminderEnabled()).isFalse();
    assertThat(candidatePreference.emailEnabled()).isFalse();
    assertThat(clientPreference.reminderEnabled()).isTrue();
    assertThat(clientPreference.emailEnabled()).isTrue();

    UUID candidateEntityId = uuid("00000000-0000-0000-0000-000000340003");
    UUID clientEntityId = uuid("00000000-0000-0000-0000-000000340004");
    service.createNotification(new NotificationService.CreateNotificationCommand(
        organizationId,
        userId,
        PortalRole.CANDIDATE,
        "candidate_follow_up_submitted",
        "Candidate follow-up submitted",
        "Candidate follow-up answer is waiting for review.",
        "/candidate/follow-up/current-profile",
        "FOLLOW_UP_SUBMISSION",
        candidateEntityId,
        "candidate-follow-up-test",
        null,
        Instant.parse("2026-05-05T00:00:00Z")));
    service.createNotification(new NotificationService.CreateNotificationCommand(
        organizationId,
        userId,
        PortalRole.CLIENT,
        "client_clarification_requested",
        "Clarification required",
        "Client clarification is waiting for an answer.",
        "/client/jobs/job-1",
        "JOB",
        clientEntityId,
        "client-clarification-test",
        null,
        Instant.parse("2026-05-05T00:00:01Z")));

    NotificationService.NotificationPage candidateNotifications =
        service.listNotifications(organizationId, userId, PortalRole.CANDIDATE, 10, 0);
    NotificationService.NotificationPage clientNotifications =
        service.listNotifications(organizationId, userId, PortalRole.CLIENT, 10, 0);

    assertThat(candidateNotifications.totalCount()).isEqualTo(1);
    assertThat(candidateNotifications.items()).singleElement().satisfies(notification -> {
      assertThat(notification.notificationType()).isEqualTo("candidate_follow_up_submitted");
      assertThat(notification.entityId()).isEqualTo(candidateEntityId);
    });
    assertThat(clientNotifications.totalCount()).isEqualTo(1);
    assertThat(clientNotifications.items()).singleElement().satisfies(notification -> {
      assertThat(notification.notificationType()).isEqualTo("client_clarification_requested");
      assertThat(notification.entityId()).isEqualTo(clientEntityId);
    });
  }

  @Test
  void processDueSchedules_cancelsReminderWhenPreferenceOptedOut() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000340101");
    UUID userId = uuid("00000000-0000-0000-0000-000000340102");
    insertOrganizationAndUser(organizationId, userId);
    NotificationService service = service();
    Instant now = Instant.parse("2026-05-05T01:00:00Z");

    service.upsertPreference(
        organizationId,
        userId,
        PortalRole.CANDIDATE,
        true,
        false,
        false,
        false,
        false,
        userId);
    NotificationService.NotificationScheduleRecord schedule =
        service.scheduleNotification(new NotificationService.ScheduleNotificationCommand(
            organizationId,
            userId,
            PortalRole.CANDIDATE,
            "candidate_follow_up_reminder",
            "FOLLOW_UP_SUBMISSION",
            uuid("00000000-0000-0000-0000-000000340103"),
            now.minusSeconds(60),
            "{\"formId\":\"current-profile\"}"));

    int processedCount = service.processDueSchedules(now, 10);

    assertThat(processedCount).isEqualTo(1);
    assertThat(service.listNotifications(organizationId, userId, PortalRole.CANDIDATE, 10, 0).items()).isEmpty();
    assertThat(findScheduleStatus(schedule.notificationScheduleId())).isEqualTo("cancelled");
    verify(workflowEventService, never()).append(any(WorkflowEventAppendCommand.class));
  }

  @Test
  void processDueSchedules_createsReminderAndWorkflowEventWhenEnabled() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000340201");
    UUID userId = uuid("00000000-0000-0000-0000-000000340202");
    insertOrganizationAndUser(organizationId, userId);
    NotificationService service = service();
    Instant now = Instant.parse("2026-05-05T02:00:00Z");

    NotificationService.NotificationScheduleRecord schedule =
        service.scheduleNotification(new NotificationService.ScheduleNotificationCommand(
            organizationId,
            userId,
            PortalRole.CANDIDATE,
            "candidate_follow_up_reminder",
            "FOLLOW_UP_SUBMISSION",
            uuid("00000000-0000-0000-0000-000000340203"),
            now.minusSeconds(60),
            "{\"formId\":\"current-profile\"}"));

    int processedCount = service.processDueSchedules(now, 10);

    NotificationService.NotificationPage notifications =
        service.listNotifications(organizationId, userId, PortalRole.CANDIDATE, 10, 0);
    assertThat(processedCount).isEqualTo(1);
    assertThat(notifications.totalCount()).isEqualTo(1);
    assertThat(notifications.items()).singleElement().satisfies(notification -> {
      assertThat(notification.notificationType()).isEqualTo("candidate_follow_up_reminder");
      assertThat(notification.title()).isEqualTo("Reminder");
      assertThat(notification.status()).isEqualTo("delivered");
    });
    assertThat(findScheduleStatus(schedule.notificationScheduleId())).isEqualTo("processed");
    ArgumentCaptor<WorkflowEventAppendCommand> workflowEventCaptor =
        ArgumentCaptor.forClass(WorkflowEventAppendCommand.class);
    verify(workflowEventService, times(1)).append(workflowEventCaptor.capture());
    WorkflowEventAppendCommand command = workflowEventCaptor.getValue();
    assertThat(command.action()).isEqualTo("NOTIFICATION_REMINDER_TRIGGERED");
    assertThat(command.entityNamespace()).isEqualTo("operations");
    assertThat(command.entity().entityType()).isEqualTo("NOTIFICATION");
    assertThat(command.sourceType()).isEqualTo("notification_scheduler");
    assertThat(command.sourceRefId()).isEqualTo(schedule.notificationScheduleId());
  }

  private static NotificationService service() {
    return new NotificationService(dataSource, workflowEventService);
  }

  private static void insertOrganizationAndUser(UUID organizationId, UUID userId) throws SQLException {
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
        PreparedStatement user = connection.prepareStatement("""
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
      organization.setString(2, "Notification Org " + organizationId);
      organization.setString(3, "Notification Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "notification-user-" + userId + "@example.test");
      user.setString(4, "Notification User");
      user.executeUpdate();
    }
  }

  private static String findScheduleStatus(UUID scheduleId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT status
            FROM operations.notification_schedule
            WHERE notification_schedule_id = ?
            """)) {
      statement.setObject(1, scheduleId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getString("status");
      }
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
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
        return new PrintWriter(System.out);
      }

      @Override
      public void setLogWriter(PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("parent logger not supported");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("unwrap not supported");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }
    };
  }
}
