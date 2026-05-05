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
  AITaskDefinitionRegistry aiTaskDefinitionRegistry() {
    return new AITaskDefinitionRegistry(List.of(
        new AITaskDefinition(
            "candidate-profile-parser",
            "candidate-profile-parser.v1",
            "prompt.candidate-profile-parser.v1",
            "/ai/prompts/candidate-profile-parser-v1.txt",
            "/ai/schemas/candidate-profile-parser-input.schema.json",
            "/ai/schemas/candidate-profile-parser-output.schema.json",
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskHumanReviewStatus.REQUIRED),
        new AITaskDefinition(
            "company-intake",
            "company-intake.v1",
            "prompt.company-intake.v1",
            "/ai/prompts/company-intake-v1.txt",
            "/ai/schemas/company-intake-input.schema.json",
            "/ai/schemas/company-intake-output.schema.json",
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskHumanReviewStatus.REQUIRED),
        new AITaskDefinition(
            "job-intake",
            "job-intake.v1",
            "prompt.job-intake.v1",
            "/ai/prompts/job-intake-v1.txt",
            "/ai/schemas/job-intake-input.schema.json",
            "/ai/schemas/job-intake-output.schema.json",
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskHumanReviewStatus.REQUIRED),
        new AITaskDefinition(
            "authenticity-risk-assessor",
            "authenticity-risk-assessor.v1",
            "prompt.authenticity-risk-assessor.v1",
            "/ai/prompts/authenticity-risk-assessor-v1.txt",
            "/ai/schemas/authenticity-risk-assessor-input.schema.json",
            "/ai/schemas/authenticity-risk-assessor-output.schema.json",
            AITaskWriteBackTarget.NO_WRITE_BACK,
            AITaskHumanReviewStatus.NOT_REQUIRED),
        new AITaskDefinition(
            "interview-feedback-structurer",
            "interview-feedback-structurer.v1",
            "prompt.interview-feedback-structurer.v1",
            "/ai/prompts/interview-feedback-structurer-v1.txt",
            "/ai/schemas/interview-feedback-structurer-input.schema.json",
            "/ai/schemas/interview-feedback-structurer-output.schema.json",
            AITaskWriteBackTarget.CLAIM_LEDGER_PROPOSAL,
            AITaskHumanReviewStatus.REQUIRED)));
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
  AITaskModelRouter aiTaskModelRouter(AITaskRunnerProperties properties) {
    return new AITaskModelRouter(properties);
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
