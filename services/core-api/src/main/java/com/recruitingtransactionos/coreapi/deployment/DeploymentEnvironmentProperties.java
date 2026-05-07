package com.recruitingtransactionos.coreapi.deployment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rto.deployment")
public class DeploymentEnvironmentProperties {

  private String frontendOrigin;
  private String publicBaseUrl;
  private final Database database = new Database();
  private final ObjectStorage objectStorage = new ObjectStorage();

  public String getFrontendOrigin() {
    return frontendOrigin;
  }

  public void setFrontendOrigin(String frontendOrigin) {
    this.frontendOrigin = frontendOrigin;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public Database getDatabase() {
    return database;
  }

  public ObjectStorage getObjectStorage() {
    return objectStorage;
  }

  public static class Database {
    private boolean managed;

    public boolean isManaged() {
      return managed;
    }

    public void setManaged(boolean managed) {
      this.managed = managed;
    }
  }

  public static class ObjectStorage {
    private String provider;
    private String bucket;
    private String endpoint;
    private String localRootDir;

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getBucket() {
      return bucket;
    }

    public void setBucket(String bucket) {
      this.bucket = bucket;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getLocalRootDir() {
      return localRootDir;
    }

    public void setLocalRootDir(String localRootDir) {
      this.localRootDir = localRootDir;
    }
  }
}
