package com.recruitingtransactionos.coreapi.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

@Service
public final class NotificationService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final UUID SYSTEM_ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000034");

  private static final String INSERT_NOTIFICATION_SQL = """
      INSERT INTO operations.notification (
        notification_id,
        organization_id,
        recipient_user_account_id,
        recipient_portal_role,
        notification_type,
        status,
        title,
        body_summary,
        deep_link,
        entity_type,
        entity_id,
        source_ref,
        metadata,
        read_at,
        dismissed_at,
        created_at,
        updated_at,
        version
      ) VALUES (?, ?, ?, ?::governance.actor_role, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
      """;

  private static final String INSERT_DELIVERY_ATTEMPT_SQL = """
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
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_NOTIFICATIONS_SQL = """
      SELECT notification_id, notification_type, status, title, body_summary, deep_link,
             entity_type, entity_id, source_ref, read_at, dismissed_at, created_at, updated_at, version
      FROM operations.notification
      WHERE organization_id = ?
        AND recipient_user_account_id = ?
        AND recipient_portal_role = ?::governance.actor_role
      ORDER BY created_at DESC
      LIMIT ? OFFSET ?
      """;

  private static final String FIND_NOTIFICATION_BY_ID_SQL = """
      SELECT notification_id, notification_type, status, title, body_summary, deep_link,
             entity_type, entity_id, source_ref, read_at, dismissed_at, created_at, updated_at, version,
             recipient_user_account_id, recipient_portal_role, metadata::text AS metadata
      FROM operations.notification
      WHERE organization_id = ?
        AND notification_id = ?
      """;

  private static final String FIND_NOTIFICATION_BY_SOURCE_REF_SQL = """
      SELECT notification_id, notification_type, status, title, body_summary, deep_link,
             entity_type, entity_id, source_ref, read_at, dismissed_at, created_at, updated_at, version
      FROM operations.notification
      WHERE organization_id = ?
        AND source_ref = ?
      ORDER BY created_at ASC
      LIMIT 1
      """;

  private static final String HAS_FAILED_DELIVERY_ATTEMPT_SQL = """
      SELECT 1
      FROM operations.notification_delivery_attempt
      WHERE organization_id = ?
        AND notification_id = ?
        AND status = 'failed'
      LIMIT 1
      """;

  private static final String COUNT_NOTIFICATIONS_SQL = """
      SELECT count(*)
      FROM operations.notification
      WHERE organization_id = ?
        AND recipient_user_account_id = ?
        AND recipient_portal_role = ?::governance.actor_role
      """;

  private static final String MARK_READ_SQL = """
      UPDATE operations.notification
      SET status = CASE WHEN status = 'dismissed' THEN status ELSE 'read' END,
          read_at = COALESCE(read_at, ?),
          updated_at = ?,
          version = version + 1
      WHERE organization_id = ?
        AND recipient_user_account_id = ?
        AND recipient_portal_role = ?::governance.actor_role
        AND notification_id = ?
      """;

  private static final String DISMISS_SQL = """
      UPDATE operations.notification
      SET status = 'dismissed',
          dismissed_at = ?,
          updated_at = ?,
          version = version + 1
      WHERE organization_id = ?
        AND recipient_user_account_id = ?
        AND recipient_portal_role = ?::governance.actor_role
        AND notification_id = ?
      """;

  private static final String FIND_PREFERENCE_SQL = """
      SELECT portal_role, in_app_enabled, email_enabled, sms_enabled, reminder_enabled, unsubscribed,
             updated_at, updated_by_user_id, version
      FROM operations.notification_preference
      WHERE organization_id = ?
        AND user_account_id = ?
        AND portal_role = ?::governance.actor_role
      """;

  private static final String UPSERT_PREFERENCE_SQL = """
      INSERT INTO operations.notification_preference (
        organization_id,
        user_account_id,
        portal_role,
        in_app_enabled,
        email_enabled,
        sms_enabled,
        reminder_enabled,
        unsubscribed,
        updated_at,
        updated_by_user_id,
        version
      ) VALUES (?, ?, ?::governance.actor_role, ?, ?, ?, ?, ?, ?, ?, 1)
      ON CONFLICT (organization_id, user_account_id, portal_role)
      DO UPDATE SET
        portal_role = EXCLUDED.portal_role,
        in_app_enabled = EXCLUDED.in_app_enabled,
        email_enabled = EXCLUDED.email_enabled,
        sms_enabled = EXCLUDED.sms_enabled,
        reminder_enabled = EXCLUDED.reminder_enabled,
        unsubscribed = EXCLUDED.unsubscribed,
        updated_at = EXCLUDED.updated_at,
        updated_by_user_id = EXCLUDED.updated_by_user_id,
        version = operations.notification_preference.version + 1
      """;

  private static final String INSERT_SCHEDULE_SQL = """
      INSERT INTO operations.notification_schedule (
        notification_schedule_id,
        organization_id,
        user_account_id,
        portal_role,
        notification_type,
        entity_type,
        entity_id,
        due_at,
        status,
        payload,
        last_attempted_at,
        processed_at,
        created_at,
        updated_at,
        version
      ) VALUES (?, ?, ?, ?::governance.actor_role, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_DUE_SCHEDULES_SQL = """
      SELECT notification_schedule_id, organization_id, user_account_id, portal_role,
             notification_type, entity_type, entity_id, due_at, status, payload,
             last_attempted_at, processed_at, created_at, updated_at, version
      FROM operations.notification_schedule
      WHERE status = 'scheduled'
        AND due_at <= ?
      ORDER BY due_at ASC
      LIMIT ?
      """;

  private static final String MARK_SCHEDULE_PROCESSED_SQL = """
      UPDATE operations.notification_schedule
      SET status = 'processed',
          last_attempted_at = ?,
          processed_at = ?,
          updated_at = ?,
          version = version + 1
      WHERE notification_schedule_id = ?
      """;

  private static final String MARK_SCHEDULE_CANCELLED_SQL = """
      UPDATE operations.notification_schedule
      SET status = 'cancelled',
          last_attempted_at = ?,
          updated_at = ?,
          version = version + 1
      WHERE notification_schedule_id = ?
      """;

  private final DataSource dataSource;
  private final WorkflowEventService workflowEventService;
  private final EmailNotificationProvider emailProvider;
  private final SmsNotificationProvider smsProvider;

  @Autowired
  public NotificationService(DataSource dataSource, WorkflowEventService workflowEventService) {
    this(
        dataSource,
        workflowEventService,
        new NoOpEmailNotificationProvider(),
        new NoOpSmsNotificationProvider());
  }

  NotificationService(
      DataSource dataSource,
      WorkflowEventService workflowEventService,
      EmailNotificationProvider emailProvider,
      SmsNotificationProvider smsProvider) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.workflowEventService = Objects.requireNonNull(
        workflowEventService, "workflowEventService must not be null");
    this.emailProvider = Objects.requireNonNull(emailProvider, "emailProvider must not be null");
    this.smsProvider = Objects.requireNonNull(smsProvider, "smsProvider must not be null");
  }

  public NotificationPage listNotifications(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      int limit,
      int offset) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    int normalizedLimit = Math.max(1, limit);
    int normalizedOffset = Math.max(0, offset);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      List<NotificationRecord> items = new ArrayList<>();
      try (PreparedStatement statement = connection.prepareStatement(FIND_NOTIFICATIONS_SQL)) {
        statement.setObject(1, organizationId);
        statement.setObject(2, userAccountId);
        statement.setString(3, portalRole.wireValue());
        statement.setInt(4, normalizedLimit);
        statement.setInt(5, normalizedOffset);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            items.add(toNotificationRecord(resultSet));
          }
        }
      }
      int totalCount = 0;
      try (PreparedStatement statement = connection.prepareStatement(COUNT_NOTIFICATIONS_SQL)) {
        statement.setObject(1, organizationId);
        statement.setObject(2, userAccountId);
        statement.setString(3, portalRole.wireValue());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            totalCount = resultSet.getInt(1);
          }
        }
      }
      return new NotificationPage(List.copyOf(items), totalCount, normalizedLimit, normalizedOffset);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to list notifications", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public void markRead(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      UUID notificationId) {
    updateNotificationStatus(MARK_READ_SQL, organizationId, userAccountId, portalRole, notificationId);
  }

  public void dismiss(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      UUID notificationId) {
    updateNotificationStatus(DISMISS_SQL, organizationId, userAccountId, portalRole, notificationId);
  }

  public NotificationPreferenceRecord loadPreference(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_PREFERENCE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, userAccountId);
      statement.setString(3, portalRole.wireValue());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return NotificationPreferenceRecord.defaults(organizationId, userAccountId, portalRole);
        }
        return toPreferenceRecord(organizationId, userAccountId, resultSet);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load notification preference", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public NotificationPreferenceRecord upsertPreference(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      boolean inAppEnabled,
      boolean emailEnabled,
      boolean smsEnabled,
      boolean reminderEnabled,
      boolean unsubscribed,
      UUID updatedByUserId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    Instant now = Instant.now();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_PREFERENCE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, userAccountId);
      statement.setString(3, portalRole.wireValue());
      statement.setBoolean(4, inAppEnabled);
      statement.setBoolean(5, emailEnabled);
      statement.setBoolean(6, smsEnabled);
      statement.setBoolean(7, reminderEnabled);
      statement.setBoolean(8, unsubscribed);
      statement.setTimestamp(9, Timestamp.from(now));
      statement.setObject(10, updatedByUserId);
      statement.executeUpdate();
      return loadPreference(organizationId, userAccountId, portalRole);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to upsert notification preference", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public NotificationRecord createNotification(CreateNotificationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Instant now = command.createdAt() != null ? command.createdAt() : Instant.now();
    NotificationPreferenceRecord preference = loadPreference(
        command.organizationId(),
        command.recipientUserAccountId(),
        command.recipientPortalRole());
    UUID notificationId = UUID.randomUUID();
    String initialStatus = preference.inAppEnabled() ? "delivered" : "pending";
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      insertNotification(connection, notificationId, command, initialStatus, now);
      if (preference.inAppEnabled()) {
        insertDeliveryAttempt(
            connection,
            command.organizationId(),
            notificationId,
            "in_app",
            "internal_in_app",
            "delivered",
            null,
            now);
      } else {
        insertDeliveryAttempt(
            connection,
            command.organizationId(),
            notificationId,
            "in_app",
            "internal_in_app",
            "skipped",
            "in_app_disabled",
            now);
      }
      if (!preference.unsubscribed() && preference.emailEnabled()) {
        DeliveryOutcome outcome = emailProvider.send(command);
        insertDeliveryAttempt(
            connection,
            command.organizationId(),
            notificationId,
            "email",
            outcome.providerKey(),
            outcome.status(),
            outcome.safeErrorCode(),
            now);
      }
      if (!preference.unsubscribed() && preference.smsEnabled()) {
        DeliveryOutcome outcome = smsProvider.send(command);
        insertDeliveryAttempt(
            connection,
            command.organizationId(),
            notificationId,
            "sms",
            outcome.providerKey(),
            outcome.status(),
            outcome.safeErrorCode(),
            now);
      }
      return new NotificationRecord(
          notificationId,
          command.notificationType(),
          initialStatus,
          command.title(),
          command.bodySummary(),
          command.deepLink(),
          command.entityType(),
          command.entityId(),
          command.sourceRef(),
          null,
          null,
          now,
          now,
          1);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create notification", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public RetryFailedNotificationResult retryFailedNotification(RetryFailedNotificationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Instant now = command.createdAt() != null ? command.createdAt() : Instant.now();
    String retrySourceRef = "support_retry:" + command.notificationId() + ":" + command.ticketRef();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotificationRetrySource source;
      try (PreparedStatement statement = connection.prepareStatement(FIND_NOTIFICATION_BY_ID_SQL)) {
        statement.setObject(1, command.organizationId());
        statement.setObject(2, command.notificationId());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            return new RetryFailedNotificationResult(false, command.notificationId(), "notification_retry_not_found");
          }
          source = toNotificationRetrySource(resultSet);
        }
      }
      try (PreparedStatement statement = connection.prepareStatement(FIND_NOTIFICATION_BY_SOURCE_REF_SQL)) {
        statement.setObject(1, command.organizationId());
        statement.setString(2, retrySourceRef);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return new RetryFailedNotificationResult(
                false,
                resultSet.getObject("notification_id", UUID.class),
                "notification_retry_duplicate_skipped");
          }
        }
      }
      try (PreparedStatement statement = connection.prepareStatement(HAS_FAILED_DELIVERY_ATTEMPT_SQL)) {
        statement.setObject(1, command.organizationId());
        statement.setObject(2, command.notificationId());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            return new RetryFailedNotificationResult(false, command.notificationId(), "notification_retry_not_failed");
          }
        }
      }
      NotificationRecord retry = createNotification(new CreateNotificationCommand(
          command.organizationId(),
          source.recipientUserAccountId(),
          source.recipientPortalRole(),
          source.record().notificationType(),
          source.record().title(),
          source.record().bodySummary(),
          source.record().deepLink(),
          source.record().entityType(),
          source.record().entityId(),
          retrySourceRef,
          retryMetadata(source.metadataJson(), command),
          now));
      return new RetryFailedNotificationResult(true, retry.notificationId(), "notification_retry_created");
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to retry failed notification", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public NotificationScheduleRecord scheduleNotification(ScheduleNotificationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Instant now = Instant.now();
    UUID scheduleId = UUID.randomUUID();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SCHEDULE_SQL)) {
      statement.setObject(1, scheduleId);
      statement.setObject(2, command.organizationId());
      statement.setObject(3, command.userAccountId());
      statement.setString(4, command.portalRole().wireValue());
      statement.setString(5, command.notificationType());
      statement.setString(6, command.entityType());
      statement.setObject(7, command.entityId());
      statement.setTimestamp(8, Timestamp.from(command.dueAt()));
      statement.setString(9, "scheduled");
      statement.setString(10, safeJson(command.payloadJson()));
      statement.setObject(11, null);
      statement.setObject(12, null);
      statement.setTimestamp(13, Timestamp.from(now));
      statement.setTimestamp(14, Timestamp.from(now));
      statement.setInt(15, 1);
      statement.executeUpdate();
      return new NotificationScheduleRecord(
          scheduleId,
          command.organizationId(),
          command.userAccountId(),
          command.portalRole(),
          command.notificationType(),
          command.entityType(),
          command.entityId(),
          command.dueAt(),
          "scheduled",
          safeJson(command.payloadJson()),
          null,
          null,
          now,
          now,
          1);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to schedule notification", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  public int processDueSchedules(Instant now, int limit) {
    Objects.requireNonNull(now, "now must not be null");
    int normalizedLimit = Math.max(1, limit);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      List<NotificationScheduleRecord> dueSchedules = new ArrayList<>();
      try (PreparedStatement statement = connection.prepareStatement(FIND_DUE_SCHEDULES_SQL)) {
        statement.setTimestamp(1, Timestamp.from(now));
        statement.setInt(2, normalizedLimit);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            dueSchedules.add(toScheduleRecord(resultSet));
          }
        }
      }
      for (NotificationScheduleRecord schedule : dueSchedules) {
        NotificationPreferenceRecord preference = loadPreference(
            schedule.organizationId(),
            schedule.userAccountId(),
            schedule.portalRole());
        if (!preference.reminderEnabled() || preference.unsubscribed()) {
          try (PreparedStatement statement = connection.prepareStatement(MARK_SCHEDULE_CANCELLED_SQL)) {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setObject(3, schedule.notificationScheduleId());
            statement.executeUpdate();
          }
          continue;
        }

        NotificationRecord reminderNotification = createNotification(new CreateNotificationCommand(
            schedule.organizationId(),
            schedule.userAccountId(),
            schedule.portalRole(),
            schedule.notificationType(),
            "Reminder",
            safeReminderBody(schedule),
            null,
            schedule.entityType(),
            schedule.entityId(),
            "schedule:" + schedule.notificationScheduleId(),
            schedule.payloadJson(),
            now));
        appendReminderTriggeredEvent(schedule, reminderNotification, now);
        try (PreparedStatement statement = connection.prepareStatement(MARK_SCHEDULE_PROCESSED_SQL)) {
          statement.setTimestamp(1, Timestamp.from(now));
          statement.setTimestamp(2, Timestamp.from(now));
          statement.setTimestamp(3, Timestamp.from(now));
          statement.setObject(4, schedule.notificationScheduleId());
          statement.executeUpdate();
        }
      }
      return dueSchedules.size();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to process due notification schedules", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void updateNotificationStatus(
      String sql,
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      UUID notificationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    Objects.requireNonNull(notificationId, "notificationId must not be null");
    Instant now = Instant.now();
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setTimestamp(1, Timestamp.from(now));
      statement.setTimestamp(2, Timestamp.from(now));
      statement.setObject(3, organizationId);
      statement.setObject(4, userAccountId);
      statement.setString(5, portalRole.wireValue());
      statement.setObject(6, notificationId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to update notification status", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void insertNotification(
      Connection connection,
      UUID notificationId,
      CreateNotificationCommand command,
      String status,
      Instant now) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_NOTIFICATION_SQL)) {
      statement.setObject(1, notificationId);
      statement.setObject(2, command.organizationId());
      statement.setObject(3, command.recipientUserAccountId());
      statement.setString(4, command.recipientPortalRole().wireValue());
      statement.setString(5, command.notificationType());
      statement.setString(6, status);
      statement.setString(7, command.title());
      statement.setString(8, command.bodySummary());
      statement.setString(9, command.deepLink());
      statement.setString(10, command.entityType());
      statement.setObject(11, command.entityId());
      statement.setString(12, command.sourceRef());
      statement.setString(13, safeJson(command.metadataJson()));
      statement.setObject(14, null);
      statement.setObject(15, null);
      statement.setTimestamp(16, Timestamp.from(now));
      statement.setTimestamp(17, Timestamp.from(now));
      statement.setInt(18, 1);
      statement.executeUpdate();
    }
  }

  private void insertDeliveryAttempt(
      Connection connection,
      UUID organizationId,
      UUID notificationId,
      String channel,
      String providerKey,
      String status,
      String safeErrorCode,
      Instant attemptedAt) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_DELIVERY_ATTEMPT_SQL)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, organizationId);
      statement.setObject(3, notificationId);
      statement.setString(4, channel);
      statement.setString(5, providerKey);
      statement.setString(6, status);
      statement.setString(7, safeErrorCode);
      statement.setTimestamp(8, Timestamp.from(attemptedAt));
      statement.setTimestamp(9, Timestamp.from(attemptedAt));
      statement.executeUpdate();
    }
  }

  private static NotificationRecord toNotificationRecord(ResultSet resultSet) throws SQLException {
    return new NotificationRecord(
        resultSet.getObject("notification_id", UUID.class),
        resultSet.getString("notification_type"),
        resultSet.getString("status"),
        resultSet.getString("title"),
        resultSet.getString("body_summary"),
        resultSet.getString("deep_link"),
        resultSet.getString("entity_type"),
        resultSet.getObject("entity_id", UUID.class),
        resultSet.getString("source_ref"),
        timestampToInstant(resultSet.getTimestamp("read_at")),
        timestampToInstant(resultSet.getTimestamp("dismissed_at")),
        timestampToInstant(resultSet.getTimestamp("created_at")),
        timestampToInstant(resultSet.getTimestamp("updated_at")),
        resultSet.getInt("version"));
  }

  private static NotificationRetrySource toNotificationRetrySource(ResultSet resultSet) throws SQLException {
    return new NotificationRetrySource(
        toNotificationRecord(resultSet),
        resultSet.getObject("recipient_user_account_id", UUID.class),
        PortalRole.fromWireValue(resultSet.getString("recipient_portal_role")),
        resultSet.getString("metadata"));
  }

  private static String retryMetadata(
      String originalMetadataJson,
      RetryFailedNotificationCommand command) {
    try {
      return OBJECT_MAPPER.createObjectNode()
          .put("retryOfNotificationId", command.notificationId().toString())
          .put("supportTicketRef", command.ticketRef())
          .put("supportActorId", command.supportActorId().toString())
          .put("supportReason", command.reason())
          .set("originalMetadata", OBJECT_MAPPER.readTree(safeJson(originalMetadataJson)))
          .toString();
    } catch (Exception exception) {
      return OBJECT_MAPPER.createObjectNode()
          .put("retryOfNotificationId", command.notificationId().toString())
          .put("supportTicketRef", command.ticketRef())
          .toString();
    }
  }

  private static NotificationPreferenceRecord toPreferenceRecord(
      UUID organizationId,
      UUID userAccountId,
      ResultSet resultSet) throws SQLException {
    return new NotificationPreferenceRecord(
        organizationId,
        userAccountId,
        PortalRole.fromWireValue(resultSet.getString("portal_role")),
        resultSet.getBoolean("in_app_enabled"),
        resultSet.getBoolean("email_enabled"),
        resultSet.getBoolean("sms_enabled"),
        resultSet.getBoolean("reminder_enabled"),
        resultSet.getBoolean("unsubscribed"),
        timestampToInstant(resultSet.getTimestamp("updated_at")),
        resultSet.getObject("updated_by_user_id", UUID.class),
        resultSet.getInt("version"));
  }

  private static NotificationScheduleRecord toScheduleRecord(ResultSet resultSet) throws SQLException {
    return new NotificationScheduleRecord(
        resultSet.getObject("notification_schedule_id", UUID.class),
        resultSet.getObject("organization_id", UUID.class),
        resultSet.getObject("user_account_id", UUID.class),
        PortalRole.fromWireValue(resultSet.getString("portal_role")),
        resultSet.getString("notification_type"),
        resultSet.getString("entity_type"),
        resultSet.getObject("entity_id", UUID.class),
        timestampToInstant(resultSet.getTimestamp("due_at")),
        resultSet.getString("status"),
        resultSet.getString("payload"),
        timestampToInstant(resultSet.getTimestamp("last_attempted_at")),
        timestampToInstant(resultSet.getTimestamp("processed_at")),
        timestampToInstant(resultSet.getTimestamp("created_at")),
        timestampToInstant(resultSet.getTimestamp("updated_at")),
        resultSet.getInt("version"));
  }

  private static Instant timestampToInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static String safeJson(String json) {
    if (json == null || json.isBlank()) {
      return "{}";
    }
    try {
      return OBJECT_MAPPER.readTree(json).toString();
    } catch (Exception exception) {
      return "{}";
    }
  }

  private static String safeReminderBody(NotificationScheduleRecord schedule) {
    return switch (schedule.notificationType()) {
      case "candidate_follow_up_reminder" -> "You still have candidate follow-up items waiting for your response.";
      case "client_clarification_reminder" -> "You still have clarification questions waiting for your response.";
      case "candidate_consent_reminder" -> "You still have a consent request waiting for your response.";
      default -> "You still have a pending action waiting in the portal.";
    };
  }

  private void appendReminderTriggeredEvent(
      NotificationScheduleRecord schedule,
      NotificationRecord notification,
      Instant occurredAt) {
    workflowEventService.append(new WorkflowEventAppendCommand(
        schedule.organizationId(),
        "operations",
        new EntityRef(WorkflowEntityType.NOTIFICATION.wireValue(), notification.notificationId()),
        notification.version(),
        WorkflowActionCode.NOTIFICATION_REMINDER_TRIGGERED.wireValue(),
        new WorkflowStateSnapshot(reminderState(schedule, "scheduled")),
        new WorkflowStateSnapshot(reminderState(schedule, "triggered")),
        new ActorRef(SYSTEM_ACTOR_ID, ActorRole.SYSTEM),
        "notification_scheduler",
        schedule.notificationScheduleId(),
        null,
        null,
        "Scheduled reminder notification was triggered.",
        new WorkflowIdempotencyKey("notification-reminder-triggered:" + schedule.notificationScheduleId()),
        null,
        null,
        occurredAt));
  }

  private static String reminderState(NotificationScheduleRecord schedule, String status) {
    return OBJECT_MAPPER.createObjectNode()
        .put("status", status)
        .put("notificationType", schedule.notificationType())
        .put("portalRole", schedule.portalRole().wireValue())
        .put("entityType", schedule.entityType())
        .put("entityId", schedule.entityId().toString())
        .toString();
  }

  public record NotificationRecord(
      UUID notificationId,
      String notificationType,
      String status,
      String title,
      String bodySummary,
      String deepLink,
      String entityType,
      UUID entityId,
      String sourceRef,
      Instant readAt,
      Instant dismissedAt,
      Instant createdAt,
      Instant updatedAt,
      int version) {}

  public record NotificationPage(
      List<NotificationRecord> items,
      int totalCount,
      int limit,
      int offset) {}

  public record NotificationPreferenceRecord(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      boolean inAppEnabled,
      boolean emailEnabled,
      boolean smsEnabled,
      boolean reminderEnabled,
      boolean unsubscribed,
      Instant updatedAt,
      UUID updatedByUserId,
      int version) {

    static NotificationPreferenceRecord defaults(
        UUID organizationId,
        UUID userAccountId,
        PortalRole portalRole) {
      return new NotificationPreferenceRecord(
          organizationId,
          userAccountId,
          portalRole,
          true,
          false,
          false,
          true,
          false,
          null,
          null,
          0);
    }
  }

  public record NotificationScheduleRecord(
      UUID notificationScheduleId,
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      String notificationType,
      String entityType,
      UUID entityId,
      Instant dueAt,
      String status,
      String payloadJson,
      Instant lastAttemptedAt,
      Instant processedAt,
      Instant createdAt,
      Instant updatedAt,
      int version) {}

  public record CreateNotificationCommand(
      UUID organizationId,
      UUID recipientUserAccountId,
      PortalRole recipientPortalRole,
      String notificationType,
      String title,
      String bodySummary,
      String deepLink,
      String entityType,
      UUID entityId,
      String sourceRef,
      String metadataJson,
      Instant createdAt) {}

  public record ScheduleNotificationCommand(
      UUID organizationId,
      UUID userAccountId,
      PortalRole portalRole,
      String notificationType,
      String entityType,
      UUID entityId,
      Instant dueAt,
      String payloadJson) {}

  private record NotificationRetrySource(
      NotificationRecord record,
      UUID recipientUserAccountId,
      PortalRole recipientPortalRole,
      String metadataJson) {}

  public record RetryFailedNotificationCommand(
      UUID organizationId,
      UUID notificationId,
      UUID supportActorId,
      String ticketRef,
      String reason,
      Instant createdAt) {

    public RetryFailedNotificationCommand {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(notificationId, "notificationId must not be null");
      Objects.requireNonNull(supportActorId, "supportActorId must not be null");
      ticketRef = requireNonBlank(ticketRef, "ticketRef");
      reason = requireNonBlank(reason, "reason");
    }
  }

  public record RetryFailedNotificationResult(
      boolean retryCreated,
      UUID notificationId,
      String resultCode) {

    public RetryFailedNotificationResult {
      Objects.requireNonNull(notificationId, "notificationId must not be null");
      resultCode = requireNonBlank(resultCode, "resultCode");
    }
  }

  public interface EmailNotificationProvider {
    DeliveryOutcome send(CreateNotificationCommand command);
  }

  public interface SmsNotificationProvider {
    DeliveryOutcome send(CreateNotificationCommand command);
  }

  public record DeliveryOutcome(String providerKey, String status, String safeErrorCode) {}

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static final class NoOpEmailNotificationProvider implements EmailNotificationProvider {
    @Override
    public DeliveryOutcome send(CreateNotificationCommand command) {
      return new DeliveryOutcome("noop_email", "skipped", "email_provider_not_configured");
    }
  }

  private static final class NoOpSmsNotificationProvider implements SmsNotificationProvider {
    @Override
    public DeliveryOutcome send(CreateNotificationCommand command) {
      return new DeliveryOutcome("noop_sms", "skipped", "sms_provider_not_configured");
    }
  }
}
