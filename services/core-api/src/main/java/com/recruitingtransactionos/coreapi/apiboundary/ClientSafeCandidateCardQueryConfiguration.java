package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.privacyredaction.RedactionAuditService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class ClientSafeCandidateCardQueryConfiguration {

  @Bean
  @Primary
  @ConditionalOnBean(DataSource.class)
  ClientSafeCandidateCardQueryPort postgresClientSafeCandidateCardQueryPort(
      DataSource dataSource,
      RedactionAuditService redactionAuditService) {
    return new AuditedPostgresClientSafeCandidateCardQueryPort(
        dataSource,
        new ClientSafeCandidateProjectionService(),
        redactionAuditService);
  }

  @Bean
  @ConditionalOnMissingBean(ClientSafeCandidateCardQueryPort.class)
  ClientSafeCandidateCardQueryPort unavailableClientSafeCandidateCardQueryPort() {
    return new UnavailableClientSafeCandidateCardQueryPort();
  }
}
