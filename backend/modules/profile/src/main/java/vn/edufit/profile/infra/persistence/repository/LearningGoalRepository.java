package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.GoalStatus;
import vn.edufit.profile.infra.persistence.entity.LearningGoal;

@Repository
public interface LearningGoalRepository extends JpaRepository<LearningGoal, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<LearningGoal> findByGoalIdAndStudentId(UUID goalId, UUID studentId);

  List<LearningGoal> findByStudentIdOrderByCreatedAtAscGoalIdAsc(UUID studentId);

  List<LearningGoal> findByStudentIdAndStatus(UUID studentId, GoalStatus status);

  List<LearningGoal> findByStudentId(UUID studentId);
}
