package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.TutorSubject;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, UUID> {

  List<TutorSubject> findByTutorId(UUID tutorId);

  List<TutorSubject> findByTutorIdIn(Collection<UUID> tutorIds);

  @Query("""
      select ts.tutorId as tutorId, ts.subjectId as subjectId, s.name as subjectName,
             ts.educationLevelId as educationLevelId, el.name as educationLevelName
      from TutorSubject ts
      join Subject s on s.subjectId = ts.subjectId
      join EducationLevel el on el.levelId = ts.educationLevelId
      where ts.tutorId in :tutorIds
      order by ts.tutorId, ts.subjectId, el.sortOrder, ts.educationLevelId
      """)
  List<DiscoverySubjectRow> findDiscoverySubjectsByTutorIdIn(@Param("tutorIds") Collection<UUID> tutorIds);

  interface DiscoverySubjectRow {
    UUID getTutorId();
    Integer getSubjectId();
    String getSubjectName();
    Integer getEducationLevelId();
    String getEducationLevelName();
  }

  boolean existsByTutorIdAndSubjectIdAndEducationLevelId(UUID tutorId, Integer subjectId, Integer educationLevelId);

  Optional<TutorSubject> findByTutorIdAndSubjectIdAndEducationLevelId(UUID tutorId, Integer subjectId, Integer educationLevelId);

  void deleteByTutorId(UUID tutorId);
}
