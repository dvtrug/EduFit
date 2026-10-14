package vn.edufit.progress.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edufit.connection.api.ConnectionFacade;
import vn.edufit.iam.api.IamFacade;
import vn.edufit.iam.api.dto.UserSummaryView;
import vn.edufit.profile.api.ProfileFacade;
import vn.edufit.profile.api.dto.ConnectionGoalDto;
import vn.edufit.profile.api.dto.StudentSummaryDto;
import vn.edufit.profile.api.dto.TutorSummaryDto;
import vn.edufit.progress.domain.model.LearningPlan;
import vn.edufit.progress.domain.model.Milestone;
import vn.edufit.progress.domain.model.MilestoneStatus;
import vn.edufit.progress.domain.repository.LearningPlanDomainRepository;
import vn.edufit.progress.infra.persistence.entity.SessionNoteJpaEntity;
import vn.edufit.progress.infra.persistence.repository.SpringDataSessionNoteRepository;
import vn.edufit.progress.web.response.PlanResponse;
import vn.edufit.progress.web.response.ProgressDashboardResponses.*;
import vn.edufit.progress.web.response.SessionNoteTimelineResponse;
import vn.edufit.scheduling.api.SessionFacade;
import vn.edufit.scheduling.api.dto.ClassSessionStatsView;
import vn.edufit.scheduling.api.dto.SessionTimelineView;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressDashboardService {

  private final LearningPlanDomainRepository planRepository;
  private final SpringDataSessionNoteRepository noteRepository;
  private final ConnectionFacade connectionFacade;
  private final ProfileFacade profileFacade;
  private final IamFacade iamFacade;
  private final SessionFacade sessionFacade;

  /**
   * Lấy dữ liệu Dashboard Tiến độ học tập cho Học sinh (hoặc Phụ huynh xem con).
   */
  @Transactional(readOnly = true)
  public StudentDashboardResponse getStudentDashboard(
      CurrentUser actor,
      UUID studentId,
      UUID specifiedClassId,
      String range
  ) {
    requireStudentAccess(actor, studentId);

    StudentSummaryDto student = profileFacade.findStudentById(studentId).orElse(null);
    String studentName = resolveStudentName(student);

    List<ConnectionFacade.ClassDetailView> classes = connectionFacade.findClassesByStudent(studentId);
    if (classes.isEmpty()) {
      return new StudentDashboardResponse(
          studentId, studentName, List.of(), null, null, null, null, null,
          null, null, null, List.of(), List.of(), false
      );
    }

    // Lấy progress của các lớp để tạo danh sách chuyển đổi (ClassSelectorItem)
    List<UUID> classIds = classes.stream().map(ConnectionFacade.ClassDetailView::classId).toList();
    List<ClassSelectorItem> selectorItems = new ArrayList<>();
    for (ConnectionFacade.ClassDetailView c : classes) {
      Integer prog = planRepository.findByClassId(c.classId())
          .map(LearningPlan::calculateProgressPercentage)
          .orElse(null);
      String title = profileFacade.findGoalForConnection(c.goalId())
          .map(g -> "Mục tiêu " + (g.goalType() != null ? g.goalType() : "học tập"))
          .orElse("Lớp học");
      selectorItems.add(new ClassSelectorItem(c.classId(), title, c.status(), prog));
    }

    // Chọn lớp hiển thị: nếu có specifiedClassId thì dùng, nếu không chọn lớp ACTIVE đầu tiên
    ConnectionFacade.ClassDetailView selectedClass = null;
    if (specifiedClassId != null) {
      selectedClass = classes.stream()
          .filter(c -> c.classId().equals(specifiedClassId))
          .findFirst()
          .orElse(null);
    }
    if (selectedClass == null) {
      selectedClass = classes.stream()
          .filter(c -> "ACTIVE".equals(c.status()))
          .findFirst()
          .orElse(classes.getFirst());
    }

    UUID classId = selectedClass.classId();
    LearningPlan plan = planRepository.findByClassId(classId).orElse(null);

    // Tutor info
    TutorSummaryDto tutor = profileFacade.findTutorById(selectedClass.tutorId()).orElse(null);
    String tutorName = tutor != null ? tutor.displayName() : "Gia sư";

    // Goal info
    ConnectionGoalDto goal = profileFacade.findGoalForConnection(selectedClass.goalId()).orElse(null);
    String goalTitle = goal != null ? "Mục tiêu " + (goal.goalType() != null ? goal.goalType() : "học tập") : "Mục tiêu học tập";

    // Session stats (1 query)
    Map<UUID, ClassSessionStatsView> statsMap = sessionFacade.getSessionStatsByClassIds(List.of(classId), null, null);
    ClassSessionStatsView stats = statsMap.get(classId);


    // Progress series tính từ milestones active
    List<ProgressSeriesPoint> series = buildProgressSeries(plan, range);

    // 10 session notes gần nhất
    List<SessionTimelineView> recentSessions = sessionFacade.findSessionsByClassId(classId, null, null);
    List<UUID> sessionIds = recentSessions.stream().map(SessionTimelineView::sessionId).toList();
    Map<UUID, SessionNoteJpaEntity> notesMap = noteRepository.findBySessionIdIn(sessionIds).stream()
        .collect(Collectors.toMap(SessionNoteJpaEntity::getSessionId, n -> n));

    List<SessionNoteTimelineResponse> noteResponses = recentSessions.stream()
        .filter(s -> notesMap.containsKey(s.sessionId()))
        .limit(10)
        .map(s -> {
          SessionNoteJpaEntity note = notesMap.get(s.sessionId());
          return new SessionNoteTimelineResponse(
              s.sessionId(), s.classId(), s.startAt(), s.endAt(), s.status(),
              note.getNoteText(), note.getCreatedAt()
          );
        })
        .toList();

    boolean hasMoreNotes = notesMap.size() > 10;

    return new StudentDashboardResponse(
        studentId,
        studentName,
        selectorItems,
        classId,
        selectedClass.tutorId(),
        tutorName,
        selectedClass.goalId(),
        goalTitle,
        plan != null ? plan.calculateProgressPercentage() : null,
        plan != null ? PlanResponse.from(plan) : null,
        stats,
        series,
        noteResponses,
        hasMoreNotes
    );
  }

  /**
   * Lấy dữ liệu Dashboard cho Phụ huynh nhóm theo từng học sinh.
   */
  @Transactional(readOnly = true)
  public ParentDashboardResponse getParentDashboard(CurrentUser actor) {
    if (!actor.hasRole("PARENT")) {
      throw new ForbiddenOperationException("Chỉ tài khoản Phụ huynh mới có quyền truy cập");
    }

    List<UUID> studentIds = connectionFacade.findConfirmedStudentIds(actor.getUserId());
    List<ParentStudentSummary> summaries = new ArrayList<>();

    for (UUID sId : studentIds) {
      StudentDashboardResponse studentDash = getStudentDashboard(actor, sId, null, "30D");
      summaries.add(new ParentStudentSummary(
          sId,
          studentDash.studentName(),
          studentDash.classes(),
          studentDash
      ));
    }

    return new ParentDashboardResponse(summaries);
  }

  /**
   * Lấy Lịch sử giảng dạy (Teaching History) của Gia sư có phân trang.
   */
  @Transactional(readOnly = true)
  public Page<TeachingHistoryItem> getTeachingHistory(
      CurrentUser actor,
      int page,
      int size,
      String studentNameFilter,
      String statusFilter,
      Instant from,
      Instant to
  ) {
    if (!actor.hasRole("TUTOR")) {
      throw new ForbiddenOperationException("Chỉ tài khoản Gia sư mới có quyền truy cập Lịch sử giảng dạy");
    }

    UUID tutorId = profileFacade.getTutorIdByUserId(actor.getUserId());
    if (tutorId == null) {
      return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
    }

    List<ConnectionFacade.ClassDetailView> classes = connectionFacade.findClassesByTutor(tutorId);
    if (classes.isEmpty()) {
      return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
    }

    // Filter theo status nếu có
    if (statusFilter != null && !statusFilter.isBlank()) {
      classes = classes.stream().filter(c -> statusFilter.equalsIgnoreCase(c.status())).toList();
    }

    // Batch query session stats cho toàn bộ classIds của tutor (tránh N+1)
    List<UUID> classIds = classes.stream().map(ConnectionFacade.ClassDetailView::classId).toList();
    Map<UUID, ClassSessionStatsView> statsMap = sessionFacade.getSessionStatsByClassIds(classIds, from, to);

    List<TeachingHistoryItem> items = new ArrayList<>();
    for (ConnectionFacade.ClassDetailView c : classes) {
      StudentSummaryDto student = profileFacade.findStudentById(c.studentId()).orElse(null);
      String studentName = resolveStudentName(student);

      // Filter theo studentName nếu có
      if (studentNameFilter != null && !studentNameFilter.isBlank()) {
        if (!studentName.toLowerCase().contains(studentNameFilter.toLowerCase())) {
          continue;
        }
      }

      ConnectionGoalDto goal = profileFacade.findGoalForConnection(c.goalId()).orElse(null);
      String goalTitle = goal != null ? "Mục tiêu " + (goal.goalType() != null ? goal.goalType() : "học tập") : "Mục tiêu";


      Integer progress = planRepository.findByClassId(c.classId())
          .map(LearningPlan::calculateProgressPercentage)
          .orElse(null);

      ClassSessionStatsView stats = statsMap.get(c.classId());

      items.add(new TeachingHistoryItem(
          c.classId(),
          c.studentId(),
          studentName,
          c.goalId(),
          goalTitle,
          c.status(),
          progress,
          c.startedAt(),
          c.endedAt(),
          stats
      ));
    }

    int start = Math.min(page * size, items.size());
    int end = Math.min(start + size, items.size());
    return new PageImpl<>(items.subList(start, end), PageRequest.of(page, size), items.size());
  }

  // ── Helper xây dựng biểu đồ tích lũy tiến độ theo thời gian ───────────────

  private List<ProgressSeriesPoint> buildProgressSeries(LearningPlan plan, String range) {
    if (plan == null) return List.of();

    List<Milestone> active = plan.getActiveMilestones();
    int totalWeight = active.stream().mapToInt(Milestone::getWeight).sum();
    if (totalWeight == 0) return List.of();

    LocalDate today = LocalDate.now();
    LocalDate rangeStart = switch (range != null ? range.toUpperCase() : "30D") {
      case "90D" -> today.minusDays(90);
      case "ALL" -> LocalDate.MIN;
      default -> today.minusDays(30);
    };

    // Lọc các milestone ACHIEVED có achievedDate
    List<Milestone> achieved = active.stream()
        .filter(m -> m.getStatus() == MilestoneStatus.ACHIEVED && m.getAchievedDate() != null)
        .sorted(Comparator.comparing(Milestone::getAchievedDate))
        .toList();

    if (achieved.isEmpty()) return List.of();

    // Điểm mở đầu: nếu có milestone achieved trước rangeStart
    int priorWeight = achieved.stream()
        .filter(m -> m.getAchievedDate().isBefore(rangeStart))
        .mapToInt(Milestone::getWeight)
        .sum();

    List<ProgressSeriesPoint> points = new ArrayList<>();
    if (rangeStart != LocalDate.MIN && priorWeight > 0) {
      int initialPercent = BigDecimal.valueOf(priorWeight)
          .multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(totalWeight), 0, RoundingMode.HALF_UP)
          .intValue();
      points.add(new ProgressSeriesPoint(rangeStart, initialPercent));
    }

    int runningWeight = priorWeight;
    for (Milestone m : achieved) {
      if (m.getAchievedDate().isBefore(rangeStart)) continue;
      runningWeight += m.getWeight();
      int percent = BigDecimal.valueOf(runningWeight)
          .multiply(BigDecimal.valueOf(100))
          .divide(BigDecimal.valueOf(totalWeight), 0, RoundingMode.HALF_UP)
          .intValue();
      points.add(new ProgressSeriesPoint(m.getAchievedDate(), percent));
    }

    return points;
  }

  private void requireStudentAccess(CurrentUser actor, UUID studentId) {
    if (actor.hasRole("ADMIN")) return;

    if (actor.hasRole("STUDENT")) {
      UUID actorStudentId = profileFacade.getStudentIdByUserId(actor.getUserId());
      if (actorStudentId != null && actorStudentId.equals(studentId)) return;
    }

    if (actor.hasRole("PARENT")) {
      if (connectionFacade.hasConfirmedParentLink(actor.getUserId(), studentId)) return;
    }

    if (actor.hasRole("TUTOR")) {
      UUID tutorId = profileFacade.getTutorIdByUserId(actor.getUserId());
      if (tutorId != null) {
        boolean teaches = connectionFacade.findClassesByTutor(tutorId).stream()
            .anyMatch(c -> c.studentId().equals(studentId));
        if (teaches) return;
      }
    }

    throw new ForbiddenOperationException("Không có quyền truy cập tiến độ của học sinh này");
  }

  private String resolveStudentName(StudentSummaryDto student) {
    if (student == null || student.userId() == null) return "Học sinh";
    return iamFacade.findUserSummaryById(student.userId())
        .map(UserSummaryView::fullName)
        .orElse("Học sinh");
  }
}
