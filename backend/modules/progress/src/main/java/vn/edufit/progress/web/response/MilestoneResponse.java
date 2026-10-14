package vn.edufit.progress.web.response;

import vn.edufit.progress.domain.model.Milestone;

import java.time.LocalDate;
import java.util.UUID;

public record MilestoneResponse(
    UUID milestoneId,
    String title,
    String description,
    LocalDate targetDate,
    int weight,
    String status,
    LocalDate achievedDate,
    boolean isArchived,
    int sortOrder
) {
  public static MilestoneResponse from(Milestone m) {
    return new MilestoneResponse(
        m.getMilestoneId(),
        m.getTitle(),
        m.getDescription(),
        m.getTargetDate(),
        m.getWeight(),
        m.getStatus().name(),
        m.getAchievedDate(),
        m.isArchived(),
        m.getSortOrder()
    );
  }
}
