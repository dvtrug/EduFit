package vn.edufit.connection.application;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.connection.infra.persistence.*;
import vn.edufit.iam.api.AccountRole;
import vn.edufit.iam.api.AccountStatus;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.audit.AuditSink;
import vn.edufit.shared.notification.NotificationSink;
import vn.edufit.shared.exception.*;

@Service
public class ConnectionService implements ConnectionFacade {
  private final LinkInvitationRepository invitations;
  private final ParentStudentLinkRepository links;
  private final ConnectionRequestRepository requests;
  private final TutoringClassRepository classes;
  private final IamFacade iam;
  private final ProfileFacade profile;
  private final EntityManager entityManager;
  private final AuditSink audit;
  private final NotificationSink notifications;
  private final ConnectionInviteQuotaRepository quotas;

  public ConnectionService(LinkInvitationRepository invitations, ParentStudentLinkRepository links,
      ConnectionRequestRepository requests, TutoringClassRepository classes, IamFacade iam,
      ProfileFacade profile, EntityManager entityManager, AuditSink audit,
      NotificationSink notifications, ConnectionInviteQuotaRepository quotas) {
    this.invitations = invitations;
    this.links = links;
    this.requests = requests;
    this.classes = classes;
    this.iam = iam;
    this.profile = profile;
    this.entityManager = entityManager;
    this.audit = audit;
    this.notifications = notifications;
    this.quotas = quotas;
  }

  // A transaction-scoped lock serializes checks that span multiple rows and modules.
  private void lock(String key) {
    entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(cast(:key as text), 0))")
        .setParameter("key", "connection:" + key).getSingleResult();
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  private static void pending(String status) {
    if (!"PENDING".equals(status)) throw new ResourceConflictException("Không còn ở trạng thái chờ xử lý");
  }

  @Transactional
  public boolean invite(CurrentUser actor, String targetEmail) {
    requireRole(actor, "PARENT", "STUDENT");
    lock("invite-quota:" + actor.getUserId());
    Instant now = Instant.now();
    ConnectionInviteQuota quota = quotas.findById(actor.getUserId())
        .orElseGet(() -> new ConnectionInviteQuota(actor.getUserId(), now));
    if (!quota.consume(now)) return false;
    quotas.save(quota);
    AccountRole opposite = actor.hasRole("PARENT") ? AccountRole.STUDENT : AccountRole.PARENT;
    Optional<UserSummaryView> target = iam.findUserSummaryByEmail(targetEmail)
        .filter(u -> u.status() == AccountStatus.ACTIVE && u.role() == opposite
            && !u.id().equals(actor.getUserId()));
    // Unknown and ineligible addresses intentionally have the same public result.
    if (target.isEmpty()) return true;
    UUID a = actor.getUserId(), b = target.get().id();
    String pair = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    lock("invite:" + pair);
    List<LinkInvitation> history = invitations.findPair(a, b);
    history.forEach(i -> i.expire(now));
    invitations.flush();
    if (history.stream().anyMatch(i -> "PENDING".equals(i.getStatus()))) return true;
    if (history.stream().anyMatch(i -> i.getInviterUserId().equals(a) && "DECLINED".equals(i.getStatus())
        && i.getReinviteAfter() != null && i.getReinviteAfter().isAfter(now))) return true;
    UUID parent = actor.hasRole("PARENT") ? a : b;
    UUID studentUser = actor.hasRole("STUDENT") ? a : b;
    Optional<StudentSummaryDto> student = profile.findStudentByUserId(studentUser);
    if (student.isEmpty() || links.existsByParentUserIdAndStudentIdAndStatus(
        parent, student.get().studentId(), "CONFIRMED")) return true;
    LinkInvitation invitation = invitations.save(new LinkInvitation(a, b, now));
    notifications.notify(b, student.get().studentId(), "LINK_INVITED", "Lời mời liên kết",
        "Bạn có lời mời liên kết mới", "LINK_INVITATION", invitation.getId());
    return true;
  }

