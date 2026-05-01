package com.recruitingtransactionos.coreapi.aitaskrunner.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProvider;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProviderException;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProviderRequest;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskProviderResponse;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class DeepSeekAITaskProvider implements AITaskProvider {

  private final RestClient restClient;
  private final AITaskRunnerProperties properties;
  private final ObjectMapper objectMapper;

  public DeepSeekAITaskProvider(
      RestClient restClient,
      AITaskRunnerProperties properties,
      ObjectMapper objectMapper) {
    this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public String providerKey() {
    return "deepseek";
  }

  @Override
  public AITaskProviderResponse execute(AITaskProviderRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    String apiKey = properties.getDeepseek().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new AITaskProviderException("deepseek_api_key_missing", "deepseek_api_key_missing");
    }

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("model", request.modelName());
    payload.putObject("response_format").put("type", "json_object");
    ArrayNode messages = payload.putArray("messages");
    messages.addObject().put("role", "system").put("content", request.systemPrompt());
    messages.addObject().put(
        "role",
        "user").put(
            "content",
            "Return only valid JSON for task " + request.taskKey() + " using this input: "
                + request.inputPayload().toString());

    Instant startedAt = Instant.now();
    try {
      JsonNode response = restClient.post()
          .uri("/chat/completions")
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .body(payload)
          .retrieve()
          .body(JsonNode.class);
      if (response == null) {
        throw new AITaskProviderException("deepseek_empty_response", "deepseek_empty_response");
      }
      JsonNode content = response.path("choices").path(0).path("message").path("content");
      if (content.isMissingNode() || content.asText().isBlank()) {
        throw new AITaskProviderException("deepseek_output_missing", "deepseek_output_missing");
      }
      JsonNode outputPayload = objectMapper.readTree(content.asText());
      JsonNode usage = response.path("usage");
      BigDecimal costUnits = usage.path("total_tokens").isNumber()
          ? BigDecimal.valueOf(usage.path("total_tokens").asLong())
          : null;
      return new AITaskProviderResponse(
          outputPayload,
          "[]",
          costUnits,
          response.path("id").asText(null),
          Duration.between(startedAt, Instant.now()));
    } catch (AITaskProviderException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new AITaskProviderException("deepseek_request_failed", "deepseek_request_failed");
    } catch (Exception exception) {
      throw new AITaskProviderException("deepseek_response_parse_failed", "deepseek_response_parse_failed");
    }
  }
}
