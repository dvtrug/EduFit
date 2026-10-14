package vn.edufit.connection.application.service;

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edufit.connection.application.port.ConnectionLock;
import vn.edufit.connection.infra.persistence.entity.ParentStudentLink;
import vn.edufit.connection.infra.persistence.repository.ParentStudentLinkRepository;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.audit.AuditSink;
import vn.edufit.shared.audit.AuditRecord;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.AccountStatus;
import java.util.Map;
import vn.edufit.shared.notification.NotificationSink;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.InvalidOperationException;

@Service
public class ParentStudentLinkService {
  private final Clock clock;
  private final ParentStudentLinkRepository links;
  private final ProfileFacade profile;
  private final IamFacade iam;
  private final ConnectionLock lock;
  private final AuditSink audit;
  private final NotificationSink notifications;

  public ParentStudentLinkService(ParentStudentLinkRepository links, ProfileFacade profile,
      ConnectionLock lock, AuditSink audit, NotificationSink notifications, IamFacade iam, Clock clock) {
    this.clock = clock;
    this.links = links;
    this.profile = profile;
    this.iam = iam;
    this.lock = lock;
    this.audit = audit;
    this.notifications = notifications;
  }

  private static void requireRole(CurrentUser actor, String... roles) {
    for (String role : roles) if (actor.hasRole(role)) return;
    throw new ForbiddenOperationException("Không có quyền thực hiện thao tác này");
  }

  @Transactional
  public void revokeLink(CurrentUser actor, UUID id, String reason) {
    requireRole(actor, "STUDENT", "ADMIN");
    ParentStudentLink initial = links.findById(id).orElseThrow(() -> EntityNotFoundException.of("Link", id));
    lock.lockStudent(initial.getStudentId());
    ParentStudentLink link = links.lockById(id).orElseThrow(() -> EntityNotFoundException.of("Link", id));
    if (iam.findUserSummaryById(actor.getUserId()).filter(user -> user.status() == AccountStatus.ACTIVE
        && user.id().equals(actor.getUserId()) && actor.hasRole(user.role().name())).isEmpty())
      throw new ForbiddenOperationException("Tài khoản không có quyền thực hiện thao tác này");
    if (!"CONFIRMED".equals(link.getStatus())) throw new ResourceConflictException("Liên kết đã bị thu hồi");
    if (actor.hasRole("ADMIN") && (reason == null || reason.isBlank()))
      throw new InvalidOperationException("Admin phải nhập lý do thu hồi");
    var student = profile.findStudentById(link.getStudentId())
        .filter(value -> value.studentId().equals(link.getStudentId()) && value.userId() != null)
        .orElseThrow(() -> new InvalidOperationException("Không thể xác định tài khoản học sinh"));
    if (!actor.hasRole("ADMIN") && !student.userId().equals(actor.getUserId()))
      throw new ForbiddenOperationException("Không phải học sinh sở hữu liên kết");
    Instant now = clock.instant();
    link.revoke(actor.getUserId(), reason, now);
    audit.record(new AuditRecord(actor.getUserId(), "LINK_REVOKED", "PARENT_STUDENT_LINK", id, reason, now,
        Map.of("parentUserId", link.getParentUserId().toString(), "studentId", link.getStudentId().toString(),
            "studentUserId", student.userId().toString())));
    notifications.notify(link.getParentUserId(), link.getStudentId(), "LINK_REVOKED",
        "Liên kết đã thu hồi", "Quyền truy cập hồ sơ học sinh đã kết thúc", "PARENT_STUDENT_LINK", id);
  }

}
