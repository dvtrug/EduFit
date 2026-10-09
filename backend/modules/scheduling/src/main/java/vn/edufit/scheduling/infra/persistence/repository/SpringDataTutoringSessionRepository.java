package vn.edufit.scheduling.infra.persistence.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edufit.scheduling.infra.persistence.entity.TutoringSessionJpaEntity;

/**
 * Spring Data JPA Repository cho bảng {@code tutoring_session}.
 *
 * <p>Mục đích thiết kế & Cơ chế bảo vệ đồng thời:
 * <ul>
 *   <li><b>{@link #findByIdForUpdate(UUID)}:</b> Sử dụng {@link LockModeType#PESSIMISTIC_WRITE}
 *       để sinh câu lệnh SQL {@code SELECT ... FOR UPDATE}, khóa trực tiếp dòng bản ghi trong database,
 *       ngăn chặn Race Condition khi hai luồng cùng xác nhận hoặc cập nhật một buổi học (NFR-17).</li>
 * </ul>
 */
@Repository
public interface SpringDataTutoringSessionRepository extends JpaRepository<TutoringSessionJpaEntity, UUID> {

  /**
   * Khóa bi quan (Pessimistic Write Lock) trên dòng bản ghi khi thay đổi trạng thái buổi học.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM TutoringSessionJpaEntity s WHERE s.sessionId = :id")
  Optional<TutoringSessionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

  /**
   * Quét các đề xuất buổi học (PROPOSED) đã quá hạn phản hồi (FR-17).
   */
  @Query("""
      SELECT s FROM TutoringSessionJpaEntity s
      WHERE s.status = 'PROPOSED' AND s.expiresAt < :now
  """)
  List<TutoringSessionJpaEntity> findExpiredProposals(@Param("now") Instant now);

  /**
   * Tra cứu lịch học của một người dùng (Gia sư hoặc Học sinh) trong một khoảng thời gian.
   */
  @Query("""
      SELECT s FROM TutoringSessionJpaEntity s
      WHERE (s.tutorId = :userId OR s.studentId = :userId)
        AND s.startAt >= :from AND s.endAt <= :to
      ORDER BY s.startAt ASC
  """)
  List<TutoringSessionJpaEntity> findCalendarSessions(
      @Param("userId") UUID userId,
      @Param("from") Instant from,
      @Param("to") Instant to
  );

  /**
   * Lấy danh sách các khung giờ bận (SCHEDULED) của Gia sư để module discovery loại trừ.
   */
  @Query("""
      SELECT s FROM TutoringSessionJpaEntity s
      WHERE s.tutorId = :tutorId
        AND s.status = 'SCHEDULED'
        AND s.startAt >= :from AND s.endAt <= :to
      ORDER BY s.startAt ASC
  """)
  List<TutoringSessionJpaEntity> findTutorBusySlots(
      @Param("tutorId") UUID tutorId,
      @Param("from") Instant from,
      @Param("to") Instant to
  );

  /**
   * Đếm số buổi học COMPLETED của một lớp học.
   */
  @Query("SELECT COUNT(s) FROM TutoringSessionJpaEntity s WHERE s.classId = :classId AND s.status = 'COMPLETED'")
  int countCompletedSessions(@Param("classId") UUID classId);

  /**
   * Đếm số buổi học ABSENT của một lớp học.
   */
  @Query("SELECT COUNT(s) FROM TutoringSessionJpaEntity s WHERE s.classId = :classId AND s.status = 'ABSENT'")
  int countAbsentSessions(@Param("classId") UUID classId);

  /**
   * Lấy mốc thời gian hoàn thành buổi học gần nhất của lớp.
   */
  @Query("SELECT MAX(s.outcomeRecordedAt) FROM TutoringSessionJpaEntity s WHERE s.classId = :classId AND s.status = 'COMPLETED'")
  Optional<Instant> findLastCompletedAt(@Param("classId") UUID classId);

  /**
   * Đếm tổng số buổi học COMPLETED giữa một Gia sư và một Học sinh (BR-61).
   */
  @Query("""
      SELECT COUNT(s) FROM TutoringSessionJpaEntity s
      WHERE s.tutorId = :tutorId AND s.studentId = :studentId AND s.status = 'COMPLETED'
  """)
  int countCompletedSessionsBetween(@Param("tutorId") UUID tutorId, @Param("studentId") UUID studentId);

  /**
   * Kiểm tra xem Gia sư có buổi học SCHEDULED nào bị giao thoa dải thời gian hay không.
   */
  @Query("""
      SELECT COUNT(s) > 0 FROM TutoringSessionJpaEntity s
      WHERE s.tutorId = :tutorId
        AND s.status = 'SCHEDULED'
        AND s.startAt < :endAt AND s.endAt > :startAt
  """)
  boolean hasActiveSessionAt(
      @Param("tutorId") UUID tutorId,
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt
  );

  /**
   * Quét các buổi học sắp diễn ra để gửi thông báo nhắc nhở trước 24h hoặc 2h (FR-31).
   */
  @Query("""
      SELECT s FROM TutoringSessionJpaEntity s
      WHERE s.status = 'SCHEDULED'
        AND s.startAt >= :windowStart AND s.startAt < :windowEnd
  """)
  List<TutoringSessionJpaEntity> findUpcomingSessions(
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd
  );

  /**
   * Quét các buổi học đã kết thúc nhưng Gia sư chưa ghi nhận kết quả (FR-20).
   */
  @Query("""
      SELECT s FROM TutoringSessionJpaEntity s
      WHERE s.status = 'SCHEDULED'
        AND s.endAt < :cutoff
      ORDER BY s.endAt ASC
  """)
  List<TutoringSessionJpaEntity> findOverdueOutcomeSessions(@Param("cutoff") Instant cutoff);
}
