package vn.edufit.connection.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionFacade {
  boolean hasConfirmedParentLink(UUID parentUserId, UUID studentId);
  Optional<ActiveClassView> findActiveClass(UUID studentId, UUID tutorId);
  boolean isClassActive(UUID classId);
  boolean isClassActive(UUID classId, UUID tutorId, UUID studentId);
  Optional<ActiveClassView> findActiveClassById(UUID classId);

  // ── Phương thức bổ sung cho module Progress (chỉ đọc) ────────────────────
  Optional<ClassDetailView> findClassById(UUID classId);
  List<ClassDetailView> findClassesByStudent(UUID studentId);
  List<ClassDetailView> findClassesByTutor(UUID tutorId);
  List<UUID> findConfirmedParentUserIds(UUID studentId);
  List<UUID> findConfirmedStudentIds(UUID parentUserId);

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
