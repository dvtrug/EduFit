package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.GoalAvailabilitySlot;

@Repository
public interface GoalAvailabilitySlotRepository extends JpaRepository<GoalAvailabilitySlot, UUID> {

  List<GoalAvailabilitySlot> findByGoalIdOrderByDayOfWeekAscStartTimeAsc(UUID goalId);
}
