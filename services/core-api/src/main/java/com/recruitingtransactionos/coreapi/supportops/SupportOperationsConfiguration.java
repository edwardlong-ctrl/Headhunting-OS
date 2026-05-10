package com.recruitingtransactionos.coreapi.supportops;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskReplayService;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SupportOperationsConfiguration {

  @Bean
  @ConditionalOnMissingBean
  SupportOperationsPermissionPolicy supportOperationsPermissionPolicy() {
    return new SupportOperationsPermissionPolicy();
  }

  @Bean
  @ConditionalOnMissingBean
  SupportUserLookupPort supportUserLookupPort(DataSource dataSource) {
    return new JdbcSupportUserLookupPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean
  FailedNotificationRetryPort failedNotificationRetryPort(NotificationService notificationService) {
    return new NotificationServiceFailedNotificationRetryPort(notificationService);
  }

  @Bean
  @ConditionalOnMissingBean
  AITaskSupportReplayPort aiTaskSupportReplayPort(AITaskReplayService aiTaskReplayService) {
    return new AITaskReplaySupportAdapter(aiTaskReplayService);
  }

  @Bean
  @ConditionalOnMissingBean
  SupportActionAuditPort supportActionAuditPort(DataSource dataSource) {
    return new JdbcSupportActionAuditPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean
  SupportOperationsService supportOperationsService(
      SupportOperationsPermissionPolicy permissionPolicy,
      SupportUserLookupPort userLookupPort,
      FailedNotificationRetryPort failedNotificationRetryPort,
      AITaskSupportReplayPort aiTaskSupportReplayPort,
      ReviewEventService reviewEventService,
      WorkflowEventService workflowEventService,
      SupportActionAuditPort supportActionAuditPort) {
    return new SupportOperationsService(
        permissionPolicy,
        userLookupPort,
        failedNotificationRetryPort,
        aiTaskSupportReplayPort,
        reviewEventService,
        workflowEventService,
        supportActionAuditPort);
  }
}
