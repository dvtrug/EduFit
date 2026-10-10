package vn.edufit.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edufit.ai.api.AiFeature;
import vn.edufit.ai.api.AiRequest;
import vn.edufit.ai.api.AiResponse;
import vn.edufit.ai.api.AiTimeoutException;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.application.AiGatewayService;
import vn.edufit.ai.application.AiRequestLogger;
import vn.edufit.ai.application.OutputSanitizer;
import vn.edufit.ai.infra.client.LlmProvider;
import vn.edufit.ai.infra.persistence.AiRequestLogRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử đơn vị cho AiGatewayService")
class AiGatewayServiceTest {

  @Mock
  private LlmProvider llmProvider;

  @Mock
  private AiRequestLogger requestLogger;

  @Mock
  private AiRequestLogRepository logRepository;

  private final OutputSanitizer outputSanitizer = new OutputSanitizer();

  private AiGatewayService gatewayService;
  private java.util.concurrent.ExecutorService executor;

  @BeforeEach
  void setUp() {
    executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    gatewayService = new AiGatewayService(llmProvider, outputSanitizer, requestLogger, logRepository,
        executor, new vn.edufit.ai.infra.config.AiProperties(null, null, null));
  }

  @AfterEach
  void closeExecutor() {
    executor.shutdownNow();
  }

  @Test
  @DisplayName("Khi nhà cung cấp AI trả về kết quả hợp lệ, trả về AiResponse và ghi log SUCCESS")
  void shouldCompleteSuccessfully() {
    UUID userId = UUID.randomUUID();
    AiRequest request = AiRequest.of(userId, AiFeature.TUTOR_BIO, "System prompt", "User prompt");

    when(llmProvider.call(any(), any(), anyDouble()))
        .thenReturn(new LlmProvider.LlmResult("  Bản nháp Bio sư phạm hoàn chỉnh  ", 45));

    AiResponse response = gatewayService.complete(request);

    assertThat(response).isNotNull();
    assertThat(response.text()).isEqualTo("Bản nháp Bio sư phạm hoàn chỉnh");
    assertThat(response.tokenCount()).isEqualTo(45);
    assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0);

    // Xác nhận đã ghi log SUCCESS vào CSDL (FR-05)
    verify(requestLogger).log(eq(userId), eq("TUTOR_BIO"), eq("SUCCESS"), anyInt());
  }

  @Test
  @DisplayName("Khi cuộc gọi bị quá 15 giây (NFR-10), ném AiTimeoutException và ghi log TIMEOUT")
  void shouldHandleTimeoutException() {
    UUID userId = UUID.randomUUID();
    AiRequest request = AiRequest.of(userId, AiFeature.MATCH_EXPLANATION, "System", "Match prompt");

    when(llmProvider.call(any(), any(), anyDouble()))
        .thenThrow(new AiTimeoutException("Timeout quá 15s"));

    assertThatThrownBy(() -> gatewayService.complete(request))
        .isInstanceOf(AiTimeoutException.class)
        .hasMessageContaining("Timeout quá 15s");

    // Xác nhận đã ghi log TIMEOUT vào CSDL (FR-05)
    verify(requestLogger).log(eq(userId), eq("MATCH_EXPLANATION"), eq("TIMEOUT"), anyInt());
  }

  @Test
  @DisplayName("Khi gặp sự cố kết nối hoặc lỗi 5xx, ném AiUnavailableException và ghi log ERROR")
  void shouldHandleGenericException() {
    UUID userId = UUID.randomUUID();
    AiRequest request = AiRequest.of(userId, AiFeature.TUTOR_BIO, "System", "Bio prompt");

    when(llmProvider.call(any(), any(), anyDouble()))
        .thenThrow(new RuntimeException("Mất kết nối mạng"));

    assertThatThrownBy(() -> gatewayService.complete(request))
        .isInstanceOf(AiUnavailableException.class)
        .hasMessageContaining("Mất kết nối mạng");

    // Xác nhận đã ghi log ERROR vào CSDL (FR-05)
    verify(requestLogger).log(eq(userId), eq("TUTOR_BIO"), eq("ERROR"), anyInt());
  }

  @Test
  @DisplayName("Khi tra cứu số lượt gọi qua AiUsageQuery, ủy quyền chính xác cho Repository")
  void shouldQueryUsageCounts() {
    UUID userId = UUID.randomUUID();
    Instant since = Instant.now().minusSeconds(3600);

    when(logRepository.countByUserIdAndFeatureAndCreatedAtAfter(userId, "TUTOR_BIO", since))
        .thenReturn(3L);

    long count = gatewayService.countRequestsSince(userId, AiFeature.TUTOR_BIO, since);

    assertThat(count).isEqualTo(3L);
    verify(logRepository).countByUserIdAndFeatureAndCreatedAtAfter(userId, "TUTOR_BIO", since);
  }
}
