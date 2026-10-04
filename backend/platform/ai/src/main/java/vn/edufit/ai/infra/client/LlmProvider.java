package vn.edufit.ai.infra.client;

/**
 * Interface trừu tượng hóa phương thức giao tiếp trực tiếp với nhà cung cấp LLM bên ngoài (Outbound Adapter SPI).
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Strategy Pattern:</b> Cho phép hoán đổi linh hoạt giữa {@link GeminiLlmProvider}
 *       (kết nối Google Gemini qua mạng thật) và {@link StubLlmProvider} (giả lập offline cho Unit Test và CI).</li>
 *   <li><b>Độc lập với chi tiết giao thức:</b> Tầng điều phối {@code application} chỉ tương tác
 *       với interface này, hoàn toàn không cần biết cấu trúc payload JSON hay endpoint URL của nhà cung cấp.</li>
 * </ul>
 */
public interface LlmProvider {

  /**
   * Kết quả trả về từ cuộc gọi LLM cấp thấp.
   *
   * @param text       Chuỗi văn bản do mô hình sinh ra.
   * @param tokenCount Tổng số lượng token tiêu thụ (có thể null nếu Provider không hỗ trợ).
   */
  record LlmResult(String text, Integer tokenCount) {
    public LlmResult {
      if (text == null) {
        text = "";
      }
    }
  }

  /**
   * Gửi prompt tới mô hình ngôn ngữ và trả về văn bản hoàn chỉnh.
   *
   * @param systemPrompt Chỉ thị hệ thống định hình vai trò mô hình (có thể null hoặc rỗng).
   * @param userPrompt   Nội dung prompt chính (bắt buộc).
   * @param temperature  Độ ngẫu nhiên/sáng tạo của mô hình.
   * @return Đối tượng {@link LlmResult} chứa văn bản sinh ra và token count.
   * @throws vn.edufit.ai.api.AiTimeoutException     Khi cuộc gọi vượt quá 15 giây (NFR-10).
   * @throws vn.edufit.ai.api.AiUnavailableException Khi xảy ra lỗi kết nối hoặc nhà cung cấp trả về lỗi HTTP 5xx/4xx.
   */
  LlmResult call(String systemPrompt, String userPrompt, Double temperature);
}
