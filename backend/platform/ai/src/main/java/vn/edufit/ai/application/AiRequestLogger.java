package vn.edufit.ai.application;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.ai.infra.persistence.AiRequestLogEntity;
import vn.edufit.ai.infra.persistence.AiRequestLogRepository;

/**
 * Dịch vụ ghi nhật ký kiểm toán yêu cầu AI độc lập (Isolated AI Audit Logger).
 *
 * <p>Mục đích thiết kế & Nguyên lý giao dịch cơ sở dữ liệu:
 * <ul>
 *   <li><b>Tuân thủ FR-05 (AI Request Logging):</b> Mọi cuộc gọi tới AI Platform đều phải được ghi nhận
 *       kỹ thuật (thời điểm, người dùng, tính năng, trạng thái, độ trễ) để phục vụ giám sát và kiểm toán.</li>
 *   <li><b>Cách ly giao dịch bằng {@code Propagation.REQUIRES_NEW}:</b>
 *       <p>Đây là điểm mấu chốt kiến trúc: Khi một module nghiệp vụ (ví dụ {@code profile} hoặc {@code discovery})
 *       đang chạy trong một {@code @Transactional} và gọi {@code complete()}, nếu nghiệp vụ phía sau bị lỗi
 *       dẫn đến Transaction chính bị <b>Rollback</b>, dòng nhật ký AI này <b>VẪN PHẢI ĐƯỢC LƯU LẠI VĨNH VIỄN</b>.</p>
 *       <p>Nếu dùng chung transaction, dòng log sẽ bị xóa sạch theo rollback, khiến hệ thống mất dấu vết
 *       chi phí thực tế đã gọi tới nhà cung cấp LLM.</p>
 *       <p>Nhờ {@code REQUIRES_NEW}, Spring sẽ tạm dừng (suspend) transaction hiện tại của module gọi,
 *       mở một kết nối/transaction mới hoàn toàn để {@code INSERT} vào bảng {@code ai_request_log}
 *       và {@code COMMIT} ngay lập tức.</p>
 *   </li>
 *   <li><b>An toàn tuyệt đối (Fail-safe):</b> Quá trình ghi log được bọc trong khối try-catch
 *       để đảm bảo nếu có sự cố bất thường về cơ sở dữ liệu thì không làm sập luồng xử lý chính.</li>
 * </ul>
 */
@Service
public class AiRequestLogger {

  private static final Logger log = LoggerFactory.getLogger(AiRequestLogger.class);

  private final AiRequestLogRepository repository;

  public AiRequestLogger(AiRequestLogRepository repository) {
    this.repository = repository;
  }

  /**
   * Ghi nhận một bản ghi nhật ký yêu cầu AI vào CSDL trong một Transaction độc lập mới.
   *
   * @param userId    Định danh tài khoản người dùng yêu cầu
   * @param feature   Tên tính năng AI ('TUTOR_BIO' hoặc 'MATCH_EXPLANATION')
   * @param status    Trạng thái kết quả ('SUCCESS', 'ERROR' hoặc 'TIMEOUT')
   * @param latencyMs Độ trễ thực thi tính bằng mili-giây
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(UUID userId, String feature, String status, Integer latencyMs) {
    Objects.requireNonNull(userId, "userId không được phép null khi ghi log AI");
    Objects.requireNonNull(feature, "feature không được phép null khi ghi log AI");
    Objects.requireNonNull(status, "status không được phép null khi ghi log AI");

    try {
      AiRequestLogEntity entity = new AiRequestLogEntity(userId, feature, status, latencyMs);
      repository.save(entity);
      log.debug("Đã lưu nhật ký AI [user: {}, feature: {}, status: {}, latency: {}ms] trong transaction riêng",
          userId, feature, status, latencyMs);
    } catch (Exception ex) {
      log.error("Không thể lưu nhật ký AI vào bảng ai_request_log: {}", ex.getMessage(), ex);
    }
  }
}
