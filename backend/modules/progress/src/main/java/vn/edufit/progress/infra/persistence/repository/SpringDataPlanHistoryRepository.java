package vn.edufit.progress.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.progress.infra.persistence.entity.PlanHistoryJpaEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPlanHistoryRepository extends JpaRepository<PlanHistoryJpaEntity, UUID> {

  List<PlanHistoryJpaEntity> findByPlanIdOrderByCreatedAtDesc(UUID planId);
}
