package vn.edufit.ai.api;

/**
 * Cổng giao tiếp AI dùng chung cho toàn bộ hệ thống EduFit (AI Gateway Public Port).
 *
 * <p>Mục đích thiết kế & Nguyên lý kiến trúc:
 * <ul>
 *   <li><b>Ports & Adapters (Hexagonal Architecture):</b> Là Driving/Inbound Port công khai duy nhất
 *       của phân hệ {@code platform/ai}. Các module nghiệp vụ (như {@code profile}, {@code discovery})
 *       <b>chỉ được phép phụ thuộc vào interface này</b>, tuyệt đối không được truy cập trực tiếp
 *       vào các tầng nội bộ {@code application} hay {@code infra} bên dưới.</li>
 *   <li><b>Thiết kế Generic (Domain-Agnostic):</b> Phân hệ AI không biết bất kỳ quy tắc nghiệp vụ
 *       nào của Gia sư (Bio) hay Thuật toán ghép đôi (Matching). Nó chỉ tiếp nhận yêu cầu văn bản
 *       đã được làm sạch, điều phối gọi LLM Provider, đo lường hiệu năng và ghi nhật ký kiểm toán.</li>
 *   <li><b>An toàn đa luồng trên Virtual Threads:</b> Được thiết kế phi trạng thái (Stateless),
 *       an toàn tuyệt đối khi được gọi đồng thời bởi nhiều luồng ảo trên Java 21.</li>
 * </ul>
 *
 * <p>Ví dụ cách gọi từ module nghiệp vụ:
 * <pre>{@code
 * AiRequest request = AiRequest.of(
 *     userId,
 *     AiFeature.TUTOR_BIO,
 *     "Bạn là trợ lý viết bio sư phạm chuẩn mực.",
 *     prompt
 * );
 *
 * try {
 *     AiResponse response = aiGateway.complete(request);
 *     return response.text();
 * } catch (AiTimeoutException ex) {
 *     // Xử lý timeout 15s theo NFR-10 / kích hoạt Fallback theo FR-06
 * } catch (AiUnavailableException ex) {
 *     // Xử lý khi AI gặp sự cố
 * }
 * }</pre>
 */ 
public interface AiGateway {

  /**
   * Gửi yêu cầu hoàn tất văn bản tới nhà cung cấp AI đã được cấu hình.
   *
   * <p>Quy trình thực thi bên trong:
   * <ol>
   *   <li>Ủy quyền cuộc gọi tới {@code LlmProvider} (Gemini hoặc Stub).</li>
   *   <li>Khống chế thời gian chờ tối đa 15 giây (NFR-10).</li>
   *   <li>Làm sạch kết quả văn bản trả về qua {@code OutputSanitizer}.</li>
   *   <li>Ghi nhật ký kiểm toán kỹ thuật vào bảng {@code ai_request_log}
   *       trong transaction độc lập (FR-05 - REQUIRES_NEW).</li>
   * </ol>
   *
   * @param request Yêu cầu xử lý AI chứa thông tin đã được loại bỏ PII (NFR-09).
   * @return Kết quả văn bản đã được kiểm tra tính hợp lệ kèm thời gian thực thi (latency).
   * @throws AiTimeoutException       Khi cuộc gọi vượt quá giới hạn 15 giây (NFR-10).
   * @throws AiUnavailableException   Khi nhà cung cấp AI trả về lỗi hoặc mất kết nối mạng.
   * @throws IllegalArgumentException Nếu dữ liệu trong {@code request} không hợp lệ.
   */
  AiResponse complete(AiRequest request);
}
