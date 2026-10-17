package vn.edufit.scheduling.application.listener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.event.TutoringClassEndedEvent;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassEndedEventListener {

  private final TutoringSessionDomainRepository sessionRepository;
  private final Clock clock;

  @EventListener
  @Transactional
  public void onClassEnded(TutoringClassEndedEvent event) {
    Instant now = clock.instant();
    log.info("Xử lý sự kiện lớp học kết thúc (Cascade Cancellation): classId={}, reason={}", event.classId(), event.reason());

    List<TutoringSession> futureSessions = sessionRepository.findByClassId(event.classId(), now, null);
    for (TutoringSession session : futureSessions) {
      if (!session.getStatus().isTerminal()) {
        session.cancelDueToClassEnded(event.tutorId(), event.reason(), now);
        sessionRepository.save(session);
        log.info("Đã tự động cascade hủy buổi học: sessionId={}, classId={}", session.getSessionId(), event.classId());
      }
    }
  }
}
