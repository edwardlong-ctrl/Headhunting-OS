package com.recruitingtransactionos.coreapi.deployment;

import java.util.Arrays;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile({"staging", "production"})
@EnableConfigurationProperties(DeploymentEnvironmentProperties.class)
public class DeploymentEnvironmentConfiguration {

  @Bean
  SmartInitializingSingleton deploymentEnvironmentStartupValidator(
      Environment environment,
      DeploymentEnvironmentProperties deploymentProperties,
      @Value("${spring.datasource.url:}") String springDatasourceUrl,
      @Value("${spring.datasource.username:}") String springDatasourceUsername,
      @Value("${spring.datasource.password:}") String springDatasourcePassword,
      @Value("${spring.flyway.enabled:false}") boolean flywayEnabled,
      @Value("${rto.auth.jwt.secret:}") String jwtSecret,
      @Value("${rto.document-storage.root-dir:}") String documentStorageRootDir,
      @Value("${rto.document-storage.virus-scan.mode:noop}") String virusScanMode,
      @Value("${rto.ai.deepseek.api-key:}") String deepSeekApiKey,
      @Value("${rto.ai.routes.candidate-profile-parser.model:}") String candidateProfileModel,
      @Value("${rto.ai.routes.authenticity-risk-assessor.model:}") String authenticityRiskModel,
      @Value("${rto.ai.routes.interview-feedback-structurer.model:}") String interviewFeedbackModel) {
    return () -> DeploymentEnvironmentValidator.validate(DeploymentEnvironmentSettings.builder(activeProfile(environment))
        .springDatasourceUrl(springDatasourceUrl)
        .springDatasourceUsername(springDatasourceUsername)
        .springDatasourcePassword(springDatasourcePassword)
        .flywayEnabled(flywayEnabled)
        .jwtSecret(jwtSecret)
        .documentStorageRootDir(documentStorageRootDir)
        .virusScanMode(virusScanMode)
        .deepSeekApiKey(deepSeekApiKey)
        .candidateProfileModel(candidateProfileModel)
        .authenticityRiskModel(authenticityRiskModel)
        .interviewFeedbackModel(interviewFeedbackModel)
        .frontendOrigin(deploymentProperties.getFrontendOrigin())
        .publicBaseUrl(deploymentProperties.getPublicBaseUrl())
        .databaseManaged(deploymentProperties.getDatabase().isManaged())
        .objectStorageProvider(deploymentProperties.getObjectStorage().getProvider())
        .objectStorageBucket(deploymentProperties.getObjectStorage().getBucket())
        .objectStorageEndpoint(deploymentProperties.getObjectStorage().getEndpoint())
        .objectStorageLocalRootDir(deploymentProperties.getObjectStorage().getLocalRootDir())
        .build());
  }

  static String activeProfile(Environment environment) {
    String[] activeProfiles = environment.getActiveProfiles();
    if (Arrays.stream(activeProfiles).anyMatch("production"::equals)) {
      return "production";
    }
    if (Arrays.stream(activeProfiles).anyMatch("staging"::equals)) {
      return "staging";
    }
    return "";
  }
}
