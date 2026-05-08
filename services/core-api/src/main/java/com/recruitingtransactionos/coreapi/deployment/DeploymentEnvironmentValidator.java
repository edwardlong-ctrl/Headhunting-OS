package com.recruitingtransactionos.coreapi.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DeploymentEnvironmentValidator {

  private DeploymentEnvironmentValidator() {
  }

  public static void validate(DeploymentEnvironmentSettings settings) {
    List<String> errors = new ArrayList<>();
    String profile = normalize(settings.profile());
    boolean production = "production".equals(profile);

    if (!"staging".equals(profile) && !production) {
      errors.add("spring.profiles.active must include staging or production");
    }
    if (!hasText(settings.springDatasourceUrl())
        || !settings.springDatasourceUrl().startsWith("jdbc:postgresql://")) {
      errors.add("spring.datasource.url must be a PostgreSQL JDBC URL");
    }
    requireText(settings.springDatasourceUsername(), "spring.datasource.username must be configured", errors);
    requireText(settings.springDatasourcePassword(), "spring.datasource.password must be configured", errors);
    if (!settings.flywayEnabled()) {
      errors.add("spring.flyway.enabled must be true for staging and production");
    }
    if (!hasText(settings.jwtSecret()) || settings.jwtSecret().getBytes().length < 32) {
      errors.add("rto.auth.jwt.secret must be at least 32 bytes");
    }
    requireText(
        settings.documentStorageRootDir(),
        "rto.document-storage.root-dir must be configured",
        errors);
    if (production && !"fail_closed".equals(normalize(settings.virusScanMode()))) {
      errors.add("rto.document-storage.virus-scan.mode must be fail_closed for production");
    }
    requireText(settings.deepSeekApiKey(), "rto.ai.deepseek.api-key must be configured", errors);
    requireText(
        settings.candidateProfileModel(),
        "rto.ai.routes.candidate-profile-parser.model must be configured",
        errors);
    requireText(
        settings.authenticityRiskModel(),
        "rto.ai.routes.authenticity-risk-assessor.model must be configured",
        errors);
    requireText(
        settings.interviewFeedbackModel(),
        "rto.ai.routes.interview-feedback-structurer.model must be configured",
        errors);
    requireHttpsUrl(settings.frontendOrigin(), "rto.deployment.frontend-origin", errors);
    requireHttpsUrl(settings.publicBaseUrl(), "rto.deployment.public-base-url", errors);

    if (production && !settings.databaseManaged()) {
      errors.add("rto.deployment.database.managed must be true for production");
    }
    validateObjectStorage(settings, production, errors);

    if (!errors.isEmpty()) {
      throw new IllegalStateException("Deployment environment is not valid: " + String.join("; ", errors));
    }
  }

  private static void validateObjectStorage(
      DeploymentEnvironmentSettings settings,
      boolean production,
      List<String> errors) {
    String provider = normalize(settings.objectStorageProvider());
    if (production && !List.of("minio", "external-object-storage").contains(provider)) {
      errors.add(
          "rto.deployment.object-storage.provider must be minio or external-object-storage for production");
      return;
    }
    if (!production && !List.of("local-filesystem", "minio", "external-object-storage").contains(provider)) {
      errors.add(
          "rto.deployment.object-storage.provider must be local-filesystem, minio, or external-object-storage");
      return;
    }
    if ("local-filesystem".equals(provider)) {
      requireText(
          settings.objectStorageLocalRootDir(),
          "rto.deployment.object-storage.local-root-dir must be configured for local-filesystem",
          errors);
    }
    if ("minio".equals(provider) || "external-object-storage".equals(provider)) {
      requireText(settings.objectStorageBucket(), "rto.deployment.object-storage.bucket must be configured", errors);
      validateObjectStorageEndpoint(settings.objectStorageEndpoint(), provider, production, errors);
      requireText(
          settings.objectStorageAccessKey(),
          "rto.deployment.object-storage.access-key must be configured",
          errors);
      requireText(
          settings.objectStorageSecretKey(),
          "rto.deployment.object-storage.secret-key must be configured",
          errors);
    }
  }

  private static void validateObjectStorageEndpoint(
      String endpoint,
      String provider,
      boolean production,
      List<String> errors) {
    if (production || "external-object-storage".equals(provider)) {
      if (!hasText(endpoint) || !endpoint.startsWith("https://")) {
        errors.add(
            "rto.deployment.object-storage.endpoint must be an https URL for production or external object storage");
      }
      return;
    }
    if (!hasText(endpoint) || !(endpoint.startsWith("http://") || endpoint.startsWith("https://"))) {
      errors.add("rto.deployment.object-storage.endpoint must be an http or https URL for staging MinIO");
    }
  }

  private static void requireText(String value, String message, List<String> errors) {
    if (!hasText(value)) {
      errors.add(message);
    }
  }

  private static void requireHttpsUrl(String value, String propertyName, List<String> errors) {
    if (!hasText(value) || !value.startsWith("https://")) {
      errors.add(propertyName + " must be an https URL");
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String normalize(String value) {
    return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
  }
}
