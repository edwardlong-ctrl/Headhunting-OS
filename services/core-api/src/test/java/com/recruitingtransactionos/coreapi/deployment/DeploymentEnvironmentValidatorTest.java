package com.recruitingtransactionos.coreapi.deployment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DeploymentEnvironmentValidatorTest {

  @Test
  void productionProfileFailsFastWhenRequiredDeploymentInputsAreMissing() {
    DeploymentEnvironmentSettings settings = DeploymentEnvironmentSettings.builder("production")
        .build();

    assertThatThrownBy(() -> DeploymentEnvironmentValidator.validate(settings))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("spring.datasource.url must be a PostgreSQL JDBC URL")
        .hasMessageContaining("spring.datasource.username must be configured")
        .hasMessageContaining("spring.datasource.password must be configured")
        .hasMessageContaining("spring.flyway.enabled must be true for staging and production")
        .hasMessageContaining("rto.auth.jwt.secret must be at least 32 bytes")
        .hasMessageContaining("rto.document-storage.root-dir must be configured")
        .hasMessageContaining("rto.document-storage.virus-scan.mode must be fail_closed for production")
        .hasMessageContaining("rto.ai.deepseek.api-key must be configured")
        .hasMessageContaining("rto.deployment.frontend-origin must be an https URL")
        .hasMessageContaining("rto.deployment.public-base-url must be an https URL")
        .hasMessageContaining("rto.deployment.database.managed must be true for production")
        .hasMessageContaining("rto.deployment.object-storage.provider must be minio or external-object-storage for production");
  }

  @Test
  void stagingProfileAcceptsLocalProductionLikeDeploymentInputs() {
    DeploymentEnvironmentSettings settings = DeploymentEnvironmentSettings.builder("staging")
        .springDatasourceUrl("jdbc:postgresql://localhost:5432/recruiting_os")
        .springDatasourceUsername("recruiting_os")
        .springDatasourcePassword("not-a-real-secret-for-test")
        .flywayEnabled(true)
        .jwtSecret("0123456789abcdef0123456789abcdef")
        .documentStorageRootDir("/var/lib/rto/documents")
        .virusScanMode("noop")
        .deepSeekApiKey("test_deepseek_key_not_real")
        .candidateProfileModel("deepseek-v4-pro")
        .authenticityRiskModel("deepseek-v4-pro")
        .interviewFeedbackModel("deepseek-v4-pro")
        .frontendOrigin("https://staging.example.invalid")
        .publicBaseUrl("https://api.staging.example.invalid")
        .databaseManaged(false)
        .objectStorageProvider("local-filesystem")
        .objectStorageLocalRootDir("/var/lib/rto/documents")
        .build();

    assertThatCode(() -> DeploymentEnvironmentValidator.validate(settings))
        .doesNotThrowAnyException();
  }

  @Test
  void productionProfileAcceptsProviderNeutralManagedDeploymentInputs() {
    DeploymentEnvironmentSettings settings = DeploymentEnvironmentSettings.builder("production")
        .springDatasourceUrl("jdbc:postgresql://managed-postgres.internal:5432/recruiting_os")
        .springDatasourceUsername("rto_app")
        .springDatasourcePassword("managed-database-password-from-secret-store")
        .flywayEnabled(true)
        .jwtSecret("0123456789abcdef0123456789abcdef")
        .documentStorageRootDir("/var/lib/rto/documents")
        .virusScanMode("fail_closed")
        .deepSeekApiKey("test_deepseek_key_not_real")
        .candidateProfileModel("deepseek-v4-pro")
        .authenticityRiskModel("deepseek-v4-pro")
        .interviewFeedbackModel("deepseek-v4-pro")
        .frontendOrigin("https://app.example.invalid")
        .publicBaseUrl("https://api.example.invalid")
        .databaseManaged(true)
        .objectStorageProvider("minio")
        .objectStorageBucket("rto-documents")
        .objectStorageEndpoint("https://object-storage.example.invalid")
        .objectStorageLocalRootDir("/var/lib/rto/documents")
        .objectStorageAccessKey("test-object-storage-access-key")
        .objectStorageSecretKey("test-object-storage-secret-key")
        .build();

    assertThatCode(() -> DeploymentEnvironmentValidator.validate(settings))
        .doesNotThrowAnyException();
  }

  @Test
  void productionProfileFailsFastWhenFlywayIsDisabled() {
    DeploymentEnvironmentSettings settings = DeploymentEnvironmentSettings.builder("production")
        .springDatasourceUrl("jdbc:postgresql://managed-postgres.internal:5432/recruiting_os")
        .springDatasourceUsername("rto_app")
        .springDatasourcePassword("managed-database-password-from-secret-store")
        .jwtSecret("0123456789abcdef0123456789abcdef")
        .documentStorageRootDir("/var/lib/rto/documents")
        .virusScanMode("fail_closed")
        .deepSeekApiKey("test_deepseek_key_not_real")
        .candidateProfileModel("deepseek-v4-pro")
        .authenticityRiskModel("deepseek-v4-pro")
        .interviewFeedbackModel("deepseek-v4-pro")
        .frontendOrigin("https://app.example.invalid")
        .publicBaseUrl("https://api.example.invalid")
        .databaseManaged(true)
        .objectStorageProvider("minio")
        .objectStorageBucket("rto-documents")
        .objectStorageEndpoint("https://object-storage.example.invalid")
        .objectStorageLocalRootDir("/var/lib/rto/documents")
        .objectStorageAccessKey("test-object-storage-access-key")
        .objectStorageSecretKey("test-object-storage-secret-key")
        .build();

    assertThatThrownBy(() -> DeploymentEnvironmentValidator.validate(settings))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("spring.flyway.enabled must be true for staging and production");
  }

  @Test
  void productionProfileFailsFastWhenObjectStorageCredentialsAreMissing() {
    DeploymentEnvironmentSettings settings = DeploymentEnvironmentSettings.builder("production")
        .springDatasourceUrl("jdbc:postgresql://managed-postgres.internal:5432/recruiting_os")
        .springDatasourceUsername("rto_app")
        .springDatasourcePassword("managed-database-password-from-secret-store")
        .flywayEnabled(true)
        .jwtSecret("0123456789abcdef0123456789abcdef")
        .documentStorageRootDir("/var/lib/rto/documents")
        .virusScanMode("fail_closed")
        .deepSeekApiKey("test_deepseek_key_not_real")
        .candidateProfileModel("deepseek-v4-pro")
        .authenticityRiskModel("deepseek-v4-pro")
        .interviewFeedbackModel("deepseek-v4-pro")
        .frontendOrigin("https://app.example.invalid")
        .publicBaseUrl("https://api.example.invalid")
        .databaseManaged(true)
        .objectStorageProvider("minio")
        .objectStorageBucket("rto-documents")
        .objectStorageEndpoint("https://object-storage.example.invalid")
        .objectStorageLocalRootDir("/var/lib/rto/documents")
        .build();

    assertThatThrownBy(() -> DeploymentEnvironmentValidator.validate(settings))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("rto.deployment.object-storage.access-key must be configured")
        .hasMessageContaining("rto.deployment.object-storage.secret-key must be configured");
  }
}
