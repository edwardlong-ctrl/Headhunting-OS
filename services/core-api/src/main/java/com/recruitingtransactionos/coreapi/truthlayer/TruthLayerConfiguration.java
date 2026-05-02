package com.recruitingtransactionos.coreapi.truthlayer;

import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcAITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TruthLayerConfiguration {

  @Bean
  @ConditionalOnMissingBean(AITaskRunPort.class)
  AITaskRunPort aiTaskRunPort(DataSource dataSource) {
    return new JdbcAITaskRunPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(AITaskRunService.class)
  AITaskRunService aiTaskRunService(AITaskRunPort aiTaskRunPort) {
    return new AITaskRunService(aiTaskRunPort);
  }

  @Bean
  @ConditionalOnMissingBean(ClaimLedgerPort.class)
  ClaimLedgerPort claimLedgerPort(DataSource dataSource) {
    return new JdbcClaimLedgerPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ClaimLedgerService.class)
  ClaimLedgerService claimLedgerService(ClaimLedgerPort claimLedgerPort) {
    return new ClaimLedgerService(claimLedgerPort);
  }

  @Bean
  @ConditionalOnMissingBean(WorkflowAuditReadPort.class)
  WorkflowAuditReadPort workflowAuditReadPort(DataSource dataSource) {
    return new JdbcWorkflowAuditReadPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(WorkflowAuditQueryService.class)
  WorkflowAuditQueryService workflowAuditQueryService(WorkflowAuditReadPort workflowAuditReadPort) {
    return new WorkflowAuditQueryService(workflowAuditReadPort);
  }
}
