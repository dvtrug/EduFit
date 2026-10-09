package vn.edufit.scheduling.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import vn.edufit.scheduling.api.event.SessionScheduledEvent;
import vn.edufit.scheduling.application.service.RespondToProposalService;
import vn.edufit.scheduling.domain.model.SessionMode;
import vn.edufit.scheduling.domain.model.SessionStatus;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.repository.SessionHistoryDomainRepository;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.ResourceConflictException;

/**
 * Unit Test cho {@link RespondToProposalService} (UC4.3).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test Service: Phản hồi đề xuất buổi học (RespondToProposalService)")
class RespondToProposalServiceTest {

  @Mock
  private TutoringSessionDomainRepository sessionRepository;

  @Mock
  private SessionHistoryDomainRepository historyRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private Clock fixedClock;
  private RespondToProposalService respondToProposalService;

  private final UUID sessionId = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final UUID tutorId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final Instant fixedNow = Instant.parse("2026-10-25T08:00:00Z");

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    respondToProposalService = new RespondToProposalService(
        sessionRepository,
        historyRepository,
        eventPublisher,
        fixedClock
    );
  }

  @Test
  @DisplayName("Chấp nhận đề xuất thành công (ACCEPT) -> Chuyển sang SCHEDULED và phát sự kiện")
  void shouldAcceptProposalSuccessfully() {
    Instant startAt = fixedNow.plus(2, ChronoUnit.DAYS);
    Instant endAt = startAt.plus(90, ChronoUnit.MINUTES);
    Instant expiresAt = startAt.minus(2, ChronoUnit.HOURS);

    TutoringSession proposedSession = TutoringSession.createProposed(
        sessionId,
        classId,
        tutorId,
        studentId,
        startAt,
        endAt,
        SessionMode.ONLINE,
        null,
        null,
        null,
        studentId, // Học sinh đề xuất
        expiresAt,
        fixedNow.minus(1, ChronoUnit.HOURS)
    );

    when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(proposedSession));
    when(sessionRepository.findTutorBusySlots(tutorId, startAt, endAt)).thenReturn(Collections.emptyList());
    when(sessionRepository.save(any(TutoringSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Gia sư chấp thuận
    TutoringSession scheduledSession = respondToProposalService.respondToProposal(
        sessionId,
        tutorId,
        true,
        null
    );

    assertNotNull(scheduledSession);
    assertEquals(SessionStatus.SCHEDULED, scheduledSession.getStatus());
    assertEquals(tutorId, scheduledSession.getRespondedBy());

    verify(sessionRepository).save(any(TutoringSession.class));
    verify(historyRepository).save(any());
    verify(eventPublisher).publishEvent(any(SessionScheduledEvent.class));
  }

  @Test
  @DisplayName("Từ chối đề xuất thành công (REJECT) -> Chuyển sang REJECTED và lưu lý do")
  void shouldRejectProposalSuccessfully() {
    Instant startAt = fixedNow.plus(2, ChronoUnit.DAYS);
    Instant endAt = startAt.plus(90, ChronoUnit.MINUTES);
    Instant expiresAt = startAt.minus(2, ChronoUnit.HOURS);

    TutoringSession proposedSession = TutoringSession.createProposed(
        sessionId,
        classId,
        tutorId,
        studentId,
        startAt,
        endAt,
        SessionMode.ONLINE,
        null,
        null,
        null,
        studentId,
        expiresAt,
        fixedNow.minus(1, ChronoUnit.HOURS)
    );

    when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(proposedSession));
    when(sessionRepository.save(any(TutoringSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TutoringSession rejectedSession = respondToProposalService.respondToProposal(
        sessionId,
        tutorId,
        false,
        "Thầy bận đột xuất vào giờ này"
    );

    assertEquals(SessionStatus.REJECTED, rejectedSession.getStatus());
    assertEquals("Thầy bận đột xuất vào giờ này", rejectedSession.getResponseReason());
    verify(sessionRepository).save(any(TutoringSession.class));
  }

  @Test
  @DisplayName("Ném ForbiddenOperationException nếu người đề xuất cố tình tự chấp thuận đề xuất của mình")
  void shouldThrowForbiddenWhenProposerTriesToRespond() {
    Instant startAt = fixedNow.plus(2, ChronoUnit.DAYS);
    Instant endAt = startAt.plus(90, ChronoUnit.MINUTES);
    Instant expiresAt = startAt.minus(2, ChronoUnit.HOURS);

    TutoringSession proposedSession = TutoringSession.createProposed(
        sessionId,
        classId,
        tutorId,
        studentId,
        startAt,
        endAt,
        SessionMode.ONLINE,
        null,
        null,
        null,
        studentId,
        expiresAt,
        fixedNow.minus(1, ChronoUnit.HOURS)
    );

    when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(proposedSession));

    assertThrows(ForbiddenOperationException.class, () ->
        respondToProposalService.respondToProposal(
            sessionId,
            studentId, // Chính học sinh cố tự accept
            true,
            null
        )
    );
  }

  @Test
  @DisplayName("Bẫy lỗi GiST constraint khi accept và ném SCHEDULE_OVERLAP (Tầng 2)")
  void shouldCatchExclusionConstraintViolationOnAccept() {
    Instant startAt = fixedNow.plus(2, ChronoUnit.DAYS);
    Instant endAt = startAt.plus(90, ChronoUnit.MINUTES);
    Instant expiresAt = startAt.minus(2, ChronoUnit.HOURS);

    TutoringSession proposedSession = TutoringSession.createProposed(
        sessionId,
        classId,
        tutorId,
        studentId,
        startAt,
        endAt,
        SessionMode.ONLINE,
        null,
        null,
        null,
        studentId,
        expiresAt,
        fixedNow.minus(1, ChronoUnit.HOURS)
    );

    when(sessionRepository.findByIdForUpdate(sessionId)).thenReturn(Optional.of(proposedSession));
    when(sessionRepository.findTutorBusySlots(tutorId, startAt, endAt)).thenReturn(Collections.emptyList());
    when(sessionRepository.save(any(TutoringSession.class)))
        .thenThrow(new DataIntegrityViolationException("ex_tutoring_session_no_tutor_overlap"));

    ResourceConflictException ex = assertThrows(ResourceConflictException.class, () ->
        respondToProposalService.respondToProposal(
            sessionId,
            tutorId,
            true,
            null
        )
    );

    assertEquals(ErrorCode.SCHEDULE_OVERLAP, ex.getErrorCode());
  }
}
