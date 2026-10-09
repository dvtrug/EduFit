package vn.edufit.scheduling.web;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edufit.scheduling.application.service.CancelSessionService;
import vn.edufit.scheduling.application.service.GetCalendarService;
import vn.edufit.scheduling.application.service.ProposeSessionService;
import vn.edufit.scheduling.application.service.RecordSessionOutcomeService;
import vn.edufit.scheduling.application.service.RescheduleSessionService;
import vn.edufit.scheduling.application.service.RespondToProposalService;
import vn.edufit.scheduling.domain.model.SessionRescheduleProposal;
import vn.edufit.scheduling.domain.model.TutoringSession;
import vn.edufit.scheduling.domain.repository.TutoringSessionDomainRepository;
import vn.edufit.scheduling.web.request.CancelSessionRequest;
import vn.edufit.scheduling.web.request.ProposeSessionRequest;
import vn.edufit.scheduling.web.request.RecordOutcomeRequest;
import vn.edufit.scheduling.web.request.RescheduleSessionRequest;
import vn.edufit.scheduling.web.request.RespondProposalRequest;
import vn.edufit.scheduling.web.request.RespondRescheduleRequest;
import vn.edufit.scheduling.web.response.CalendarItemResponse;
import vn.edufit.scheduling.web.response.RescheduleProposalResponse;
import vn.edufit.scheduling.web.response.SessionResponse;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.response.ApiResponse;

