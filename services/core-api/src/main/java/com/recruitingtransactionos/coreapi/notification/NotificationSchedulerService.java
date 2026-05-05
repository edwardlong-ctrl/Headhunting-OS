package com.recruitingtransactionos.coreapi.notification;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class NotificationSchedulerService {

  private final NotificationService notificationService;

  public NotificationSchedulerService(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(fixedDelayString = "${rto.notification.scheduler.fixed-delay-ms:300000}")
  public void processDueSchedules() {
    notificationService.processDueSchedules(Instant.now(), 50);
  }
}
