package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.deepseek.DeepSeekAITaskProvider;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestFactory;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeTaskService;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskHumanReviewStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskWriteBackTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.AITaskRunService;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AITaskRunnerProperties.class)
public class AITaskRunnerConfiguration {

  @Bean
  public AITaskDefinitionRegistry aiTaskDefinitionRegistry() {
    return new AITaskDefinitionRegistry(List.of(
        productionTask("0.1", "source-classifier", "Source Classifier", "base", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("0.2", "document-version-resolver", "Document Version Resolver", "base", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("0.3", "entity-resolver", "Entity Resolver", "base", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("0.4", "conflict-detector", "Conflict Detector", "base", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("0.5", "canonical-record-builder", "Canonical Record Builder", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("1", "job-intake", "Job Intake Parser", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("2", "company-intake", "Company Intelligence Structurer", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("3", "candidate-profile-parser", "Candidate Profile Parser", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("4", "evidence-extractor", "Evidence Extractor", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("5", "trust-tagger", "Trust Tagger", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("6", "consultant-note-structurer", "Consultant Note Structurer", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("7", "candidate-deduplication-assistant", "Candidate Deduplication Assistant", "base", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("8", "match-report-generator", "Match Report Generator", "base", AITaskWriteBackTarget.NO_WRITE_BACK, AITaskHumanReviewStatus.REQUIRED),
        productionTask("9", "outreach-question-generator", "Outreach Question Generator", "base", AITaskWriteBackTarget.NO_WRITE_BACK, AITaskHumanReviewStatus.REQUIRED),
        productionTask("10", "shortlist-generator", "Shortlist Generator", "base", AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION, AITaskHumanReviewStatus.REQUIRED),
        productionTask("11", "interview-feedback-structurer", "Interview Feedback Structurer", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("12", "workflow-action-recommender", "Workflow Action Recommender", "base", AITaskWriteBackTarget.WORKFLOW_ACTION, AITaskHumanReviewStatus.REQUIRED),
        productionTask("13", "outcome-labeler", "Outcome Labeler", "base", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("14", "claim-ledger-builder", "Claim Ledger Builder", "governance", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("15", "canonical-write-back-gate", "Canonical Write-back Gate", "governance", AITaskWriteBackTarget.HUMAN_REVIEW_REQUIRED, AITaskHumanReviewStatus.REQUIRED),
        productionTask("16", "review-quality-auditor", "Review Quality Auditor", "governance", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("17", "reidentification-risk-scorer", "Re-identification Risk Scorer", "governance", AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION, AITaskHumanReviewStatus.REQUIRED),
        productionTask("18", "client-safe-summary-generator", "Client-safe Summary Generator", "governance", AITaskWriteBackTarget.CLIENT_SAFE_PROJECTION, AITaskHumanReviewStatus.REQUIRED),
        productionTask("19", "ontology-drift-detector", "Ontology Drift Detector", "governance", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("20", "industry-pack-calibrator", "Industry Pack Calibrator", "governance", AITaskWriteBackTarget.REVIEW_QUEUE, AITaskHumanReviewStatus.REQUIRED),
        productionTask("21", "authenticity-risk-assessor", "Authenticity Risk Assessor", "governance", AITaskWriteBackTarget.NO_WRITE_BACK, AITaskHumanReviewStatus.NOT_REQUIRED),
        productionTask("22", "evidence-provenance-scorer", "Evidence Provenance Scorer", "governance", AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL, AITaskHumanReviewStatus.REQUIRED),
        productionTask("23", "negative-case-generator", "Negative Case Generator", "governance", AITaskWriteBackTarget.NO_WRITE_BACK, AITaskHumanReviewStatus.NOT_REQUIRED)));
  }

  private static AITaskDefinition productionTask(
      String registryTaskId,
      String taskKey,
      String displayName,
      String registryGroup,
      AITaskWriteBackTarget writeBackTarget,
      AITaskHumanReviewStatus humanReviewStatus) {
    return new AITaskDefinition(
        registryTaskId,
        taskKey,
        displayName,
        registryGroup,
        taskKey + ".v1",
        "prompt." + taskKey + ".v1",
        "/ai/prompts/" + taskKey + "-v1.txt",
        "/ai/schemas/" + taskKey + "-input.schema.json",
        "/ai/schemas/" + taskKey + "-output.schema.json",
        "/ai/evals/" + taskKey + "-eval-cases.json",
        writeBackTarget,
        humanReviewStatus);
  }

  @Bean
  AITaskPromptRegistry aiTaskPromptRegistry() {
    return new AITaskPromptRegistry();
  }

  @Bean
  AITaskSchemaValidator aiTaskSchemaValidator(ObjectMapper objectMapper) {
    return new AITaskSchemaValidator(objectMapper);
  }

  @Bean
  AITaskModelRouter aiTaskModelRouter(
      AITaskRunnerProperties properties,
      com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService governanceConfigService,
      List<AITaskProvider> providers) {
    return new AITaskModelRouter(
        properties,
        governanceConfigService,
        providers.stream().map(AITaskProvider::providerKey).collect(Collectors.toSet()));
  }

  @Bean
  AITaskDefinitionCatalog aiTaskDefinitionCatalog(
      DataSource dataSource,
      ObjectMapper objectMapper) {
    return new JdbcAITaskDefinitionCatalog(dataSource, objectMapper);
  }

  @Bean
  RestClient deepSeekRestClient(AITaskRunnerProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getDeepseek().getConnectTimeoutSeconds()));
    requestFactory.setReadTimeout(Duration.ofSeconds(properties.getDeepseek().getReadTimeoutSeconds()));
    return RestClient.builder()
        .baseUrl(properties.getDeepseek().getBaseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  @Bean
  DeepSeekAITaskProvider deepSeekAITaskProvider(
      RestClient deepSeekRestClient,
      AITaskRunnerProperties properties,
      ObjectMapper objectMapper) {
    return new DeepSeekAITaskProvider(deepSeekRestClient, properties, objectMapper);
  }

  @Bean
  DeterministicPilotAITaskProvider deterministicPilotAITaskProvider(ObjectMapper objectMapper) {
    return new DeterministicPilotAITaskProvider(objectMapper);
  }

  @Bean
  AITaskRunnerService aiTaskRunnerService(
      AITaskRunService aiTaskRunService,
      AITaskDefinitionCatalog definitionCatalog,
      AITaskDefinitionRegistry definitionRegistry,
      AITaskPromptRegistry promptRegistry,
      AITaskSchemaValidator schemaValidator,
      AITaskModelRouter modelRouter,
      List<AITaskProvider> providers,
      ObjectMapper objectMapper) {
    return new AITaskRunnerService(
        aiTaskRunService,
        definitionCatalog,
        definitionRegistry,
        promptRegistry,
        schemaValidator,
        modelRouter,
        providers,
        objectMapper);
  }

  @Bean
  AITaskReplayService aiTaskReplayService(
      AITaskRunService aiTaskRunService,
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new AITaskReplayService(aiTaskRunService, aiTaskRunnerService, objectMapper);
  }

  @Bean
  CandidateProfileParserTaskService candidateProfileParserTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new CandidateProfileParserTaskService(aiTaskRunnerService, objectMapper);
  }

  @Bean
  CompanyIntakeTaskService companyIntakeTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new CompanyIntakeTaskService(aiTaskRunnerService, objectMapper);
  }

  @Bean
  JobIntakeTaskService jobIntakeTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new JobIntakeTaskService(aiTaskRunnerService, objectMapper);
  }

  @Bean
  AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new AuthenticityRiskAssessorTaskService(aiTaskRunnerService, objectMapper);
  }

  @Bean
  InterviewFeedbackStructurerTaskService interviewFeedbackStructurerTaskService(
      AITaskRunnerService aiTaskRunnerService,
      ObjectMapper objectMapper) {
    return new InterviewFeedbackStructurerTaskService(aiTaskRunnerService, objectMapper);
  }

  @Bean
  AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory() {
    return new AuthenticityAwareMatchRequestFactory();
  }
}
