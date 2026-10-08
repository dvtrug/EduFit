package vn.edufit.ai.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edufit.ai.api.AiFeature;
import vn.edufit.ai.api.AiGateway;
import vn.edufit.ai.api.AiRequest;
import vn.edufit.ai.api.AiResponse;
import vn.edufit.ai.api.AiTimeoutException;
import vn.edufit.ai.api.AiUnavailableException;
import vn.edufit.ai.api.AiUsageQuery;
import vn.edufit.ai.infra.client.LlmProvider;
import vn.edufit.ai.infra.persistence.AiRequestLogRepository;

/**
 * Dịch vụ điều phối chính của phân hệ AI Gateway (Main Orchestration Service).
 *
 * <p>Mục đích thiết kế & Kiến trúc phân tầng:
 * <ul>
 *   <li><b>Hiện thực hóa cả 2 Public Ports:</b>
 *     <ul>
 *       <li>{@link AiGateway}: Điều phối luồng gọi LLM, đo độ trễ, lọc chuỗi đầu ra và ghi log.</li>
 *       <li>{@link AiUsageQuery}: Cung cấp số liệu thống kê lượt gọi phục vụ kiểm soát hạn ngạch ở tầng nghiệp vụ.</li>
 *     </ul>
 *   </li>
 *   <li><b>Độc lập nghiệp vụ hoàn toàn:</b> Không chứa logic về gia sư, học viên, hay thuật toán matching.</li>
 *   <li><b>Phân định rõ ngoại lệ kỹ thuật:</b>
 *     <ul>
 *       <li>Ngoại lệ quá hạn 15s ({@link AiTimeoutException} - NFR-10).</li>
 *       <li>Ngoại lệ lỗi kết nối hoặc phản hồi lỗi ({@link AiUnavailableException}).</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Service
public class AiGatewayService implements AiGateway, AiUsageQuery {

  private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

  private final LlmProvider llmProvider;
  private final OutputSanitizer outputSanitizer;
  private final AiRequestLogger requestLogger;
  private final AiRequestLogRepository logRepository;

  public AiGatewayService(
      LlmProvider llmProvider,
      OutputSanitizer outputSanitizer,
      AiRequestLogger requestLogger,
      AiRequestLogRepository logRepository
  ) {
    this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider không được phép null");
    this.outputSanitizer = Objects.requireNonNull(outputSanitizer, "outputSanitizer không được phép null");
    this.requestLogger = Objects.requireNonNull(requestLogger, "requestLogger không được phép null");
    this.logRepository = Objects.requireNonNull(logRepository, "logRepository không được phép null");
  }

  @Override
  public AiResponse complete(AiRequest request) {
    Objects.requireNonNull(request, "AiRequest không được phép null");

    UUID userId = request.userId();
    String featureName = request.feature().name();
    long startTime = System.currentTimeMillis();

    log.debug("Bắt đầu xử lý AI request [user: {}, feature: {}]", userId, featureName);

    try {
      // 1. Gửi yêu cầu tới LlmProvider (Gemini hoặc Stub). Timeout 15s được cấu hình ở tầng Socket (NFR-10).
      LlmProvider.LlmResult rawResult = llmProvider.call(
          request.systemPrompt(),
          request.userPrompt(),
          request.temperature()
      );

      // 2. Làm sạch và kiểm duyệt chuỗi văn bản đầu ra
      String sanitizedText = outputSanitizer.sanitize(rawResult.text());
      int latency = (int) (System.currentTimeMillis() - startTime);

      // 3. Ghi nhận nhật ký thành công vào CSDL với transaction độc lập (FR-05)
      requestLogger.log(userId, featureName, "SUCCESS", latency);

      log.info("Hoàn tất AI request thành công [user: {}, feature: {}, latency: {}ms]", userId, featureName, latency);

      return new AiResponse(sanitizedText, latency, rawResult.tokenCount());

    } catch (AiTimeoutException ex) {
      // Cuộc gọi bị quá thời gian 15 giây (NFR-10)
      int latency = (int) (System.currentTimeMillis() - startTime);
      requestLogger.log(userId, featureName, "TIMEOUT", latency);
      log.warn("AI request bị TIMEOUT sau {}ms [user: {}, feature: {}]: {}", latency, userId, featureName, ex.getMessage());
      throw ex;

    } catch (Exception ex) {
      // Các lỗi kết nối mạng, lỗi 5xx từ nhà cung cấp hoặc dữ liệu không hợp lệ
      int latency = (int) (System.currentTimeMillis() - startTime);
      requestLogger.log(userId, featureName, "ERROR", latency);
      log.error("AI request gặp ERROR sau {}ms [user: {}, feature: {}]: {}", latency, userId, featureName, ex.getMessage());

      if (ex instanceof AiUnavailableException aue) {
        throw aue;
      }
      throw new AiUnavailableException("Dịch vụ AI hiện không khả dụng: " + ex.getMessage(), ex);
    }
  }

  @Override
  public long countRequestsSince(UUID userId, AiFeature feature, Instant since) {
    Objects.requireNonNull(userId, "userId không được phép null");
    Objects.requireNonNull(feature, "feature không được phép null");
    Objects.requireNonNull(since, "mốc thời gian since không được phép null");

    return logRepository.countByUserIdAndFeatureAndCreatedAtAfter(userId, feature.name(), since);
  }
}
