package vn.edufit.progress.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.progress.domain.model.LearningPlan;
import vn.edufit.progress.domain.model.Milestone;
import vn.edufit.progress.domain.model.MilestoneStatus;
import vn.edufit.progress.domain.model.PlanHistory;
import vn.edufit.progress.domain.repository.LearningPlanDomainRepository;
import vn.edufit.progress.domain.repository.PlanHistoryDomainRepository;
import vn.edufit.progress.web.request.CreatePlanRequest;
import vn.edufit.progress.web.request.UpdateMilestoneStatusRequest;
import vn.edufit.progress.web.request.UpdatePlanRequest;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ErrorCode;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;
import vn.edufit.shared.exception.ResourceConflictException;
import vn.edufit.shared.notification.NotificationSink;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPlanService {

  private final LearningPlanDomainRepository planRepository;
  private final PlanHistoryDomainRepository historyRepository;
  private final ConnectionFacade connectionFacade;
  private final ProfileFacade profileFacade;
  private final NotificationSink notifications;

  /**
   * Tạo Kế hoạch học tập mới cho lớp học (UC5.1).
   * Chỉ Gia sư của lớp học ACTIVE mới có quyền tạo.
   */
  @Transactional
  public LearningPlan createPlan(CurrentUser actor, CreatePlanRequest req) {
    ConnectionFacade.ClassDetailView cls = requireClass(req.classId());
    requireTutorOfClass(actor, cls);

    if (!"ACTIVE".equals(cls.status())) {
      throw new InvalidOperationException("Chỉ có thể tạo kế hoạch học tập cho lớp học đang hoạt động");
    }

    if (planRepository.existsByClassId(req.classId())) {
      throw new ResourceConflictException("Lớp học này đã có kế hoạch học tập");
    }

    LocalDate today = LocalDate.now();
    List<Milestone> initialMilestones = new ArrayList<>();
    int order = 0;
    for (CreatePlanRequest.CreateMilestoneItem item : req.milestones()) {
      int weight = item.weight() != null ? item.weight() : 1;
      initialMilestones.add(Milestone.createNew(
          null, // planId will be set by LearningPlan.createNew
          item.title(),
          item.description(),
          item.targetDate(),
          weight,
          order++,
          today
      ));
    }

    LearningPlan plan = LearningPlan.createNew(req.classId(), req.summary(), initialMilestones);
    LearningPlan saved = planRepository.save(plan);

    // Ghi Audit Trail
    historyRepository.save(PlanHistory.of(
        saved.getPlanId(),
        null,
        "PLAN_CREATED",
        actor.getUserId(),
        null,
        saved.getSummary()
    ));

    log.info("Tạo kế hoạch học tập thành công: planId={}, classId={}, tutorUserId={}",
        saved.getPlanId(), saved.getClassId(), actor.getUserId());

    return saved;
  }

  /**
   * Xem Kế hoạch học tập theo mã lớp học (UC5.2).
   * Quyền xem: Gia sư của lớp, Học sinh của lớp, hoặc Phụ huynh có liên kết CONFIRMED.
   */
  @Transactional(readOnly = true)
  public LearningPlan getPlanByClassId(CurrentUser actor, UUID classId) {
    ConnectionFacade.ClassDetailView cls = requireClass(classId);
    requireMemberOrParentAccess(actor, cls);

    return planRepository.findByClassId(classId)
        .orElseThrow(() -> new EntityNotFoundException("Lớp học này chưa có kế hoạch học tập"));
  }

  /**
   * Cập nhật Kế hoạch học tập (UC5.3).
   * Dùng Full Aggregate Update kèm Optimistic Locking (version).
   */
  @Transactional
  public LearningPlan updatePlan(CurrentUser actor, UUID planId, UpdatePlanRequest req) {
    LearningPlan plan = planRepository.findById(planId)
        .orElseThrow(() -> EntityNotFoundException.of("LearningPlan", planId));

    ConnectionFacade.ClassDetailView cls = requireClass(plan.getClassId());
    requireTutorOfClass(actor, cls);

    if (!"ACTIVE".equals(cls.status())) {
      throw new InvalidOperationException("Chỉ có thể cập nhật kế hoạch cho lớp học đang hoạt động");
    }

    // 1. Kiểm tra Optimistic Locking
    if (plan.getVersion() != req.version()) {
      throw new ResourceConflictException(
          ErrorCode.PLAN_VERSION_CONFLICT,
          "Kế hoạch đã bị chỉnh sửa bởi phiên làm việc khác (phiên bản máy chủ: "
              + plan.getVersion() + ", phiên bản gửi lên: " + req.version() + ")"
      );
    }

    LocalDate today = LocalDate.now();

    // 2. Audit cập nhật summary
    if (!Objects.equals(plan.getSummary(), req.summary())) {
      historyRepository.save(PlanHistory.of(
          planId, null, "SUMMARY_UPDATED", actor.getUserId(), plan.getSummary(), req.summary()
      ));
      plan.updateSummary(req.summary());
    }

    // 3. Xử lý danh sách Milestone: phân loại giữ lại, thêm mới, archive hoặc xóa
    Map<UUID, UpdatePlanRequest.UpdateMilestoneItem> incomingMap = req.milestones().stream()
        .filter(m -> m.milestoneId() != null)
        .collect(Collectors.toMap(UpdatePlanRequest.UpdateMilestoneItem::milestoneId, m -> m));

    List<Milestone> currentList = new ArrayList<>(plan.getMilestones());
    List<Milestone> nextList = new ArrayList<>();

    // 3.1. Xử lý các milestone hiện có
    for (Milestone existing : currentList) {
      if (incomingMap.containsKey(existing.getMilestoneId())) {
        UpdatePlanRequest.UpdateMilestoneItem incoming = incomingMap.get(existing.getMilestoneId());
        int weight = incoming.weight() != null ? incoming.weight() : existing.getWeight();
        int sortOrder = req.milestones().indexOf(incoming);

        existing.updateDetails(incoming.title(), incoming.description(), incoming.targetDate(), weight, sortOrder, today);
        nextList.add(existing);
      } else {
        // Milestone không còn trong payload: nếu đã từng tiến triển thì archive, nếu chưa thì xóa thật
        if (existing.hasProgressed()) {
          existing.archive();
          nextList.add(existing);
          historyRepository.save(PlanHistory.of(
              planId, existing.getMilestoneId(), "MILESTONE_ARCHIVED", actor.getUserId(), existing.getTitle(), null
          ));
        } else {
          historyRepository.save(PlanHistory.of(
              planId, existing.getMilestoneId(), "MILESTONE_DELETED", actor.getUserId(), existing.getTitle(), null
          ));
        }
      }
    }

    // 3.2. Thêm các milestone mới (chưa có milestoneId hoặc milestoneId mới)
    for (int i = 0; i < req.milestones().size(); i++) {
      UpdatePlanRequest.UpdateMilestoneItem item = req.milestones().get(i);
      if (item.milestoneId() == null || currentList.stream().noneMatch(m -> m.getMilestoneId().equals(item.milestoneId()))) {
        int weight = item.weight() != null ? item.weight() : 1;
        Milestone newMilestone = Milestone.createNew(planId, item.title(), item.description(), item.targetDate(), weight, i, today);
        nextList.add(newMilestone);
        historyRepository.save(PlanHistory.of(
            planId, newMilestone.getMilestoneId(), "MILESTONE_CREATED", actor.getUserId(), null, item.title()
        ));
      }
    }

    // Tạo aggregate mới với danh sách milestone đã cập nhật và validate 1-20 active
    LearningPlan updatedPlan = new LearningPlan(
        plan.getPlanId(),
        plan.getClassId(),
        plan.getSummary(),
        plan.getVersion(),
        plan.getCreatedAt(),
        plan.getUpdatedAt(),
        nextList
    );
    updatedPlan.validateActiveMilestonesCount();

    LearningPlan saved = planRepository.save(updatedPlan);
    log.info("Cập nhật kế hoạch học tập thành công: planId={}, newVersion={}", saved.getPlanId(), saved.getVersion());
    return saved;
  }

  /**
   * Cập nhật trạng thái một Cột mốc (UC5.4).
   * Phát notification khi milestone chuyển từ trạng thái khác sang ACHIEVED.
   */
  @Transactional
  public LearningPlan updateMilestoneStatus(CurrentUser actor, UUID milestoneId, UpdateMilestoneStatusRequest req) {

    LearningPlan plan = planRepository.findByMilestoneId(milestoneId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cột mốc với ID: " + milestoneId));

    ConnectionFacade.ClassDetailView cls = requireClass(plan.getClassId());

    requireTutorOfClass(actor, cls);

    if (!"ACTIVE".equals(cls.status())) {
      throw new InvalidOperationException("Chỉ có thể cập nhật cột mốc cho lớp học đang hoạt động");
    }

    // 1. Kiểm tra Optimistic Locking
    if (plan.getVersion() != req.version()) {
      throw new ResourceConflictException(
          ErrorCode.PLAN_VERSION_CONFLICT,
          "Kế hoạch đã bị chỉnh sửa bởi phiên làm việc khác (phiên bản máy chủ: "
              + plan.getVersion() + ", phiên bản gửi lên: " + req.version() + ")"
      );
    }

    Milestone milestone = plan.getMilestones().stream()
        .filter(m -> m.getMilestoneId().equals(milestoneId))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cột mốc trong kế hoạch"));

    MilestoneStatus oldStatus = milestone.getStatus();
    milestone.updateStatus(req.status(), req.achievedDate(), LocalDate.now());

    // 2. Ghi Audit Trail
    historyRepository.save(PlanHistory.of(
        plan.getPlanId(),
        milestoneId,
        "MILESTONE_STATUS_CHANGED",
        actor.getUserId(),
        oldStatus.name(),
        req.status().name()
    ));


    // 3. Lưu kế hoạch
    LearningPlan saved = planRepository.save(plan);

    // 4. Nếu vừa đạt trạng thái ACHIEVED -> gửi notification tới Học sinh và các Phụ huynh liên kết
    if (oldStatus != MilestoneStatus.ACHIEVED && req.status() == MilestoneStatus.ACHIEVED) {
      notifyMilestoneAchieved(cls, milestone);
    }

    log.info("Cập nhật trạng thái milestone thành công: milestoneId={}, oldStatus={}, newStatus={}",
        milestoneId, oldStatus, req.status());

    return saved;
  }

  private void notifyMilestoneAchieved(ConnectionFacade.ClassDetailView cls, Milestone milestone) {
    // Thông báo cho Học sinh
    profileFacade.findStudentById(cls.studentId()).ifPresent(student -> {
      notifications.notify(
          student.userId(),
          cls.studentId(),
          "MILESTONE_ACHIEVED",
          "Chúc mừng! Đạt cột mốc mới",
          "Bạn đã hoàn thành cột mốc: " + milestone.getTitle(),
          "MILESTONE",
          milestone.getMilestoneId()
      );
    });

    // Thông báo cho các Phụ huynh đã xác nhận liên kết
    List<UUID> parentUserIds = connectionFacade.findConfirmedParentUserIds(cls.studentId());
    for (UUID parentUserId : parentUserIds) {
      notifications.notify(
          parentUserId,
          cls.studentId(),
          "MILESTONE_ACHIEVED",
          "Học sinh đạt cột mốc mới",
          "Học sinh đã hoàn thành cột mốc: " + milestone.getTitle(),
          "MILESTONE",
          milestone.getMilestoneId()
      );
    }
  }


  // ── Helper xác thực quyền ────────────────────────────────────────────────

  private ConnectionFacade.ClassDetailView requireClass(UUID classId) {
    return connectionFacade.findClassById(classId)
        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với ID: " + classId));
  }

  private void requireTutorOfClass(CurrentUser actor, ConnectionFacade.ClassDetailView cls) {
    if (!actor.hasRole("TUTOR")) {
      throw new ForbiddenOperationException("Chỉ tài khoản Gia sư mới có quyền thực hiện thao tác này");
    }
    UUID tutorId = profileFacade.getTutorIdByUserId(actor.getUserId());
    if (tutorId == null || !tutorId.equals(cls.tutorId())) {
      throw new ForbiddenOperationException("Chỉ Gia sư được phân công của lớp học mới có quyền thực hiện thao tác này");
    }
  }

  private void requireMemberOrParentAccess(CurrentUser actor, ConnectionFacade.ClassDetailView cls) {
    if (actor.hasRole("ADMIN")) return;

    if (actor.hasRole("TUTOR")) {
      UUID tutorId = profileFacade.getTutorIdByUserId(actor.getUserId());
      if (tutorId != null && tutorId.equals(cls.tutorId())) return;
    }

    if (actor.hasRole("STUDENT")) {
      UUID studentId = profileFacade.getStudentIdByUserId(actor.getUserId());
      if (studentId != null && studentId.equals(cls.studentId())) return;
    }

    if (actor.hasRole("PARENT")) {
      if (connectionFacade.hasConfirmedParentLink(actor.getUserId(), cls.studentId())) return;
    }

    throw new ForbiddenOperationException("Bạn không có quyền truy cập kế hoạch học tập của lớp này");
  }
}
