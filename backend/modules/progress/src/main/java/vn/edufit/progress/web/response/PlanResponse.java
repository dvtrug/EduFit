package vn.edufit.progress.web.response;

import vn.edufit.progress.domain.model.LearningPlan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanResponse(
    UUID planId,
    UUID classId,
    String summary,
    int version,
    Integer progressPercentage,
    List<MilestoneResponse> milestones,
    Instant createdAt,
    Instant updatedAt
) {
  public static PlanResponse from(LearningPlan plan) {
    List<MilestoneResponse> milestoneResponses = plan.getMilestones().stream()
        .map(MilestoneResponse::from)
        .toList();
    return new PlanResponse(
        plan.getPlanId(),
        plan.getClassId(),
        plan.getSummary(),
        plan.getVersion(),
        plan.calculateProgressPercentage(),
        milestoneResponses,
        plan.getCreatedAt(),
        plan.getUpdatedAt()
    );
  }
}
