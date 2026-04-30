package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.FailClosedVirusScanPort;
import com.recruitingtransactionos.coreapi.documentstorage.LocalFilesystemDocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class GovernedIntakeConfiguration {

  @Bean
  SourceItemPersistencePort sourceItemPersistencePort(DataSource dataSource) {
    return new JdbcSourceItemPersistencePort(dataSource);
  }

  @Bean
  InformationPacketPersistencePort informationPacketPersistencePort(DataSource dataSource) {
    return new JdbcInformationPacketPersistencePort(dataSource);
  }

  @Bean
  WorkflowEventPort workflowEventPort(DataSource dataSource) {
    return new JdbcWorkflowEventPort(dataSource);
  }

  @Bean
  WorkflowEventService workflowEventService(WorkflowEventPort workflowEventPort) {
    return new WorkflowEventService(workflowEventPort);
  }

  @Bean
  WorkflowTransitionAuditService workflowTransitionAuditService(
      WorkflowEventService workflowEventService) {
    return new WorkflowTransitionAuditService(workflowEventService);
  }

  @Bean
  CanonicalWriteTransactionBoundary canonicalWriteTransactionBoundary(
      PlatformTransactionManager transactionManager) {
    return new SpringCanonicalWriteTransactionBoundary(transactionManager);
  }

  @Bean
  DocumentStore documentStore(
      @Value("${rto.document-storage.root-dir:}")
      String rootDirectory) {
    return new LocalFilesystemDocumentStore(Path.of(requireConfiguredStorageRoot(rootDirectory)));
  }

  @Bean
  VirusScanPort virusScanPort() {
    return new FailClosedVirusScanPort();
  }

  @Bean
  GovernedIntakeService governedIntakeService(
      SourceItemPersistencePort sourceItemPersistencePort,
      InformationPacketPersistencePort informationPacketPersistencePort) {
    return new GovernedIntakeService(sourceItemPersistencePort, informationPacketPersistencePort);
  }

  @Bean
  DocumentUploadService documentUploadService(
      SourceItemPersistencePort sourceItemPersistencePort,
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      VirusScanPort virusScanPort,
      CanonicalWriteTransactionBoundary transactionBoundary,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    return new DocumentUploadService(
        sourceItemPersistencePort,
        governedIntakeService,
        documentStore,
        virusScanPort,
        transactionBoundary,
        workflowTransitionAuditService);
  }

  private static String requireConfiguredStorageRoot(String rootDirectory) {
    if (rootDirectory == null || rootDirectory.isBlank()) {
      throw new IllegalStateException(
          "rto.document-storage.root-dir must be configured to a persistent location");
    }
    return rootDirectory.strip();
  }
}
