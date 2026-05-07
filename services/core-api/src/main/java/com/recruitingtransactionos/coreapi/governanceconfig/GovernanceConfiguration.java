package com.recruitingtransactionos.coreapi.governanceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GovernanceConfiguration {

  @Bean
  @ConditionalOnMissingBean(GovernanceConfigPort.class)
  GovernanceConfigPort governanceConfigPort(DataSource dataSource) {
    return new JdbcGovernanceConfigPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(GovernanceConfigService.class)
  GovernanceConfigService governanceConfigService(
      GovernanceConfigPort governanceConfigPort,
      ObjectMapper objectMapper) {
    return new GovernanceConfigService(governanceConfigPort, objectMapper);
  }
}
