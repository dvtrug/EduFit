package vn.edufit.connection.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bề mặt công khai của module Connection (ADR-002).
 * Module Progress và các module khác chỉ được tương tác qua interface này.
 */
public interface ConnectionFacade {

  // ── Phương thức hiện có ────────────────────────────────────────────────────

  boolean hasConfirmedParentLink(UUID parentUserId, UUID studentId);

  Optional<ActiveClassView> findActiveClass(UUID studentId, UUID tutorId);

  // ── Phương thức bổ sung cho module Progress (chỉ đọc) ────────────────────

  /**
   * Tìm thông tin chi tiết một lớp học theo classId.
   * Progress dùng để xác minh quyền Tutor và lấy studentId/goalId.
   */
  Optional<ClassDetailView> findClassById(UUID classId);

  /**
   * Lấy danh sách tất cả lớp học (mọi trạng thái) của một học sinh.
   * Progress dùng để xây dựng danh sách lớp trong Student/Parent dashboard.
   */
  List<ClassDetailView> findClassesByStudent(UUID studentId);

  /**
   * Lấy danh sách tất cả lớp học (mọi trạng thái) của một gia sư.
   * Progress dùng để xây dựng Tutor Teaching History.
   */
  List<ClassDetailView> findClassesByTutor(UUID tutorId);

  /**
   * Lấy danh sách userId của tất cả Phụ huynh đang liên kết CONFIRMED với học sinh.
   * Progress dùng để gửi notification khi milestone đạt được.
   */
  List<UUID> findConfirmedParentUserIds(UUID studentId);

  // ── Immutable Java Record DTOs ────────────────────────────────────────────

  record ActiveClassView(UUID classId, UUID studentId, UUID tutorId, UUID goalId) {}

  record ClassDetailView(
      UUID classId,
      UUID studentId,
      UUID tutorId,
      UUID goalId,
      String status,
      Instant startedAt,
      Instant endedAt
  ) {}
}
