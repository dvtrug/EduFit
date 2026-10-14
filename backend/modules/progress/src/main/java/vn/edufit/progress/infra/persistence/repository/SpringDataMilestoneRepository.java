package vn.edufit.progress.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edufit.progress.infra.persistence.entity.MilestoneJpaEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataMilestoneRepository extends JpaRepository<MilestoneJpaEntity, UUID> {

  List<MilestoneJpaEntity> findByPlan_PlanIdOrderBySortOrderAsc(UUID planId);

  @Query("SELECT m.plan.planId FROM MilestoneJpaEntity m WHERE m.milestoneId = :milestoneId")
  Optional<UUID> findPlanIdByMilestoneId(@Param("milestoneId") UUID milestoneId);
}

