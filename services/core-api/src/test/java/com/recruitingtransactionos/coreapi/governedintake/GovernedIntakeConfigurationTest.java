package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.ConsultantDocumentController;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerConfiguration;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeTaskService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedAiIntakeOrchestrator;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeCanonicalWriteBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewDecisionService;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.truthlayer.TruthLayerConfiguration;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
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
        .withBean(CandidateProfileParserTaskService.class, () -> mock(CandidateProfileParserTaskService.class))
        .withBean(CompanyIntakeTaskService.class, () -> mock(CompanyIntakeTaskService.class))
        .withBean(JobIntakeTaskService.class, () -> mock(JobIntakeTaskService.class))
        .withBean(ClaimLedgerService.class, () -> mock(ClaimLedgerService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> {
          assertThat(context).hasSingleBean(DocumentStore.class);
          assertThat(context).hasSingleBean(VirusScanPort.class);
          assertThat(context).hasSingleBean(ReviewEventService.class);
          assertThat(context).hasSingleBean(WorkflowEventService.class);
          assertThat(context).hasSingleBean(WorkflowEntityStatePort.class);
          assertThat(context).hasSingleBean(WorkflowTransitionAuditService.class);
          assertThat(context).hasSingleBean(CanonicalWriteService.class);
          assertThat(context).hasSingleBean(DocumentUploadService.class);
          assertThat(context).hasSingleBean(ConsultantDocumentController.class);
          assertThat(context.getBean(VirusScanPort.class).scan(new ByteArrayInputStream(new byte[0])))
              .isEqualTo(VirusScanPort.ScanResult.CLEAN);
        });
  }

  @Test
  void configurationSupportsExplicitFailClosedVirusScanMode() throws Exception {
    Path tempDirectory = Files.createTempDirectory("governed-intake-config-test-fail-closed");

    new ApplicationContextRunner()
        .withPropertyValues(
            "rto.document-storage.root-dir=" + tempDirectory,
            "rto.document-storage.virus-scan.mode=fail_closed")
        .withUserConfiguration(GovernedIntakeConfiguration.class, WiringProbeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(CandidateProfileParserTaskService.class, () -> mock(CandidateProfileParserTaskService.class))
        .withBean(CompanyIntakeTaskService.class, () -> mock(CompanyIntakeTaskService.class))
        .withBean(JobIntakeTaskService.class, () -> mock(JobIntakeTaskService.class))
        .withBean(ClaimLedgerService.class, () -> mock(ClaimLedgerService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> assertThat(context.getBean(VirusScanPort.class)
            .scan(new ByteArrayInputStream(new byte[0]))).isEqualTo(VirusScanPort.ScanResult.ERROR));
  }

  @Test
  void configurationWiresMinioDocumentStoreWhenObjectStorageProviderIsMinio() throws Exception {
    Path tempDirectory = Files.createTempDirectory("governed-intake-config-test-minio");

    new ApplicationContextRunner()
        .withPropertyValues(
            "rto.document-storage.root-dir=" + tempDirectory,
            "rto.deployment.object-storage.provider=minio",
            "rto.deployment.object-storage.bucket=rto-documents",
            "rto.deployment.object-storage.endpoint=http://minio:9000",
            "rto.deployment.object-storage.access-key=minio-access-key",
            "rto.deployment.object-storage.secret-key=minio-secret-key")
        .withUserConfiguration(GovernedIntakeConfiguration.class, WiringProbeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(CandidateProfileParserTaskService.class, () -> mock(CandidateProfileParserTaskService.class))
        .withBean(CompanyIntakeTaskService.class, () -> mock(CompanyIntakeTaskService.class))
        .withBean(JobIntakeTaskService.class, () -> mock(JobIntakeTaskService.class))
        .withBean(ClaimLedgerService.class, () -> mock(ClaimLedgerService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> {
          assertThat(context).hasSingleBean(DocumentStore.class);
          assertThat(context.getBean(DocumentStore.class).getClass().getSimpleName())
              .isEqualTo("MinioDocumentStore");
        });
  }

  @Test
  void configurationFailsClosedWhenDocumentStorageRootIsMissing() {
    new ApplicationContextRunner()
        .withUserConfiguration(GovernedIntakeConfiguration.class, WiringProbeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(CandidateProfileParserTaskService.class, () -> mock(CandidateProfileParserTaskService.class))
        .withBean(CompanyIntakeTaskService.class, () -> mock(CompanyIntakeTaskService.class))
        .withBean(JobIntakeTaskService.class, () -> mock(JobIntakeTaskService.class))
        .withBean(ClaimLedgerService.class, () -> mock(ClaimLedgerService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasMessageContaining(
                  "rto.document-storage.root-dir must be configured to a persistent location");
        });
  }

  @Test
  void configurationWiresGovernedAiIntakeOrchestratorWhenTaskAndClaimServicesArePresent()
      throws Exception {
    Path tempDirectory = Files.createTempDirectory("governed-intake-config-test-ai");

    new ApplicationContextRunner()
        .withPropertyValues("rto.document-storage.root-dir=" + tempDirectory)
        .withUserConfiguration(
            GovernedIntakeConfiguration.class,
            AITaskRunnerConfiguration.class,
            TruthLayerConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(GovernanceConfigService.class, () -> mock(GovernanceConfigService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> assertThat(context).hasSingleBean(GovernedAiIntakeOrchestrator.class));
  }

  @Test
  void configurationWiresReviewDecisionServiceWithRecruitingDomainServices() throws Exception {
    Path tempDirectory = Files.createTempDirectory("governed-intake-review-decision-config-test");

    new ApplicationContextRunner()
        .withPropertyValues("rto.document-storage.root-dir=" + tempDirectory)
        .withUserConfiguration(GovernedIntakeConfiguration.class)
        .withBean(DataSource.class, () -> mock(DataSource.class))
        .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
        .withBean(CandidateProfileParserTaskService.class, () -> mock(CandidateProfileParserTaskService.class))
        .withBean(CompanyIntakeTaskService.class, () -> mock(CompanyIntakeTaskService.class))
        .withBean(JobIntakeTaskService.class, () -> mock(JobIntakeTaskService.class))
        .withBean(ClaimLedgerService.class, () -> mock(ClaimLedgerService.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(NotificationService.class, () -> mock(NotificationService.class))
        .withBean(CandidateProfileService.class, () -> mock(CandidateProfileService.class))
        .withBean(IntakeReviewBridgeService.class, () -> mock(IntakeReviewBridgeService.class))
        .withBean(IntakeCanonicalWriteBridgeService.class, () -> mock(IntakeCanonicalWriteBridgeService.class))
        .run(context -> assertThat(context).hasSingleBean(IntakeReviewDecisionService.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class WiringProbeConfiguration {

    @Bean
    ConsultantDocumentController consultantDocumentController(
        DocumentUploadService documentUploadService,
        com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService documentParsingService) {
      return new ConsultantDocumentController(documentUploadService, documentParsingService);
    }
  }
}
