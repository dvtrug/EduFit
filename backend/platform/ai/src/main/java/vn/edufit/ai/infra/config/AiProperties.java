package vn.edufit.ai.infra.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Lớp cấu hình nạp các thuộc tính {@code edufit.ai.*} từ file {@code application.yml} hoặc biến môi trường.
 *
 * <p>Mục đích thiết kế:
 * <ul>
 *   <li><b>Ánh xạ cấu hình chuẩn 12-Factor App:</b> Cho phép ghi đè linh hoạt qua biến môi trường
 *       ({@code AI_PROVIDER}, {@code GEMINI_API_KEY}, {@code GEMINI_MODEL}) mà không làm lộ secret trên Git.</li>
 *   <li><b>Khống chế Timeout chặt chẽ (NFR-10):</b> Giá trị mặc định là 15 giây ({@code 15s}),
 *       được dùng để cấu hình trực tiếp vào tầng Socket của {@link org.springframework.web.client.RestClient}.</li>
 *   <li><b>Tính bất biến (Immutability):</b> Được hiện thực dưới dạng Java Record, an toàn tuyệt đối
 *       khi nạp cấu hình trong môi trường đa luồng.</li>
 * </ul>
 *
 * @param provider Tên nhà cung cấp AI muốn kích hoạt ("gemini" hoặc "stub", mặc định là "stub").
 * @param timeout  Thời gian chờ tối đa cho mỗi cuộc gọi LLM (mặc định 15 giây theo NFR-10).
 * @param gemini   Các thông số kết nối chi tiết tới Google Gemini API.
 */
@ConfigurationProperties(prefix = "edufit.ai")
public record AiProperties(
    @DefaultValue("stub") String provider,
    @DefaultValue("15s") Duration timeout,
    @DefaultValue GeminiProperties gemini
) {

  /**
   * Cấu hình chi tiết dành riêng cho nhà cung cấp Google Gemini.
   *
   * @param apiKey  Khóa bí mật API của Google Gemini (đọc từ biến môi trường GEMINI_API_KEY).
   * @param model   Tên mô hình ngôn ngữ muốn sử dụng (ví dụ: gemini-1.5-flash).
   * @param baseUrl Địa chỉ gốc của Google Generative Language API.
   */
  public record GeminiProperties(
      @DefaultValue("") String apiKey,
      @DefaultValue("gemini-1.5-flash") String model,
      @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl
  ) {

    public GeminiProperties {
      if (apiKey == null) {
        apiKey = "";
      }
      if (model == null || model.isBlank()) {
        model = "gemini-1.5-flash";
      }
      if (baseUrl == null || baseUrl.isBlank()) {
        baseUrl = "https://generativelanguage.googleapis.com";
      }
    }
  }

  public AiProperties {
    if (provider == null || provider.isBlank()) {
      provider = "stub";
    }
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      timeout = Duration.ofSeconds(15);
    }
    if (gemini == null) {
      gemini = new GeminiProperties("", "gemini-1.5-flash", "https://generativelanguage.googleapis.com");
    }
  }
}
