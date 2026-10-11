package vn.edufit.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;
import vn.edufit.ai.api.AiTimeoutException;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.infra.client.GeminiLlmProvider;
import vn.edufit.ai.infra.config.AiClientConfig;
import vn.edufit.ai.infra.config.AiProperties;

class GeminiLlmProviderTest {
  private static final String SECRET = "fake-key-never-log";
  private static final String PRIVATE_TEXT = "private@example.test prompt-and-output";
  private HttpServer server;
  private volatile int status = 200;
  private volatile long delayMs;
  private volatile String response = success("[{\"text\":\"Đúng dữ kiện.\"}]");
  private final AtomicReference<String> uri = new AtomicReference<>();
  private final AtomicReference<String> keyHeader = new AtomicReference<>();
  private final AtomicReference<String> request = new AtomicReference<>();
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  private Logger logger;

  @BeforeEach
  void startServer() throws IOException {
    logger = (Logger) LoggerFactory.getLogger("vn.edufit.ai");
    logs.start();
    logger.addAppender(logs);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          uri.set(exchange.getRequestURI().toString());
          keyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
          request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          try {
            Thread.sleep(delayMs);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } catch (IOException ex) {
            // A timed-out client is expected to close before the slow response is written.
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
    logger.detachAppender(logs);
    logs.stop();
  }

  @Test
  void sendsHeaderAuthenticationAndExpectedPayloadOverRealHttp() {
    var result = provider(Duration.ofSeconds(2)).call("System", "Verified facts", 0.2);
    assertThat(result.text()).isEqualTo("Đúng dữ kiện.");
    assertThat(uri.get()).isEqualTo("/v1beta/models/test-model:generateContent");
    assertThat(keyHeader.get()).isEqualTo(SECRET);
    var json = JsonMapper.builder().build().readTree(request.get());
    assertThat(json.at("/systemInstruction/parts/0/text").asText()).isEqualTo("System");
    assertThat(json.at("/contents/0/role").asText()).isEqualTo("user");
    assertThat(json.at("/contents/0/parts/0/text").asText()).isEqualTo("Verified facts");
    assertThat(json.at("/generationConfig/temperature").asDouble()).isEqualTo(0.2);
    assertThat(result.tokenCount()).isNull();
    assertThat(result.generatedByModel()).isTrue();
  }

  @Test
  void joinsTextPartsAndSkipsThoughtsAndNonTextParts() {
    response =
        success(
                "[{\"thought\":true,\"text\":\"hidden thought\"},"
                    + "{\"inlineData\":{\"mimeType\":\"image/png\",\"data\":\"AA==\"}},"
                    + "{\"text\":\"Câu một. \"},{\"text\":\"Câu hai.\"}]")
            .replace("}]}", "}],\"usageMetadata\":{\"totalTokenCount\":42}}");
    var result = provider(Duration.ofSeconds(2)).call(null, "Facts", null);
    assertThat(result.text()).isEqualTo("Câu một. Câu hai.");
    assertThat(result.tokenCount()).isEqualTo(42);
    var json = JsonMapper.builder().build().readTree(request.get());
    assertThat(json.has("systemInstruction")).isFalse();
    assertThat(json.has("generationConfig")).isFalse();
  }

  @ParameterizedTest
  @MethodSource("unusableResponses")
  void rejectsUnusableProviderResponsesWithoutLoggingContent(String body) {
    response = body;
    assertThatThrownBy(() -> provider(Duration.ofSeconds(2)).call("System", PRIVATE_TEXT, 0.2))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageNotContaining(SECRET)
        .hasMessageNotContaining(PRIVATE_TEXT)
        .hasNoCause();
    assertSafeLogs();
  }

  static Stream<String> unusableResponses() {
    return Stream.of(
        "",
        "null",
        "{not-json " + PRIVATE_TEXT,
        "{}",
        "{\"candidates\":[]}",
        "{\"candidates\":[{\"finishReason\":\"STOP\"}]}",
        "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":{}}}]}",
        "{\"promptFeedback\":{\"blockReason\":\"SAFETY\",\"message\":\"" + PRIVATE_TEXT + "\"}}",
        success("[]"),
        success("[{\"text\":\"  \"}]"),
        success("[{\"text\":42}]"),
        success("[{\"thought\":true,\"text\":\"" + PRIVATE_TEXT + "\"}]"),
        success("[{\"functionCall\":{\"name\":\"tool\"}}]"),
        success("[{\"text\":\"" + PRIVATE_TEXT + "\"}]").replace("STOP", "SAFETY"),
        success("[{\"text\":\"partial\"}]").replace("STOP", "MAX_TOKENS"),
        success("[{\"text\":\"partial\"}]").replace("\"finishReason\":\"STOP\",", ""),
        success("[{\"text\":\"blocked\"}]")
            .replace("\"content\":", "\"safetyRatings\":[{\"blocked\":true}],\"content\":"));
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 429, 500, 503})
  void httpErrorsNeverExposeProviderBodyOrKey(int httpStatus) {
    status = httpStatus;
    response = "{\"error\":{\"message\":\"" + SECRET + " " + PRIVATE_TEXT + "\"}}";
    assertThatThrownBy(() -> provider(Duration.ofSeconds(2)).call("System", PRIVATE_TEXT, 0.2))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageContaining("HTTP " + httpStatus)
        .hasMessageNotContaining(SECRET)
        .hasMessageNotContaining(PRIVATE_TEXT)
        .hasNoCause();
    assertSafeLogs();
  }

  @Test
  void socketTimeoutIsClassifiedWithoutRawTransportCause() {
    delayMs = 500;
    assertThatThrownBy(() -> provider(Duration.ofMillis(50)).call("System", "Facts", null))
        .isInstanceOf(AiTimeoutException.class)
        .hasNoCause();
    assertSafeLogs();
  }

  @Test
  void connectionFailureIsSafeForCallers() {
    var client = provider(Duration.ofSeconds(2));
    server.stop(0);
    assertThatThrownBy(() -> client.call("System", PRIVATE_TEXT, null))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageNotContaining(SECRET)
        .hasMessageNotContaining(PRIVATE_TEXT)
        .hasNoCause();
    assertSafeLogs();
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "\"unknown\"", "-1", "2147483648"})
  void invalidTokenMetadataDoesNotDiscardValidAnswer(String tokens) {
    response =
        success("[{\"text\":\"Valid answer\"}]")
            .replace("}]}", "}],\"usageMetadata\":{\"totalTokenCount\":" + tokens + "}}");
    var result = provider(Duration.ofSeconds(2)).call("System", "Facts", null);
    assertThat(result.text()).isEqualTo("Valid answer");
    assertThat(result.tokenCount()).isNull();
  }

  @Test
  void unspecifiedPromptBlockAndUnblockedRatingDoNotRejectCompletedAnswer() {
    response =
        success("[{\"text\":\"Valid answer\"}]")
            .replace("\"content\":", "\"safetyRatings\":[{\"blocked\":false}],\"content\":")
            .replace(
                "{\"candidates\":",
                "{\"promptFeedback\":{\"blockReason\":\"BLOCK_REASON_UNSPECIFIED\"},\"candidates\":");
    assertThat(provider(Duration.ofSeconds(2)).call("", "Facts", null).text())
        .isEqualTo("Valid answer");
    assertThat(JsonMapper.builder().build().readTree(request.get()).has("systemInstruction"))
        .isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void blankPromptIsRejectedBeforeSendingKey(String prompt) {
    assertThatThrownBy(() -> provider(Duration.ofSeconds(2)).call("System", prompt, null))
        .isInstanceOf(AiUnavailableException.class)
        .hasNoCause();
    assertThat(uri.get()).isNull();
  }

  private GeminiLlmProvider provider(Duration timeout) {
    var properties =
        new AiProperties(
            "gemini",
            timeout,
            new AiProperties.GeminiProperties(
                SECRET, "test-model", "http://127.0.0.1:" + server.getAddress().getPort()));
    return new GeminiLlmProvider(new AiClientConfig().aiRestClient(properties), properties);
  }

  private void assertSafeLogs() {
    assertThat(logs.list)
        .allSatisfy(
            event -> {
              assertThat(event.getFormattedMessage())
                  .doesNotContain(SECRET, PRIVATE_TEXT, "hidden thought");
              assertThat(event.getThrowableProxy()).isNull();
            });
  }

  private static String success(String parts) {
    return "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":" + parts + "}}]}";
  }
}
