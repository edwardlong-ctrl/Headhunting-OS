package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.recruitingtransactionos.coreapi.privacyredaction.RedactionAuditService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClientSafeCandidateCardQueryConfigurationTest {

  @Test
  void configurationWiresAuditedQueryPortWhenDataSourceAndAuditServiceExist() {
    new ApplicationContextRunner()
        .withUserConfiguration(ClientSafeCandidateCardQueryConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(RedactionAuditService.class, () -> mock(RedactionAuditService.class))
        .run(context -> {
          assertThat(context).hasSingleBean(ClientSafeCandidateCardQueryPort.class);
          assertThat(context.getBean(ClientSafeCandidateCardQueryPort.class))
              .isInstanceOf(AuditedPostgresClientSafeCandidateCardQueryPort.class);
          assertThat(context).doesNotHaveBean(UnavailableClientSafeCandidateCardQueryPort.class);
        });
  }
}
