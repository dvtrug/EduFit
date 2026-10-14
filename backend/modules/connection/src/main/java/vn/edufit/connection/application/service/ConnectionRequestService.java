package vn.edufit.connection.application.service;

import java.time.Instant;
import java.time.Clock;
import vn.edufit.connection.application.dto.TransitionResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.application.policy.ConnectionRequestInput;
import vn.edufit.connection.application.dto.ConnectionRequestResult;
import vn.edufit.connection.application.dto.ClassResult;
import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.infra.persistence.entity.ConnectionRequest;
import vn.edufit.connection.infra.persistence.entity.TutoringClass;
import vn.edufit.connection.infra.persistence.repository.ConnectionRequestRepository;
import vn.edufit.connection.infra.persistence.repository.TutoringClassRepository;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.notification.NotificationSink;
import org.springframework.context.ApplicationEventPublisher;
import vn.edufit.connection.api.event.ConnectionRequestChangedEvent;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class ConnectionRequestService {
  private final Clock clock;
  private final ConnectionRequestRepository requests;
  private final TutoringClassRepository classes;
  private final ProfileFacade profile;
  private final IamFacade iam;
  private final ConnectionLock lock;
  private final NotificationSink notifications;
  private final ConnectionFacade facade;
  private final ConnectionRequestInput input;
  private final ApplicationEventPublisher events;

  public ConnectionRequestService(ConnectionRequestRepository requests, TutoringClassRepository classes,
      ProfileFacade profile, IamFacade iam, ConnectionLock lock, NotificationSink notifications,
      ConnectionFacade facade, ConnectionRequestInput input, ApplicationEventPublisher events, Clock clock) {
    this.clock = clock;
    this.requests = requests;
    this.classes = classes;
    this.profile = profile;
    this.iam = iam;
    this.lock = lock;
    this.notifications = notifications;
    this.facade = facade;
    this.input = input;
    this.events = events;
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private void requireActiveActor(CurrentUser actor) {
    AccountRole expected = actor.hasRole("TUTOR") ? AccountRole.TUTOR
        : actor.hasRole("PARENT") ? AccountRole.PARENT : AccountRole.STUDENT;
    iam.findUserSummaryById(actor.getUserId())
        .filter(user -> user.id().equals(actor.getUserId()) && user.status() == AccountStatus.ACTIVE && user.role() == expected)
        .orElseThrow(() -> new ForbiddenOperationException("Tài khoản không còn quyền thực hiện thao tác"));
  }

  private void requireStudentAccess(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("STUDENT") && profile.findStudentById(studentId)
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false)) return;
    if (actor.hasRole("PARENT") && facade.hasConfirmedParentLink(actor.getUserId(), studentId)) return;
    throw new ForbiddenOperationException("Không có quyền với học sinh này");
  }

  @Transactional
  public TransitionResult<ConnectionRequestResult> request(CurrentUser actor, UUID goalId, UUID tutorId,
      List<String> desiredSlots, String message) {
    requireRole(actor, "STUDENT", "PARENT");
    requireActiveActor(actor);
    ConnectionGoalDto goal = profile.findGoalForConnection(goalId)
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", goalId));
    lock.lockStudent(goal.studentId());
    requireStudentAccess(actor, goal.studentId());
    if (!goalId.equals(goal.goalId()) || profile.findStudentById(goal.studentId())
        .filter(student -> goal.studentId().equals(student.studentId()) && goal.studentUserId().equals(student.userId())).isEmpty())
      throw new InvalidOperationException("Tài khoản sở hữu mục tiêu không còn hợp lệ");
    if (!"ACTIVE".equals(goal.status())) throw new InvalidOperationException("Mục tiêu không còn hoạt động");
    input.validate(desiredSlots, message);
    TutorSummaryDto tutor = profile.findTutorById(tutorId)
        .orElseThrow(() -> EntityNotFoundException.of("Tutor", tutorId));
    if (!tutorId.equals(tutor.tutorId()) || !"VERIFIED".equals(tutor.status())
        || iam.findUserSummaryById(tutor.userId()).filter(user -> tutor.userId().equals(user.id())
            && user.role() == AccountRole.TUTOR && user.status() == AccountStatus.ACTIVE).isEmpty()
        || !profile.tutorTeaches(tutorId,
        goal.subjectId(), goal.educationLevelId()))
      throw new InvalidOperationException("Gia sư không phù hợp mục tiêu");
    Instant now = clock.instant();
    requests.findByStudentIdAndStatus(goal.studentId(), "PENDING").forEach(r -> r.expire(now));
    requests.flush();
    if (requests.existsByStudentIdAndTutorIdAndStatus(goal.studentId(), tutorId, "PENDING"))
      return TransitionResult.conflict("PENDING", "Đã có yêu cầu chờ gia sư này");
    if (requests.countByStudentIdAndStatus(goal.studentId(), "PENDING") >= 5)
      return TransitionResult.conflict("PENDING", "Đã đạt giới hạn 5 yêu cầu chờ");
    if (classes.existsByStudentIdAndTutorIdAndStatus(goal.studentId(), tutorId, "ACTIVE"))
      return TransitionResult.conflict("ACTIVE", "Đã có lớp đang hoạt động");
    ConnectionRequest created = requests.save(new ConnectionRequest(goal.studentId(), tutorId, goalId,
        actor.getUserId(), desiredSlots, message, now));
    events.publishEvent(new ConnectionRequestChangedEvent(UUID.randomUUID(), created.getId(),
        actor.getUserId(), goal.studentId(), null, "PENDING", now,
        actor.getUserId(), goal.studentUserId(), tutor.userId(), null));
    return TransitionResult.success(ConnectionRequestResult.from(created));
  }

  @Transactional
  public TransitionResult<Void> cancelRequest(CurrentUser actor, UUID id) {
    requireRole(actor, "STUDENT", "PARENT");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    lock.lockStudent(initial.getStudentId());
    ConnectionRequest request = requests.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    requireActiveActor(actor);
    requireStudentAccess(actor, request.getStudentId());
    Instant now = clock.instant();
    request.expire(now);
    if (!"PENDING".equals(request.getStatus()))
      return TransitionResult.conflict(request.getStatus(), "Không còn ở trạng thái chờ xử lý");
    UUID studentUserId = profile.findStudentById(request.getStudentId())
        .map(StudentSummaryDto::userId)
        .orElseThrow(() -> new InvalidOperationException("Không thể xác định tài khoản học sinh"));
    UUID tutorUserId = profile.findTutorById(request.getTutorId())
        .map(TutorSummaryDto::userId)
        .orElseThrow(() -> new InvalidOperationException("Không thể xác định tài khoản gia sư"));
    request.cancel(now);
    events.publishEvent(new ConnectionRequestChangedEvent(UUID.randomUUID(), id, actor.getUserId(),
        request.getStudentId(), "PENDING", "CANCELLED", now, request.getSentByUserId(),
        studentUserId, tutorUserId, null));
    return TransitionResult.success(null);
  }

  @Transactional
  public TransitionResult<ClassResult> respondRequest(CurrentUser actor, UUID id, boolean accept, String reason) {
    requireRole(actor, "TUTOR");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
    if (!initial.getTutorId().equals(tutorId)) throw new ForbiddenOperationException("Không phải gia sư nhận yêu cầu");
    lock.lockStudent(initial.getStudentId());
    ConnectionRequest request = requests.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    Instant now = clock.instant();
    request.expire(now);
    if (!"PENDING".equals(request.getStatus()))
      return TransitionResult.conflict(request.getStatus(), "Không còn ở trạng thái chờ xử lý");
    if (!accept) {
      request.reject(reason, now);
      notifications.notify(request.getSentByUserId(), request.getStudentId(), "CONNECTION_REJECTED",
          "Yêu cầu đã bị từ chối", "Gia sư đã từ chối yêu cầu", "CONNECTION_REQUEST", id);
      return TransitionResult.success(null);
    }
    if (classes.existsByStudentIdAndTutorIdAndStatus(request.getStudentId(), tutorId, "ACTIVE"))
      return TransitionResult.conflict("ACTIVE", "Đã có lớp đang hoạt động");
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
    return TransitionResult.success(ClassResult.from(created));
  }
}
