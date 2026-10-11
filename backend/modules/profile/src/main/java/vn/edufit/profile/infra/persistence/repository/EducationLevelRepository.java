package vn.edufit.profile.infra.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edufit.profile.infra.persistence.entity.EducationLevel;

@Repository
public interface EducationLevelRepository extends JpaRepository<EducationLevel, Integer> {

  List<EducationLevel> findByIsActiveTrueOrderBySortOrderAsc();

  List<EducationLevel> findAllByOrderBySortOrderAscLevelIdAsc();

  Optional<EducationLevel> findByName(String name);

  boolean existsByName(String name);
}
