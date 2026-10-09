package vn.edufit.scheduling.infra.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;
import vn.edufit.scheduling.infra.persistence.entity.TutoringSessionJpaEntity;
import vn.edufit.scheduling.infra.persistence.mapper.SessionMapper;
import vn.edufit.scheduling.infra.persistence.repository.SpringDataTutoringSessionRepository;

/**
 * Repository Adapter hiện thực hóa interface {@link TutoringSessionDomainRepository} của tầng Domain.
 *
 * <p>Theo nguyên lý Dependency Inversion của Clean Architecture:
 * Class này đóng vai trò là một Outbound Adapter, kết nối giữa nhu cầu lưu trữ của Domain
 * và công nghệ Spring Data JPA bên dưới.
 */
@Component
@RequiredArgsConstructor
public class TutoringSessionRepositoryAdapter implements TutoringSessionDomainRepository {

  private final SpringDataTutoringSessionRepository springDataRepository;
  private final SessionMapper sessionMapper;

  @Override
  public Optional<TutoringSession> findById(UUID sessionId) {
    return springDataRepository.findById(sessionId)
        .map(sessionMapper::toDomain);
  }

  @Override
  public Optional<TutoringSession> findByIdForUpdate(UUID sessionId) {
    return springDataRepository.findByIdForUpdate(sessionId)
        .map(sessionMapper::toDomain);
  }

  @Override
  public TutoringSession save(TutoringSession session) {
    TutoringSessionJpaEntity entity = sessionMapper.toJpaEntity(session);
    TutoringSessionJpaEntity saved = springDataRepository.save(entity);
    return sessionMapper.toDomain(saved);
  }

  @Override
  public List<TutoringSession> findExpiredProposals(Instant now) {
    return springDataRepository.findExpiredProposals(now)
        .stream()
        .map(sessionMapper::toDomain)
        .toList();
  }

  @Override
  public List<TutoringSession> findCalendarSessions(UUID userId, Instant from, Instant to) {
    return springDataRepository.findCalendarSessions(userId, from, to)
        .stream()
        .map(sessionMapper::toDomain)
        .toList();
  }

  @Override
  public List<TutoringSession> findTutorBusySlots(UUID tutorId, Instant from, Instant to) {
    return springDataRepository.findTutorBusySlots(tutorId, from, to)
        .stream()
        .map(sessionMapper::toDomain)
        .toList();
  }

  @Override
  public int countCompletedSessions(UUID classId) {
    return springDataRepository.countCompletedSessions(classId);
  }

  @Override
  public int countCompletedSessionsBetween(UUID tutorId, UUID studentId) {
    return springDataRepository.countCompletedSessionsBetween(tutorId, studentId);
  }
}
