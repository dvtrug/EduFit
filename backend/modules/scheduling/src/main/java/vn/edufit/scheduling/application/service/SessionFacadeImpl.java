package vn.edufit.scheduling.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.scheduling.api.SessionFacade;
import vn.edufit.scheduling.api.dto.ClassSessionStatsView;
import vn.edufit.scheduling.api.dto.CompletedSessionStatsView;
import vn.edufit.scheduling.api.dto.SessionSummaryView;
import vn.edufit.scheduling.api.dto.SessionTimelineView;
import vn.edufit.scheduling.api.dto.TutorBusySlotView;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.policy.ConflictPolicy;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;

/**
 * Hiện thực hóa interface {@link SessionFacade} - Bề mặt công khai duy nhất cho các module khác.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionFacadeImpl implements SessionFacade {

  private final TutoringSessionDomainRepository sessionRepository;

  @Override
  public Optional<SessionSummaryView> findSessionById(UUID sessionId) {
    return sessionRepository.findById(sessionId)
        .map(s -> new SessionSummaryView(
            s.getSessionId(),
            s.getClassId(),
            s.getTutorId(),
            s.getStudentId(),
            s.getStartAt(),
            s.getEndAt(),
            s.getMode().name(),
            s.getStatus().name(),
            s.isLateCancel(),
            s.getCreatedAt()
        ));
  }

  @Override
  public List<TutorBusySlotView> findBusySlotsByTutorId(UUID tutorId, Instant from, Instant to) {
    return sessionRepository.findTutorBusySlots(tutorId, from, to)
        .stream()
        .map(s -> new TutorBusySlotView(
            s.getSessionId(),
            s.getTutorId(),
            s.getStartAt(),
            s.getEndAt()
        ))
        .toList();
  }

  @Override
  public CompletedSessionStatsView getCompletedStatsByClassId(UUID classId) {
    int count = sessionRepository.countCompletedSessions(classId);
    return new CompletedSessionStatsView(classId, null, null, count, 0, null);
  }

  @Override
  public int countCompletedSessionsBetween(UUID tutorId, UUID studentId) {
    return sessionRepository.countCompletedSessionsBetween(tutorId, studentId);
  }

  @Override
  public boolean hasActiveSessionAt(UUID tutorId, Instant startAt, Instant endAt) {
    return sessionRepository.findTutorBusySlots(tutorId, startAt, endAt)
        .stream()
        .anyMatch(s -> ConflictPolicy.isOverlapping(startAt, endAt, s.getStartAt(), s.getEndAt()));
  }

  // ── Progress batch methods ────────────────────────────────────────────────

  @Override
  public List<SessionTimelineView> findSessionsByClassId(UUID classId, Instant from, Instant to) {
    return sessionRepository.findByClassId(classId, from, to)
        .stream()
        .map(s -> new SessionTimelineView(
            s.getSessionId(),
            s.getClassId(),
            s.getStartAt(),
            s.getEndAt(),
            s.getStatus().name(),
            null  // noteText được lấy từ module Progress (session_note table), không từ Scheduling
        ))
        .toList();
  }

  @Override
  public Map<UUID, ClassSessionStatsView> getSessionStatsByClassIds(
      List<UUID> classIds, Instant from, Instant to
  ) {
    if (classIds == null || classIds.isEmpty()) return Map.of();

    List<TutoringSession> sessions = sessionRepository.findByClassIdIn(classIds, from, to);

    // Nhóm theo classId và tính thống kê trong memory — 1 query duy nhất
    Map<UUID, List<TutoringSession>> grouped = sessions.stream()
        .collect(Collectors.groupingBy(TutoringSession::getClassId));

    return classIds.stream().collect(Collectors.toMap(
        classId -> classId,
        classId -> {
          List<TutoringSession> cls = grouped.getOrDefault(classId, List.of());
          long completed  = cls.stream().filter(s -> "COMPLETED".equals(s.getStatus().name())).count();
          long absent     = cls.stream().filter(s -> "ABSENT".equals(s.getStatus().name())).count();
          long cancelled  = cls.stream().filter(s -> "CANCELLED".equals(s.getStatus().name())).count();
          long upcoming   = cls.stream().filter(s -> "SCHEDULED".equals(s.getStatus().name())).count();
          Instant nextAt  = cls.stream()
              .filter(s -> "SCHEDULED".equals(s.getStatus().name()))
              .map(TutoringSession::getStartAt)
              .min(Instant::compareTo)
              .orElse(null);
          Instant lastAt  = cls.stream()
              .filter(s -> "COMPLETED".equals(s.getStatus().name()) || "ABSENT".equals(s.getStatus().name()))
              .map(TutoringSession::getStartAt)
              .max(Instant::compareTo)
              .orElse(null);
          return new ClassSessionStatsView(classId,
              (int) completed, (int) absent, (int) cancelled, (int) upcoming, nextAt, lastAt);
        }
    ));
  }
}
