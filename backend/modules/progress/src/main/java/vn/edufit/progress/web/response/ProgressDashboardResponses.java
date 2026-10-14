package vn.edufit.progress.web.response;

import vn.edufit.scheduling.api.dto.ClassSessionStatsView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ProgressDashboardResponses {

  public record ProgressSeriesPoint(
      LocalDate date,
      int progressPercent
  ) {}

  public record ClassSelectorItem(
      UUID classId,
      String title,
      String status,
      Integer progressPercentage
  ) {}

  public record StudentDashboardResponse(
      UUID studentId,
      String studentName,
      List<ClassSelectorItem> classes,
      UUID selectedClassId,
      UUID tutorId,
      String tutorName,
      UUID goalId,
      String goalTitle,
      Integer progressPercentage,
      PlanResponse plan,
      ClassSessionStatsView sessionStats,
      List<ProgressSeriesPoint> progressSeries,
      List<SessionNoteTimelineResponse> recentNotes,
      boolean hasMoreNotes
  ) {}

  public record ParentStudentSummary(
      UUID studentId,
      String studentName,
      List<ClassSelectorItem> classes,
      StudentDashboardResponse activeClassDashboard
  ) {}

  public record ParentDashboardResponse(
      List<ParentStudentSummary> students
  ) {}

  public record TeachingHistoryItem(
      UUID classId,
      UUID studentId,
      String studentName,
      UUID goalId,
      String goalTitle,
      String status,
      Integer progressPercentage,
      Instant startedAt,
      Instant endedAt,
      ClassSessionStatsView sessionStats
  ) {}
}
