package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.TutorAvailabilitySlot;

@Repository
public interface TutorAvailabilitySlotRepository extends JpaRepository<TutorAvailabilitySlot, UUID> {

  List<TutorAvailabilitySlot> findByTutorIdOrderByDayOfWeekAscStartTimeAsc(UUID tutorId);

  List<TutorAvailabilitySlot> findByTutorIdInOrderByDayOfWeekAscStartTimeAsc(Collection<UUID> tutorIds);

  void deleteByTutorId(UUID tutorId);
}
