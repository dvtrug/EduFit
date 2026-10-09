package vn.edufit.scheduling.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;

/**
 * Service xử lý tra cứu lịch học (Calendar View) cho người dùng trong một khoảng thời gian.
 */
@Service
@RequiredArgsConstructor
public class GetCalendarService {

  private final TutoringSessionDomainRepository sessionRepository;

  @Transactional(readOnly = true)
  public List<TutoringSession> getCalendarSessions(UUID userId, Instant from, Instant to) {
    return sessionRepository.findCalendarSessions(userId, from, to);
  }
}
