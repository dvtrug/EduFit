package vn.edufit.progress.domain.repository;

import vn.edufit.progress.domain.model.LearningPlan;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain Repository Interface cho LearningPlan.
 */
public interface LearningPlanDomainRepository {

  Optional<LearningPlan> findByClassId(UUID classId);

  Optional<LearningPlan> findById(UUID planId);

  Optional<LearningPlan> findByMilestoneId(UUID milestoneId);

  boolean existsByClassId(UUID classId);

  LearningPlan save(LearningPlan plan);
}

