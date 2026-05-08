package com.recruitingtransactionos.coreapi.deployment;

public record DeploymentEnvironmentSettings(
    String profile,
    String springDatasourceUrl,
    String springDatasourceUsername,
    String springDatasourcePassword,
    boolean flywayEnabled,
    String jwtSecret,
    String documentStorageRootDir,
    String virusScanMode,
    String deepSeekApiKey,
    String candidateProfileModel,
    String authenticityRiskModel,
    String interviewFeedbackModel,
    String frontendOrigin,
    String publicBaseUrl,
    boolean databaseManaged,
    String objectStorageProvider,
    String objectStorageBucket,
    String objectStorageEndpoint,
    String objectStorageLocalRootDir) {

  public static Builder builder(String profile) {
    return new Builder(profile);
  }

  public static final class Builder {
    private final String profile;
    private String springDatasourceUrl;
    private String springDatasourceUsername;
    private String springDatasourcePassword;
    private boolean flywayEnabled;
    private String jwtSecret;
    private String documentStorageRootDir;
    private String virusScanMode;
    private String deepSeekApiKey;
    private String candidateProfileModel;
    private String authenticityRiskModel;
    private String interviewFeedbackModel;
    private String frontendOrigin;
    private String publicBaseUrl;
    private boolean databaseManaged;
    private String objectStorageProvider;
    private String objectStorageBucket;
    private String objectStorageEndpoint;
    private String objectStorageLocalRootDir;

    private Builder(String profile) {
      this.profile = profile;
    }

    public Builder springDatasourceUrl(String value) {
      this.springDatasourceUrl = value;
      return this;
    }

    public Builder springDatasourceUsername(String value) {
      this.springDatasourceUsername = value;
      return this;
    }

    public Builder springDatasourcePassword(String value) {
      this.springDatasourcePassword = value;
      return this;
    }

    public Builder flywayEnabled(boolean value) {
      this.flywayEnabled = value;
      return this;
    }

    public Builder jwtSecret(String value) {
      this.jwtSecret = value;
      return this;
    }

    public Builder documentStorageRootDir(String value) {
      this.documentStorageRootDir = value;
      return this;
    }

    public Builder virusScanMode(String value) {
      this.virusScanMode = value;
      return this;
    }

    public Builder deepSeekApiKey(String value) {
      this.deepSeekApiKey = value;
      return this;
    }

    public Builder candidateProfileModel(String value) {
      this.candidateProfileModel = value;
      return this;
    }

    public Builder authenticityRiskModel(String value) {
      this.authenticityRiskModel = value;
      return this;
    }

    public Builder interviewFeedbackModel(String value) {
      this.interviewFeedbackModel = value;
      return this;
    }

    public Builder frontendOrigin(String value) {
      this.frontendOrigin = value;
      return this;
    }

    public Builder publicBaseUrl(String value) {
      this.publicBaseUrl = value;
      return this;
    }

    public Builder databaseManaged(boolean value) {
      this.databaseManaged = value;
      return this;
    }

    public Builder objectStorageProvider(String value) {
      this.objectStorageProvider = value;
      return this;
    }

    public Builder objectStorageBucket(String value) {
      this.objectStorageBucket = value;
      return this;
    }

    public Builder objectStorageEndpoint(String value) {
      this.objectStorageEndpoint = value;
      return this;
    }

    public Builder objectStorageLocalRootDir(String value) {
      this.objectStorageLocalRootDir = value;
      return this;
    }

    public DeploymentEnvironmentSettings build() {
      return new DeploymentEnvironmentSettings(
          profile,
          springDatasourceUrl,
          springDatasourceUsername,
          springDatasourcePassword,
          flywayEnabled,
          jwtSecret,
          documentStorageRootDir,
          virusScanMode,
          deepSeekApiKey,
          candidateProfileModel,
          authenticityRiskModel,
          interviewFeedbackModel,
          frontendOrigin,
          publicBaseUrl,
          databaseManaged,
          objectStorageProvider,
          objectStorageBucket,
          objectStorageEndpoint,
          objectStorageLocalRootDir);
    }
  }
}
