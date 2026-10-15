package vn.edufit.scheduling.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.scheduling.api.SessionFacade;
import vn.edufit.scheduling.api.dto.CompletedSessionStatsView;
import vn.edufit.scheduling.api.dto.SessionSummaryView;
import vn.edufit.scheduling.api.dto.TutorBusySlotView;
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
}
