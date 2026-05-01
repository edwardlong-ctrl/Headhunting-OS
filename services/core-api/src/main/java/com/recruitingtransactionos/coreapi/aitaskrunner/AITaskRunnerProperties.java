package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rto.ai")
public class AITaskRunnerProperties {

  private final DeepSeek deepseek = new DeepSeek();
  private final Map<String, Route> routes = new LinkedHashMap<>();

  public DeepSeek getDeepseek() {
    return deepseek;
  }

  public Map<String, Route> getRoutes() {
    return routes;
  }

  public static class DeepSeek {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public int getConnectTimeoutSeconds() {
      return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
      this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
      return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
      this.readTimeoutSeconds = readTimeoutSeconds;
    }
  }

  public static class Route {
    private String provider;
    private String model;

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }
}
