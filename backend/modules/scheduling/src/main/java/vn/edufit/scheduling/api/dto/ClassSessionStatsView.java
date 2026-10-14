package vn.edufit.scheduling.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Thống kê buổi học theo từng lớp học — dùng cho batch query của module Progress.
 *
 * <p>Trả về đủ số liệu để hiển thị dashboard mà không cần N+1 query.
 *
 * @param classId            Mã lớp học.
 * @param totalCompleted     Số buổi COMPLETED.
 * @param totalAbsent        Số buổi ABSENT.
 * @param totalCancelled     Số buổi CANCELLED.
 * @param totalUpcoming      Số buổi SCHEDULED (sắp tới).
 * @param nextSessionAt      Thời điểm buổi học SCHEDULED gần nhất (null nếu không có).
 * @param lastSessionAt      Thời điểm buổi học gần nhất đã hoàn thành/vắng (null nếu không có).
 */
public record ClassSessionStatsView(
    UUID classId,
    int totalCompleted,
    int totalAbsent,
    int totalCancelled,
    int totalUpcoming,
    Instant nextSessionAt,
    Instant lastSessionAt
) {}
