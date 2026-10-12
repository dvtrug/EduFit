package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.GoalAvailabilitySlot;

@Repository
public interface GoalAvailabilitySlotRepository extends JpaRepository<GoalAvailabilitySlot, UUID> {

  @Modifying
  @Query("delete from GoalAvailabilitySlot slot where slot.goalId = :goalId")
  void deleteForGoal(UUID goalId);

  List<GoalAvailabilitySlot> findByGoalIdInOrderByDayOfWeekAscStartTimeAsc(List<UUID> goalIds);

  List<GoalAvailabilitySlot> findByGoalIdOrderByDayOfWeekAscStartTimeAsc(UUID goalId);
}
