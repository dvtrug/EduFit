package vn.edufit.scheduling.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Thông tin tóm tắt của một buổi học để hiển thị trong timeline của module Progress.
 *
 * @param sessionId     Mã buổi học.
 * @param classId       Mã lớp học.
 * @param startAt       Thời điểm bắt đầu.
 * @param endAt         Thời điểm kết thúc.
 * @param status        Trạng thái buổi học (COMPLETED, ABSENT, CANCELLED, SCHEDULED...).
 * @param noteText      Nội dung ghi chú (có thể null nếu chưa ghi hoặc outcome chưa xác nhận).
 */
public record SessionTimelineView(
    UUID sessionId,
    UUID classId,
    Instant startAt,
    Instant endAt,
    String status,
    String noteText
) {}
