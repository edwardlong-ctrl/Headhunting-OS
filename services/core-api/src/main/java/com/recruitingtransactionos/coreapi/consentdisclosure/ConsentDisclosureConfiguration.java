package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcCandidateWorkflowStatePort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcDisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcUnlockDecisionPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.CandidateWorkflowStatePort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsentDisclosureConfiguration {

  @Bean
  ConsentRecordPort consentRecordPort(DataSource dataSource) {
    return new JdbcConsentRecordPort(dataSource);
  }

  @Bean
  DisclosureRecordPort disclosureRecordPort(DataSource dataSource) {
    return new JdbcDisclosureRecordPort(dataSource);
  }

  @Bean
  UnlockDecisionPort unlockDecisionPort(DataSource dataSource) {
    return new JdbcUnlockDecisionPort(dataSource);
  }

  @Bean
  CandidateWorkflowStatePort candidateWorkflowStatePort(DataSource dataSource) {
    return new JdbcCandidateWorkflowStatePort(dataSource);
  }

  @Bean
  ConsentDisclosurePrerequisiteEvaluator consentDisclosurePrerequisiteEvaluator(DataSource dataSource) {
    return new JdbcConsentDisclosurePrerequisiteEvaluator(dataSource);
  }
}
