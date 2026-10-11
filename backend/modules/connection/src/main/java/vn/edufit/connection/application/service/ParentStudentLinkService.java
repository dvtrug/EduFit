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
  private final ConnectionLock lock;
  private final AuditSink audit;
  private final NotificationSink notifications;

  public ParentStudentLinkService(ParentStudentLinkRepository links, ProfileFacade profile,
      ConnectionLock lock, AuditSink audit, NotificationSink notifications, Clock clock) {
    this.clock = clock;
    this.links = links;
    this.profile = profile;
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
    if (!"CONFIRMED".equals(link.getStatus())) throw new ResourceConflictException("Liên kết đã bị thu hồi");
    if (actor.hasRole("ADMIN") && (reason == null || reason.isBlank()))
      throw new InvalidOperationException("Admin phải nhập lý do thu hồi");
    if (!actor.hasRole("ADMIN") && !profile.findStudentById(link.getStudentId())
        .map(s -> s.userId().equals(actor.getUserId())).orElse(false))
      throw new ForbiddenOperationException("Không phải học sinh sở hữu liên kết");
    link.revoke(actor.getUserId(), reason, clock.instant());
    audit.record(actor.getUserId(), "LINK_REVOKED", "PARENT_STUDENT_LINK", id, reason);
    notifications.notify(link.getParentUserId(), link.getStudentId(), "LINK_REVOKED",
        "Liên kết đã thu hồi", "Quyền truy cập hồ sơ học sinh đã kết thúc", "PARENT_STUDENT_LINK", id);
  }

}
