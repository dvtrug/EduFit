package vn.edufit.ai.infra.client;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import vn.edufit.ai.api.AiTimeoutException;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.infra.config.AiProperties;

/** Gemini REST adapter. Provider bodies and transport causes stay inside this boundary. */
@Component
@ConditionalOnProperty(name = "edufit.ai.provider", havingValue = "gemini")
public class GeminiLlmProvider implements LlmProvider {
  private final RestClient restClient;
  private final AiProperties properties;

  public GeminiLlmProvider(RestClient aiRestClient, AiProperties properties) {
    this.restClient = aiRestClient;
    this.properties = properties;
  }

  @Override
  public LlmResult call(String systemPrompt, String userPrompt, Double temperature) {
    if (properties.gemini().apiKey().isBlank()) {
      throw new AiUnavailableException("GEMINI_API_KEY is not configured");
    }
    if (userPrompt == null || userPrompt.isBlank()) {
      throw new AiUnavailableException("Gemini requires a non-empty prompt");
    }
    Map<String, Object> body = new HashMap<>();
    if (systemPrompt != null && !systemPrompt.isBlank()) {
      body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
    }
    body.put(
        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))));
    if (temperature != null) {
      body.put("generationConfig", Map.of("temperature", temperature));
    }

    try {
      JsonNode response =
          restClient
              .post()
              .uri("/v1beta/models/{model}:generateContent", properties.gemini().model())
              .header("x-goog-api-key", properties.gemini().apiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      return parse(response);
    } catch (RestClientResponseException ex) {
      // Raw bodies, URLs and causes may contain a key or prompt data.
      throw new AiUnavailableException("Gemini returned HTTP " + ex.getStatusCode().value());
    } catch (AiUnavailableException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      // Read timeouts can also be wrapped by a message converter while reading the body.
      for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
        if (cause instanceof SocketTimeoutException) {
          throw new AiTimeoutException("Gemini transport timeout");
        }
      }
      throw new AiUnavailableException(
          ex instanceof ResourceAccessException
              ? "Gemini transport unavailable"
              : "Gemini response could not be processed");
    }
  }

  private LlmResult parse(JsonNode response) {
    if (response == null || !response.isObject()) {
      throw new AiUnavailableException("Gemini returned an empty or invalid response");
    }
    String blockReason = response.path("promptFeedback").path("blockReason").asText("");
    if (!blockReason.isEmpty() && !blockReason.equals("BLOCK_REASON_UNSPECIFIED")) {
      throw new AiUnavailableException("Gemini blocked the prompt");
    }
    JsonNode candidates = response.path("candidates");
    if (!candidates.isArray() || candidates.isEmpty()) {
      throw new AiUnavailableException("Gemini returned no candidates");
    }
    JsonNode candidate = candidates.get(0);
    if (!candidate.path("finishReason").asText("").equals("STOP")) {
      throw new AiUnavailableException("Gemini did not return a completed answer");
    }
    for (JsonNode rating : candidate.path("safetyRatings")) {
      if (rating.path("blocked").asBoolean(false)) {
        throw new AiUnavailableException("Gemini blocked the answer");
      }
    }
    JsonNode parts = candidate.path("content").path("parts");
    if (!parts.isArray()) {
      throw new AiUnavailableException("Gemini returned invalid content parts");
    }
    StringBuilder text = new StringBuilder();
    for (JsonNode part : parts) {
      JsonNode value = part.path("text");
      if (!part.path("thought").asBoolean(false) && value.isString()) {
        text.append(value.asText());
      }
    }
    if (text.toString().isBlank()) {
      throw new AiUnavailableException("Gemini returned no answer text");
    }
    JsonNode tokens = response.path("usageMetadata").path("totalTokenCount");
    Integer count =
        tokens.isIntegralNumber() && tokens.canConvertToInt() && tokens.asInt() >= 0
            ? tokens.asInt()
            : null;
    return new LlmResult(text.toString(), count);
  }
}
