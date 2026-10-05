package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.TutorSubject;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, UUID> {

  List<TutorSubject> findByTutorId(UUID tutorId);

  boolean existsByTutorIdAndSubjectIdAndEducationLevelId(UUID tutorId, Integer subjectId, Integer educationLevelId);

  Optional<TutorSubject> findByTutorIdAndSubjectIdAndEducationLevelId(UUID tutorId, Integer subjectId, Integer educationLevelId);

  void deleteByTutorId(UUID tutorId);
}
