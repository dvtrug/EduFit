package vn.edufit.connection.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.application.dto.*;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.infra.persistence.entity.*;
import vn.edufit.connection.infra.persistence.repository.*;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.notification.NotificationSink;
import vn.edufit.shared.exception.*;

@Service
public class ConnectionService {
  private final ConnectionRequestRepository requests;
  private final TutoringClassRepository classes;
  private final ProfileFacade profile;
  private final ConnectionLock lock;
  private final NotificationSink notifications;
  private final ConnectionFacade facade;

  public ConnectionService(ConnectionRequestRepository requests, TutoringClassRepository classes, ProfileFacade profile, ConnectionLock lock, NotificationSink notifications, ConnectionFacade facade) {
    this.requests = requests;
    this.classes = classes;
    this.profile = profile;
    this.lock = lock;
    this.notifications = notifications;
    this.facade = facade;
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private static void pending(String status) {
    if (!"PENDING".equals(status)) throw new ResourceConflictException("Không còn ở trạng thái chờ xử lý");
  }

  private void requireStudentAccess(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("STUDENT") && profile.findStudentById(studentId)
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false)) return;
    if (actor.hasRole("PARENT") && facade.hasConfirmedParentLink(actor.getUserId(), studentId)) return;
    throw new ForbiddenOperationException("Không có quyền với học sinh này");
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public ConnectionRequestResult request(CurrentUser actor, UUID goalId, UUID tutorId,
      List<String> desiredSlots, String message) {
    requireRole(actor, "STUDENT", "PARENT");
    ConnectionGoalDto goal = profile.findGoalForConnection(goalId)
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", goalId));
    lock.lockStudent(goal.studentId());
    requireStudentAccess(actor, goal.studentId());
    if (!"ACTIVE".equals(goal.status())) throw new InvalidOperationException("Mục tiêu không còn hoạt động");
    if (message != null && message.length() > 500) throw new InvalidOperationException("Lời nhắn tối đa 500 ký tự");
    if (desiredSlots == null || desiredSlots.isEmpty()
        || desiredSlots.stream().anyMatch(s -> s == null || s.isBlank() || s.length() > 100))
      throw new InvalidOperationException("Cần chọn ít nhất một khung giờ");
    TutorSummaryDto tutor = profile.findTutorById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("Tutor", tutorId));
    if (!"VERIFIED".equals(tutor.status()) || !profile.tutorTeaches(tutorId,
        goal.subjectId(), goal.educationLevelId()))
      throw new InvalidOperationException("Gia sư không phù hợp mục tiêu");
    Instant now = Instant.now();
    requests.findByStudentIdAndStatus(goal.studentId(), "PENDING").forEach(r -> r.expire(now));
    requests.flush();
    if (requests.existsByStudentIdAndTutorIdAndStatus(goal.studentId(), tutorId, "PENDING"))
      throw new ResourceConflictException("Đã có yêu cầu chờ gia sư này");
    if (requests.findByStudentIdAndStatus(goal.studentId(), "PENDING").size() >= 5)
      throw new ResourceConflictException("Đã đạt giới hạn 5 yêu cầu chờ");
    if (classes.existsByStudentIdAndTutorIdAndStatus(goal.studentId(), tutorId, "ACTIVE"))
      throw new ResourceConflictException("Đã có lớp đang hoạt động");
    ConnectionRequest created = requests.save(new ConnectionRequest(goal.studentId(), tutorId, goalId,
        actor.getUserId(), desiredSlots, message, now));
    notifications.notify(tutor.userId(), goal.studentId(), "CONNECTION_REQUESTED",
        "Yêu cầu kết nối mới", "Bạn có yêu cầu dạy học mới", "CONNECTION_REQUEST", created.getId());
    return ConnectionRequestResult.from(created);
  }

  @Transactional
  public List<ConnectionRequestResult> requests(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("TUTOR")) {
      UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
      List<ConnectionRequest> result = requests.findByTutorIdOrderByCreatedAtDesc(tutorId);
      result.forEach(r -> r.expire(Instant.now()));
      return result.stream().map(ConnectionRequestResult::from).toList();
    }
    requireRole(actor, "STUDENT", "PARENT");
    UUID scope = actor.hasRole("STUDENT") ? profile.getStudentIdByUserId(actor.getUserId()) : studentId;
    if (scope == null) throw new InvalidOperationException("Cần chỉ định học sinh");
    requireStudentAccess(actor, scope);
    List<ConnectionRequest> result = requests.findByStudentIdOrderByCreatedAtDesc(scope);
    result.forEach(r -> r.expire(Instant.now()));
    return result.stream().map(ConnectionRequestResult::from).toList();
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public void cancelRequest(CurrentUser actor, UUID id) {
    requireRole(actor, "STUDENT", "PARENT");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    lock.lockStudent(initial.getStudentId());
    ConnectionRequest request = requests.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    requireStudentAccess(actor, request.getStudentId());
    request.expire(Instant.now());
    pending(request.getStatus());
    request.cancel(Instant.now());
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public ClassResult respondRequest(CurrentUser actor, UUID id, boolean accept, String reason) {
    requireRole(actor, "TUTOR");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
    if (!initial.getTutorId().equals(tutorId)) throw new ForbiddenOperationException("Không phải gia sư nhận yêu cầu");
    lock.lockStudent(initial.getStudentId());
    ConnectionRequest request = requests.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    Instant now = Instant.now();
    request.expire(now);
    pending(request.getStatus());
    if (!accept) {
      request.reject(reason, now);
      notifications.notify(request.getSentByUserId(), request.getStudentId(), "CONNECTION_REJECTED",
          "Yêu cầu đã bị từ chối", "Gia sư đã từ chối yêu cầu", "CONNECTION_REQUEST", id);
      return null;
    }
    if (classes.existsByStudentIdAndTutorIdAndStatus(request.getStudentId(), tutorId, "ACTIVE"))
      throw new ResourceConflictException("Đã có lớp đang hoạt động");
    if (!"VERIFIED".equals(profile.findTutorById(tutorId).map(TutorSummaryDto::status).orElse(null)))
      throw new InvalidOperationException("Gia sư chưa được xác minh");
    ConnectionGoalDto goal = profile.findGoalForConnection(request.getGoalId())
        .orElseThrow(() -> new InvalidOperationException("Mục tiêu không còn hợp lệ"));
    if (!"ACTIVE".equals(goal.status()) || !profile.tutorTeaches(tutorId,
        goal.subjectId(), goal.educationLevelId()))
      throw new InvalidOperationException("Mục tiêu hoặc chuyên môn gia sư đã thay đổi");
    request.accept(now);
    requests.flush();
    TutoringClass created = classes.save(new TutoringClass(request, now));
    notifications.notify(request.getSentByUserId(), request.getStudentId(), "CONNECTION_ACCEPTED",
        "Yêu cầu đã được chấp nhận", "Gia sư đã chấp nhận yêu cầu", "CLASS", created.getId());
    return ClassResult.from(created);
  }
}
