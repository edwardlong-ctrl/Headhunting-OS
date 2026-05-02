package com.recruitingtransactionos.coreapi.governedintake;

import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.FailClosedVirusScanPort;
import com.recruitingtransactionos.coreapi.documentstorage.LocalFilesystemDocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.NoOpVirusScanPort;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.persistence.JdbcDocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentConversionWorkerPort;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentIntelligenceExtractionService;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.documentintelligence.service.NoOpDocumentConversionWorkerPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcLatestReviewEventLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.port.LatestReviewEventLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedAiIntakeOrchestrator;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeCanonicalWriteBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewDecisionService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewBridgeService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeTaskService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
  IntakeExtractionRunPort intakeExtractionRunPort(DataSource dataSource) {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  @Bean
  LatestReviewEventLookupPort latestReviewEventLookupPort(DataSource dataSource) {
    return new JdbcLatestReviewEventLookupPort(dataSource);
  }

  @Bean
  ClaimLedgerItemReviewLookupPort claimLedgerItemReviewLookupPort(DataSource dataSource) {
    return new JdbcClaimLedgerItemReviewLookupPort(dataSource);
  }

  @Bean
  DocumentIntelligencePersistencePort documentIntelligencePersistencePort(DataSource dataSource) {
    return new JdbcDocumentIntelligencePersistencePort(dataSource);
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
      WorkflowEventService workflowEventService, DataSource dataSource) {
    return new WorkflowTransitionAuditService(workflowEventService, new com.recruitingtransactionos.coreapi.workflowaudit.persistence.JdbcWorkflowEntityStatePort(dataSource));
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
  VirusScanPort virusScanPort(
      @Value("${rto.document-storage.virus-scan.mode:noop}")
      String virusScanMode) {
    return switch (virusScanMode.strip().toLowerCase()) {
      case "noop" -> new NoOpVirusScanPort();
      case "fail_closed" -> new FailClosedVirusScanPort();
      default -> throw new IllegalStateException(
          "rto.document-storage.virus-scan.mode must be noop or fail_closed");
    };
  }

  @Bean
  DocumentConversionWorkerPort documentConversionWorkerPort() {
    return new NoOpDocumentConversionWorkerPort();
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

  @Bean
  DocumentParsingService documentParsingService(
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      DocumentIntelligencePersistencePort documentIntelligencePersistencePort,
      DocumentConversionWorkerPort documentConversionWorkerPort) {
    return new DocumentParsingService(
        governedIntakeService,
        documentStore,
        documentIntelligencePersistencePort,
        documentConversionWorkerPort);
  }

  @Bean
  DocumentIntelligenceExtractionService documentIntelligenceExtractionService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort documentIntelligencePersistencePort) {
    return new DocumentIntelligenceExtractionService(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        documentIntelligencePersistencePort);
  }

  @Bean
  @ConditionalOnBean({
    CandidateProfileParserTaskService.class,
    CompanyIntakeTaskService.class,
    JobIntakeTaskService.class,
    ClaimLedgerService.class,
    ClaimLedgerSourceReferenceLookupPort.class
  })
  IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService(
      IntakeExtractionRunPort intakeExtractionRunPort,
      InformationPacketPersistencePort informationPacketPersistencePort,
      ClaimLedgerService claimLedgerService,
      ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort) {
    return new IntakeClaimLedgerBridgeService(
        intakeExtractionRunPort,
        informationPacketPersistencePort,
        claimLedgerService,
        claimLedgerSourceReferenceLookupPort);
  }

  @Bean
  @ConditionalOnBean({
    ClaimLedgerItemReviewLookupPort.class,
    ReviewEventService.class,
    ReviewEventSourceReferenceLookupPort.class
  })
  IntakeReviewBridgeService intakeReviewBridgeService(
      ClaimLedgerItemReviewLookupPort claimLedgerItemReviewLookupPort,
      ReviewEventService reviewEventService,
      ReviewEventSourceReferenceLookupPort reviewEventSourceReferenceLookupPort) {
    return new IntakeReviewBridgeService(
        claimLedgerItemReviewLookupPort,
        reviewEventService,
        reviewEventSourceReferenceLookupPort);
  }

  @Bean
  @ConditionalOnBean({
    ClaimLedgerItemCanonicalWriteLookupPort.class,
    ReviewEventCanonicalWriteLookupPort.class,
    CanonicalWriteService.class
  })
  IntakeCanonicalWriteBridgeService intakeCanonicalWriteBridgeService(
      ClaimLedgerItemCanonicalWriteLookupPort claimLedgerItemLookupPort,
      ReviewEventCanonicalWriteLookupPort reviewEventLookupPort,
      CanonicalWriteService canonicalWriteService) {
    return new IntakeCanonicalWriteBridgeService(
        claimLedgerItemLookupPort,
        reviewEventLookupPort,
        canonicalWriteService);
  }

  @Bean
  @ConditionalOnBean({
    CandidateProfileParserTaskService.class,
    CompanyIntakeTaskService.class,
    JobIntakeTaskService.class,
    IntakeClaimLedgerBridgeService.class
  })
  GovernedAiIntakeOrchestrator governedAiIntakeOrchestrator(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort documentIntelligencePersistencePort,
      CandidateProfileParserTaskService candidateProfileParserTaskService,
      CompanyIntakeTaskService companyIntakeTaskService,
      JobIntakeTaskService jobIntakeTaskService,
      IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService) {
    return new GovernedAiIntakeOrchestrator(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        documentIntelligencePersistencePort,
        candidateProfileParserTaskService,
        companyIntakeTaskService,
        jobIntakeTaskService,
        intakeClaimLedgerBridgeService);
  }

  @Bean
  @ConditionalOnBean(ClaimLedgerSourceReferenceLookupPort.class)
  IntakeReviewQueryService intakeReviewQueryService(
      IntakeExtractionRunPort intakeExtractionRunPort,
      ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort,
      LatestReviewEventLookupPort latestReviewEventLookupPort) {
    return new IntakeReviewQueryService(
        intakeExtractionRunPort,
        claimLedgerSourceReferenceLookupPort,
        latestReviewEventLookupPort);
  }

  @Bean
  @ConditionalOnBean({
    CandidateProfileService.class,
    IntakeReviewBridgeService.class,
    IntakeCanonicalWriteBridgeService.class,
    IntakeReviewQueryService.class
  })
  IntakeReviewDecisionService intakeReviewDecisionService(
      IntakeReviewBridgeService intakeReviewBridgeService,
      IntakeCanonicalWriteBridgeService intakeCanonicalWriteBridgeService,
      IntakeReviewQueryService intakeReviewQueryService,
      CandidateProfileService candidateProfileService) {
    return new IntakeReviewDecisionService(
        intakeReviewBridgeService,
        intakeCanonicalWriteBridgeService,
        intakeReviewQueryService,
        candidateProfileService);
  }

  private static String requireConfiguredStorageRoot(String rootDirectory) {
    if (rootDirectory == null || rootDirectory.isBlank()) {
      throw new IllegalStateException(
          "rto.document-storage.root-dir must be configured to a persistent location");
    }
    return rootDirectory.strip();
  }
}
