package vn.edufit.ai.infra.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cấu hình khởi tạo Bean {@link RestClient} phục vụ việc giao tiếp HTTP với các nhà cung cấp AI.
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>RestClient trên Java 21 Virtual Threads:</b> {@link RestClient} là HTTP Client đồng bộ (synchronous)
 *       mới của Spring Boot 3.2 trở lên. Nó sử dụng I/O blocking tự nhiên, tương thích hoàn hảo với
 *       Java 21 Virtual Threads (khi thread bị block chờ I/O, JVM sẽ tự động unmount và trao CPU cho luồng khác),
 *       mang lại hiệu năng tương đương Reactive Programming mà code tuần tự, dễ debug.</li>
 *   <li><b>Cưỡng chế Timeout 15s ở tầng Socket (NFR-10):</b> Đặt cả {@code ConnectTimeout} và
 *       {@code ReadTimeout} theo thuộc tính {@link AiProperties#timeout()} (mặc định 15s).
 *       Socket timeout bổ sung cho deadline tổng của provider tại AiGatewayService;
 *       riêng connect/read timeout không giới hạn tổng thời gian của một response chậm.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

  @Bean(destroyMethod = "shutdownNow")
  public ExecutorService aiProviderExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Tạo Bean {@link RestClient} chuyên dụng cho các yêu cầu gọi AI.
   *
   * @param properties Thuộc tính cấu hình AI đã được Spring Boot nạp
   * @return Đối tượng {@link RestClient} đã được cấu hình timeout và baseUrl
   */
  @Bean
  public RestClient aiRestClient(AiProperties properties) {
    Duration timeout = properties.timeout();

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);

    return RestClient.builder()
        .baseUrl(properties.gemini().baseUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
