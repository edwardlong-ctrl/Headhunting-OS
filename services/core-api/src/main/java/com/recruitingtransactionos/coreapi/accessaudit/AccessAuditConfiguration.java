package com.recruitingtransactionos.coreapi.accessaudit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAuditRecorder;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AccessAuditConfiguration {

  @Bean
  AccessAuditRecorder accessAuditRecorder(DataSource dataSource, ObjectMapper objectMapper) {
    return new JdbcAccessAuditRecorder(dataSource, objectMapper);
  }

  @Bean
  AccessAuditSearchReader accessAuditSearchReader(DataSource dataSource) {
    return new JdbcAccessAuditSearchReader(dataSource);
  }

  @Bean
  PermissionEnforcer permissionEnforcer(AccessAuditRecorder accessAuditRecorder) {
    return new PermissionEnforcer(new PermissionEvaluator(), accessAuditRecorder);
  }
}
