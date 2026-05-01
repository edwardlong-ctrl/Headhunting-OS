package com.recruitingtransactionos.coreapi.apiboundary;

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
      DataSource dataSource) {
    return new PostgresClientSafeCandidateCardQueryPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ClientSafeCandidateCardQueryPort.class)
  ClientSafeCandidateCardQueryPort unavailableClientSafeCandidateCardQueryPort() {
    return new UnavailableClientSafeCandidateCardQueryPort();
  }
}
