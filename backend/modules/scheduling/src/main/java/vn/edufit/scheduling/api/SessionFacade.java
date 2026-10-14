package vn.edufit.scheduling.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import vn.edufit.scheduling.api.dto.ClassSessionStatsView;
import vn.edufit.scheduling.api.dto.CompletedSessionStatsView;
import vn.edufit.scheduling.api.dto.SessionSummaryView;
import vn.edufit.scheduling.api.dto.SessionTimelineView;
import vn.edufit.scheduling.api.dto.TutorBusySlotView;

/**
 * Bề mặt công khai duy nhất (Public Facade Interface) của module Scheduling theo chuẩn Modular Monolith (ADR-002).
 *
 * <p>Quy tắc bất biến:
 * <ul>
 *   <li>Các module Java khác trong hệ thống (như {@code discovery}, {@code progress}, {@code review})
 *       <b>CHỈ ĐƯỢC PHÉP</b> giao tiếp với module {@code scheduling} thông qua Interface này.</li>
 *   <li>Tuyệt đối không cho phép module ngoài inject trực tiếp Repository hoặc JPA Entity nội bộ.</li>
 *   <li>Mọi dữ liệu trả về đều sử dụng các immutable Java Record DTO an toàn.</li>
 * </ul>
 */
public interface SessionFacade {

  /**
   * Tra cứu thông tin tóm tắt an toàn của một buổi học theo mã định danh.
   *
   * @param sessionId Mã buổi học.
   * @return {@link Optional} chứa {@link SessionSummaryView} nếu tìm thấy.
   */
  Optional<SessionSummaryView> findSessionById(UUID sessionId);

  /**
   * Lấy danh sách các khung giờ bận (đã chốt lịch SCHEDULED) của Gia sư trong một khoảng thời gian.
   *
   * <p>Được module {@code discovery} gọi để loại trừ các slot giờ trùng khi phụ huynh/học sinh lọc gia sư.
   *
   * @param tutorId Mã hồ sơ gia sư.
   * @param from    Thời điểm bắt đầu khoảng tìm kiếm (UTC).
   * @param to      Thời điểm kết thúc khoảng tìm kiếm (UTC).
   * @return Danh sách các khung giờ bận {@link TutorBusySlotView}.
   */
  List<TutorBusySlotView> findBusySlotsByTutorId(UUID tutorId, Instant from, Instant to);

  /**
   * Lấy số liệu thống kê các buổi học đã hoàn thành theo lớp học.
   *
   * <p>Được module {@code progress} và {@code review} gọi để kiểm tra điều kiện đánh giá (BR-61).
   *
   * @param classId Mã lớp học.
   * @return Đối tượng {@link CompletedSessionStatsView}.
   */
  CompletedSessionStatsView getCompletedStatsByClassId(UUID classId);

  /**
   * Đếm tổng số buổi học đã hoàn thành (COMPLETED) giữa một Gia sư và một Học sinh cụ thể.
   *
   * @param tutorId   Mã gia sư.
   * @param studentId Mã học sinh.
   * @return Tổng số buổi học COMPLETED.
   */
  int countCompletedSessionsBetween(UUID tutorId, UUID studentId);

  /**
   * Kiểm tra xem Gia sư có buổi học nào đã được lên lịch (SCHEDULED) trong khoảng thời gian này hay không.
   *
   * @param tutorId Mã gia sư.
   * @param startAt Thời điểm bắt đầu (UTC).
   * @param endAt   Thời điểm kết thúc (UTC).
   * @return {@code true} nếu có buổi học SCHEDULED bị trùng.
   */
  boolean hasActiveSessionAt(UUID tutorId, Instant startAt, Instant endAt);

  // ── Phương thức bổ sung cho module Progress ─────────────────────────────

  /**
   * Lấy danh sách buổi học của một lớp trong khoảng thời gian cho timeline hiển thị.
   * Dùng cho endpoint GET /progress/classes/{classId}/notes.
   *
   * @param classId Mã lớp học.
   * @param from    Thời điểm bắt đầu (UTC), null = không giới hạn từ.
   * @param to      Thời điểm kết thúc (UTC), null = không giới hạn đến.
   * @return Danh sách buổi học sắp xếp giảm dần theo startAt.
   */
  List<SessionTimelineView> findSessionsByClassId(UUID classId, Instant from, Instant to);

  /**
   * Lấy số liệu thống kê buổi học theo batch cho nhiều lớp học cùng lúc.
   * Tránh N+1 query khi Progress cần stats của nhiều lớp (dashboard/teaching history).
   *
   * @param classIds Danh sách mã lớp học.
   * @param from     Thời điểm bắt đầu khoảng thống kê (UTC), null = toàn bộ.
   * @param to       Thời điểm kết thúc khoảng thống kê (UTC), null = toàn bộ.
   * @return Map từ classId → {@link ClassSessionStatsView}.
   */
  Map<UUID, ClassSessionStatsView> getSessionStatsByClassIds(List<UUID> classIds, Instant from, Instant to);
}

