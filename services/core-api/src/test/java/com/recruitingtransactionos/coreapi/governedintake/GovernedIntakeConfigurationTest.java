package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.recruitingtransactionos.coreapi.apiboundary.consultant.ConsultantDocumentController;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

class GovernedIntakeConfigurationTest {

  @Test
  void configurationWiresDocumentUploadChainForConsultantController() throws Exception {
    Path tempDirectory = Files.createTempDirectory("governed-intake-config-test");

    new ApplicationContextRunner()
        .withPropertyValues("rto.document-storage.root-dir=" + tempDirectory)
        .withUserConfiguration(GovernedIntakeConfiguration.class, WiringProbeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .run(context -> {
          assertThat(context).hasSingleBean(DocumentStore.class);
          assertThat(context).hasSingleBean(VirusScanPort.class);
          assertThat(context).hasSingleBean(WorkflowTransitionAuditService.class);
          assertThat(context).hasSingleBean(DocumentUploadService.class);
          assertThat(context).hasSingleBean(ConsultantDocumentController.class);
          assertThat(context.getBean(VirusScanPort.class).scan(new ByteArrayInputStream(new byte[0])))
              .isEqualTo(VirusScanPort.ScanResult.ERROR);
        });
  }

  @Test
  void configurationFailsClosedWhenDocumentStorageRootIsMissing() {
    new ApplicationContextRunner()
        .withUserConfiguration(GovernedIntakeConfiguration.class, WiringProbeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasMessageContaining(
                  "rto.document-storage.root-dir must be configured to a persistent location");
        });
  }

  @Configuration(proxyBeanMethods = false)
  static class WiringProbeConfiguration {

    @Bean
    ConsultantDocumentController consultantDocumentController(
        DocumentUploadService documentUploadService) {
      return new ConsultantDocumentController(documentUploadService);
    }
  }
}
