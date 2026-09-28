package vn.edufit.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker Interface (Giao diện đánh dấu) cho mọi Sự kiện Miền (Domain Event) nội bộ trong hệ thống EduFit.
 *
 * <p>Theo kiến trúc Event-Driven Architecture (EDA) nội bộ quy định tại ADR-002 Decision 2:
 * <ul>
 *   <li>Khi một hành động nghiệp vụ hoàn thành (ví dụ: đề xuất buổi học, hoàn thành cột mốc, đăng ký tài khoản),
 *       module sở hữu sẽ phát ra một sự kiện kế thừa interface này.</li>
 *   <li>Các module khác (đặc biệt là {@code notification} và {@code audit}) sẽ lắng nghe sự kiện để gửi
 *       thông báo hoặc ghi log mà không cần module phát sự kiện phải gọi trực tiếp sang chúng.</li>
 *   <li>Giúp giảm tối đa sự phụ thuộc chéo (Coupling) giữa các module.</li>
 * </ul>
 */
public interface BaseDomainEvent {

  /**
   * Lấy mã định danh duy nhất của sự kiện (dùng để truy vết hoặc chống xử lý trùng lặp - Idempotency).
   *
   * @return UUID định danh sự kiện.
   */
  UUID getEventId();

  /**
   * Lấy thời điểm chính xác sự kiện này phát sinh trong hệ thống.
   *
   * @return Mốc thời gian dưới dạng {@link Instant} (UTC).
   */
  Instant getOccurredAt();
}
