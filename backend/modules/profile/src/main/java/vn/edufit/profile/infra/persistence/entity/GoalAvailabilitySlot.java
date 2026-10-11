package vn.edufit.profile.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "goal_availability_slot")
public class GoalAvailabilitySlot {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "slot_id", updatable = false, nullable = false)
  private UUID slotId;

  @Column(name = "goal_id", nullable = false)
  private UUID goalId;

  @Column(name = "day_of_week", nullable = false)
  private Short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  protected GoalAvailabilitySlot() {
    // JPA required
  }

  public UUID getGoalId() {
    return goalId;
  }

  public Short getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }
}
