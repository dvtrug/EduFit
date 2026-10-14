package vn.edufit.progress.domain.repository;

import vn.edufit.progress.domain.model.PlanHistory;

import java.util.List;
import java.util.UUID;

/**
 * Domain Repository Interface cho PlanHistory.
 */
public interface PlanHistoryDomainRepository {

  PlanHistory save(PlanHistory history);

  List<PlanHistory> findByPlanId(UUID planId);
}
