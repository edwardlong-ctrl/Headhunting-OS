package com.recruitingtransactionos.coreapi.supportops;

import com.recruitingtransactionos.coreapi.notification.NotificationService;
import java.time.Instant;
import java.util.Objects;

public final class NotificationServiceFailedNotificationRetryPort implements FailedNotificationRetryPort {

  private final NotificationService notificationService;

  public NotificationServiceFailedNotificationRetryPort(NotificationService notificationService) {
    this.notificationService = Objects.requireNonNull(
        notificationService, "notificationService must not be null");
  }

  @Override
  public FailedNotificationRetryOutcome retry(FailedNotificationRetryRequest command) {
    Objects.requireNonNull(command, "command must not be null");
    NotificationService.RetryFailedNotificationResult result =
        notificationService.retryFailedNotification(new NotificationService.RetryFailedNotificationCommand(
            command.organizationId(),
            command.notificationId(),
            command.supportActorId(),
            command.ticketRef(),
            command.reason(),
            Instant.now()));
    return new FailedNotificationRetryOutcome(
        result.retryCreated(),
        result.notificationId(),
        result.resultCode());
  }
}
