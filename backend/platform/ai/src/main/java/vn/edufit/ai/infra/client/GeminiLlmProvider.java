package vn.edufit.ai.infra.client;

import tools.jackson.databind.JsonNode;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import vn.edufit.ai.api.AiTimeoutException;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.infra.config.AiProperties;

/**
 * Hiện thực {@link LlmProvider} kết nối tới Google Generative Language API (Gemini 1.5 Flash).
 *
 * <p>Mục đích thiết kế & Nguyên lý hoạt động:
 * <ul>
 *   <li><b>Kích hoạt có điều kiện:</b> Chỉ được nạp vào Spring IoC Container khi cấu hình
 *       {@code edufit.ai.provider=gemini}.</li>
 *   <li><b>Chuẩn hóa định dạng Google Gemini REST API:</b>
 *     <ul>
 *       <li>URL endpoint: {@code /v1beta/models/{model}:generateContent?key={apiKey}}</li>
 *       <li>Hỗ trợ cả {@code systemInstruction} và {@code contents} theo chuẩn Gemini v1beta.</li>
 *       <li>Trích xuất văn bản từ {@code candidates[0].content.parts[0].text}.</li>
 *       <li>Thu thập thống kê token từ {@code usageMetadata.totalTokenCount}.</li>
 *     </ul>
 *   </li>
 *   <li><b>Xử lý ngoại lệ chuẩn hóa (NFR-10):</b>
 *       Khi {@link RestClient} gặp {@link SocketTimeoutException} (do vượt quá 15 giây),
 *       chuyển đổi chính xác thành {@link AiTimeoutException} để kích hoạt cơ chế Fallback ở tầng trên.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "edufit.ai.provider", havingValue = "gemini")
public class GeminiLlmProvider implements LlmProvider {

  private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);

  private final RestClient restClient;
  private final AiProperties properties;

  public GeminiLlmProvider(RestClient aiRestClient, AiProperties properties) {
    this.restClient = aiRestClient;
    this.properties = properties;
  }

  @Override
  public LlmResult call(String systemPrompt, String userPrompt, Double temperature) {
    String apiKey = properties.gemini().apiKey();
    String model = properties.gemini().model();

    if (apiKey == null || apiKey.isBlank()) {
      throw new AiUnavailableException("Chưa cấu hình GEMINI_API_KEY trong file .env hoặc biến môi trường.");
    }

    String path = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);

    // 1. Chuẩn bị payload yêu cầu gửi tới Gemini API
    Map<String, Object> requestBody = new HashMap<>();

    // Thêm system instruction nếu có
    if (systemPrompt != null && !systemPrompt.isBlank()) {
      requestBody.put("systemInstruction", Map.of(
          "parts", List.of(Map.of("text", systemPrompt))
      ));
    }

    // Thêm nội dung người dùng (User Prompt)
    List<Map<String, Object>> contents = new ArrayList<>();
    contents.add(Map.of(
        "role", "user",
        "parts", List.of(Map.of("text", userPrompt))
    ));
    requestBody.put("contents", contents);

    // Thêm cấu hình nhiệt độ (Temperature)
    if (temperature != null) {
      requestBody.put("generationConfig", Map.of("temperature", temperature));
    }

    // 2. Gửi HTTP Request qua RestClient
    try {
      log.debug("Đang gửi yêu cầu tới Gemini model: {}", model);

      JsonNode responseNode = restClient.post()
          .uri(path)
          .contentType(MediaType.APPLICATION_JSON)
          .body(requestBody)
          .retrieve()
          .body(JsonNode.class);

      if (responseNode == null) {
        throw new AiUnavailableException("Gemini API trả về phản hồi rỗng (null response).");
      }

      // 3. Trích xuất nội dung văn bản từ candidates
      JsonNode candidates = responseNode.get("candidates");
      if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
        // Kiểm tra xem có bị block bởi bộ lọc an toàn (Safety Filter) không
        JsonNode promptFeedback = responseNode.get("promptFeedback");
        String blockReason = promptFeedback != null ? promptFeedback.toString() : "Không rõ nguyên nhân";
        log.warn("Gemini không trả về candidates hợp lệ. Feedback: {}", blockReason);
        throw new AiUnavailableException("Gemini từ chối sinh nội dung (Prompt bị chặn bởi bộ lọc an toàn).");
      }

      JsonNode firstCandidate = candidates.get(0);
      JsonNode contentNode = firstCandidate.get("content");
      if (contentNode == null || !contentNode.has("parts")) {
        throw new AiUnavailableException("Cấu trúc nội dung phản hồi từ Gemini không đúng định dạng mong đợi.");
      }

      String generatedText = contentNode.get("parts").get(0).get("text").asText();

      // 4. Trích xuất thống kê token từ usageMetadata (nếu có)
      Integer totalTokens = null;
      JsonNode usageMetadata = responseNode.get("usageMetadata");
      if (usageMetadata != null && usageMetadata.has("totalTokenCount")) {
        totalTokens = usageMetadata.get("totalTokenCount").asInt();
      }

      return new LlmResult(generatedText, totalTokens);

    } catch (ResourceAccessException ex) {
      // Bắt lỗi Socket Timeout (NFR-10: Vượt quá 15 giây)
      if (ex.getCause() instanceof SocketTimeoutException
          || (ex.getMessage() != null && ex.getMessage().contains("timed out"))) {
        log.warn("Gemini API bị quá thời gian chờ 15s (NFR-10): {}", ex.getMessage());
        throw new AiTimeoutException("Cuộc gọi tới Gemini API vượt quá giới hạn thời gian chờ 15 giây (NFR-10).", ex);
      }
      log.error("Lỗi I/O kết nối tới Gemini API: {}", ex.getMessage());
      throw new AiUnavailableException("Không thể kết nối tới dịch vụ Gemini: " + ex.getMessage(), ex);

    } catch (AiUnavailableException ex) {
      throw ex;

    } catch (Exception ex) {
      log.error("Lỗi không xác định khi gọi Gemini API: {}", ex.getMessage(), ex);
      throw new AiUnavailableException("Lỗi giao tiếp với Gemini API: " + ex.getMessage(), ex);
    }
  }
}
