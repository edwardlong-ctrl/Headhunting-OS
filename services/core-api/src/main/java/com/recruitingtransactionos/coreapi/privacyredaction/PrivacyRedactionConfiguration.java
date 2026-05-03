package com.recruitingtransactionos.coreapi.privacyredaction;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.privacyredaction.persistence.JdbcReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.privacyredaction.port.ReidentificationRiskAssessmentPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for Task 30 privacy redaction.
 */
@Configuration
public class PrivacyRedactionConfiguration {

  @Bean
  ReidentificationRiskAssessmentPort reidentificationRiskAssessmentPort(DataSource dataSource) {
    return new JdbcReidentificationRiskAssessmentPort(dataSource);
  }

  @Bean
  ReidentificationRiskAssessmentService reidentificationRiskAssessmentService() {
    return new ReidentificationRiskAssessmentService();
  }

  @Bean
  RedactionAuditService redactionAuditService(
      ReidentificationRiskAssessmentService reidentificationRiskAssessmentService,
      ReidentificationRiskAssessmentPort reidentificationRiskAssessmentPort,
      WorkflowEventService workflowEventService) {
    return new RedactionAuditService(
        reidentificationRiskAssessmentService,
        reidentificationRiskAssessmentPort,
        workflowEventService);
  }
}
