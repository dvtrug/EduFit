package vn.edufit.ai.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Thực thể JPA ánh xạ tới bảng {@code ai_request_log} trong cơ sở dữ liệu PostgreSQL.
 *
 * <p>Mục đích thiết kế & Tuân thủ quy định:
 * <ul>
 *   <li><b>Tuân thủ FR-05 (Audit Logging - Không lưu nội dung):</b> Bảng này chỉ lưu lại
 *       các thông số kỹ thuật (thời điểm gọi, người gọi, tính năng, trạng thái thành công/lỗi, độ trễ),
 *       <b>tuyệt đối không lưu chuỗi prompt hay văn bản phản hồi</b> nhằm bảo vệ bí mật thông tin của người dùng.</li>
 *   <li><b>Ràng buộc toàn vẹn:</b> Cột {@code feature} nhận giá trị 'TUTOR_BIO' hoặc 'MATCH_EXPLANATION';
 *       Cột {@code status} nhận giá trị 'SUCCESS', 'ERROR' hoặc 'TIMEOUT'.</li>
 *   <li><b>Hỗ trợ tra cứu hạn ngạch (BR-10, BR-25):</b> Kết hợp với index {@code (user_id, created_at)}
 *       để truy vấn số lượt gọi trong phiên hoặc trong ngày với tốc độ tối ưu.</li>
 * </ul>
 */
@Entity
@Table(name = "ai_request_log")
public class AiRequestLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "log_id", updatable = false, nullable = false)
  private UUID logId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "feature", nullable = false, length = 40)
  private String feature;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "latency_ms")
  private Integer latencyMs;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Constructor mặc định cho JPA.
   */
  protected AiRequestLogEntity() {}

  /**
   * Constructor khởi tạo bản ghi nhật ký mới.
   *
   * @param userId    ID tài khoản thực hiện yêu cầu
   * @param feature   Tên tính năng AI ('TUTOR_BIO' hoặc 'MATCH_EXPLANATION')
   * @param status    Trạng thái kết quả ('SUCCESS', 'ERROR', 'TIMEOUT')
   * @param latencyMs Độ trễ thực thi tính bằng mili-giây
   */
  public AiRequestLogEntity(UUID userId, String feature, String status, Integer latencyMs) {
    this.userId = Objects.requireNonNull(userId, "userId không được phép null");
    this.feature = Objects.requireNonNull(feature, "feature không được phép null");
    this.status = Objects.requireNonNull(status, "status không được phép null");
    this.latencyMs = latencyMs;
    this.createdAt = Instant.now();
  }

  public UUID getLogId() {
    return logId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getFeature() {
    return feature;
  }

  public String getStatus() {
    return status;
  }

  public Integer getLatencyMs() {
    return latencyMs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