/**
 * REST Controller cho phân hệ Scheduling (/api/v1/scheduling).
 *
 * <p>Cung cấp các API:
 * <ul>
 *   <li>{@code POST /sessions/propose}: Đề xuất lịch học mới (UC4.1).</li>
 *   <li>{@code POST /sessions/{id}/respond}: Chấp nhận / Từ chối đề xuất (UC4.3).</li>
 *   <li>{@code POST /sessions/{id}/reschedule}: Yêu cầu đổi lịch (UC4.4).</li>
 *   <li>{@code POST /sessions/reschedule-proposals/{proposalId}/respond}: Phản hồi đổi lịch (UC4.4).</li>
 *   <li>{@code POST /sessions/{id}/cancel}: Hủy buổi học đã lên lịch (UC4.5).</li>
 *   <li>{@code POST /sessions/{id}/outcome}: Ghi nhận kết quả và biên bản (UC4.6).</li>
 *   <li>{@code GET /sessions/{id}}: Chi tiết buổi học.</li>
 *   <li>{@code GET /calendar}: Tra cứu lịch học theo khoảng thời gian.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
public class SessionController {

  private final ProposeSessionService proposeSessionService;
  private final RespondToProposalService respondToProposalService;
  private final RescheduleSessionService rescheduleSessionService;
  private final CancelSessionService cancelSessionService;
  private final RecordSessionOutcomeService recordSessionOutcomeService;
  private final GetCalendarService getCalendarService;
  private final TutoringSessionDomainRepository sessionRepository;
  private final ObjectProvider<CurrentUser> currentUserProvider;

  @PostMapping("/sessions/propose")
  public ResponseEntity<ApiResponse<SessionResponse>> proposeSession(
      @Valid @RequestBody ProposeSessionRequest req
  ) {
    TutoringSession session = proposeSessionService.proposeSession(
        req.classId(),
        req.tutorId(),
        req.studentId(),
        req.startAt(),
        req.endAt(),
        req.mode(),
        req.placeOrLink(),
        req.repeatNote(),
        req.message(),
        currentUser().getUserId()
    );
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(SessionResponse.from(session), "Đề xuất lịch học thành công"));
  }

  @PostMapping("/sessions/{id}/respond")
  public ResponseEntity<ApiResponse<SessionResponse>> respondToProposal(
      @PathVariable("id") UUID id,
      @Valid @RequestBody RespondProposalRequest req
  ) {
    TutoringSession session = respondToProposalService.respondToProposal(
        id,
        currentUser().getUserId(),
        req.accept(),
        req.reason()
    );
    return ResponseEntity.ok(ApiResponse.success(SessionResponse.from(session), "Phản hồi đề xuất lịch học thành công"));
  }

  @PostMapping("/sessions/{id}/reschedule")
  public ResponseEntity<ApiResponse<RescheduleProposalResponse>> proposeReschedule(
      @PathVariable("id") UUID id,
      @Valid @RequestBody RescheduleSessionRequest req
  ) {
    SessionRescheduleProposal proposal = rescheduleSessionService.proposeReschedule(
        id,
        currentUser().getUserId(),
        req.newStartAt(),
        req.newEndAt(),
        req.reason()
    );
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(RescheduleProposalResponse.from(proposal), "Tạo đề xuất đổi lịch thành công"));
  }

  @PostMapping("/sessions/reschedule-proposals/{proposalId}/respond")
  public ResponseEntity<ApiResponse<RescheduleProposalResponse>> respondToReschedule(
      @PathVariable("proposalId") UUID proposalId,
      @Valid @RequestBody RespondRescheduleRequest req
  ) {
    SessionRescheduleProposal proposal = rescheduleSessionService.respondToReschedule(
        proposalId,
        currentUser().getUserId(),
        req.accept(),
        req.responseReason()
    );
    return ResponseEntity.ok(
        ApiResponse.success(RescheduleProposalResponse.from(proposal), "Phản hồi đề xuất đổi lịch thành công")
    );
  }

  @PostMapping("/sessions/{id}/cancel")
  public ResponseEntity<ApiResponse<SessionResponse>> cancelSession(
      @PathVariable("id") UUID id,
      @Valid @RequestBody CancelSessionRequest req
  ) {
    TutoringSession session = cancelSessionService.cancelSession(
        id,
        currentUser().getUserId(),
        req.reason(),
        req.comment()
    );
    return ResponseEntity.ok(ApiResponse.success(SessionResponse.from(session), "Hủy buổi học thành công"));
  }

  @PostMapping("/sessions/{id}/outcome")
  public ResponseEntity<ApiResponse<SessionResponse>> recordOutcome(
      @PathVariable("id") UUID id,
      @Valid @RequestBody RecordOutcomeRequest req
  ) {
    TutoringSession session = recordSessionOutcomeService.recordOutcome(
        id,
        currentUser().getUserId(),
        req.outcome(),
        req.noteContent()
    );
    return ResponseEntity.ok(ApiResponse.success(SessionResponse.from(session), "Ghi nhận kết quả buổi học thành công"));
  }

  @GetMapping("/sessions/{id}")
  public ResponseEntity<ApiResponse<SessionResponse>> getSessionDetail(
      @PathVariable("id") UUID id
  ) {
    TutoringSession session = sessionRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy buổi học với ID: " + id));
    return ResponseEntity.ok(ApiResponse.success(SessionResponse.from(session), "Lấy thông tin buổi học thành công"));
  }

  @GetMapping("/calendar")
  public ResponseEntity<ApiResponse<List<CalendarItemResponse>>> getCalendar(
      @RequestParam("from") Instant from,
      @RequestParam("to") Instant to
  ) {
    List<TutoringSession> sessions = getCalendarService.getCalendarSessions(currentUser().getUserId(), from, to);
    List<CalendarItemResponse> responseList = sessions.stream()
        .map(CalendarItemResponse::from)
        .toList();
    return ResponseEntity.ok(ApiResponse.success(responseList, "Lấy danh sách lịch học thành công"));
  }

  private CurrentUser currentUser() {
    CurrentUser currentUser = currentUserProvider.getIfAvailable();
    if (currentUser == null || currentUser.getUserId() == null) {
      throw new InvalidOperationException(ErrorCode.UNAUTHORIZED, "Phiên làm việc chưa được xác thực hoặc đã hết hạn");
    }
    return currentUser;
  }
}