  @Transactional
  public List<LinkInvitation> invitations(CurrentUser actor) {
    requireRole(actor, "PARENT", "STUDENT");
    List<LinkInvitation> result = invitations.findByInviterUserIdOrInvitedUserIdOrderByCreatedAtDesc(
        actor.getUserId(), actor.getUserId());
    result.forEach(i -> i.expire(Instant.now()));
    return result;
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public ParentStudentLink respondInvitation(CurrentUser actor, UUID id, boolean accept) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    if (!b.equals(actor.getUserId())) throw new ForbiddenOperationException("Không phải người nhận lời mời");
    String pair = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    lock("invite:" + pair);
    LinkInvitation invitation = invitations.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    Instant now = Instant.now();
    invitation.expire(now);
    pending(invitation.getStatus());
    if (!accept) {
      invitation.decline(now);
      notifications.notify(a, null, "LINK_DECLINED", "Lời mời đã bị từ chối",
          "Người nhận đã từ chối lời mời", "LINK_INVITATION", id);
      return null;
    }
    UserSummaryView inviter = iam.findUserSummaryById(a)
        .orElseThrow(() -> new InvalidOperationException("Tài khoản gửi không còn hợp lệ"));
    AccountRole expected = actor.hasRole("PARENT") ? AccountRole.STUDENT : AccountRole.PARENT;
    if (inviter.status() != AccountStatus.ACTIVE || inviter.role() != expected)
      throw new InvalidOperationException("Tài khoản gửi không còn hợp lệ");
    UUID parent = actor.hasRole("PARENT") ? b : a;
    UUID studentUser = actor.hasRole("STUDENT") ? b : a;
    StudentSummaryDto student = profile.findStudentByUserId(studentUser)
        .orElseThrow(() -> new InvalidOperationException("Hồ sơ học sinh không còn hợp lệ"));
    if (links.existsByParentUserIdAndStudentIdAndStatus(parent, student.studentId(), "CONFIRMED"))
      throw new ResourceConflictException("Liên kết đã tồn tại");
    invitation.accept(now);
    ParentStudentLink link = links.save(new ParentStudentLink(parent, student.studentId(), now));
    notifications.notify(a, student.studentId(), "LINK_CONFIRMED", "Liên kết đã xác nhận",
        "Người nhận đã chấp nhận lời mời", "PARENT_STUDENT_LINK", link.getId());
    return link;
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public void withdrawInvitation(CurrentUser actor, UUID id) {
    requireRole(actor, "PARENT", "STUDENT");
    LinkInvitation initial = invitations.findById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    UUID a = initial.getInviterUserId(), b = initial.getInvitedUserId();
    String pair = a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    lock("invite:" + pair);
    LinkInvitation invitation = invitations.lockById(id)
        .orElseThrow(() -> EntityNotFoundException.of("Invitation", id));
    if (!invitation.getInviterUserId().equals(actor.getUserId()))
      throw new ForbiddenOperationException("Không phải người gửi lời mời");
    invitation.expire(Instant.now());
    pending(invitation.getStatus());
    invitation.withdraw(Instant.now());
  }

  @Transactional(readOnly = true)
  public List<ParentStudentLink> links(CurrentUser actor) {
    requireRole(actor, "PARENT", "STUDENT");
    if (actor.hasRole("PARENT")) return links.findByParentUserIdAndStatus(actor.getUserId(), "CONFIRMED");
    UUID studentId = profile.getStudentIdByUserId(actor.getUserId());
    return links.findByStudentIdAndStatus(studentId, "CONFIRMED");
  }

  @Transactional
  public void revokeLink(CurrentUser actor, UUID id, String reason) {
    requireRole(actor, "STUDENT", "ADMIN");
    ParentStudentLink initial = links.findById(id).orElseThrow(() -> EntityNotFoundException.of("Link", id));
    lock("student:" + initial.getStudentId());
    ParentStudentLink link = links.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Link", id));
    if (!"CONFIRMED".equals(link.getStatus())) throw new ResourceConflictException("Liên kết đã bị thu hồi");
    if (actor.hasRole("ADMIN") && (reason == null || reason.isBlank()))
      throw new InvalidOperationException("Admin phải nhập lý do thu hồi");
    if (!actor.hasRole("ADMIN") && !profile.findStudentById(link.getStudentId())
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false))
      throw new ForbiddenOperationException("Không phải học sinh sở hữu liên kết");
    link.revoke(actor.getUserId(), reason, Instant.now());
    audit.record(actor.getUserId(), "LINK_REVOKED", "PARENT_STUDENT_LINK", id, reason);
    notifications.notify(link.getParentUserId(), link.getStudentId(), "LINK_REVOKED",
        "Liên kết đã thu hồi", "Quyền truy cập hồ sơ học sinh đã kết thúc", "PARENT_STUDENT_LINK", id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasConfirmedParentLink(UUID parentUserId, UUID studentId) {
    return links.existsByParentUserIdAndStudentIdAndStatus(parentUserId, studentId, "CONFIRMED");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ActiveClassView> findActiveClass(UUID studentId, UUID tutorId) {
    return classes.findByStudentIdAndTutorIdAndStatus(studentId, tutorId, "ACTIVE")
        .map(c -> new ActiveClassView(c.getId(), c.getStudentId(), c.getTutorId(), c.getGoalId()));
  }

  private void requireStudentAccess(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("STUDENT") && profile.findStudentById(studentId)
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false)) return;
    if (actor.hasRole("PARENT") && hasConfirmedParentLink(actor.getUserId(), studentId)) return;
    throw new ForbiddenOperationException("Không có quyền với học sinh này");
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public ConnectionRequest request(CurrentUser actor, UUID goalId, UUID tutorId,
      List<String> desiredSlots, String message) {
    requireRole(actor, "STUDENT", "PARENT");
    ConnectionGoalDto goal = profile.findGoalForConnection(goalId)
        .orElseThrow(() -> EntityNotFoundException.of("LearningGoal", goalId));
    lock("student:" + goal.studentId());
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
    return created;
  }

  @Transactional
  public List<ConnectionRequest> requests(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("TUTOR")) {
      UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
      List<ConnectionRequest> result = requests.findByTutorIdOrderByCreatedAtDesc(tutorId);
      result.forEach(r -> r.expire(Instant.now()));
      return result;
    }
    requireRole(actor, "STUDENT", "PARENT");
    UUID scope = actor.hasRole("STUDENT") ? profile.getStudentIdByUserId(actor.getUserId()) : studentId;
    if (scope == null) throw new InvalidOperationException("Cần chỉ định học sinh");
    requireStudentAccess(actor, scope);
    List<ConnectionRequest> result = requests.findByStudentIdOrderByCreatedAtDesc(scope);
    result.forEach(r -> r.expire(Instant.now()));
    return result;
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public void cancelRequest(CurrentUser actor, UUID id) {
    requireRole(actor, "STUDENT", "PARENT");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    lock("student:" + initial.getStudentId());
    ConnectionRequest request = requests.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    requireStudentAccess(actor, request.getStudentId());
    request.expire(Instant.now());
    pending(request.getStatus());
    request.cancel(Instant.now());
  }

  @Transactional(noRollbackFor = ResourceConflictException.class)
  public TutoringClass respondRequest(CurrentUser actor, UUID id, boolean accept, String reason) {
    requireRole(actor, "TUTOR");
    ConnectionRequest initial = requests.findById(id).orElseThrow(() -> EntityNotFoundException.of("Request", id));
    UUID tutorId = profile.getTutorIdByUserId(actor.getUserId());
    if (!initial.getTutorId().equals(tutorId)) throw new ForbiddenOperationException("Không phải gia sư nhận yêu cầu");
    lock("student:" + initial.getStudentId());
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
    return created;
  }
}
