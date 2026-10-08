package vn.edufit.ai.infra.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository quản lý việc lưu trữ và truy vấn bảng {@code ai_request_log}.
 *
 * <p>Mục đích thiết kế:
 * <ul>
 *   <li><b>Ghi nhận kiểm toán (FR-05):</b> Lưu trữ mọi lượt gọi tới AI Platform.</li>
 *   <li><b>Hỗ trợ tra cứu hạn ngạch nhanh (BR-10, BR-25):</b> Tận dụng chỉ mục CSDL
 *       {@code idx_ai_log_user} trên {@code (user_id, created_at)} để đếm nhanh số lượt gọi
 *       kể từ một mốc thời gian {@code since} với độ phức tạp truy vấn thấp.</li>
 * </ul>
 */
@Repository
public interface AiRequestLogRepository extends JpaRepository<AiRequestLogEntity, UUID> {

  /**
   * Đếm số lượt yêu cầu AI của một tài khoản cho một tính năng cụ thể kể từ mốc thời gian {@code since}.
   *
   * @param userId    Định danh tài khoản người dùng
   * @param feature   Tên tính năng AI
   * @param createdAt Mốc thời gian bắt đầu tính
   * @return Số lượng bản ghi khớp điều kiện
   */
  long countByUserIdAndFeatureAndCreatedAtAfter(UUID userId, String feature, Instant createdAt);
}
